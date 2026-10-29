package mods.eln.mechanical;

import mods.eln.fluid.FluidWorld;
import mods.eln.fluid.FuelRegistry;
import mods.eln.fluid.PreciseElementFluidHandler;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.CompoundTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Gas Turbine, rebuilt (Baughn 2026-10-29, the shaft redo; Argon: a fast peaker and black-start machine, its hot
 * exhaust the start of a combined cycle). Fuel from its tank (any pipe or fluid capability: fuel needs no pressure)
 * burns in its own compressed air: an open (Brayton) cycle putting ETA of the fuel's heat onto the shaft at rated speed,
 * less below it (the compressor takes its share); under CUTOFF of rated speed it can't hold a flame, so it must be spun
 * up (a Shaft Motor on its shaft). Light (H 1 s) with a quick fuel valve: full power in seconds. The rest of the heat
 * leaves as hot air at EXHAUST_T out of its back: into a pipe (a heat exchanger: an economizer, a steam raiser) or the
 * air. Governor, trip and windage as the steam turbines; a signal on its front sets the fuel valve by hand (no governor,
 * no trip).
 */
public class GasTurbineElement extends SimpleShaftElement implements FluidDevice {
    /** Shaft power at full fuel and rated speed (W), the cycle's efficiency there, its exhaust's temperature (K). */
    public static final double NOMINAL_P = 10000, ETA = 0.28, EXHAUST_T = 773.15;
    public static final double RATED_RADS = ShaftNetworkKt.nominalShaftRads, CUTOFF = 0.5, DROOP = 0.05, VALVE_TAU = 0.3, TRIP = 1.1,
        WINDAGE = 0.02, H = 1.0;
    /** Its tank (mB): sixteen buckets. */
    public static final int TANK_MB = 16000;
    public final PreciseElementFluidHandler tank = new PreciseElementFluidHandler(TANK_MB);
    public final NbtElectricalGateInput control = new NbtElectricalGateInput("control");
    public FluidNode exhaust;
    private double[] loadedEx;
    private List<FluidPort> ports = List.of();
    public double gate;
    public boolean tripped, burning, exhaustPiped;
    /** Fuel heat (W), shaft power (W), averaged a second; totals since loaded (J). */
    public double fuelW, shaftW, totalFuelJ, totalWork, totalExhaustJ;
    double meterFuel, meterWork, meterTime;

    public GasTurbineElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        String[] fuels = new String[FuelRegistry.INSTANCE.getGasolineList().length + FuelRegistry.INSTANCE.getGasList().length + FuelRegistry.INSTANCE.getDieselList().length];
        int i = 0;
        for (String f : FuelRegistry.INSTANCE.getGasolineList()) fuels[i++] = f;
        for (String f : FuelRegistry.INSTANCE.getGasList()) fuels[i++] = f;
        for (String f : FuelRegistry.INSTANCE.getDieselList()) fuels[i++] = f;
        tank.setFilter(FuelRegistry.INSTANCE.fluidListToFluids(fuels));
        electricalLoadList.add(control);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new Run());
    }

    @Override
    public double getShaftInertia() {
        return ShaftNetworkKt.inertiaOf(H, NOMINAL_P, RATED_RADS);
    }

    /** Under manual control: a signal on its front sets the fuel valve. */
    public boolean manual() {
        return !control.getConnectedComponents().isEmpty();
    }

    /** Its share of ETA at this speed share: none under CUTOFF, all at rated speed. */
    static double speedEfficiency(double r) {
        return Math.max(0, Math.min(1.05, (r - CUTOFF) / (1 - CUTOFF)));
    }

    class Run implements IProcess {
        @Override
        public void process(double time) {
            if (exhaust == null) return;
            double rads = getShaft().getRads(), r = rads / RATED_RADS;
            boolean manual = manual();
            if (manual || rads < RATED_RADS) tripped = false;
            else if (rads > TRIP * RATED_RADS) tripped = true;
            double target = manual ? control.getNormalized() : tripped ? 0 : Math.max(0, Math.min(1, (RATED_RADS * (1 + DROOP) - rads) / (DROOP * RATED_RADS)));
            gate += (target - gate) * Math.min(1, time / VALVE_TAU);
            // the flame: only over CUTOFF of rated speed, and with fuel
            double fuelJ = 0;
            burning = r >= CUTOFF && gate > 0.002 && tank.getFluidAmount() > 0;
            if (burning) fuelJ = tank.drainEnergy(gate * NOMINAL_P / ETA * time);
            if (fuelJ <= 0) burning = false;
            double work = getShaft().drive(fuelJ * ETA * speedEfficiency(r) / time, time, 2 * NOMINAL_P / RATED_RADS);
            double windage = Math.min(getShaft().getEnergy(), WINDAGE * NOMINAL_P * r * r * r * time);
            getShaft().setEnergy(getShaft().getEnergy() - windage);
            // the rest of the fuel's heat leaves as hot air at EXHAUST_T (air drawn in at the ambient, heated by it)
            double Tamb = mods.eln.sim.Ambient.kelvin(node.coordonate), heat = fuelJ - work;
            if (heat > 0) {
                double air = heat / Math.max(1, Water.hAir(EXHAUST_T) - Water.hAir(Tamb));
                exhaust.mAir += air;
                exhaust.energy += air * Water.hAir(EXHAUST_T);
                exhaust.update();
                totalExhaustJ += heat;
            }
            exhaustPiped = !FluidWorld.edgesOf(exhaust).isEmpty();
            if (!exhaustPiped) exhaust.fill(0, Water.P_ATM, Tamb);
            totalFuelJ += fuelJ;
            totalWork += work;
            meterFuel += fuelJ;
            meterWork += work;
            meterTime += time;
            if (meterTime >= 1) {
                fuelW = meterFuel / meterTime;
                shaftW = meterWork / meterTime;
                meterFuel = meterWork = meterTime = 0;
                needPublish();
            }
        }
    }

    @Override
    public Object getFluidHandler() {
        return tank;   // (fuel: any pipe or capability, Baughn)
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    void makePorts() {
        ports = List.of(new FluidPort(exhaust, node.coordonate.pos().asLong(), front.back().toEnumFacing().get3DDataValue(), 0.5,
            mods.eln.node.voxel.PipePart.LARGE_END_K));   // (its exhaust: a large pipe's flange)
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (exhaust != null) {
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
        exhaust = new FluidNode(0.004, node.coordonate.y, 1.0, false);
        if (loadedEx != null) {
            exhaust.mWater = loadedEx[0];
            exhaust.mAir = loadedEx[1];
            exhaust.energy = loadedEx[2];
            exhaust.update();
        } else exhaust.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        makePorts();
        super.initialize();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? control : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? NodeBase.maskElectricalInputGate : 0;
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
            stream.writeFloat((float) Math.min(1, shaftW / NOMINAL_P));
            stream.writeFloat(tripped ? 0 : (float) gate);   // (the fuel valve's stem)
            stream.writeBoolean(burning);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "fuel");
        nbt.putDouble("gate", gate);
        nbt.putBoolean("tripped", tripped);
        if (exhaust != null) {
            nbt.putDouble("exhaustW", exhaust.mWater);
            nbt.putDouble("exhaustA", exhaust.mAir);
            nbt.putDouble("exhaustU", exhaust.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "fuel");   // (the 1.12 turbine's 4 mB tank was "tank": dropped)
        gate = nbt.getDoubleOr("gate", 0);
        tripped = nbt.getBooleanOr("tripped", false);
        if (nbt.contains("exhaustW"))
            loadedEx = new double[]{nbt.getDoubleOr("exhaustW", 0), nbt.getDoubleOr("exhaustA", 0), nbt.getDoubleOr("exhaustU", 0)};
    }

    /** What it is doing, or what stops it, first (Baughn 2026-10-29: Jade says why). */
    public String status() {
        double rads = getShaft().getRads();
        if (tripped) return I18N.tr("Tripped: overspeed (resets below rated speed)");
        if (tank.getFluidAmount() <= 0) return I18N.tr("No fuel");
        if (!burning && rads < CUTOFF * RATED_RADS) return String.format(I18N.tr("Needs a starter: spin it over %.0f rad/s (a Shaft Motor)"), CUTOFF * RATED_RADS);
        if (!burning) return I18N.tr("Fuel valve shut");
        if (manual()) return I18N.tr("Manual control: no governor or trip");
        return exhaustPiped ? I18N.tr("Running") : I18N.tr("Running, venting its heat (pipe its exhaust to a heat exchanger)");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        double rads = getShaft().getRads();
        info.put(I18N.tr("Status"), status());
        info.put(I18N.tr("Speed"), Utils.plotRads("", rads));
        info.put(I18N.tr("State"), burning ? I18N.tr("burning") : rads < CUTOFF * RATED_RADS ? String.format(I18N.tr("cold: spin it over %.0f rad/s to light"), CUTOFF * RATED_RADS)
            : tank.getFluidAmount() <= 0 ? I18N.tr("out of fuel") : I18N.tr("idle"));
        info.put(I18N.tr("Shaft power"), Utils.plotPower("", shaftW));
        info.put(I18N.tr("Fuel"), tank.getFluidAmount() <= 0 ? I18N.tr("empty") : String.format("%d mB of %s, %s", tank.getFluidAmount(),
            FuelRegistry.INSTANCE.fluidName(tank.getFluid().getFluid()), Utils.plotPower("", fuelW)));
        info.put(I18N.tr("Exhaust"), exhaustPiped ? String.format("%.0f °C, piped", exhaust == null ? 0 : exhaust.T() - Water.T0) : I18N.tr("to the air"));
        info.put(I18N.tr("Fuel valve"), tripped ? I18N.tr("tripped (overspeed)") : String.format("%.0f%%%s", gate * 100, manual() ? I18N.tr(", by hand: no governor, no trip") : ""));
        return info;
    }
}
