package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.fluid.FluidSaves;
import mods.eln.misc.Coordonate;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.IBurnable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Tank Minecart (Baughn 2026-11-25, todo #611): a vanilla minecart carrying an Iron or Alloy Tank, of that tier's
 * volume and burst rating. Its contents are one fluid node, held by the cart alone for its whole life: a Tank Loading
 * Station it stands on only couples that node to its flange while it is docked (TankStationEntity), so nothing is copied
 * on docking or leaving and nothing can be made or lost there. Saved as the tank item saves them (TankContents, key
 * "tank"). It trades heat with the ambient where it is (the tank's loss coefficient), and over its burst pressure it
 * bursts as a tank does (Failure.pressureVessel): the cart is destroyed and drops only the minecart. Broken, it drops a
 * minecart and the tank, with the contents by the tank's rule (TankDescriptor.dropStack).
 */
public class TankCart extends AbstractMinecart implements IBurnable {
    /** The tank's bottom above the rail block's floor and its inside height (m): where it sits on the cart. */
    public static final double TANK_Z = 6 / 16.0, TANK_H = 9 / 16.0;
    /** The cart counts as standing (and docks at a station) under this speed (blocks a tick). */
    static final double DOCK_SPEED = 0.05;
    /** Released by its station, a cart standing on it is sent off the way it came in at this speed (blocks a tick). */
    static final double RELEASE_SPEED = 0.1;

    public final boolean alloy;
    /** Its contents (server only; made at the first use from `loaded`, or fresh). */
    private FluidNode node;
    /** Contents read from a save or the item it was placed from, to restore when the node is made. */
    private TankContents loaded;
    /** The station it is docked at, or null. */
    TankStationEntity dockedAt;
    /** The ambient where it is (K) and the block it was read at: read again only when the cart moves to another block. */
    private double ambientK = Double.NaN;
    private BlockPos ambientAt;
    /** Which way it last travelled (horizontal, unit), to leave a station the way it came in. */
    private Vec3 travel = Vec3.ZERO;
    private int heatTicks;
    /** A burst queued (Failure, run at the end of the next simulator tick), and done. */
    private boolean bursting;
    private FailureCause burnt;

    public TankCart(EntityType<? extends TankCart> type, Level level) {
        super(type, level);
        alloy = type == ElnRegistry.ALLOY_TANK_MINECART_ENTITY.get();
    }

    /** Its tank's tier: the Iron or Alloy Tank. */
    public TankDescriptor desc() {
        return (TankDescriptor) Eln.transparentNodeItem.getDescriptor(alloy ? TankDescriptor.ALLOY_ID : TankDescriptor.IRON_ID);
    }

    /** Its contents (server side), made on first use: what it was saved or placed with, else air at 1 atm and the ambient. */
    public FluidNode node() {
        if (node == null) {
            node = newNode(Math.floor(getY()) + TANK_Z);
            if (loaded != null) {
                FluidSaves.restore(node, loaded.saved(), ambientK(), this);
                node.update();
                loaded = null;
            } else node.fill(0, Water.P_ATM, ambientK());
        }
        return node;
    }

    private FluidNode newNode(double z) {
        FluidNode n = new FluidNode(desc().volume, z, TANK_H, false);
        n.lossWPerK = 0;   // (its heat goes by exchangeHeat, to the ambient where it is, docked or not)
        return n;
    }

    /**
     * Its node with its bottom at z (m): a node's height is fixed, so a station at another height gets a new node with
     * the same contents, through the tank item's arrays (TankContents, FluidSaves), exactly. Only while not in a network.
     */
    void rebase(double z) {
        FluidNode n = node();
        if (n.z == z) return;
        FluidNode m = newNode(z);
        FluidSaves.restore(m, TankContents.of(n).saved(), ambientK(), this);
        m.update();
        node = m;
    }

    /** The ambient where it stands (K), read again when it has moved to another block. */
    public double ambientK() {
        BlockPos p = blockPosition();
        if (Double.isNaN(ambientK) || !p.equals(ambientAt)) {
            ambientAt = p;
            ambientK = mods.eln.sim.Ambient.kelvin(level(), p);
        }
        return ambientK;
    }

    /**
     * dt seconds of heat to the ambient where it stands, through the tank's loss coefficient (TankElement.LOSS_W_PER_K),
     * never past it. A tank in the fluid world loses its heat in the core towards a fixed 20 C; the cart goes to the
     * local ambient instead, which it reads where it is.
     */
    void exchangeHeat(double dt) {
        FluidNode n = node();
        n.update();
        double ta = ambientK(), t = n.T();
        if (!(Math.abs(t - ta) > 1e-9)) return;
        double target = n.energyAt(ta);
        double dE = -TankElement.LOSS_W_PER_K * (t - ta) * dt;
        if (Math.abs(dE) > Math.abs(target - n.energy)) dE = target - n.energy;
        n.energy += dE;
        n.update();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isRemoved()) return;
        Vec3 v = getDeltaMovement();
        double speed = v.horizontalDistance();
        if (speed > 0.01) travel = new Vec3(v.x / speed, 0, v.z / speed);
        FluidNode n = node();
        if (bursting) {
            // (the burst ran: an explosion that left the cart standing, or none with explosions off)
            burn(FailureCause.OVERPRESSURE, Failure.Kind.PRESSURE_VESSEL);
            return;
        }
        if (n.P() > desc().burstPa) {
            burst();
            return;
        }
        dock(speed);
        if (dockedAt != null) {
            dockedAt.whileDocked();
            if (!dockedAt.released()) setDeltaMovement(Vec3.ZERO);   // (held)
            else if (speed < 0.01 && travel.lengthSqr() > 0) setDeltaMovement(travel.scale(RELEASE_SPEED));
        } else if (++heatTicks >= 20) {
            heatTicks = 0;
            exchangeHeat(1.0);
        }
    }

    /** Docks at the station under it while standing there, and leaves it otherwise. */
    private void dock(double speed) {
        BlockPos rail = getCurrentBlockPosOrRailBelow();
        TankStationEntity s = speed < DOCK_SPEED && level().getBlockEntity(rail) instanceof TankStationEntity t ? t : null;
        if (dockedAt != null && dockedAt != s) undock();
        if (s != null && dockedAt == null && s.dock(this)) dockedAt = s;
    }

    /** Leaves its station: the station drops its port; the contents stay in the cart's node, as they always were. */
    void undock() {
        TankStationEntity s = dockedAt;
        dockedAt = null;
        if (s != null) s.undock(this);
    }

    /** Its station went (broken, or its chunk unloaded). */
    void stationGone(TankStationEntity s) {
        if (dockedAt == s) dockedAt = null;
    }

    /** Removed for any reason (broken, killed, unloaded with its chunk, changing dimension): it leaves its station first. */
    @Override
    public void onRemoval(RemovalReason reason) {
        undock();
        super.onRemoval(reason);
    }

    /** Over its burst pressure: the tank's failure (a pressure vessel's burst, scaled by what would flash), once. */
    private void burst() {
        bursting = true;
        undock();
        FluidNode n = node();
        new Failure(this, null, new Coordonate(blockPosition(), level()), "Tank Minecart at " + blockPosition().toShortString())
            .pressureVessel(() -> n.energy - n.mWater * Water.uLiquid(Water.TB))
            .fail(FailureCause.OVERPRESSURE, n.P());
    }

    @Override
    public FailureCause getBurnt() {
        return burnt;
    }

    @Override
    public Failure.Kind getBurntKind() {
        return burnt == null ? null : Failure.Kind.PRESSURE_VESSEL;
    }

    /** Burst: the tank vented, the cart destroyed; only the minecart drops. */
    @Override
    public void burn(FailureCause cause, Failure.Kind kind) {
        if (burnt != null || isRemoved()) return;
        burnt = cause;
        bursting = true;
        undock();
        if (level() instanceof ServerLevel sl) destroy(sl, sl.damageSources().generic());
    }

    @Override
    public void repair() {
    }

    @Override
    public Coordonate getBurnableCoordonate() {
        return new Coordonate(blockPosition(), level());
    }

    /** The tank as it would drop now (TankDescriptor.dropStack: with its contents, or plain). */
    @Override
    public ItemStack getDropItemStack() {
        return desc().dropStack(node());
    }

    /** Broken: a minecart and the tank with its contents (as a tank breaks); burst: the minecart only. */
    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        ItemStack tank = bursting ? ItemStack.EMPTY : getDropItemStack();
        undock();
        kill(level);
        if (!level.getGameRules().get(GameRules.ENTITY_DROPS)) return;
        ItemStack cart = new ItemStack(Items.MINECART);
        cart.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        spawnAtLocation(level, cart);
        if (!tank.isEmpty()) spawnAtLocation(level, tank);
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    /** A plain cart: a copy of its contents is not a pick. */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(alloy ? ElnRegistry.ALLOY_TANK_MINECART.get() : ElnRegistry.IRON_TANK_MINECART.get());
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ElnRegistry.TANK_CART_LOAD.get().defaultBlockState().setValue(TankCartLoadBlock.ALLOY, alloy);
    }

    /** Its saddles on the cart's floor, as a chest minecart's chest. */
    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    /** Placed from an item that carries contents (TankContents: not made in survival, but a command can give one). */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        applyImplicitComponentIfPresent(components, ElnRegistry.TANK_CONTENTS.get());
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == ElnRegistry.TANK_CONTENTS.get()) {
            undock();   // (its station holds the old node: it docks again with the new one next tick)
            loaded = (TankContents) value;
            node = null;
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        TankContents c = node != null ? TankContents.of(node) : loaded;
        if (c != null) out.store("tank", TankContents.CODEC, c);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.read("tank", TankContents.CODEC).ifPresent(c -> {
            undock();   // (a command's data merge on a docked cart: its station holds the old node)
            loaded = c;
            node = null;
        });
    }

    /** Its Jade lines: a tank's (TankDescriptor.putContents), and whether it is docked. */
    public Map<String, String> waila() {
        Map<String, String> info = new LinkedHashMap<>();
        FluidNode n = node();
        n.update();
        TankDescriptor.putContents(info, n);
        desc().putWarning(info, n);
        if (dockedAt != null) info.put(mods.eln.i18n.I18N.tr("Station"), dockedAt.released() ? mods.eln.i18n.I18N.tr("docked, released")
            : mods.eln.i18n.I18N.tr("docked, held"));
        return info;
    }
}
