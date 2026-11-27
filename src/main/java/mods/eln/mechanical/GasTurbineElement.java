package mods.eln.mechanical;

import mods.eln.fluid.FluidWorld;
import mods.eln.fluid.FuelRegistry;
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
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.FuelQuality;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Vessels;
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
 * exhaust the start of a combined cycle). Fuel from its fuel vessel (a sealed DeviceInlet on its top flange: liquid
 * fuel or fuel gas, by the line's pressure or head; running, it burns the vessel's gas down to 1 atm first, which also
 * lets the air out so a liquid under a low head fills it) burns in its own compressed air: an open (Brayton) cycle putting ETA of the fuel's heat onto the shaft at rated speed,
 * less below it (the compressor takes its share); under CUTOFF of rated speed it can't hold a flame, so it must be spun
 * up (a Shaft Motor on its shaft). Light (H 1 s) with a quick fuel valve: full power in seconds. The rest of the heat
 * leaves as hot air at EXHAUST_T out of its back: into a pipe (a heat exchanger: an economizer, a steam raiser) or the
 * air. Governor, trip and windage as the steam turbines; a signal on its front sets the fuel valve by hand (no governor,
 * no trip).
 */
public class GasTurbineElement extends SimpleShaftElement implements FluidDevice, mods.eln.fluid.FluidPlacement.Turnable {
    /** Shaft power at full fuel and rated speed (W), the cycle's efficiency there, its exhaust's temperature (K). */
    public static final double NOMINAL_P = 10000, ETA = 0.28, EXHAUST_T = 773.15;
    /**
     * Its exhaust casing (m^3). The exhaust (~1.9 g a tick at full power) went in as one lump between fluid steps; in 4 L
     * that was a pressure spike of over half its contents a tick, and the flow behind it came in a burst early in each
     * step. A 1 B a side heat exchanger on it (0.7 g of gas a 2 x 2 x 2 side) passed the burst at too little NTU: the
     * combined cycle's steam raiser gave 9.6 kW where it gives 10.5 (2026-11-16, todo #495); for a while the casing was
     * 40 L (the spike 6 %). The exhaust is now a source inside the fluid step (exhaustFlow), so 4 L again. Saves carry
     * the casing's volume ("exhaustV", since the 40 L casing) and load rescaled to this one at the same pressure; saves
     * without it are from a 4 L casing.
     */
    public static final double EXHAUST_V = 0.004;
    public static final double RATED_RADS = ShaftNetworkKt.nominalShaftRads, CUTOFF = 0.5, DROOP = 0.05, VALVE_TAU = 0.3, TRIP = 1.1,
        WINDAGE = 0.02, H = 1.0;
    /** Its fuel vessel's filling level (mB): sixteen buckets. */
    public static final int TANK_MB = 16000;
    public final NbtElectricalGateInput control = new NbtElectricalGateInput("control");
    public FluidNode exhaust;
    /**
     * Its exhaust into the casing, a source inside the fluid step (FluidDevice.Flow: air at EXHAUST_T). exhaustOwed: air
     * asked for and not yet delivered (kg; one tick's worth in the steady state, more after a step cut short: the next
     * rate carries it, so the heat balance holds); exhaustDh: the heat a kg of it carries over the ambient air (J/kg).
     * Not saved (at most a tick's exhaust is lost on a reload).
     */
    FluidDevice.Flow exhaustFlow;
    private List<Flow> flows = List.of();
    double exhaustOwed, exhaustDh = 1;
    private double[] loadedEx;
    /** The saved casing's volume (m^3; 0: a save from before it was saved, a 4 L casing). */
    private double loadedV;
    private List<FluidPort> ports = List.of();
    /**
     * Fuel in on its small top flange (mods.eln.cad.models.ShaftGas), into its fuel vessel: gasoline, diesel and heavy
     * oil as liquids, natural gas as gas. Sealed, so a gas line's pressure stands in it.
     */
    public final mods.eln.fluid.DeviceInlet fuelIn = new mods.eln.fluid.DeviceInlet("fuelIn", TANK_MB, fuels()).sealed();
    /**
     * Fuel heat taken out of the vessel and not yet burnt (J), and what the flame asked for since the last fluid step
     * (J): the flame burns out of the bank, and the vessel refills it between fluid steps.
     */
    double bank, wanted;
    /**
     * Fuel quality (refinery M1, Baughn 2026-11-15; sim.fluid.FuelQuality): what it burnt last, rated for its boiling
     * window (it loses 2 % of its power per % outside it), and how fouled it is (0..FOUL_MAX, that share of its power
     * gone): fuel over the engines' coking balance fouls it (heavy oil: ~10 % an hour at full power), cleaner fuel
     * clears it again, wool cleans it at once. No damage. An old save loads clean. Liquid fuel that chokes it
     * (FuelQuality.TURBINE_CHOKE) it doesn't take: choked(), live from its vessel, so no state is saved for it.
     */
    public FuelQuality.Rating fuel = FuelQuality.Rating.CLEAN;
    public double foul;
    /** Is there fuel the flame can get at: liquid fuel, or fuel gas over 1 atm. */
    volatile boolean fuelReady;

    /** The fluids it burns: the registry's gasolines, gases and diesels that are fluid species. */
    static net.minecraft.world.level.material.Fluid[] fuels() {
        var names = new java.util.ArrayList<String>();
        names.addAll(List.of(FuelRegistry.INSTANCE.getGasolineList()));
        names.addAll(List.of(FuelRegistry.INSTANCE.getGasList()));
        names.addAll(List.of(FuelRegistry.INSTANCE.getDieselList()));
        return FuelRegistry.INSTANCE.fluidListToFluids(names.toArray(new String[0]));
    }

    @Override
    public void beforeFluidStep(double dt) {
        fuelIn.sync(dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
        FluidNode v = fuelIn.vessel;
        if (v != null) {
            // (a little over what the flame asked for, so the bank lasts to the next step)
            double need = wanted > 0 ? 1.2 * wanted - bank : 0;
            wanted = 0;
            if (need > 0) bank += fuelHeat(v, need);
            fuelReady = liquidReady(v) || gasHeat(v) > 0;
        }
        fuelIn.settle(dt);
    }

    /** The exhaust it owes its pipe goes out over this long (s): longer than a background fluid step (CoreStep.MAX_SPAN). */
    static final double EXHAUST_S = 2.0;

    @Override
    public void afterFluidStep(double dt) {
        fuelIn.after();
        // (the exhaust's heat booked as it went in: the source delivers what it was asked unless a step was cut short)
        if (exhaustFlow != null) {
            double kg = exhaustFlow.delivered * dt;
            exhaustOwed -= kg;
            totalExhaustJ += kg * exhaustDh;
        }
    }

    /** The fuel gas the vessel holds most of (by heat), or -1; a species that is a gas, not a liquid fuel's vapour. */
    private static int fuelGas(FluidNode v) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        int best = -1;
        double most = 0;
        for (int g = 2; g < v.speciesBound(); g++) {
            var sp = reg.byId(g);
            if (sp.gas() && sp.heatingValue > 0 && v.vapour(g) * sp.heatingValue > most) {
                best = g;
                most = v.vapour(g) * sp.heatingValue;
            }
        }
        return best;
    }

    /** The heat of the fuel gas the flame can draw (J): what is over 1 atm. */
    private static double gasHeat(FluidNode v) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double j = 0;
        for (int g = 2; g < v.speciesBound(); g++) {
            var sp = reg.byId(g);
            if (sp.gas() && sp.heatingValue > 0) j += EaUnits.vapourAvailable(v, g) * sp.heatingValue;
        }
        return j;
    }

    /**
     * Take up to need J of fuel out of the vessel, at the species' real heating values; returns the heat taken (J).
     * Its gas first, the whole mix down to 1 atm (Vessels.drawMix): the fuel gases in it burn, and the air a new line
     * pushed ahead goes through the turbine with them, so the vessel fills. Then the liquid fuel layer, every species
     * in it by its share.
     */
    private double fuelHeat(FluidNode v, double need) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double got = 0;
        int g = fuelGas(v);
        if (g >= 0) {
            // (heat per kg of g drawn: the other fuel gases come with it in proportion)
            double per = 0;
            for (int j = 2; j < v.speciesBound(); j++) {
                var sp = reg.byId(j);
                if (sp.gas() && sp.heatingValue > 0) per += v.vapour(j) / v.vapour(g) * sp.heatingValue;
            }
            got += Vessels.drawMix(v, g, need / per) * per;
            if (got > 0) fuel = FuelQuality.Rating.CLEAN;
        } else if (fuelIn.feedId() >= 0) {
            // (no fuel gas: let the gas cap out through the burner, half of what is over 1 atm a step)
            EaUnits.drainGas(v, 0.5 * Math.max(0, 1 - Water.P_ATM / v.P()) * (v.mVapour() + v.mAir));
        }
        int id = fuelIn.feedId();
        if (got >= need || id < 0) return got;
        double hv = EaUnits.layerHeatingValue(v, id);
        if (hv <= 0) return got;
        FuelQuality.Rating q = FuelQuality.rate(v, id, FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
        if (q.choked(FuelQuality.TURBINE_CHOKE)) return got;   // (it chokes the injectors: none taken)
        double liquid = EaUnits.drainLayer(v, id, (need - got) / hv) * hv, w = liquid / Math.max(1e-9, got + liquid);
        // (what it burns next: the liquid's rating by its share of the heat drawn; fuel gas is clean)
        if (liquid > 0) fuel = new FuelQuality.Rating(q.light() * w, q.heavy() * w, q.coking() * w);
        return got + liquid;
    }

    /** Its liquid fuel's rating for its window, or null if it holds none. */
    FuelQuality.Rating liquidRating(FluidNode v) {
        int id = fuelIn.feedId();
        return v == null || id < 0 ? null : FuelQuality.rate(v, id, FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
    }

    /** Is there liquid fuel it can burn: some, and not so heavy it chokes it. */
    private boolean liquidReady(FluidNode v) {
        var q = liquidRating(v);
        return q != null && !q.choked(FuelQuality.TURBINE_CHOKE);
    }

    /**
     * Is it choked (FuelQuality.TURBINE_CHOKE; Baughn 2026-11-23): its liquid fuel has so much heavy oil or residue in it
     * that it takes none, and it has no fuel gas to burn instead. It runs again once the fuel is diluted; piped full
     * (its filling valve shuts at Vessels.FILL_LEVEL) there is no room to, and breaking it empties it.
     */
    public boolean choked() {
        FluidNode v = fuelIn.vessel;
        var q = liquidRating(v);
        return q != null && q.choked(FuelQuality.TURBINE_CHOKE) && gasHeat(v) <= 0;
    }

    /** Its liquid fuel (mB), and its fuel gas (kg). */
    public double fuelMb() {
        return fuelIn.feedMb();
    }

    public double fuelGasKg() {
        FluidNode v = fuelIn.vessel;
        int g = v == null ? -1 : fuelGas(v);
        return g < 0 ? 0 : v.vapour(g);
    }

    /** Put mB of fuel straight into its vessel, as a bucket would (tests). */
    public void putFuel(net.minecraft.world.level.material.Fluid fluid, int mB) {
        fuelIn.put(fluid, mB, mods.eln.sim.Ambient.kelvin(node.coordonate));
        fuelReady = fuelIn.vessel != null && (liquidReady(fuelIn.vessel) || gasHeat(fuelIn.vessel) > 0);
    }

    public double gate;
    public boolean tripped, burning, exhaustPiped;
    /** Fuel heat (W), shaft power (W), averaged a second; totals since loaded (J). */
    public double fuelW, shaftW, totalFuelJ, totalWork, totalExhaustJ;
    double meterFuel, meterWork, meterTime;

    public GasTurbineElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
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

    /** The share of its power its fuel's quality and its fouling leave it. */
    public double quality() {
        return fuel.power() * (1 - foul);
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
            gate = tripped ? 0 : gate + (target - gate) * Math.min(1, time / VALVE_TAU);   // (a trip shuts the fuel at once)
            // the flame: only over CUTOFF of rated speed, and with fuel
            double fuelJ = 0;
            burning = r >= CUTOFF && gate > 0.002 && (bank > 0 || fuelReady);
            if (burning) {
                double want = gate * NOMINAL_P / ETA * time;
                fuelJ = Math.min(want, bank);
                bank -= fuelJ;
                wanted += want;
            }
            // (fuel quality: off-window fuel and a fouled turbine give less work for the same fuel; the rest is exhaust heat)
            double work = getShaft().drive(fuelJ * ETA * speedEfficiency(r) * quality() / time, time, 2 * NOMINAL_P / RATED_RADS);
            if (fuelJ > 0) foul = FuelQuality.foulEngine(foul, fuel.coking(), fuelJ / (NOMINAL_P / ETA * time), time);
            double windage = Math.min(getShaft().getEnergy(), WINDAGE * NOMINAL_P * r * r * r * time);
            getShaft().setEnergy(getShaft().getEnergy() - windage);
            // the rest of the fuel's heat leaves as hot air at EXHAUST_T (air drawn in at the ambient, heated by it): piped, as
            // a source into its casing inside the fluid step (exhaustFlow), at the air this tick's heat needs plus what
            // the last steps didn't deliver; to the air, booked here (the casing stays at 1 atm and the ambient)
            double Tamb = mods.eln.sim.Ambient.kelvin(node.coordonate), heat = Math.max(0, fuelJ - work);
            exhaustDh = Math.max(1, Water.hAir(EXHAUST_T) - Water.hAir(Tamb));
            exhaustPiped = !FluidWorld.edgesOf(exhaust).isEmpty();
            if (exhaustPiped) exhaustOwed += heat / exhaustDh;
            else {
                totalExhaustJ += heat + exhaustOwed * exhaustDh;
                exhaustOwed = 0;
                exhaust.fill(0, Water.P_ATM, Tamb);
            }
            // (what it owes, over EXHAUST_S: a rate that holds for a step of many ticks; all of it asked in one tick, a
            // background step of k ticks took k times what was owed, and the next few nothing)
            exhaustFlow.rate = Math.max(0, exhaustOwed) / EXHAUST_S;
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
        return fuelIn.view;   // (fuel: any pipe or capability, Baughn)
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
    public void remakePorts() {
        var exhaustPort = new FluidPort(exhaust, node.coordonate.pos().asLong(), front.back().toEnumFacing().get3DDataValue(), 0.5,
            mods.eln.node.voxel.PipePart.LARGE_END_K);   // (its exhaust: a large pipe's flange)
        ports = fuelIn.stub == null ? List.of(exhaustPort)
            : List.of(exhaustPort, fuelIn.port(node.coordonate.pos().asLong(), 1, FluidPort.CENTRE));
    }

    @Override
    public List<Link> fluidLinks() {
        return fuelIn.links();
    }

    @Override
    public List<Flow> fluidFlows() {
        return flows;
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (exhaust != null) {
            remakePorts();
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
        exhaust = new FluidNode(EXHAUST_V, node.coordonate.y, 1.0, false);
        if (loadedEx != null) {
            mods.eln.fluid.FluidSaves.restore(exhaust, loadedEx);
            // (saved from another casing: its contents scaled to this one, so it loads at the pressure and temperature it had)
            if (loadedV > 0 && loadedV != EXHAUST_V) scale(exhaust, EXHAUST_V / loadedV);
            exhaust.update();
        } else exhaust.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        exhaustFlow = Flow.source(exhaust, 0.5, SpeciesRegistry.AIR, EXHAUST_T);
        flows = List.of(exhaustFlow);
        fuelIn.make(node.coordonate.y, node.coordonate.y + 1, mods.eln.sim.Ambient.kelvin(node.coordonate));
        remakePorts();
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
        // (wool on a fouled turbine cleans it, and the wool is used: as the fuel heat furnace's burner)
        var held = player == null ? null : player.getMainHandItem();
        if (held == null || foul <= 0.01 || !held.is(net.minecraft.tags.ItemTags.WOOL)) return false;
        foul = 0;
        if (!player.isCreative()) held.shrink(1);
        needPublish();
        return true;
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
        nbt.putDouble("fuelBank", bank);
        nbt.putDouble("gate", gate);
        nbt.putBoolean("tripped", tripped);
        nbt.putDouble("foul", foul);
        nbt.putDouble("fuelLight", fuel.light());
        nbt.putDouble("fuelHeavy", fuel.heavy());
        nbt.putDouble("fuelCoking", fuel.coking());
        if (exhaust != null) {
            mods.eln.fluid.FluidSaves.put(nbt, "exhaust", exhaust);
            // the casing's volume with its contents: a later build with another EXHAUST_V rescales them on load
            nbt.putDouble("exhaustV", EXHAUST_V);
        }
        fuelIn.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        bank = nbt.getDoubleOr("fuelBank", 0);
        gate = nbt.getDoubleOr("gate", 0);
        tripped = nbt.getBooleanOr("tripped", false);
        foul = nbt.getDoubleOr("foul", 0);   // (an old save: clean)
        fuel = new FuelQuality.Rating(nbt.getDoubleOr("fuelLight", 0), nbt.getDoubleOr("fuelHeavy", 0), nbt.getDoubleOr("fuelCoking", 0));
        if (nbt.contains("exhaustW")) {
            loadedEx = mods.eln.fluid.FluidSaves.array(nbt, "exhaust");
            loadedV = nbt.getDoubleOr("exhaustV", 0);
        }
        fuelIn.read(nbt, "fuel", TANK_MB);   // (an old save's fuel tank goes into the vessel; the 1.12 turbine's 4 mB "tank" is dropped)
    }

    /** Scale a node's contents (every species and its energy) by f, in place (no update). */
    static void scale(FluidNode n, double f) {
        n.mWater *= f;
        n.mAir *= f;
        n.energy *= f;
        if (n.extra != null) for (int i = 0; i < n.extra.length; i++) n.extra[i] *= f;
    }

    /** Has it fuel: liquid fuel or fuel gas in its vessel, or heat in the bank. */
    boolean hasFuel() {
        return bank > 0 || fuelMb() > 0 || fuelGasKg() > 1e-9;
    }

    /** What it is doing, or what stops it, first (Baughn 2026-10-29: Jade says why). */
    public String status() {
        double rads = getShaft().getRads();
        if (tripped) return I18N.tr("Tripped: overspeed");
        if (!hasFuel()) {
            String wrong = fuelIn.blockedBy();
            return wrong == null ? I18N.tr("No fuel") : String.format(I18N.tr("Wrong fuel (%s)"), wrong);
        }
        if (choked()) return I18N.tr("Choked (heavy fuel)");
        if (bank <= 0 && !fuelReady) return I18N.tr("Fuel gas under 1 atm");
        if (!burning && rads < CUTOFF * RATED_RADS) return String.format(I18N.tr("Needs starter (>%.0f rad/s)"), CUTOFF * RATED_RADS);
        if (!burning) return I18N.tr("Fuel valve shut");
        if (fuel.power() <= 0) return I18N.tr("Off-spec fuel (no power)");
        if (quality() < 0.995) return String.format(I18N.tr("Running (%.0f%% power)"), quality() * 100);
        if (manual()) return I18N.tr("Manual control (no governor)");
        return I18N.tr("Running");
    }

    @Override
    public Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        double rads = getShaft().getRads();
        info.headline(I18N.tr("State"), status());
        if (foul > 0.25) info.headline(I18N.tr("Fouling"), String.format(I18N.tr("%.0f%% fouled (%.0f%% power)"), foul * 100, (1 - foul) * 100));
        info.headline(I18N.tr("Speed"), Utils.plotRads("", rads));
        info.headline(I18N.tr("Shaft power"), Utils.plotPower("", shaftW));
        info.detail(I18N.tr("Fuel"), fuelIn.vessel == null ? I18N.tr("Empty") : String.format("%s, %.2f bar, %s", EaUnits.composition(fuelIn.vessel),
            fuelIn.vessel.P() / 1e5, Utils.plotPower("", fuelW)));
        if (fuelIn.vessel != null && fuelIn.feedId() >= 0) {
            var q = FuelQuality.rate(fuelIn.vessel, fuelIn.feedId(), FuelQuality.TURBINE_LO, FuelQuality.TURBINE_HI);
            double rate = FuelQuality.foulRate(q.coking(), FuelQuality.ENGINE_BALANCE);
            String visc = EaUnits.viscosityText(FuelQuality.layerViscosity(fuelIn.vessel, fuelIn.feedId()));
            // (choked: no fouling clause, it burns none; a choking liquid beside fuel gas: it burns the gas only)
            info.detail(I18N.tr("Fuel quality"), q.choked(FuelQuality.TURBINE_CHOKE) ? FuelQuality.describe(q, FuelQuality.TURBINE_CHOKE) + (choked() ? "" : " " + I18N.tr("(burning gas only)")) + "; " + visc : String.format("%s; %s; %s", FuelQuality.describe(q, FuelQuality.TURBINE_CHOKE),
                rate > 0 ? String.format(I18N.tr("fouls ~%.0f%%/h"), FuelQuality.engineFoulPerHour(q.coking()) * 100) : I18N.tr("burns clean"),
                visc));
        }
        if (foul > 0.01 && foul <= 0.25) info.detail(I18N.tr("Fouling"), String.format(I18N.tr("%.0f%% fouled (%.0f%% power)"), foul * 100, (1 - foul) * 100));
        info.detail(I18N.tr("Exhaust"), exhaustPiped ? mods.eln.api.Meters.celsius(exhaust == null ? 0 : exhaust.T() - Water.T0) + ", " + I18N.tr("piped") : I18N.tr("Venting"));
        info.detail(I18N.tr("Fuel valve"), tripped ? I18N.tr("Tripped (overspeed)") : String.format("%.0f%%%s", gate * 100, manual() ? " (" + I18N.tr("manual") + ")" : ""));
        return info;
    }
}
