package mods.eln.oil;

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
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

import mods.eln.fluid.FluidWorld;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SteamGeneratorDescriptor: steam in, electricity out of the front and back (like the fuel generator). The steam
 * collects in an inlet (a fluid-core node, notes/pipes/DESIGN.md phase 2): from a boiler set on top (its dome port
 * couples to the inlet port on top) or by EA pipe on that port. No fluid capability (Baughn: the turbine runs off
 * pressure, and other mods' pipes have none; an Energized Power pipe fed it a cold boiler's water); tank stays as the
 * inlet's steam gauge. The power source offers what the inlet's steam
 * above 1 atm can give over the next second, up to nominal; the steam actually used (exhausted) follows the power the
 * load really takes, so an idle turbine keeps its steam.
 */
public class SteamGeneratorElement extends TransparentNodeElement implements FluidDevice {
    final SteamGeneratorDescriptor desc;
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    public final PowerSource source = new PowerSource("source", load);
    public final PreciseElementFluidHandler tank;
    public static final double PORT_K = 6e-11;   // (two coupled ports are in series: 3e-11 boiler to turbine)
    /** The inlet holds TANK_SECONDS of nominal steam at about this density (kg/m^3; ~5 bar). */
    static final double INLET_RHO = 2.5;
    public double steamRate;   // mB/s used, averaged over whole seconds
    double meterSteam, meterTime;
    public FluidNode inlet;
    private double[] loaded;
    private int shown;
    private List<FluidPort> ports = List.of();

    public SteamGeneratorElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (SteamGeneratorDescriptor) d;
        tank = new PreciseElementFluidHandler((int) Math.ceil(desc.nominalSteam() * SteamGeneratorDescriptor.TANK_SECONDS));
        tank.setFilter(new Fluid[]{Oil.STEAM.get()});
        electricalLoadList.add(load);
        electricalComponentList.add(source);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new Run());
    }

    /**
     * Work per kg of the inlet's steam (Baughn 2026-10-26: by the expansion): ETA of its isentropic drop from the
     * inlet (its temperature and vapour pressure) to the exhaust, 1 atm (venting to air).
     */
    double jPerKg() {
        return SteamGeneratorDescriptor.ETA * Water.isentropicDrop(inlet.T(), inlet.pVapour(), exhaustPa());
    }

    /** Where the exhaust goes: the air. */
    double exhaustPa() {
        return Water.P_ATM;
    }

    /**
     * The shaft (Baughn: overspeed, and voltage with speed). Inertia constant H (kinetic energy at nominal speed =
     * H x nominal power); the governor opens the steam valve on a speed error (PI) and moves with a short lag; steam
     * flows through the nozzle in proportion to the inlet pressure (choked), plus a small leak past the shut valve; the
     * generator's EMF is in proportion to speed, so an overloaded turbine slows and its voltage sags. Above TRIP the shaft
     * bursts (EA's overspeed failure): an unloaded turbine fed very high-pressure steam runs away on the leak alone.
     */
    static final double H = 4, GOV_TAU = 0.5, KP = 8, KI = 2, LEAK = 0.005, LOSS = 0.05, TRIP = 1.25, P_REF = SteamGeneratorDescriptor.NOMINAL_PA, FLOW_MAX = 1.5;
    /**
     * Water in the inlet a turning turbine survives, as a share of the inlet's volume (Baughn: if any significant
     * quantity of water goes into a steam turbine, it should be damaged): over WATER_MAX while the shaft turns faster
     * than WATER_SPEED of nominal, the blades take the slug and it burns out in place (FailureCause.WATER, repairable).
     * A stopped turbine only holds the water.
     */
    static final double WATER_MAX = 0.1, WATER_SPEED = 0.2;

    /** The share of the inlet's volume that is liquid water. */
    public double waterShare() {
        return inlet == null ? 0 : inlet.mLiquid() / Water.RHO_L / inlet.volume;
    }

    /** Repaired, the turbine is drained of the water that damaged it (or it would fail again as it spins up). */
    @Override
    public void repair() {
        if (isBurnt() && inlet != null) {
            double mL = inlet.mLiquid(), T = inlet.T();
            inlet.mWater -= mL;
            inlet.energy -= mL * Water.hLiquid(T);
            inlet.update();
            omega = 0;
            source.setUmax(0);
        }
        super.repair();
    }

    /** The low-pressure tip: under this inlet pressure (Pa) for this long (s) while loaded. */
    static final double LOW_PA = 5e5, LOW_SECONDS = 30;
    double lowSeconds;
    /** Times the tip was due (GameTests). */
    public int lowPressureTips;
    /** Who placed it (for the low-pressure advancement); null for turbines placed before. */
    java.util.UUID placer;

    @Override
    public void initializeFromThat(Direction front, net.minecraft.world.entity.LivingEntity entityLiving, CompoundTag itemStackNbt) {
        if (entityLiving instanceof net.minecraft.world.entity.player.Player p) placer = p.getUUID();
        super.initializeFromThat(front, entityLiving, itemStackNbt);
    }

    /** "Under Pressure" (data/eln/advancement/guide/low_pressure.json) to its placer, if online. */
    void awardLowPressure() {
        if (!(node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        var player = sl.getServer().getPlayerList().getPlayer(placer);
        var adv = sl.getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "guide/low_pressure"));
        if (player != null && adv != null) player.getAdvancements().award(adv, "low");
    }

    /** Work per kg now against its rating (10 bar venting to air), for WAILA: what pressure buys. */
    public double pressureEfficiency() {
        return inlet == null ? 0 : jPerKg() / SteamGeneratorDescriptor.NOMINAL_J_PER_KG;
    }

    /** Shaft speed (1 = nominal), governor valve (0..1) and its integral. */
    public double omega, gate, gateI;

    class Run implements IProcess {
        @Override
        public void process(double time) {
            if (inlet == null) return;
            double pe = Math.max(0, source.getEffectiveP());
            // governor
            double err = 1 - omega;
            double raw = gateI + KP * err;
            // (integrate only while the valve isn't pinned, or the spin-up winds it up and the shaft overshoots)
            if (raw > 0 && raw < 1 || raw <= 0 && err > 0 || raw >= 1 && err < 0) gateI = Math.max(0, Math.min(1, gateI + KI * err * time));
            double target = Math.max(0, Math.min(1, gateI + KP * err));
            gate += (target - gate) * Math.min(1, time / GOV_TAU);
            // steam through the nozzle (choked: by the inlet pressure), and only what the inlet has above 1 atm
            double mNom = desc.nominalSteam() * EaUnits.KG_PER_MB_STEAM;   // kg/s at nominal power
            double want = (LEAK + (1 - LEAK) * gate) * FLOW_MAX * mNom * inlet.P() / P_REF * time;
            want = Math.min(want, EaUnits.steamAvailable(inlet));
            double mv = inlet.mVapour(), mg = mv + inlet.mAir;
            double jpk = jPerKg();
            double got = want > 0 && mv > 0 ? EaUnits.drainGas(inlet, want * mg / mv) : 0;
            double pMech = got * jpk / time;
            // the shaft
            double pNom = desc.nominalP;
            double ek = H * pNom * omega * omega + (pMech - pe - LOSS * pNom * omega * omega) * time;
            omega = Math.sqrt(Math.max(0, ek) / (H * pNom));
            source.setUmax(desc.maxU * omega);
            source.setP(omega > 0.05 ? 10 * pNom : 0);
            if (inlet.P() > SteamGeneratorDescriptor.BURST_PA)   // (the casing: Baughn, 25 bar for a small turbine)
                new mods.eln.sim.process.destruct.Failure(SteamGeneratorElement.this).pressureVessel(() -> inlet.energy - inlet.mWater * Water.uLiquid(Water.TB))
                    .fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, inlet.P());
            else if (omega > TRIP)
                new mods.eln.sim.process.destruct.Failure(SteamGeneratorElement.this).rotating(() -> H * pNom * omega * omega)
                    .fail(mods.eln.sim.process.destruct.FailureCause.OVERSPEED, omega);
            else if (omega > WATER_SPEED && waterShare() > WATER_MAX)
                new mods.eln.sim.process.destruct.Failure(SteamGeneratorElement.this).fail(mods.eln.sim.process.destruct.FailureCause.WATER, waterShare());
            else if (omega > 1.1 && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel sl && sl.getGameTime() % 10 == 0)
                sl.playSound(null, node.coordonate.pos(), net.minecraft.sounds.SoundEvents.MINECART_RIDING, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.8f, (float) Math.min(2, omega * 1.4));   // (the whine of a shaft over speed)
            // (Baughn: a tip for players) loaded on under LOW_PA for LOW_SECONDS: an advancement telling its placer to
            // throttle it with a valve, so the boiler can build pressure
            if (omega > 0.2 && pe > 0.05 * pNom && inlet.P() < LOW_PA) lowSeconds += time;
            else lowSeconds = 0;
            if (lowSeconds > LOW_SECONDS) {
                lowSeconds = 0;
                lowPressureTips++;
                if (placer != null) awardLowPressure();
            }
            // meters
            meterSteam += got / EaUnits.KG_PER_MB_STEAM;
            meterTime += time;
            if (meterTime >= 1) {
                steamRate = meterSteam / meterTime;
                meterSteam = 0;
                meterTime = 0;
            }
            if (got > 0) {
                syncTank();
                show();
            }
        }
    }

    /** None: steam comes only through the inlet port (see the class comment). */
    @Override
    public Object getFluidHandler() {
        return null;
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    /** Take in the steam pipes put in since the last step (each step, and before saving). */
    void syncTank() {
        int in = tank.getFluidAmount() - shown;
        if (in > 0) EaUnits.addWater(inlet, in * EaUnits.KG_PER_MB_STEAM, Water.TB, true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncTank();
    }

    @Override
    public void afterFluidStep(double dt) {
        show();
    }

    /** The tank shows the inlet's steam above 1 atm (WAILA; the GameTests feed through it). */
    private void show() {
        int n = (int) Math.min(tank.getCapacity(), Math.floor(EaUnits.steamAvailable(inlet) / EaUnits.KG_PER_MB_STEAM));
        if (tank.getFluidAmount() > 0) tank.drain(tank.getFluidAmount(), true);
        if (n > 0) tank.fill(new net.neoforged.neoforge.fluids.FluidStack(Oil.STEAM.get(), n), true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? load : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        source.setUmax(desc.maxU * omega);   // (the EMF follows the shaft: Run)
        source.setImax(desc.nominalP * 5 / desc.maxU);
        var c = node.coordonate;
        double volume = desc.nominalSteam() * SteamGeneratorDescriptor.TANK_SECONDS * EaUnits.KG_PER_MB_STEAM / INLET_RHO;
        inlet = new FluidNode(volume, c.y, 1.0, false);
        if (loaded != null) {
            inlet.mWater = loaded[0];
            inlet.mAir = loaded[1];
            inlet.energy = loaded[2];
            inlet.update();
        } else {
            // new, or saved before the inlet: air, and the old tank's steam
            inlet.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
            EaUnits.addWater(inlet, tank.getFluidAmount() * EaUnits.KG_PER_MB_STEAM, Water.TB, true);
        }
        show();
        ports = List.of(new FluidPort(inlet, c.pos().asLong(), 1, 0.5, PORT_K));   // the inlet on top
        connect();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) omega);   // (the rotor's turn, SteamGeneratorRender)
            stream.writeFloat((float) (steamRate / desc.nominalSteam()));   // (the steam out of its vent)
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (inlet != null) {
            syncTank();
            show();
        }
        tank.writeToNBT(nbt, "tank");
        nbt.putDouble("omega", omega);
        nbt.putDouble("gate", gate);
        nbt.putDouble("gateI", gateI);
        if (placer != null) nbt.putString("placer", placer.toString());
        if (inlet != null) {
            nbt.putDouble("inletW", inlet.mWater);
            nbt.putDouble("inletA", inlet.mAir);
            nbt.putDouble("inletU", inlet.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        omega = nbt.getDoubleOr("omega", 0);
        gate = nbt.getDoubleOr("gate", 0);
        gateI = nbt.getDoubleOr("gateI", 0);
        String who = nbt.getStringOr("placer", "");
        try {
            placer = who.isEmpty() ? null : java.util.UUID.fromString(who);
        } catch (IllegalArgumentException e) {
            placer = null;
        }
        if (nbt.contains("inletW"))
            loaded = new double[]{nbt.getDoubleOr("inletW", 0), nbt.getDoubleOr("inletA", 0), nbt.getDoubleOr("inletU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Steam"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Steam used"), String.format("%.0f mB/s", steamRate));
        if (inlet != null) info.put(I18N.tr("Inlet pressure"), String.format("%.2f bar", inlet.P() / 1e5));
        // (Baughn: so players see what pressure buys; 100% = its rating, 10 bar venting to air)
        if (inlet != null) info.put(I18N.tr("Pressure efficiency"), String.format("%.0f%% of its %.0f bar rating", pressureEfficiency() * 100,
            SteamGeneratorDescriptor.NOMINAL_PA / 1e5));
        if (waterShare() > 0.01) info.put(I18N.tr("Water in inlet"), String.format("%.0f%%", waterShare() * 100));
        info.put(I18N.tr("Speed"), String.format("%.0f%%", omega * 100));
        info.put(I18N.tr("Steam valve"), String.format("%.0f%%", gate * 100));
        info.put(I18N.tr("Generated power"), Utils.plotPower("", source.getEffectiveP()));
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", load.getU()));
        return info;
    }
}
