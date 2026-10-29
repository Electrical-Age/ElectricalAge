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
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** PlantCondenserDescriptor: a CondenserVessel fed by pipe, its pump and ejector on a shaft motor beside it. */
public class PlantCondenserElement extends TransparentNodeElement implements FluidDevice {
    final PlantCondenserDescriptor desc;
    public final NbtThermalLoad shell = new NbtThermalLoad("shell");
    public final CondenserVessel v;
    /** The exhaust ports' conductance: an LP turbine's big bore. */
    public static final double EXHAUST_K = mods.eln.mechanical.SteamTurbineSpec.LP.portK;
    private List<FluidPort> ports = List.of();
    /** The shaft motor against its side (null: none) and its speed share now. */
    public mods.eln.mechanical.ShaftMotorElement motor;
    public double motorShare;
    /** Shaft power its pump and ejector take (W, last step) and the air that leaked in since loaded (kg). */
    public double pumpW, totalLeakKg;

    public PlantCondenserElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (PlantCondenserDescriptor) d;
        v = new CondenserVessel(shell, desc.volume, desc.nominalHeat, desc.pumpKgPerS);
        thermalLoadList.add(shell);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(t -> findMotor());
    }

    /** A shaft motor against any of its sides drives its pumps (its shaft end). */
    void findMotor() {
        mods.eln.mechanical.ShaftMotorElement found = null;
        for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP})
            if (node.getNeighbor(d) instanceof TransparentNode t && t.element instanceof mods.eln.mechanical.ShaftMotorElement m) found = m;
        motor = found;
    }

    public double floodShare() {
        return v.floodShare();
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
        return v.pump == null ? List.of() : List.of(v.pump, v.makeup);
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (v.vessel == null) return;
        motorShare = motor == null || motor.node.isDestructing() ? 0 : motor.pumpSpeed();
        double flood = v.floodShare(), f = PlantCondenserDescriptor.FLOOD;
        double cooling = flood > f ? Math.max(0.1, 1 - (flood - f) / (1 - f)) : 1;   // (drowned tubes)
        // air leaks in while it holds a vacuum (the ejector's work)
        var n = v.vessel;
        if (n.P() < Water.P_ATM) {
            double air = PlantCondenserDescriptor.AIR_LEAK_KG_PER_S * (1 - n.P() / Water.P_ATM) * dt, T = mods.eln.sim.Ambient.kelvin(node.coordonate);
            n.mAir += air;
            n.energy += air * Water.hAir(T);
            n.update();
            totalLeakKg += air;
        }
        v.before(dt, cooling, motorShare, motorShare);
    }

    @Override
    public void afterFluidStep(double dt) {
        double kg = v.after(dt);
        // the motor turns them: the pump's power for what it moved, the ejector's while it runs
        pumpW = kg / dt / Water.RHO_L * (CondenserDescriptor.PUMP_PA - Water.P_ATM) / PlantCondenserDescriptor.PUMP_EFFICIENCY
            + PlantCondenserDescriptor.EJECTOR_W * Math.min(1, motorShare);
        if (motor != null && motorShare > 0) motor.takeShaftPower(pumpW, dt);
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        int back = front.back().toEnumFacing().get3DDataValue(), right = front.right().toEnumFacing().get3DDataValue(),
            fr = front.toEnumFacing().get3DDataValue(), left = front.left().toEnumFacing().get3DDataValue();
        // (exhaust in at the face centres: an LP turbine's exhaust port meets one directly, set against it; low-pressure
        // steam is too thin for a long pipe: a run of alloy pipe between them lost most of the LP's drop)
        ports = List.of(new FluidPort(v.vessel, at, 1, 1.0, EXHAUST_K),       // exhaust in: top
            new FluidPort(v.vessel, at, back, 1.0, EXHAUST_K),                  // exhaust in: back
            new FluidPort(v.vessel, at, right, 1.0, EXHAUST_K),                 // exhaust in: right
            new FluidPort(v.outlet, at, fr, 0.0, CondenserVessel.PORT_K, TankElement.sideSpot(fr, TankElement.LOW)),   // condensate out: front, low
            new FluidPort(v.inlet, at, left, 1.0, CondenserVessel.PORT_K, TankElement.sideSpot(left, TankElement.HIGH)));   // makeup in: left, high
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (v.vessel != null) {
            makePorts();
            FluidWorld.add(this);
        }
        findMotor();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        var c = node.coordonate;
        v.initialize(c.y, mods.eln.sim.Ambient.kelvin(c));
        makePorts();
        connect();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    /** Thermal cables on its sides (not its front, the condensate flange). */
    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return side == front ? null : shell;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP || side == front) return 0;
        return NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T:", shell.Tc);
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        v.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        v.read(nbt, false);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        var n = v.vessel;
        if (n != null) {
            info.put(I18N.tr("Pressure"), String.format("%.2f bar of %s", n.P() / 1e5, EaUnits.contents(n)));
            info.put(I18N.tr("Temperature"), String.format("%.0f °C", n.T() - Water.T0));
            info.put(I18N.tr("Fill"), String.format("%.0f%% (%.0f mB)%s", v.floodShare() * 100, n.mLiquid() / EaUnits.KG_PER_MB_WATER,
                v.floodShare() > PlantCondenserDescriptor.FLOOD ? I18N.tr(": FLOODING, its tubes drown") : ""));
        }
        info.put(I18N.tr("Heat rejected"), Utils.plotPower("", v.heatW));
        info.put(I18N.tr("Motor"), motor == null ? I18N.tr("none: no condensate pump, no air ejector (put a Shaft Motor against its side)")
            : String.format("%.0f%% speed, pumping %.0f mB/s, %.0f W off its shaft", motorShare * 100, v.pumped, pumpW));
        return info;
    }
}
