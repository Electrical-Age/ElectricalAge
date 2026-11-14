package mods.eln.transparentnode.polymeriser;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.DeviceInlet;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mods.eln.transparentnode.polymeriser.PolymeriserDescriptor.*;

/** The Polymeriser (PolymeriserDescriptor): butadiene in at the back, Soul Dust as initiator, Raw Rubber out below. */
public class PolymeriserElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    public static final int SOUL_SLOT = 0, RUBBER_SLOT = 1;
    final PolymeriserDescriptor descriptor;
    public final ElementMachine machine;
    final PolymeriserInventory inventory = new PolymeriserInventory(this);
    /** Its feed vessel, sealed (butadiene boils in it at 2.4 bar), filled by the line on the blue flange. */
    public final DeviceInlet inlet = new DeviceInlet("butadieneIn", TANK_MB, mods.eln.oil.Butadiene.SOURCE.get()).sealed();
    /** Butadiene drawn out of the vessel toward the next Raw Rubber (mB as liquid, up to MB_PER_RUBBER). */
    double charge;
    /** Energy toward the next Raw Rubber (J), Raw Rubber the current Soul Dust still initiates, made in all. */
    double progress;
    int initiator, made;
    double sinceRun = 1e9, sincePush;
    boolean running;
    String status = "idle";

    public PolymeriserElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (PolymeriserDescriptor) descriptor;
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    boolean roomForRubber() {
        ItemStack out = inventory.getItem(RUBBER_SLOT);
        return out.isEmpty() || out.is(ElnRegistry.RAW_RUBBER.get()) && out.getCount() < out.getMaxStackSize();
    }

    /** Its on/off lever (Nero 2026-11-08): off, it does nothing and draws nothing. */
    public final mods.eln.node.transparent.MachineSwitch onOff = new mods.eln.node.transparent.MachineSwitch();

    void tick(double dt) {
        if (!onOff.on) {
            if (running) {
                running = false;
                needPublish();
            }
            return;
        }
        boolean was = running;
        sinceRun += dt;
        boolean soul = initiator > 0 || inventory.getItem(SOUL_SLOT).is(ElnRegistry.SOUL_DUST.get());
        if (charge < MB_PER_RUBBER - 1e-6) status = "no butadiene";
        else if (!soul) status = "no Soul Dust";
        else if (!roomForRubber()) status = "its Raw Rubber is full";
        else {
            status = "polymerising";
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            progress += got;
            if (rated > 0 && got / rated > 0.05) sinceRun = 0;
            else status = "no power";
            if (progress >= JOULES_PER_RUBBER) {
                progress -= JOULES_PER_RUBBER;
                if (initiator <= 0) {
                    inventory.removeItem(SOUL_SLOT, 1);
                    initiator = RUBBER_PER_SOUL_DUST;
                }
                initiator--;
                charge = Math.max(0, charge - MB_PER_RUBBER);
                ItemStack out = inventory.getItem(RUBBER_SLOT);
                if (out.isEmpty()) inventory.setItem(RUBBER_SLOT, new ItemStack(ElnRegistry.RAW_RUBBER.get()));
                else out.grow(1);
                inventory.setChanged();
                made++;
            }
        }
        running = sinceRun < 2;
        if (running != was) needPublish();
        sincePush += dt;
        if (sincePush >= 1 && !inventory.getItem(RUBBER_SLOT).isEmpty()) {
            sincePush = 0;
            var level = node.coordonate.world();
            var h = level == null ? null : level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, node.coordonate.pos().below(), net.minecraft.core.Direction.UP);
            if (h != null) {
                ItemStack out = inventory.getItem(RUBBER_SLOT);
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    int n = net.neoforged.neoforge.transfer.ResourceHandlerUtil.insertStacking(h, net.neoforged.neoforge.transfer.item.ItemResource.of(out), out.getCount(), tx);
                    tx.commit();
                    if (n > 0) {
                        inventory.removeItem(RUBBER_SLOT, n);
                        inventory.setChanged();
                    }
                }
            }
        }
    }

    // ---- tests

    /** (tests) pour butadiene (liquid or vapour) into its feed. */
    public int fillIn(FluidStack f) {
        inlet.put(f.getFluid(), f.getAmount(), mods.eln.sim.Ambient.kelvin(node.coordonate));
        return f.getAmount();
    }

    static double kgPerMb() {
        return mods.eln.fluid.FluidSpecies.kgPerMb(mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:butadiene"), false);
    }

    /** The butadiene it can use: what it has drawn toward the next Raw Rubber, and the vessel's liquid and its vapour
     *  over 1 atm (mB as liquid, by mass). What the vessel's gas holds at 1 atm stays in it: it has no vacuum pump. */
    public double butadieneMb() {
        return charge + inlet.usableKg() / kgPerMb();
    }

    /** Butadiene drawn toward the next Raw Rubber (mB as liquid; tests). */
    public double charge() {
        return charge;
    }

    public int made() {
        return made;
    }

    public String status() {
        return status;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    /** Soul Dust in hand goes in; an empty hand takes the Raw Rubber. */
    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (onOff.activated(this, player, descriptor.toggle(), vx, vy, vz)) return true;
        if (node.coordonate.world().isClientSide()) return true;
        ItemStack hand = player.getMainHandItem();
        if (hand.is(ElnRegistry.SOUL_DUST.get())) {
            ItemStack in = inventory.getItem(SOUL_SLOT);
            int n = Math.min(hand.getCount(), 64 - in.getCount());
            if (n <= 0) return true;
            if (in.isEmpty()) inventory.setItem(SOUL_SLOT, hand.copyWithCount(n));
            else in.grow(n);
            hand.shrink(n);
            inventory.setChanged();
            return true;
        }
        ItemStack out = inventory.getItem(RUBBER_SLOT);
        if (out.isEmpty()) return false;
        inventory.setItem(RUBBER_SLOT, ItemStack.EMPTY);
        inventory.setChanged();
        if (!player.getInventory().add(out)) {
            var p = player.position();
            net.minecraft.world.Containers.dropItemStack(player.level(), p.x, p.y, p.z, out);
        }
        return true;
    }

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (inlet.stub == null) return java.util.List.of();
        Direction back = front.back();
        return java.util.List.of(inlet.port(node.coordonate.pos().asLong(), back.toEnumFacing().get3DDataValue(),
            PortBridge.endSpot(front, back, FLANGE_Y, FLANGE_Z)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        return inlet.links();
    }

    /** Switched on, it draws the next Raw Rubber's butadiene out of the vessel: the liquid first, then the gas, and
     *  with the gas whatever else is in it (the air of a new line), which is how that leaves. */
    @Override
    public void beforeFluidStep(double dt) {
        inlet.sync(dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
        if (onOff.on && charge < MB_PER_RUBBER) charge += inlet.drawKg((MB_PER_RUBBER - charge) * kgPerMb()) / kgPerMb();
        inlet.settle(dt);
    }

    @Override
    public void afterFluidStep(double dt) {
        inlet.after();
    }

    @Override
    public void initialize() {
        inlet.make(node.coordonate.y, node.coordonate.y + FLANGE_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    // ---- electrical

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.powerLoad();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return machine.connectionMask(side);
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(machine.volts() > descriptor.nominalU * 0.5);
            stream.writeBoolean(running);
            stream.writeBoolean(onOff.on);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        onOff.write(nbt);
        inlet.write(nbt);
        nbt.putDouble("butadieneCharge", charge);
        nbt.putDouble("progress", progress);
        nbt.putInt("initiator", initiator);
        nbt.putInt("made", made);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        onOff.read(nbt);
        if (!inlet.read(nbt)) {
            // (an old save: its feed tank's two counts, liquid and vapour mB)
            inlet.legacy(new FluidStack(mods.eln.oil.Butadiene.SOURCE.get(), nbt.getIntOr("butadieneLiquid", 0)));
            inlet.legacy(new FluidStack(mods.eln.oil.Butadiene.GAS.get(), nbt.getIntOr("butadieneGas", 0)));
        }
        charge = nbt.getDoubleOr("butadieneCharge", 0);
        progress = nbt.getDoubleOr("progress", 0);
        initiator = nbt.getIntOr("initiator", 0);
        made = nbt.getIntOr("made", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        if (!onOff.on) info.put(I18N.tr("State"), I18N.tr("Switched off"));
        info.put(I18N.tr("Status"), I18N.tr(status));
        String wrong = inlet.blockedBy();
        if (wrong != null) info.put(I18N.tr("Blocked"), I18N.tr("it holds %1$, which it can't use", wrong));
        info.put(I18N.tr("Butadiene (blue)"), String.format("%.0f / %d mB", butadieneMb(), TANK_MB));
        info.put(I18N.tr("Soul Dust"), inventory.getItem(SOUL_SLOT).getCount() + (initiator > 0 ? " (+" + initiator + " rubber left)" : ""));
        info.put(I18N.tr("Raw Rubber"), String.valueOf(inventory.getItem(RUBBER_SLOT).getCount()));
        return info;
    }
}
