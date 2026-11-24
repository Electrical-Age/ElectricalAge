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
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Still Pot (refinery M2, todo #591; Baughn: batch first, the pot is the refinery's first rung): a 2x2x2 kettle,
 * one fluid node for any species, heated through its shell like the Large Boiler, its vapour off the top to a condenser
 * and the player's receivers. Constants from the step-0 sketch (StillSketchTest) at the pinned game scale: 32 B of pot
 * for a 16-24 B charge, a wall of WALL_UA (a burner, not the wall, sets the heat), the Large Boiler's shell, and a shell
 * limit over heavy oil's 700 K and cracking's ~720 K.
 * Local frame as GhostCells' (front = XN): x 0 front, 1 back; y 0 low, 1 high; z 0 left, 1 right. Faces:
 * <ul>
 * <li>thermal (the shell): the lower blocks' back and both flanks, six faces, as the Large Boiler's;</li>
 * <li>vapour out: the top of the rear upper left block (1,1,0), from the neck (draw 0: whatever the neck holds goes
 * on with the vapour);</li>
 * <li>bottoms drain: the front of the lower right block (0,0,1), at the floor (draw 0), to drain the residue hot;</li>
 * <li>fill: the front of the upper right block (0,1,1), into the pot over the top through a check valve;</li>
 * <li>steam sparge: the front of the upper left block (0,1,0), down a dip pipe to the floor (draw 0.05) through a
 * check valve (Baughn: the still has a steam input; steam lowers the oils' partial pressures);</li>
 * <li>the pot's temperature as a signal: the front of the origin (0,0,0), 1 V per 100 °C of its liquid.</li>
 * </ul>
 * The lower front holds the signal and the drain, so fill and sparge enter from the upper front (the sparge as a dip
 * pipe) and the six heat faces stay as the Large Boiler's.
 * Entrainment (Baughn 2026-11-24: yes): boiled too hard, a pot carries liquid over with its vapour. Outside the fluid
 * core: a check link from the pot's floor to the neck, metered by an orifice, opened before each step from the smoothed
 * Souders-Brown factor of the vapour leaving the pot (zero while cold crude heats, zero when bottled up, worse under
 * vacuum). It is how a badly run still makes fuel that chokes an engine.
 */
public class StillPotElement extends TransparentNodeElement implements FluidDevice, GhostCells.Host {
    /** The pot: 32 B, two blocks high; its working charge (the view's fill limit) 24 B. */
    public static final double POT_B = 32, CHARGE_B = 24, HEIGHT = 2;
    public static final double VOLUME = TankDescriptor.bucketsVolume(POT_B);
    /** The wall (W/K, shell to contents) and the shell (as the Large Boiler's: J/K, K/W to the air, K/W a face). */
    public static final double WALL_UA = 60, SHELL_C = 2800, LOSS_RP = 0.4, FACE_RS = 0.0025;
    /** The shell's limit (K, absolute; its watchdog): over heavy oil's 700 K and cracking's ~720 K, with the wall's lead. */
    public static final double SHELL_MAX_K = 850;
    /** Its watchdog trips from WATCH_MARGIN_K under the limit on a WATCH_BUDGET (K s): boiled down to residue under a big
     *  burner the shell climbs ~2.5 K/s (StillSketchTest), and it trips by the limit. */
    public static final double WATCH_MARGIN_K = 10, WATCH_BUDGET = 20;
    /** Uncovered, the wall passes DRY_SHARE as much: covered while the liquid stands over WET_LEVEL of the height. */
    public static final double WET_LEVEL = 0.1, DRY_SHARE = 0.05;
    /** The neck at the vapour port (B; its height, m), the link into it, the ports' conductance (a tank's). */
    public static final double NECK_B = 0.5, NECK_H = 0.25, VAPOUR_K = 1e-9, PORT_K = TankElement.PORT_K;
    /** The fill and sparge stubs (m^3; as the boiler's feed stub) and where the sparge's dip pipe ends. */
    public static final double STUB_VOLUME = 0.00002, SPARGE_DRAW = 0.05;
    /**
     * Entrainment: the onset (Souders-Brown factor, m/s, on the node's own cross-section V/h; StillSketchTest: a medium
     * burner peaks at 0.079, a big one flat out 0.14, forced firing 0.26-0.43; Baughn to confirm), the liquid carried a
     * kg of vapour at twice it and at most, the smoothing (s), and the orifice (m^2) with the drive (Pa) its boost leaves.
     */
    public static final double ONSET_F = 0.10, E_AT_2F0 = 2, E_MAX = 5, F_TAU = 2, ENT_AREA = 2e-5, ENT_DP = 5000;
    /** The temperature signal: 5 V at SIGNAL_MAX_C (1 V a 100 °C). */
    public static final double SIGNAL_MAX_C = 500;
    /** Under this the pot reads as under vacuum (Pa). */
    public static final double VACUUM_PA = 0.9e5;

    /** (Neon's wording.) */
    static final String[] TOOLTIP = {
        "2x2x2, placed at front bottom left; extends right, back, up",
        "Heat on lower back and flanks; vapour out at top back left",
        "Front: temp low left 1 V/100 °C, drain low right, fill high right, steam high left",
        "Holds 32 B; fill with up to 24 B of crude"};

    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final NbtElectricalGateOutput tempOut = new NbtElectricalGateOutput("tempOut");
    public final NbtElectricalGateOutputProcess tempOutProcess = new NbtElectricalGateOutputProcess("tempOutProcess", tempOut);
    private final GhostCells cells = new GhostCells();
    /** The pot, the neck at its vapour port, the fill and sparge stubs. */
    public FluidNode vessel, neck, feed, sparge;
    private FluidDevice.Link vapour, entrain, feedCheck, spargeCheck;
    private List<FluidPort> ports = List.of();
    private double[] loaded, loadedNeck, loadedFeed, loadedSparge;
    private final mods.eln.fluid.NodeFluidView view = new mods.eln.fluid.NodeFluidView(new mods.eln.fluid.NodeFluidView.Spec().intake().liquidShare(CHARGE_B / POT_B));
    /** The smoothed vapour velocity out of the pot (m/s) and its Souders-Brown factor (m/s); not saved. */
    double uSmooth;
    public double factor;
    /** Averaged over a second: vapour leaving the pot (kg/s) by species, liquid carried over (kg/s). */
    public double vapourRate, entrainRate;
    public double[] vapourSpecies = new double[0];
    double meterTime, meterVapour, meterEntrain;
    double[] meterSpecies = new double[0];

    public StillPotElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        thermalLoadList.add(thermal);
        electricalLoadList.add(tempOut);
        electricalComponentList.add(tempOutProcess);
        slowProcessList.add((mods.eln.sim.IProcess) t -> {
            if (vessel != null) tempOutProcess.setOutputNormalized(Math.max(0, Math.min(1, (vessel.T() - Water.T0) / SIGNAL_MAX_C)));
        });
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        // (boiled down to residue, the shell climbs as fast as the burner heats it: it fails at its limit)
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(SHELL_MAX_K - WATCH_MARGIN_K - PhysicalConstant.Tref).setBudget(WATCH_BUDGET)
            .set(new Failure(this).machine()));
    }

    // ---- the frame and faces

    @Override
    public boolean thermalFace(int x, int y, int z, Direction side) {
        return GhostCells.lowerBackOrFlank(front, x, y, z, side);
    }

    @Override
    public ThermalLoad shell() {
        return thermal;
    }

    long at(int x, int y, int z) {
        return GhostCells.at(this, x, y, z).pos().asLong();
    }

    @Override
    public List<FluidPort> portsAt(long pos) {
        List<FluidPort> out = new ArrayList<>();
        for (FluidPort p : fluidPorts()) if (p.pos() == pos) out.add(p);
        return out;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return side == front && lrdu == LRDU.Down ? tempOut : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalFace(0, 0, 0, side) ? thermal : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front) return NodeBase.maskElectricalOutputGate;
        return thermalFace(0, 0, 0, side) ? NodeBase.maskThermal : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return side == front ? Utils.plotSignal(tempOut.getU(), tempOut.getCurrent()) : "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    // ---- the fluid device

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
        return vapour == null ? List.of() : List.of(vapour, entrain, feedCheck, spargeCheck);
    }

    /** The share of its heating surface under liquid, as heat passes (DRY_SHARE..1): residue covers it too. */
    public double heatingShare() {
        double w = Math.max(0, Math.min(1, vessel.level() / WET_LEVEL));
        return DRY_SHARE + (1 - DRY_SHARE) * w;
    }

    /** The gas's and the liquid's densities in the pot (kg/m^3; the liquid's 850 with none). */
    static double[] densities(FluidNode n) {
        double mg = 0, ml = 0;
        for (int i = 0; i < n.speciesBound(); i++) {
            mg += n.vapour(i);
            ml += n.liquid(i);
        }
        double vl = n.liquidVolume(), vg = Math.max(1e-12, n.volume - vl);
        return new double[]{mg / vg, vl > 0 ? ml / vl : 850};
    }

    /** The liquid each kg of vapour carries at Souders-Brown factor f (kg/kg): none under the onset. */
    public static double entrainment(double f) {
        return f > ONSET_F ? Math.min(E_MAX, E_AT_2F0 * (f / ONSET_F - 1)) : 0;
    }

    @Override
    public void beforeFluidStep(double dt) {
        view.sync(vessel, mods.eln.sim.Ambient.kelvin(node.coordonate));
        // the shell heats the contents through a heat link inside the fluid core's step (as the boiler's)
        vessel.wallT = thermal.Tc + PhysicalConstant.Tref;
        vessel.wallUA = heatingShare() * WALL_UA;
        // entrainment: the vapour's smoothed velocity out of the pot (last step's flow over the node's cross-section)
        double[] rho = densities(vessel);
        double flow = vapour.edge == null ? 0 : Math.max(0, vapour.edge.flow);
        double u = rho[0] > 0 ? flow / rho[0] / (VOLUME / HEIGHT) : 0;
        uSmooth += (u - uSmooth) * Math.min(1, dt / F_TAU);
        factor = rho[0] > 0 ? uSmooth * Math.sqrt(rho[0] / Math.max(1, rho[1] - rho[0])) : 0;
        double target = entrainment(factor) * flow;
        double lift = (neck.z + neck.height) - (vessel.z + vessel.level() * vessel.height);
        entrain.boost = rho[1] * mods.eln.sim.fluid.FluidNetwork.G * lift + ENT_DP;
        double drive = Math.max(1000, vessel.P() - neck.P() + ENT_DP);
        entrain.open = target <= 0 || vessel.liquidVolume() <= 0 ? 0 : Math.min(1, target / (FluidEdge.CD * ENT_AREA * Math.sqrt(2 * rho[1] * drive)));
    }

    @Override
    public void afterFluidStep(double dt) {
        thermal.moveEnergyTo(-vessel.wallJ);
        // the meters: vapour off the pot by species, liquid carried over (each a second's mean)
        FluidEdge ve = vapour.edge, ee = entrain.edge;
        if (ve != null && ve.flow > 0 && ve.fSpecies != null) {
            meterVapour += ve.flow * dt;
            if (meterSpecies.length < ve.fSpecies.length) meterSpecies = java.util.Arrays.copyOf(meterSpecies, ve.fSpecies.length);
            for (int i = 0; i < ve.fSpecies.length; i++) meterSpecies[i] += Math.max(0, ve.fSpecies[i]) * dt;
        }
        if (ee != null) meterEntrain += Math.max(0, ee.flow) * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            vapourRate = meterVapour / meterTime;
            entrainRate = meterEntrain / meterTime;
            vapourSpecies = new double[meterSpecies.length];
            for (int i = 0; i < meterSpecies.length; i++) vapourSpecies[i] = meterSpecies[i] / meterTime;
            java.util.Arrays.fill(meterSpecies, 0);
            meterVapour = meterEntrain = meterTime = 0;
        }
        view.show(vessel);
    }

    /** Vapour leaving the pot that counts as boiling (kg/s): under it, no "Boiling now" (a cold pot's air breathing). */
    public static final double BOILING_KG_S = 2e-5;

    /** What boils off now: the main species of the vapour leaving (not air) and its share, or null while none leaves. */
    public Object[] boiling() {
        if (vapourRate < BOILING_KG_S) return null;
        double sum = 0, best = 0;
        int id = -1;
        for (int i = 0; i < vapourSpecies.length; i++) {
            if (i == SpeciesRegistry.AIR) continue;
            sum += vapourSpecies[i];
            if (vapourSpecies[i] > best) {
                best = vapourSpecies[i];
                id = i;
            }
        }
        if (id < 0 || sum < BOILING_KG_S) return null;
        return new Object[]{SpeciesRegistry.get().byId(id).displayName(), best / sum};
    }

    /** Only residue left: liquid, of which under 2 % can boil (Species.Kind.NONE never does), and the burner on. */
    public boolean onlyResidue() {
        if (vessel == null) return false;
        double all = 0, never = 0;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int i = 0; i < vessel.speciesBound(); i++) {
            double l = vessel.liquid(i);
            all += l;
            if (reg.byId(i).kind == Species.Kind.NONE) never += l;
        }
        return all > 1e-6 && never > 0 && all - never < 0.02 * all;
    }

    // ---- life cycle

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

    static FluidNode restored(FluidNode n, double[] saved, double kelvin) {
        if (saved != null) {
            mods.eln.fluid.FluidSaves.restore(n, saved);
            n.update();
        } else n.fill(0, Water.P_ATM, kelvin);
        return n;
    }

    @Override
    public void initialize() {
        thermal.set(FACE_RS, LOSS_RP, SHELL_C);
        var c = node.coordonate;
        double amb = mods.eln.sim.Ambient.kelvin(c);
        vessel = restored(new FluidNode(VOLUME, c.y, HEIGHT, false), loaded, amb);
        // (the neck's floor at the pot's top face, where its port draws: a coupling must be level with the pipe's end; the
        // stubs centred on their flanges, half way up the upper blocks)
        neck = restored(new FluidNode(TankDescriptor.bucketsVolume(NECK_B), c.y + HEIGHT, NECK_H, false), loadedNeck, amb);
        feed = restored(new FluidNode(STUB_VOLUME, c.y + 1.45, 0.1, false), loadedFeed, amb);
        sparge = restored(new FluidNode(STUB_VOLUME, c.y + 1.45, 0.1, false), loadedSparge, amb);
        loaded = loadedNeck = loadedFeed = loadedSparge = null;
        for (FluidNode stub : new FluidNode[]{feed, sparge}) {
            stub.lumpable = true;   // (it merges into the pipe on its flange, as the boiler's feed stub)
            stub.resistance = 1 / PORT_K;
        }
        vapour = new Link(vessel, 1.0, neck, 0, VAPOUR_K);
        entrain = new Link(vessel, 0, neck, 1.0, 0).kind(FluidEdge.Kind.CHECK).area(ENT_AREA);
        entrain.open = 0;
        feedCheck = new Link(feed, 0, vessel, 1.0, PORT_K).kind(FluidEdge.Kind.CHECK);
        spargeCheck = new Link(sparge, 0, vessel, SPARGE_DRAW, PORT_K).kind(FluidEdge.Kind.CHECK);
        int f = front.toEnumFacing().get3DDataValue();
        ports = List.of(new FluidPort(neck, at(1, 1, 0), 1, 0, PORT_K),
            new FluidPort(vessel, at(0, 0, 1), f, 0, PORT_K),
            new FluidPort(feed, at(0, 1, 1), f, 0.5, PORT_K),
            new FluidPort(sparge, at(0, 1, 0), f, 0.5, PORT_K));
        view.show(vessel);
        cells.make(this);
        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        cells.drop();
    }

    /** (Buckets go through its fluid capability, on its origin block.) */
    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return view;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (vessel == null) return;
        mods.eln.fluid.FluidSaves.put(nbt, "vessel", vessel);
        mods.eln.fluid.FluidSaves.put(nbt, "neck", neck);
        mods.eln.fluid.FluidSaves.put(nbt, "feed", feed);
        mods.eln.fluid.FluidSaves.put(nbt, "sparge", sparge);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (nbt.contains("vesselW")) loaded = mods.eln.fluid.FluidSaves.array(nbt, "vessel");
        if (nbt.contains("neckW")) loadedNeck = mods.eln.fluid.FluidSaves.array(nbt, "neck");
        if (nbt.contains("feedW")) loadedFeed = mods.eln.fluid.FluidSaves.array(nbt, "feed");
        if (nbt.contains("spargeW")) loadedSparge = mods.eln.fluid.FluidSaves.array(nbt, "sparge");
    }

    // ---- what the player reads

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        if (vessel == null) return info;
        // (warnings first: Jade's headline is the first lines; values by Neon)
        if (entrainRate > 1e-6) info.put(I18N.tr("Boiling over"), String.format(I18N.tr("%s/s liquid in vapour; lower heat"), EaUnits.grams(entrainRate * 1000)));
        if (onlyResidue()) info.put(I18N.tr("Only residue left"), I18N.tr("shell overheating; cut heat and drain"));
        if (vessel.P() < VACUUM_PA) info.put(I18N.tr("Under vacuum"), String.format(I18N.tr("%.2f bar; fit a breather on the receiver"), vessel.P() / 1e5));
        info.put(I18N.tr("Pressure"), String.format("%.2f bar", vessel.P() / 1e5));
        info.put(I18N.tr("Temperature"), mods.eln.api.Meters.celsius(vessel.T() - Water.T0));
        info.put(I18N.tr("Shell"), mods.eln.api.Meters.celsius(thermal.Tc + PhysicalConstant.Tref - Water.T0));
        Object[] b = boiling();
        if (b != null) info.put(I18N.tr("Boiling now"), String.format(I18N.tr("mostly %s, %s"), b[0], Utils.plotPercent((Double) b[1])));
        info.put(I18N.tr("Contents"), EaUnits.composition(vessel));
        double mb = vessel.liquidVolume() * Water.RHO_L / EaUnits.KG_PER_MB_WATER;
        info.put(I18N.tr("Liquid"), EaUnits.tank(mb, POT_B * 1000));
        EaUnits.putViscosity(info, vessel);
        info.put(I18N.tr("Signal"), mods.eln.api.Meters.volts(tempOut.getU()));
        return info;
    }
}
