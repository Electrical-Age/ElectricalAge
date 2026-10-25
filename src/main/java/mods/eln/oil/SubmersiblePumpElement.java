package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeBlock;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The submersible pump (SubmersiblePumpDescriptor): a motor (a resistor at its nominal power, scaled by the speed the
 * front signal asks for) driving a pump that is part of the fluid solve (Baughn: a matrix stamp, not water injected per
 * tick): a one-way edge from its sump (the water it stands in, at 1 atm) to its outlet behind the flange on top, with
 * a head of SHUTOFF_PA above the sump and a conductance such that it delivers its nominal flow against 1 atm. So the
 * flow falls linearly to nothing at the shutoff pressure, exactly, whatever the pipe; the speed and the motor's power
 * scale the conductance. The outlet stub merges into the pipe on it. Out of water (not waterlogged) it doesn't run.
 */
public class SubmersiblePumpElement extends TransparentNodeElement implements FluidDevice {
    final SubmersiblePumpDescriptor desc;
    final NbtElectricalLoad load = new NbtElectricalLoad("load");
    public final Resistor motor = new Resistor(load, null);
    final NbtElectricalGateInput control = new NbtElectricalGateInput("control");
    final VoltageStateWatchDog watchdog = new VoltageStateWatchDog();
    public static final double OUTLET_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    public FluidNode outlet;
    /** The water it stands in, and the pump between it and the outlet. */
    FluidNode sump;
    private FluidDevice.Link pumpEdge;
    private double[] loaded;
    private List<FluidPort> ports = List.of();
    /** In water (waterlogged), and the speed asked for (0..1; no cable on the front: 1). */
    public boolean submerged;
    public double speed;
    /** mB/s pumped, for WAILA. */
    public double rate;
    double meter, meterTime;

    public SubmersiblePumpElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (SubmersiblePumpDescriptor) d;
        electricalLoadList.add(load);
        electricalLoadList.add(control);
        electricalComponentList.add(motor);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(watchdog.set(load).setUNominal(desc.nominalU).set(new Failure(this).machine()));
        slowProcessList.add((IProcess) t -> {
            var state = node.coordonate.world().getBlockState(node.coordonate.pos());
            submerged = state.hasProperty(TransparentNodeBlock.WATERLOGGED) && state.getValue(TransparentNodeBlock.WATERLOGGED);
            speed = node.lrduCubeMask.get(front, LRDU.Down) ? control.getNormalized() : 1;
            motor.setR(submerged && speed > 0.01 ? desc.resistorR / speed : MnaConst.highImpedance);
        });
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    @Override
    public List<FluidDevice.Link> fluidLinks() {
        return pumpEdge == null ? List.of() : List.of(pumpEdge);
    }

    /** The pump this step: its conductance scaled by the motor's power (the speed asked for, and the voltage it gets). */
    @Override
    public void beforeFluidStep(double dt) {
        if (pumpEdge == null) return;
        pumpEdge.open = submerged ? Math.min(1.2, motor.getP() / desc.nominalP) : 0;
        pumpEdge.boost = SubmersiblePumpDescriptor.SHUTOFF_PA - Water.P_ATM;
    }

    @Override
    public void afterFluidStep(double dt) {
        if (pumpEdge != null && pumpEdge.edge != null) meter += Math.max(0, pumpEdge.edge.fLiquid) / EaUnits.KG_PER_MB_WATER * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            rate = meter / meterTime;
            meter = 0;
            meterTime = 0;
        }
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outlet != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front) return control;
        if (side == front.left() || side == front.right()) return load;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front) return NodeBase.maskElectricalInputGate;
        if (side == front.left() || side == front.right()) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        if (side == front) return Utils.plotSignal(control.getU(), control.getCurrent());
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        var c = node.coordonate;
        outlet = new FluidNode(OUTLET_VOLUME, c.y + 0.5, 0.5, false);
        if (loaded != null) {
            outlet.mWater = loaded[0];
            outlet.mAir = loaded[1];
            outlet.energy = loaded[2];
            outlet.update();
        } else outlet.fill(0, Water.P_ATM, Water.T_AMBIENT);
        outlet.lumpable = true;   // (it merges into the pipe on its flange)
        outlet.resistance = 1 / PORT_K;
        sump = FluidNode.sump(c.y);
        // g = rho k / mu = nominal flow / shutoff head (kg/s/Pa): the nominal flow against 1 atm, none at the shutoff
        double dp = SubmersiblePumpDescriptor.SHUTOFF_PA - Water.P_ATM;
        double kgPerS = desc.nominalMbPerS * EaUnits.KG_PER_MB_WATER;
        pumpEdge = new FluidDevice.Link(sump, 0, outlet, 0, kgPerS / dp * Water.MU_L / Water.RHO_L).kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
        pumpEdge.boost = dp;
        pumpEdge.open = 0;
        ports = List.of(new FluidPort(outlet, c.pos().asLong(), 1, 0.0, PORT_K));   // the flange on top
        connect();
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (outlet != null) {
            nbt.putDouble("outletW", outlet.mWater);
            nbt.putDouble("outletA", outlet.mAir);
            nbt.putDouble("outletU", outlet.energy);
            nbt.putDouble("outletV", OUTLET_VOLUME);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        // (saved for an outlet of another size, e.g. the first 0.5 L one: its contents would not fit; it starts empty)
        if (nbt.contains("outletW") && nbt.getDoubleOr("outletV", 0) == OUTLET_VOLUME)
            loaded = new double[]{nbt.getDoubleOr("outletW", 0), nbt.getDoubleOr("outletA", 0), nbt.getDoubleOr("outletU", 0)};
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("In water"), submerged ? I18N.tr("yes") : I18N.tr("no: place it in water"));
        info.put(I18N.tr("Speed"), String.format("%.0f%%", speed * 100));
        info.put(I18N.tr("Flow"), String.format("%.0f mB/s", rate));
        if (outlet != null) info.put(I18N.tr("Outlet pressure"), String.format("%.2f bar", outlet.P() / 1e5));
        info.put(I18N.tr("Power"), Utils.plotPower("", motor.getP()));
        return info;
    }
}
