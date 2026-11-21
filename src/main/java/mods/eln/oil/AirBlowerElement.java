package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AirBlowerDescriptor: the air around it (a fixed node at 1 atm) and an outlet node (small, lumped into the pipe on it)
 * with a gas pump between them (as the condenser's ejector, the other way). Each fluid step sets its speed from the
 * supply; the electrical load draws the work the core did on the air over the motor's efficiency, plus its idling.
 */
public class AirBlowerElement extends TransparentNodeElement implements FluidDevice {
    final AirBlowerDescriptor desc;
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor resistor = new Resistor(load, null);
    /** A speed signal on its back (0-5 V; none: full speed), as the Inline Pump's: its pressure goes with the speed's square. */
    public final mods.eln.sim.nbt.NbtElectricalGateInput speed = new mods.eln.sim.nbt.NbtElectricalGateInput("speed");
    public FluidNode intake, outlet;
    private double[] loadedOut;
    public Link blower;
    private List<FluidPort> ports = List.of();
    /** Speed now (0..1), the air moved (kg/s, averaged over a second, and this step), the power wanted (W), air in all (kg). */
    public double share, kgPerS, stepKgPerS, wantW, totalKg;
    /** Of wantW, its idling (W: not scaled by OUTPUT_PER_WATT). */
    public double idleW;
    double meterKg, meterTime;

    public AirBlowerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (AirBlowerDescriptor) d;
        electricalLoadList.add(load);
        electricalLoadList.add(speed);
        electricalComponentList.add(resistor);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        resistor.setR(1e9);
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (blower == null) return;
        double signal = speed.getConnectedComponents().isEmpty() ? 1 : speed.getNormalized();
        share = Math.max(0, Math.min(1, signal)) * Math.max(0, Math.min(1, load.getU() / desc.nominalU));
        // (the fan laws: flow with the speed, pressure with its square)
        blower.speed = share;
        blower.h0 = Math.max(1, AirBlowerDescriptor.SHUTOFF_PA * share * share);
        // (the air around it stays the air around it)
        intake.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void afterFluidStep(double dt) {
        if (blower == null) return;
        stepKgPerS = blower.edge == null ? 0 : Math.max(0, blower.edge.flow);
        totalKg += stepKgPerS * dt;
        meterKg += stepKgPerS * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            kgPerS = meterKg / meterTime;
            meterKg = meterTime = 0;
            needPublish();
        }
        double work = blower.edge == null ? 0 : Math.max(0, blower.edge.work);
        idleW = share <= 0.01 ? 0 : AirBlowerDescriptor.BLOWER_IDLE_W * share;
        wantW = share <= 0.01 ? 0 : work / (AirBlowerDescriptor.MOTOR_ETA * AirBlowerDescriptor.OUTPUT_PER_WATT) + idleW;
        double u = load.getU();
        resistor.setR(wantW > 1 && u > 1 ? u * u / wantW : 1e9);
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
    public List<Link> fluidLinks() {
        return blower == null ? List.of() : List.of(blower);
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        int out = front.right().toEnumFacing().get3DDataValue();
        // (its outlet on its right flank at the pipes' low line, as the Inline Pump's and the furnace's fuel flange)
        ports = List.of(new FluidPort(outlet, at, out, 0.5, InlinePumpElement.PORT_K, TankElement.sideSpot(out, InlinePumpElement.LINE_Y)));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (outlet != null) {
            makePorts();
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        double amb = mods.eln.sim.Ambient.kelvin(node.coordonate);
        intake = new FluidNode(1.0, node.coordonate.y, 0.25, true);
        intake.fill(0, Water.P_ATM, amb);
        outlet = new FluidNode(InlinePumpElement.NODE_VOLUME, node.coordonate.y, 0.25, false);
        outlet.lumpable = true;
        outlet.resistance = 1 / InlinePumpElement.PORT_K;
        if (loadedOut != null) {
            mods.eln.fluid.FluidSaves.restore(outlet, loadedOut);
            outlet.update();
        } else outlet.fill(0, Water.P_ATM, amb);
        blower = new Link(intake, 0.5, outlet, 0.5, InlinePumpElement.PORT_K).gasPump(AirBlowerDescriptor.FREE_M3_S, AirBlowerDescriptor.SHUTOFF_PA);
        // (the core's work, flow x rise at the inlet's density, is already ~1.45 x isentropic at 1.6 bar: no further loss in the air)
        blower.eta = 1.0;
        blower.speed = 0;
        makePorts();
        connect();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        return side == front ? load : side == front.back() ? speed : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return side == front ? NodeBase.maskElectricalPower : side == front.back() ? NodeBase.maskElectricalInputGate : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            // (as the Inline Pump's, for its render: speed, cavitating)
            stream.writeFloat((float) share);
            stream.writeBoolean(false);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (outlet != null) mods.eln.fluid.FluidSaves.put(nbt, "outlet", outlet);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedOut = nbt.contains("outletW") ? mods.eln.fluid.FluidSaves.array(nbt, "outlet") : null;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Speed"), Utils.plotPercent(share));
        if (outlet != null) info.put(I18N.tr("Outlet"), String.format("%.2f bar gauge", (FluidWorld.nodeFor(outlet).P() - Water.P_ATM) / 1e5));
        info.put(I18N.tr("Air"), String.format("%.1f g/s", kgPerS * 1000));
        info.put(I18N.tr("Power"), Utils.plotPower("", resistor.getP()));
        return info;
    }
}
