package mods.eln.oil;

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
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import mods.eln.fluid.FluidWorld;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BoilerDescriptor: a vessel (a fluid-core node, notes/pipes/DESIGN.md phase 2) heated through its shell, a thermal load
 * on its sides. Water comes in by pipe (the fluid capability); steam leaves by pipe, or through the dome port under it
 * straight into a turbine below (the dome pipe draws at the top of the vessel and comes down the outside), or through
 * its top port. A safety valve vents above SAFETY_PA.
 */
public class BoilerElement extends TransparentNodeElement implements FluidDevice {
    final BoilerDescriptor desc;
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final BoilerTanks tanks = new BoilerTanks();
    public static final double VOLUME = 0.008, SHELL_C = 700, SHELL_TO_WATER_K_PER_W = 0.005;
    public static final double SAFETY_PA = 10e5, PORT_K = 6e-11, VALVE_K = 3e-13;
    /** The vessel (built in initialize, where the block's height is known). */
    public FluidNode vessel;
    private double[] loaded;
    private List<FluidPort> ports = List.of();
    private final List<Vent> vents = new java.util.ArrayList<>();
    public double steamRate;   // mB/s that left the vessel as steam, for WAILA
    double meterSteam, meterTime;

    public BoilerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (BoilerDescriptor) d;
        thermalLoadList.add(thermal);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        // dry and very hot: it fails (no explosion this round)
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(400).set(new Failure(this).machine()));
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
    public List<Vent> fluidVents() {
        return vents;
    }

    /** Take in what pipes put in or took out since the last step (each step, and before saving). */
    public void syncTanks() {
        int in = tanks.takeWaterIn();
        if (in > 0) EaUnits.addWater(vessel, in * EaUnits.KG_PER_MB_WATER, Water.T_AMBIENT, false);
        int out = tanks.takeSteamOut();
        if (out > 0) EaUnits.drainVapour(vessel, out * EaUnits.KG_PER_MB_STEAM);
        meterSteam += out;
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncTanks();
        // the shell heats the water (EA thermal temperatures are relative to the ambient, Tref)
        double shellK = thermal.Tc + mods.eln.sim.PhysicalConstant.Tref;
        double q = (shellK - vessel.T()) / SHELL_TO_WATER_K_PER_W;
        vessel.heat += q;
        thermal.moveEnergyTo(-q * dt);
        // the safety valve: shut below 95% of its setting, wide open at 105%
        vents.get(0).open = Math.max(0, Math.min(1, (vessel.P() / SAFETY_PA - 0.95) / 0.1));
    }

    @Override
    public void afterFluidStep(double dt) {
        // steam out through the ports and the valve, for the meter
        double gone = 0;
        for (FluidEdge e : FluidWorld.edgesOf(vessel)) gone += (e.a == vessel ? e.fVapour : -e.fVapour) * dt;
        meterSteam += Math.max(0, gone) / EaUnits.KG_PER_MB_STEAM;
        meterTime += dt;
        if (meterTime >= 1) {
            steamRate = meterSteam / meterTime;
            meterSteam = 0;
            meterTime = 0;
        }
        showTanks();
    }

    /** A new boiler, or one saved before the vessel (old saves): its tanks' water and steam (mB), air for the rest, at
     *  the shell's temperature. Public for the GameTests. */
    public void fromTanks(int waterMb, int steamMb) {
        double T = thermal.Tc + mods.eln.sim.PhysicalConstant.Tref;
        vessel.fill(waterMb * EaUnits.KG_PER_MB_WATER, Water.P_ATM, T);
        EaUnits.addWater(vessel, steamMb * EaUnits.KG_PER_MB_STEAM, T, true);
    }

    public void showTanks() {
        tanks.show((int) Math.floor(vessel.mLiquid() / EaUnits.KG_PER_MB_WATER), (int) Math.floor(EaUnits.steamAvailable(vessel) / EaUnits.KG_PER_MB_STEAM));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (vessel != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermal;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP) return 0;
        return NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    @Override
    public void initialize() {
        // a well-insulated kettle: the water's heat capacity is the vessel's now (8 B is ~540 g in EA's scale, ~2.3
        // kJ/K); the shell keeps ~0.7 kJ/K and loses ~80 W at 100 degrees. From cold, a 1 kW heater brings it to the
        // boil in ~5 minutes.
        thermal.set(0.01, 1.0, SHELL_C);
        var c = node.coordonate;
        vessel = new FluidNode(VOLUME, c.y, 1.0, false);
        if (loaded != null) {
            vessel.mWater = loaded[0];
            vessel.mAir = loaded[1];
            vessel.energy = loaded[2];
            vessel.update();
        } else fromTanks(tanks.water(), tanks.steam());
        showTanks();
        long pos = c.pos().asLong();
        ports = List.of(new FluidPort(vessel, pos, 0, 0.97, PORT_K),    // the dome pipe's flange, underneath
            new FluidPort(vessel, pos, 1, 1.0, PORT_K));                  // the top port
        vents.clear();
        vents.add(new Vent(vessel, 1.0, VALVE_K));
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tanks;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (vessel != null) {
            syncTanks();
            showTanks();
        }
        tanks.writeToNBT(nbt, "boiler");
        if (vessel != null) {
            nbt.putDouble("vesselW", vessel.mWater);
            nbt.putDouble("vesselA", vessel.mAir);
            nbt.putDouble("vesselU", vessel.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tanks.readFromNBT(nbt, "boiler");
        if (nbt.contains("vesselW"))
            loaded = new double[]{nbt.getDoubleOr("vesselW", 0), nbt.getDoubleOr("vesselA", 0), nbt.getDoubleOr("vesselU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        if (vessel != null) {
            info.put(I18N.tr("Water temperature"), String.format("%.0f °C", vessel.T() - Water.T0));
            info.put(I18N.tr("Pressure"), String.format("%.2f bar", vessel.P() / 1e5));
        }
        info.put(I18N.tr("Water"), tanks.water() + " / " + BoilerTanks.WATER_MB + " mB");
        info.put(I18N.tr("Steam"), tanks.steam() + " / " + BoilerTanks.STEAM_MB + " mB");
        info.put(I18N.tr("Steam made"), String.format("%.0f mB/s", steamRate));
        return info;
    }
}
