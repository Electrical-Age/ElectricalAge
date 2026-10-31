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
public class PlantCondenserElement extends TransparentNodeElement implements FluidDevice, mods.eln.fluid.FluidPlacement.Turnable {
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
    /**
     * The cooling-water tube bundle (Baughn 2026-10-29): water piped in low on its left and out low on its right
     * passes through tubes in the vessel; heat goes from the vessel to it at TUBES_UA (W/K), as far as the two
     * temperatures allow (half the way to even in a step at most). A full 16 kW HP turbine's steam (~39 g/s, ~90 kW)
     * condenses with about a kilogram a second of cold water through it.
     */
    public static final double TUBES_UA = 4000, TUBES_VOLUME = 0.002;
    public mods.eln.sim.fluid.FluidNode tubes;
    private double[] loadedTubes;
    /** Heat into the cooling water (W, last step) and since loaded (J), the water through it since loaded (kg). */
    public double tubesW, totalTubesJ, totalCoolingKg;

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
        Direction at = null;
        for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP})
            if (node.getNeighbor(d) instanceof TransparentNode t && t.element instanceof mods.eln.mechanical.ShaftMotorElement m) {
                found = m;
                at = d;
            }
        motor = found;
        // (its drive coupling is drawn on its left or front, where the pumps' shaft comes out: Baughn's art pass)
        int side = at == null ? 0 : at == front.left() ? 1 : at == front ? 2 : 0;
        if (side != motorSide) {
            motorSide = side;
            needPublish();
        }
    }

    /** Which drive coupling it draws: 0 none, 1 left, 2 front (a motor there). */
    int motorSide;

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(motorSide);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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
        // the tube bundle: the vessel's heat into the cooling water (drowned tubes still work: they are in the water)
        if (tubes != null) {
            double tv = n.T(), tt = tubes.T(), q = TUBES_UA * (tv - tt);
            double fromVessel = (n.energy - mods.eln.sim.fluid.Flash.energyAt(n.mWater, n.mAir, n.volume, tt)) / dt;
            double intoTubes = (mods.eln.sim.fluid.Flash.energyAt(tubes.mWater, tubes.mAir, tubes.volume, tv) - tubes.energy) / dt;
            q = q >= 0 ? Math.max(0, Math.min(q, Math.min(0.5 * fromVessel, 0.5 * intoTubes))) : 0;
            n.heat -= q;
            tubes.heat += q;
            tubesW = q;
            totalTubesJ += q * dt;
        }
    }

    @Override
    public void afterFluidStep(double dt) {
        double kg = v.after(dt);
        // the cooling water through it: what flows into the tubes this step
        if (tubes != null) {
            var t = FluidWorld.nodeFor(tubes);
            for (var e : FluidWorld.edgesOf(tubes))
                if (e.b == t && e.flow > 0 || e.a == t && e.flow < 0) totalCoolingKg += Math.abs(e.flow) * dt;
        }
        // the motor turns them: the pump's power for what it moved, the ejector's while it runs
        pumpW = kg / dt / Water.RHO_L * (CondenserDescriptor.PUMP_PA - Water.P_ATM) / PlantCondenserDescriptor.PUMP_EFFICIENCY
            + PlantCondenserDescriptor.EJECTOR_W * Math.min(1, motorShare);
        if (motor != null && motorShare > 0) motor.takeShaftPower(pumpW, dt);
    }

    /** Placed facing the player, its ports made from that front; the wrench turns it (FluidPlacement). */
    @Override
    public void initializeFromThat(Direction front, net.minecraft.world.entity.LivingEntity entityLiving, net.minecraft.nbt.CompoundTag itemStackNbt) {
        super.initializeFromThat(front, entityLiving, itemStackNbt);
        remakePorts();
    }

    @Override
    public Direction[] fronts() {
        return new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP};
    }

    @Override
    public boolean wrenchTurn(net.minecraft.world.entity.player.Player player) {
        return mods.eln.fluid.FluidPlacement.turn(this);
    }

    @Override
    public void remakePorts() {
        makePorts();
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
            new FluidPort(v.inlet, at, left, 1.0, CondenserVessel.PORT_K, TankElement.sideSpot(left, TankElement.HIGH)),   // makeup in: left, high
            new FluidPort(tubes, at, left, 0.0, CondenserVessel.PORT_K, TankElement.sideSpot(left, TankElement.LOW)),     // cooling water in: left, low
            new FluidPort(tubes, at, right, 0.0, CondenserVessel.PORT_K, TankElement.sideSpot(right, TankElement.LOW)));  // cooling water out: right, low
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
        tubes = new mods.eln.sim.fluid.FluidNode(TUBES_VOLUME, c.y, 0.25, false);
        if (loadedTubes != null) {
            tubes.mWater = loadedTubes[0];
            tubes.mAir = loadedTubes[1];
            tubes.energy = loadedTubes[2];
            tubes.update();
        } else tubes.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(c));
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
        if (tubes != null) CondenserVessel.put(nbt, "tubes", tubes);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        v.read(nbt, false);
        loadedTubes = CondenserVessel.get(nbt, "tubes");
    }

    /** What holds its vacuum back, first (Baughn 2026-10-29: Jade says why). */
    public String status() {
        var n = v.vessel;
        if (motor == null) return I18N.tr("No motor: no pump, no air ejector, no vacuum");
        if (v.floodShare() > PlantCondenserDescriptor.FLOOD) return I18N.tr("Flooding: pump its condensate out");
        if (motorShare < 0.5) return I18N.tr("Motor slow: its pumps lag");
        if (n != null && n.P() > 0.3e5)
            return tubesW < 1000 ? I18N.tr("Low vacuum: no cooling water") : I18N.tr("Low vacuum: condenser hot");
        return I18N.tr("Holding vacuum");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put(I18N.tr("Status"), status());
        var n = v.vessel;
        if (n != null) {
            info.put(I18N.tr("Pressure"), String.format("%.2f bar of %s", n.P() / 1e5, EaUnits.contents(n)));
            info.put(I18N.tr("Temperature"), String.format("%.0f °C", n.T() - Water.T0));
            info.put(I18N.tr("Fill"), String.format("%.0f%%, %.0f mB%s", v.floodShare() * 100, n.mLiquid() / EaUnits.KG_PER_MB_WATER,
                v.floodShare() > PlantCondenserDescriptor.FLOOD ? I18N.tr(": FLOODING, its tubes drown") : ""));
        }
        info.put(I18N.tr("Heat rejected"), Utils.plotPower("", v.heatW) + I18N.tr(" by its shell, ") + Utils.plotPower("", tubesW) + I18N.tr(" to its cooling water"));
        info.put(I18N.tr("Motor"), motor == null ? I18N.tr("none: put a Shaft Motor against its side")
            : String.format("%.0f%% speed, pumping %.0f mB/s, %.0f W off its shaft", motorShare * 100, v.pumped, pumpW));
        return info;
    }
}
