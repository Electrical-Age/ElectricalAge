package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Tank Loading Station's state (TankStationBlock), nothing of it saved. While a Tank Minecart stands on it (docked:
 * TankCart.dock) its flanges are ports on the cart's own node (a dip pipe to the tank's bottom), so pumps and pressure
 * fill or drain the cart through the pipes on them, and it is in the fluid world; with no cart it is not, and its
 * flanges pass nothing. The cart keeps its node all along, so when it leaves (rolls off, is broken, unloads with its
 * chunk, or the station goes) it simply takes its contents with it. Its flanges are on both sides across the track, 4 px
 * up, centred (a tank's low side port: TankElement.sideSpot), so a tank or a pipe beside it meets them.
 */
public class TankStationEntity extends BlockEntity implements FluidDevice, mods.eln.fluid.FluidPlacement.PortBlockEntity {
    /** A cart's liquid share from which the comparator reads full (15): an air cushion stays over the liquid. */
    static final double FULL = 0.9;
    /** Its flanges' node with no cart (placement only: the pipe kit asks for the ports; never in the fluid world). */
    private static final FluidNode NO_CART = new FluidNode(1e-3, 0, 1, false);

    private TankCart docked;
    private List<FluidPort> ports = List.of();
    private RailShape portsFor;
    private int shownSignal;

    public TankStationEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.TANK_STATION_ENTITY.get(), pos, state);
    }

    /** The cart docked here, or null. */
    public TankCart docked() {
        return docked != null && !docked.isRemoved() ? docked : null;
    }

    /** Powered: it lets its cart go (else it holds it). */
    public boolean released() {
        BlockState s = getBlockState();
        return s.hasProperty(PoweredRailBlock.POWERED) && s.getValue(PoweredRailBlock.POWERED);
    }

    /** Couples cart's node to its flanges (one cart at a time); false if another is docked or it is gone. */
    boolean dock(TankCart cart) {
        if (level == null || level.isClientSide() || isRemoved()) return false;
        if (docked() != null && docked != cart) return false;
        docked = cart;
        // (the cart's node at the height it stands at here: its tank's bottom above the station's floor)
        cart.rebase(worldPosition.getY() + TankCart.TANK_Z);
        remakePorts();
        FluidWorld.add(this);
        FluidWorld.changed(this);
        signalChanged();
        return true;
    }

    /** The cart left: no ports, out of the fluid world. Its contents are in its node, where they always were. */
    void undock(TankCart cart) {
        if (docked != cart) return;
        docked = null;
        ports = List.of();
        FluidWorld.remove(this);
        signalChanged();
    }

    /** Each tick of its docked cart: its flanges follow the rail's turn, and the comparator its fill. */
    void whileDocked() {
        if (getBlockState().hasProperty(PoweredRailBlock.SHAPE) && getBlockState().getValue(PoweredRailBlock.SHAPE) != portsFor) {
            remakePorts();
            FluidWorld.changed(this);
        }
        signalChanged();
    }

    /** Broken, or unloaded with its chunk: the cart is let go (its node leaves with it). */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (docked != null) docked.stationGone(this);
        docked = null;
        ports = List.of();
        FluidWorld.remove(this);
    }

    private void remakePorts() {
        TankCart c = docked();
        ports = c == null ? List.of() : portsOn(c.node());
    }

    /** Its two flanges on n: across the track, at a tank's low side port's spot, drawing at the tank's bottom. */
    private List<FluidPort> portsOn(FluidNode n) {
        BlockState s = getBlockState();
        RailShape shape = s.hasProperty(PoweredRailBlock.SHAPE) ? s.getValue(PoweredRailBlock.SHAPE) : RailShape.NORTH_SOUTH;
        portsFor = shape;
        boolean alongX = shape == RailShape.EAST_WEST || shape == RailShape.ASCENDING_EAST || shape == RailShape.ASCENDING_WEST;
        List<FluidPort> out = new ArrayList<>();
        for (int f : alongX ? new int[]{2, 3} : new int[]{4, 5})
            out.add(new FluidPort(n, worldPosition.asLong(), f, 0.0, TankElement.PORT_K, TankElement.sideSpot(f, TankElement.LOW)));
        return out;
    }

    /** Its flanges for placing pipes (FluidPlacement.portsAt): the cart's, or with none ports that are in no network. */
    @Override
    public List<FluidPort> placementPorts() {
        return docked() != null ? ports : portsOn(NO_CART);
    }

    @Override
    public Object fluidLevel() {
        return mods.eln.misc.Dimensions.id(level);
    }

    @Override
    public long fluidPos() {
        return worldPosition.asLong();
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    /** The docked cart's heat goes to the ambient where it stands (TankCart.exchangeHeat), step by step while docked. */
    @Override
    public void beforeFluidStep(double dt) {
        TankCart c = docked();
        if (c != null) c.exchangeHeat(dt);
    }

    /**
     * The comparator's reading: 0 no cart; 1 a cart with no liquid; 2..14 by its liquid share; 15 at least FULL. Gas
     * alone reads as empty (its pressure is in Jade).
     */
    public int signal() {
        TankCart c = docked();
        if (c == null) return 0;
        FluidNode n = c.node();
        double liquid = n.liquidVolume();
        if (liquid < TankContents.NO_LIQUID_M3) return 1;
        double fill = liquid / n.volume;
        if (fill >= FULL) return 15;
        return 2 + (int) Math.min(12, Math.floor(fill / FULL * 13));
    }

    private void signalChanged() {
        int s = signal();
        if (s == shownSignal || level == null || isRemoved() || !level.isLoaded(worldPosition)) return;
        shownSignal = s;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** Its Jade lines: the docked cart's, as a tank shows them, or that it has none. */
    public Map<String, String> waila() {
        TankCart c = docked();
        if (c != null) {
            Map<String, String> info = c.waila();
            info.put(mods.eln.i18n.I18N.tr("Comparator"), Integer.toString(signal()));
            return info;
        }
        Map<String, String> info = new LinkedHashMap<>();
        info.put(mods.eln.i18n.I18N.tr("Cart"), mods.eln.i18n.I18N.tr("none docked"));
        info.put(mods.eln.i18n.I18N.tr("Flanges"), mods.eln.i18n.I18N.tr("both sides, across the track"));
        return info;
    }
}
