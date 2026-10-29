package mods.eln.mechanical;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.oil.SteamValveControl;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shaft Steam Turbine, rebuilt on the steam network (Baughn 2026-10-29: the old one took steam only through the
 * fluid capability, which nothing in EA fills). Steam comes in by EA pipe on its top port and leaves, after doing its
 * work, by the exhaust port on its back: into whatever is piped there (a heater, a later turbine, a condenser) or, with
 * nothing there, into the air. Work per kg is ETA of the steam's isentropic drop from the inlet to the exhaust's
 * pressure, so a condenser's vacuum behind it buys more. It drives the shaft (no generator of its own: Generators on the
 * shaft take the power, four 4 kW ones for its 16 kW, as Baughn intends).
 * <p>
 * Its governor is a 5 % droop: the valve is full open at 800 rad/s and shut at 840, so turbines on one shaft share
 * the load without fighting (Argon). The valve follows with a short lag; the controls (throttle, regulator holding an
 * inlet pressure, an external signal on its front) cap it, as on the 50V/200V turbines (SteamValveControl). Over
 * TRIP of its rated speed it trips (the valve shuts until the shaft is back under rated speed). Under manual control (the
 * throttle sets the valve, or an external signal does) there is no governor and no trip (Baughn 2026-10-29: whoever takes
 * manual control duplicates the safeties): an overdriven shaft races to its burst speed. Water in the
 * inlet strips its blades (repaired with a Turbine Blade Set); over BURST_PA its casing bursts.
 */
public class ShaftSteamTurbineElement extends SimpleShaftElement implements FluidDevice {
    /** Shaft power at its rating (W), the rating (saturated steam, venting to air), the casing's burst (Pa). */
    public static final double NOMINAL_P = SteamTurbineSpec.HP.nominalP, NOMINAL_PA = SteamTurbineSpec.HP.nominalPa, BURST_PA = SteamTurbineSpec.HP.burstPa;
    /** Work per kg out of the ideal (isentropic) expansion: a big multi-stage turbine does better than the 50V/200V. */
    public static final double ETA = SteamTurbineSpec.HP.eta;
    public static final double NOMINAL_J_PER_KG = SteamTurbineSpec.HP.nominalJPerKg;
    /** Rated speed (rad/s), droop, valve lag (s), leak past the shut valve, nozzle capacity over nominal, trip (x rated). */
    public static final double RATED_RADS = ShaftNetworkKt.nominalShaftRads, DROOP = 0.05, GOV_TAU = 0.2, LEAK = 0.005, FLOW_MAX = 1.5, TRIP = 1.1;
    /** Windage and bearings: this share of rated power at rated speed, growing with the cube of speed (so the leak
     *  past a shut valve can't run an unloaded turbine away: it settles under rated speed). */
    public static final double WINDAGE = 0.02;
    /** Its inertia constant (s of rated power at rated speed) and the most torque it gives (N m): twice rated. */
    public static final double H = SteamTurbineSpec.HP.h;
    /** Water in the inlet a turning turbine survives (share of its volume), the speed share it must turn at, and the
     *  casing drain (share of the volume a second), as on the 50V/200V. */
    public static final double WATER_MAX = 0.1, WATER_SPEED = 0.2, DRAIN_SHARE_PER_S = 0.005;
    /** Inlet and exhaust: two seconds of nominal steam at ~10 bar (kg/m^3), and the ports (m^3: an alloy pipe's end). */
    public static final double TARGET_MAX_BAR = SteamTurbineSpec.HP.targetMaxBar;

    final SteamTurbineDescriptor desc;
    /** Its ratings (HP or LP). */
    public final SteamTurbineSpec spec;
    public final SteamValveControl controls;
    public FluidNode inlet, exhaust;
    private double[] loadedIn, loadedEx;
    private List<FluidPort> ports = List.of();
    /** Governor valve (0..1), tripped, and its meters: steam (kg/s), shaft power (W), exhaust piped somewhere. */
    public double gate;
    public boolean tripped, exhaustPiped;
    public double steamKgPerS, shaftW;
    double meterKg, meterJ, meterTime;
    /** Totals since loaded (GameTests): steam through (kg), work put on the shaft (J), the steam's enthalpy in (J). */
    public double totalKg, totalWork, totalInJ, totalWindage;

    public ShaftSteamTurbineElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (SteamTurbineDescriptor) d;
        spec = desc.spec();
        controls = new SteamValveControl(this, () -> this.inlet == null ? 0 : this.inlet.P(), spec.nominalPa / 1e5, spec.targetMaxBar);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(controls.pressure);
        slowProcessList.add(new Run());
        electricalLoadList.add(controls.control);
    }

    private SteamTurbineDescriptor desc() {
        return (SteamTurbineDescriptor) transparentNodeDescriptor;
    }

    @Override
    public double getShaftInertia() {
        return ShaftNetworkKt.inertiaOf(desc().spec().h, desc().spec().nominalP, RATED_RADS);
    }

    /** Steam (kg/s) at its rating. */
    public static double nominalKgPerS() {
        return SteamTurbineSpec.HP.nominalKgPerS;
    }

    /** Its steam (kg/s) at its rating. */
    public double kgPerS() {
        return spec.nominalKgPerS;
    }

    /** The exhaust's pressure (Pa): what the steam expands to. */
    public double exhaustPa() {
        return exhaust == null ? Water.P_ATM : Math.max(1000, exhaust.P());
    }

    /** Work per kg of the inlet's steam expanding to the exhaust's pressure (J/kg). */
    public double jPerKg() {
        return spec.eta * Water.isentropicDrop(inlet.T(), inlet.pVapour(), exhaustPa());
    }

    /** The share of the inlet's volume that is liquid water. */
    public double waterShare() {
        return inlet == null ? 0 : inlet.mLiquid() / Water.RHO_L / inlet.volume;
    }

    /** Water in its blades' way: in the inlet, or backed up into the exhaust from a flooded condenser (share of the
     *  node's volume). */
    public double wetShare() {
        return Math.max(waterShare(), exhaust == null ? 0 : exhaust.mLiquid() / Water.RHO_L / exhaust.volume);
    }

    /**
     * Wet steam in an LP turbine (spec.wetBraking; Baughn 2026-10-29): water over WET_START of a node at speed brakes
     * the shaft (up to WET_BRAKE of rated power at WATER_MAX, with its speed) and knocks; over WATER_MAX for WET_BURN_S
     * (counting down as fast while it's under), it strips its blades.
     */
    public static final double WET_START = 0.01, WET_BRAKE = 0.5, WET_BURN_S = 12;
    /** Braking now (W), seconds over WATER_MAX, and the braking energy since loaded (J; GameTests). */
    public double brakeW, wetSeconds, totalBrake;

    class Run implements IProcess {
        @Override
        public void process(double time) {
            if (inlet == null) return;
            double rads = getShaft().getRads();
            // governor: 5 % droop, a short valve lag; a trip shuts it over TRIP until back under rated speed
            // (manual control, Baughn 2026-10-29: the valve is the player's; no governor, no trip: their safeties)
            boolean manual = controls.isManual();
            if (manual || rads < RATED_RADS) tripped = false;
            else if (rads > TRIP * RATED_RADS) tripped = true;
            double target = manual ? 1 : tripped ? 0 : Math.max(0, Math.min(1, (RATED_RADS * (1 + DROOP) - rads) / (DROOP * RATED_RADS)));
            gate += (target - gate) * Math.min(1, time / GOV_TAU);
            double open = Math.min(gate, controls.limit());
            // steam through the nozzle (choked: by the inlet pressure), only what the inlet has above the exhaust
            double pEx = exhaustPa(), p = inlet.P();
            double want = (LEAK + (1 - LEAK) * open) * FLOW_MAX * spec.nominalKgPerS * p / spec.nominalPa * time;
            double mv = inlet.mVapour(), mg = mv + inlet.mAir;
            want = Math.min(want, p > pEx ? mv * (1 - pEx / p) : 0);
            double tIn = inlet.T(), air0 = inlet.mAir;
            double jpk = jPerKg(), hIn = Water.hVapour(tIn);
            double got = want > 0 && mv > 0 ? EaUnits.drainGas(inlet, want * mg / mv) : 0;
            double air = Math.max(0, air0 - inlet.mAir);
            // the work onto the shaft, under its torque limit; what the shaft can't take stays in the steam
            double work = getShaft().drive(got * jpk / time, time, 2 * spec.nominalP / RATED_RADS);
            double r = rads / RATED_RADS;
            double windage = Math.min(getShaft().getEnergy(), WINDAGE * spec.nominalP * r * r * r * time);
            getShaft().setEnergy(getShaft().getEnergy() - windage);
            totalWindage += windage;
            exhaust.mWater += got;
            exhaust.mAir += air;
            exhaust.energy += got * hIn - work + air * Water.hAir(tIn);
            exhaust.update();
            totalKg += got;
            totalWork += work;
            totalInJ += got * hIn;
            // nothing piped to the exhaust: it goes into the air
            exhaustPiped = !FluidWorld.edgesOf(exhaust).isEmpty();
            if (!exhaustPiped) exhaust.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
            drain(time);
            // failures
            if (inlet.P() > spec.burstPa)
                new Failure(ShaftSteamTurbineElement.this).pressureVessel(() -> inlet.energy - inlet.mWater * Water.uLiquid(Water.TB))
                    .fail(FailureCause.OVERPRESSURE, inlet.P());
            else if (!spec.wetBraking && rads > WATER_SPEED * RATED_RADS && wetShare() > WATER_MAX)
                new Failure(ShaftSteamTurbineElement.this).fail(FailureCause.WATER, wetShare());
            else if (spec.wetBraking) wet(rads, time);
            // meters
            meterKg += got;
            meterJ += work;
            meterTime += time;
            if (meterTime >= 1) {
                steamKgPerS = meterKg / meterTime;
                shaftW = meterJ / meterTime;
                meterKg = meterJ = meterTime = 0;
                needPublish();
            }
        }
    }

    /** An LP turbine's wet steam this step (spec.wetBraking): braking, knocking, and its blades if it lasts. */
    void wet(double rads, double time) {
        double w = wetShare(), r = rads / RATED_RADS;
        brakeW = 0;
        if (r > WATER_SPEED && w > WET_START) {
            brakeW = WET_BRAKE * spec.nominalP * Math.min(1, w / WATER_MAX) * r;
            double e = Math.min(getShaft().getEnergy(), brakeW * time);
            getShaft().setEnergy(getShaft().getEnergy() - e);
            totalBrake += e;
            if (node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel sl && sl.getGameTime() % 6 == 0)
                sl.playSound(null, node.coordonate.pos(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.BLOCKS,
                    (float) Math.min(0.6, 0.15 + w), 0.6f);   // (the knock of water on the blades)
        }
        wetSeconds = r > WATER_SPEED && w > WATER_MAX ? wetSeconds + time : Math.max(0, wetSeconds - time);
        if (wetSeconds > WET_BURN_S) new Failure(ShaftSteamTurbineElement.this).fail(FailureCause.WATER, w);
    }

    /** The casing drain over time (s): liquid water leaves the inlet, with its heat. */
    void drain(double time) {
        double mL = Math.min(inlet.mLiquid(), spec.drainShare * inlet.volume * Water.RHO_L * time);
        if (mL <= 0) return;
        double T = inlet.T();
        inlet.mWater -= mL;
        inlet.energy -= mL * Water.hLiquid(T);
        inlet.update();
    }

    /** Stripped blades (water) take a cheap Turbine Blade Set (Baughn 2026-10-29); other failures the usual part. */
    @Override
    public ItemStack getRepairPart() {
        if (getBurnt() == FailureCause.WATER) {
            ItemStack s = mods.eln.Eln.findItemStack("Turbine Blade Set");
            if (!Utils.isEmpty(s)) return s;
        }
        return super.getRepairPart();
    }

    /** Repaired, it is drained of the water that damaged it (or it would fail again as it spins up). */
    @Override
    public void repair() {
        if (isBurnt() && inlet != null) {
            double mL = inlet.mLiquid(), T = inlet.T();
            inlet.mWater -= mL;
            inlet.energy -= mL * Water.hLiquid(T);
            inlet.update();
            if (exhaust != null) {
                double mE = exhaust.mLiquid(), tE = exhaust.T();
                exhaust.mWater -= mE;
                exhaust.energy -= mE * Water.hLiquid(tE);
                exhaust.update();
            }
            gate = 0;
            wetSeconds = 0;
        }
        super.repair();
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public net.minecraft.world.Container getInventory() {
        return controls.inventory;
    }

    @Override
    public void inventoryChange(net.minecraft.world.Container inv) {
        super.inventoryChange(inv);
        controls.computeInventory();
        needPublish();
    }

    @Override
    public byte networkUnserialize(java.io.DataInputStream stream) {
        byte type = super.networkUnserialize(stream);
        try {
            if (!controls.unserialize(type, stream)) return type;
        } catch (IOException e) {
            e.printStackTrace();
        }
        needPublish();
        return unserializeNulldId;
    }

    @Override
    public Object getFluidHandler() {
        return null;   // (steam by EA pipe only: Baughn, the turbine runs off pressure)
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    /** Its ports, from where it faces now (a turned turbine's exhaust moves with it). */
    void makePorts() {
        long pos = node.coordonate.pos().asLong();
        ports = List.of(new FluidPort(inlet, pos, 1, 1.0, spec.portK),   // the inlet on top
            // the exhaust behind: made for a large alloy pipe (low-pressure steam, to an LP turbine or a condenser)
            new FluidPort(exhaust, pos, exhaustSide().toEnumFacing().get3DDataValue(), 0.5, mods.eln.node.voxel.PipePart.LARGE_END_K));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet != null) {
            makePorts();
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    /** The exhaust port's face: its back. */
    public Direction exhaustSide() {
        return front.back();
    }

    @Override
    public void initialize() {
        controls.computeInventory();
        var c = node.coordonate;
        double volume = spec.nominalKgPerS * 2 / spec.inletRho;
        inlet = new FluidNode(volume, c.y, 1.0, false);
        exhaust = new FluidNode(volume, c.y, 1.0, false);
        double T = mods.eln.sim.Ambient.kelvin(c);
        load(inlet, loadedIn, T);
        load(exhaust, loadedEx, T);
        makePorts();
        super.initialize();
    }

    private static void load(FluidNode n, double[] saved, double T) {
        if (saved != null) {
            n.mWater = saved[0];
            n.mAir = saved[1];
            n.energy = saved[2];
            n.update();
        } else n.fill(0, Water.P_ATM, T);   // (new, or the old capability turbine: its tank is dropped)
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? controls.control : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? NodeBase.maskElectricalInputGate : 0;   // (external control)
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) Math.min(1, steamKgPerS / spec.nominalKgPerS));   // (the sound's loudness)
            controls.serialize(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("gate", gate);
        nbt.putBoolean("tripped", tripped);
        controls.writeToNBT(nbt);
        save(nbt, "inlet", inlet);
        save(nbt, "exhaust", exhaust);
    }

    private static void save(CompoundTag nbt, String key, FluidNode n) {
        if (n == null) return;
        nbt.putDouble(key + "W", n.mWater);
        nbt.putDouble(key + "A", n.mAir);
        nbt.putDouble(key + "U", n.energy);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        gate = nbt.getDoubleOr("gate", 0);
        tripped = nbt.getBooleanOr("tripped", false);
        controls.readFromNBT(nbt, spec.nominalPa / 1e5);
        loadedIn = read(nbt, "inlet");
        loadedEx = read(nbt, "exhaust");
    }

    private static double[] read(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "W")) return null;
        return new double[]{nbt.getDoubleOr(key + "W", 0), nbt.getDoubleOr(key + "A", 0), nbt.getDoubleOr(key + "U", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Speed"), Utils.plotRads("", getShaft().getRads()));
        info.put(I18N.tr("Shaft power"), Utils.plotPower("", shaftW));
        info.put(I18N.tr("Steam used"), String.format("%.1f g/s", steamKgPerS * 1000));
        if (inlet != null) info.put(I18N.tr("Inlet pressure"), String.format("%.2f bar of %s", inlet.P() / 1e5, EaUnits.contents(inlet)));
        info.put(I18N.tr("Exhaust"), exhaustPiped ? String.format("%.2f bar, piped", exhaustPa() / 1e5) : I18N.tr("to the air"));
        if (inlet != null) info.put(I18N.tr("Work per kg"), String.format("%.0f kJ/kg", jPerKg() / 1000));
        if (wetShare() > 0.01) info.put(I18N.tr("Water in its way"), String.format("%.0f%%%s", wetShare() * 100,
            brakeW > 0 ? String.format(", braking %.1f kW: dry the steam (a separator tank)", brakeW / 1000) : ""));
        info.put(I18N.tr("Steam valve"), tripped ? I18N.tr("tripped (overspeed)") : String.format("%.0f%%", Math.min(gate, controls.limit) * 100));
        info.put(I18N.tr("Governor"), controls.isManual() ? I18N.tr("off: manual control, no overspeed trip") : I18N.tr("automatic, trips over 880 rad/s"));
        info.put(I18N.tr("Control"), controls.controlExternal ? String.format("external, %.0f%%", controls.limit * 100)
            : controls.hasRegulator() ? String.format("holds %.1f bar at the inlet", controls.targetBar) : String.format("throttle %.0f%%", controls.throttle * 100));
        return info;
    }
}
