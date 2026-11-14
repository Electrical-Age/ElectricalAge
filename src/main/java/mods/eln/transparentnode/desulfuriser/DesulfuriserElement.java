package mods.eln.transparentnode.desulfuriser;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.fluid.DeviceInlet;
import mods.eln.fluid.DeviceOutlet;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.oil.Oil;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** The Desulfuriser (DesulfuriserDescriptor): a cut in at the back, treated, out at the front; sulfur left behind. */
public class DesulfuriserElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final DesulfuriserDescriptor descriptor;
    public final ElementMachine machine;
    /** Its IN vessel, filled by the line on the blue flange, and its OUT basin with the outlet pump. */
    public final DeviceInlet inlet;
    public final DeviceOutlet outlet;
    /** Treated toward the next Sulfur Dust (mB), dust waiting, treated in all (mB), and mB paid for and not yet moved. */
    double treatedToward, treatedAll, carry;
    int sulfur;
    double sinceRun = 1e9, sincePush;
    boolean running;

    public DesulfuriserElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (DesulfuriserDescriptor) descriptor;
        Fluid[] cuts = {Oil.Cut.GASOLINE.source.get(), Oil.Cut.DIESEL.source.get(), Oil.Cut.HEAVYOIL.source.get(), Oil.CRUDE.get()};
        inlet = new DeviceInlet("cutIn", DesulfuriserDescriptor.TANK_MB, cuts);
        outlet = new DeviceOutlet("cutOut", DesulfuriserDescriptor.TANK_MB / 0.95, DesulfuriserDescriptor.PORT_MB_PER_S, DesulfuriserDescriptor.OUT_HEAD_PA, true, false)
            .takes(cuts).keys("cutOutVessel", "cutOutTank");
        machine = Machines.forElement(this, this.descriptor.spec());
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
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
        // (the power is drawn here, a tick at a time; the cut moves before the next fluid step, in treat())
        if (inlet.feedMb() > 0 && outlet.room(1) && sulfur < DesulfuriserDescriptor.MAX_SULFUR) {
            double rated = machine.ratedJoulesPerTick();
            double got = rated > 0 ? machine.takeJoules(rated) : 0;
            double share = rated > 0 ? got / rated : 0;
            carry += DesulfuriserDescriptor.TREAT_MB_PER_S * share * dt;
            if (share > 0.05) sinceRun = 0;
        } else carry = Math.min(carry, 1);
        running = sinceRun < 2;
        if (running != was) needPublish();
        if (node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel level)
            outlet.export(level, node.coordonate.pos(), front.toEnumFacing(), (int) Math.ceil(DesulfuriserDescriptor.PORT_MB_PER_S * dt));
        sincePush += dt;
        if (sincePush >= 1 && sulfur > 0) {
            sincePush = 0;
            var level = node.coordonate.world();
            var h = level == null ? null : level.getCapability(Capabilities.Item.BLOCK, node.coordonate.pos().below(), net.minecraft.core.Direction.UP);
            if (h != null)
                try (Transaction tx = Transaction.openRoot()) {
                    int gotItems = ResourceHandlerUtil.insertStacking(h, ItemResource.of(new ItemStack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get())), sulfur, tx);
                    tx.commit();
                    sulfur -= gotItems;
                }
        }
    }

    /** Move what the power has paid for from the IN vessel to the OUT basin: the whole mix of the feed layer, at its
     *  temperature (a diesel with some gasoline in it comes out as that). */
    void treat() {
        double move = Math.min(carry, inlet.feedMb());
        if (move <= 0 || sulfur >= DesulfuriserDescriptor.MAX_SULFUR || !outlet.room(move)) return;
        var got = inlet.takeMix(move);
        if (got == null) return;
        carry -= got.mB();
        outlet.madeKg(got.kg(), got.kelvin());
        treatedToward += got.mB();
        treatedAll += got.mB();
        while (treatedToward >= DesulfuriserDescriptor.MB_PER_SULFUR) {
            treatedToward -= DesulfuriserDescriptor.MB_PER_SULFUR;
            sulfur++;
        }
    }

    /** (tests) pour a cut into its IN vessel. */
    public int fillIn(FluidStack f) {
        inlet.put(f.getFluid(), f.getAmount(), mods.eln.sim.Ambient.kelvin(node.coordonate));
        return f.getAmount();
    }

    /** (tests) */
    public double treated() {
        return treatedAll;
    }

    public int inMb() {
        return (int) Math.round(inlet.feedMb());
    }

    public int outMb() {
        return (int) Math.round(outlet.liquidMb());
    }

    public int sulfur() {
        return sulfur;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (onOff.activated(this, player, descriptor.toggle(), vx, vy, vz)) return true;
        if (sulfur <= 0 || node.coordonate.world().isClientSide()) return sulfur > 0;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get(), sulfur);
        sulfur = 0;
        if (!player.getInventory().add(s)) {
            var p = player.position();
            net.minecraft.world.Containers.dropItemStack(player.level(), p.x, p.y, p.z, s);
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
        if (inlet.stub == null || outlet.outlet == null) return java.util.List.of();
        long pos = node.coordonate.pos().asLong();
        Direction back = front.back();
        return java.util.List.of(
            inlet.port(pos, back.toEnumFacing().get3DDataValue(), PortBridge.endSpot(front, back, DesulfuriserDescriptor.FLANGE_Y, DesulfuriserDescriptor.FLANGE_Z)),
            outlet.port(pos, front.toEnumFacing().get3DDataValue(), PortBridge.endSpot(front, front, DesulfuriserDescriptor.FLANGE_Y, DesulfuriserDescriptor.FLANGE_Z)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        var all = new java.util.ArrayList<>(inlet.links());
        all.addAll(outlet.links());
        return all;
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        var all = new java.util.ArrayList<>(inlet.vents());
        all.addAll(outlet.vents());
        return all;
    }

    /** The cut moves here, between the views' sync and show: a change in the tick would leave the views stale. */
    @Override
    public void beforeFluidStep(double dt) {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inlet.sync(dt, t);
        if (onOff.on) treat();
        inlet.settle(dt);
        outlet.before(t);
    }

    @Override
    public void afterFluidStep(double dt) {
        inlet.after();
        outlet.after();
    }

    @Override
    public void initialize() {
        double t = mods.eln.sim.Ambient.kelvin(node.coordonate);
        inlet.make(node.coordonate.y, node.coordonate.y + DesulfuriserDescriptor.FLANGE_Y / 16.0, t);
        outlet.make(node.coordonate.y, node.coordonate.y + DesulfuriserDescriptor.FLANGE_Y / 16.0, t);
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

    /** Buckets and other mods' pipes see the treated cut. */
    @Override
    public Object getFluidHandler() {
        return outlet.view;
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
        outlet.write(nbt);
        nbt.putDouble("treatedToward", treatedToward);
        nbt.putDouble("treatedAll", treatedAll);
        nbt.putInt("sulfur", sulfur);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        onOff.read(nbt);
        inlet.read(nbt, "cutInTank", DesulfuriserDescriptor.TANK_MB);
        outlet.read(nbt, DesulfuriserDescriptor.TANK_MB);
        treatedToward = nbt.getDoubleOr("treatedToward", 0);
        treatedAll = nbt.getDoubleOr("treatedAll", 0);
        sulfur = nbt.getIntOr("sulfur", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        if (!onOff.on) info.put(I18N.tr("State"), I18N.tr("Switched off"));
        String wrong = inlet.blockedBy();
        if (onOff.on && wrong != null) info.put(I18N.tr("State"), I18N.tr("blocked: it holds %1$, which it can't treat", wrong));
        info.put(I18N.tr("In (blue)"), inMb() + " / " + DesulfuriserDescriptor.TANK_MB + " mB");
        info.put(I18N.tr("Out (red)"), outMb() + " / " + DesulfuriserDescriptor.TANK_MB + " mB");
        info.put(I18N.tr("Sulfur Dust"), String.valueOf(sulfur));
        return info;
    }
}
