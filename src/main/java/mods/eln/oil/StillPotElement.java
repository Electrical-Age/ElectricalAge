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
 * <li>thermal (the shell): one face, the back of the lower right block (1,0,1) (Baughn 2026-11-24: "one heat face
 * should be sufficient"; a burner sets the heat, not the face);</li>
 * <li>vapour out: the top of the rear upper left block (1,1,0), an opening over the pot's top tenth (its charge never
 * reaches it): gas out, and liquid in the vapour line runs back in under it;</li>
 * <li>bottoms drain: the front of the lower right block (0,0,1), its flange low (a tank's low flange) and the pot's floor
 * at the flange's bottom edge (FLOOR), so the residue drains out completely, hot;</li>
 * <li>fill: low on the left flank, of the rear block (1,0,0), into the pot through a check valve, its float shutting
 * at the 24 B charge (fillOpening);</li>
 * <li>steam sparge: low on the right flank, of the rear block (1,0,1), into the pot at its flange's height through a
 * check valve (Baughn: the still has a steam input; steam lowers the oils' partial pressures);</li>
 * <li>the pot's temperature as a signal: the front of the origin (0,0,0), 1 V per 100 °C of its liquid.</li>
 * </ul>
 * Entrainment (Baughn 2026-11-24: yes): boiled too hard, a pot carries liquid over with its vapour. Outside the fluid
 * core's solve: before each step a share of the pot's liquid, by the smoothed Souders-Brown factor of the vapour leaving
 * the pot (zero while cold crude heats, zero when bottled up, worse under vacuum), moves into the node its vapour port
 * leads to (the vapour line), every species by its share and with its liquid enthalpy at the pot's temperature, so mass
 * and energy are conserved exactly. (A check link from the pot's floor into a neck node, solved in the core, made
 * the network stiff: overfired, it crawled for a second at a time.) It is how a badly run still fouls engines.
 */
public class StillPotElement extends TransparentNodeElement implements FluidDevice, GhostCells.Host {
    /** The pot: 32 B, two blocks high; its working charge (the view's fill limit) 24 B. */
    public static final double POT_B = 32, CHARGE_B = 24, HEIGHT = 2;
    public static final double VOLUME = TankDescriptor.bucketsVolume(POT_B);
    /**
     * Its floor over the blocks' bottom (m): the drain flange's bottom edge (a bottoms drain is at the floor: it drains
     * the pot completely; the kettle stands on its skid), so its inside is HEIGHT - FLOOR tall, its top at the blocks' top.
     */
    public static final double FLOOR = (TankElement.LOW - TankElement.FLANGE_HALF) / 16.0;
    /**
     * Entrainment's calibration area (m^2): VOLUME / HEIGHT, the nominal cross-section its Souders-Brown velocity was
     * calibrated on (not the inside's, HEIGHT - FLOOR tall).
     */
    static final double AREA = VOLUME / HEIGHT;
    /** The wall (W/K, shell to contents) and the shell (as the Large Boiler's: J/K, K/W to the air, K/W a face). */
    public static final double WALL_UA = 60, SHELL_C = 2800, LOSS_RP = 0.4, FACE_RS = 0.0025;
    /** The shell's limit (K, absolute; its watchdog): over heavy oil's 700 K and cracking's ~720 K, with the wall's lead. */
    public static final double SHELL_MAX_K = 850;
    /** Its watchdog trips from WATCH_MARGIN_K under the limit on a WATCH_BUDGET (K s): boiled down to residue under a big
     *  burner the shell climbs ~2.5 K/s (StillSketchTest), and it trips by the limit. */
    public static final double WATCH_MARGIN_K = 10, WATCH_BUDGET = 20;
    /** Uncovered, the wall passes DRY_SHARE as much: covered while the liquid stands over WET_LEVEL of the height. */
    public static final double WET_LEVEL = 0.1, DRY_SHARE = 0.05;
    /** The liquid share of the vapour line at which entrainment stops (what is carried falls back while it stands full). */
    public static final double LINE_FULL = 0.3;
    /** The ports' conductance (a tank's); the vapour port's opening (a share of the pot's height, at its top). */
    public static final double PORT_K = TankElement.PORT_K, VAPOUR_SPAN = 0.1;
    /** The fill and sparge stubs (m^3; as the boiler's feed stub); their flanges' height (px in the lower block, a tank's
     *  low flange) and where that is on the pot. */
    public static final double STUB_VOLUME = 0.00002, INLET_DRAW = (TankElement.LOW / 16.0 - FLOOR) / (HEIGHT - FLOOR);
    /** The block whose back is its heat face (local x, y, z). */
    static final int[] HEAT_CELL = {1, 0, 1};
    /**
     * Entrainment: the onset (Souders-Brown factor, m/s, on the node's own cross-section V/h; StillSketchTest: a medium
     * burner peaks at 0.079, a big one flat out 0.14, forced firing 0.26-0.43; Baughn to confirm), the liquid carried a
     * kg of vapour at twice it and at most, and the smoothing (s).
     */
    public static final double ONSET_F = 0.10, E_AT_2F0 = 2, E_MAX = 5, F_TAU = 2;
    /** The temperature signal: 5 V at SIGNAL_MAX_C (1 V a 100 °C). */
    public static final double SIGNAL_MAX_C = 500;
    /** Under this the pot reads as under vacuum (Pa). */
    public static final double VACUUM_PA = 0.9e5;
    /**
     * Its safety valve on the crown (Baughn 2026-11-24: a copper pot at 13 bar is unrealistic): a pot still is an
     * atmospheric vessel of thin copper, its relief valve lifting at a fraction of a bar (0.5 bar gauge here), as the
     * fluid core's relief regulator (a pipe safety valve's, wide open as one); RELIEF_K passes a big burner's whole
     * boil-up a few hundred Pa over the setting. If the relief can't keep up (a fire under a blocked pot at the limits of
     * the solve, or liquid at the valve), the pot bursts at BURST_PA (2 bar gauge).
     */
    public static final double RELIEF_PA = Water.P_ATM + 0.5e5, BURST_PA = Water.P_ATM + 2e5, RELIEF_K = 2e-10;
    /** Venting through its safety valve over this (kg/s, a second's mean) shows "Relieving". */
    public static final double RELIEVING_KG_S = 1e-5;

    /** Its tooltip lines. */
    static List<String> tooltip() {
        return List.of(I18N.tr("Batch crude-oil still (2×2×2 multiblock; 32 B capacity)."),
            I18N.tr("Relieves at 0.5 bar gauge; bursts over 2 bar."));
    }

    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final NbtElectricalGateOutput tempOut = new NbtElectricalGateOutput("tempOut");
    public final NbtElectricalGateOutputProcess tempOutProcess = new NbtElectricalGateOutputProcess("tempOutProcess", tempOut);
    private final GhostCells cells = new GhostCells();
    /**
     * The pot, and the fill and sparge stubs: lumped into the pipe on their flange, and linked to the pot only while one
     * is (unpiped, a stub is a still volume of its own that sleeps: no node in the pot's network).
     */
    public FluidNode vessel, feed, sparge;
    private FluidDevice.Link feedCheck, spargeCheck;
    private List<FluidPort> ports = List.of();
    private double[] loaded, loadedFeed, loadedSparge;
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
        return x == HEAT_CELL[0] && y == HEAT_CELL[1] && z == HEAT_CELL[2] && side == front.getInverse();
    }

    @Override
    public ThermalLoad shell() {
        return thermal;
    }

    long at(int x, int y, int z) {
        return GhostCells.at(this, x, y, z).pos().asLong();
    }


    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return side == front && lrdu == LRDU.Down ? tempOut : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;   // (its one heat face is on a ghost cell)
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return side == front ? NodeBase.maskElectricalOutputGate : 0;   // (its one heat face is on a ghost cell)
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
    public List<Vent> fluidVents() {
        return relief == null ? List.of() : List.of(relief);
    }

    /** Its safety valve: a relief regulator to the air at RELIEF_PA off its top. */
    Vent relief;
    /**
     * What its safety valve has let out to the air since loaded (kg by species id), as a pipe safety valve books it
     * (SafetyValvePart.ventedKg: a room-air model reads these); its rate, a second's mean (kg/s). Server only, not saved.
     */
    public double[] ventedKg = new double[0];
    public double reliefRate;
    double meterRelief;

    @Override
    public List<Link> fluidLinks() {
        if (feedCheck == null) return List.of();
        List<Link> out = new ArrayList<>(2);
        if (piped(ports.get(2))) out.add(feedCheck);
        if (piped(ports.get(3))) out.add(spargeCheck);
        return out;
    }

    /** Is something coupled to this port (a pipe, a device)? */
    boolean piped(FluidPort p) {
        var level = node.coordonate.world();
        return level != null && !mods.eln.fluid.FluidPlacement.partners(level, p).isEmpty();
    }

    /** The vapour port's edges this step (out of the pot through the opening at its top). */
    List<FluidEdge> vapourEdges() {
        List<FluidEdge> out = new ArrayList<>(2);
        for (FluidEdge e : FluidWorld.edgesOf(vessel))
            if ((e.a == vessel ? e.portA : e.portB) > 1 - VAPOUR_SPAN && (relief == null || e != relief.edge)) out.add(e);
        return out;
    }

    /** The vapour leaving the pot by its vapour port this step (kg/s; 0 while it comes in). */
    double vapourOut() {
        double f = 0;
        for (FluidEdge e : vapourEdges()) f += e.a == vessel ? e.flow : -e.flow;
        return Math.max(0, f);
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

    /**
     * The fill's float (0..1): open under the 24 B charge, shut over the last FILL_BAND_B under it, as the view stops
     * buckets.
     */
    public double fillOpening() {
        double room = TankDescriptor.bucketsVolume(CHARGE_B) - vessel.liquidVolume();
        return Math.max(0, Math.min(1, room / TankDescriptor.bucketsVolume(FILL_BAND_B)));
    }

    /** The band (B) under the charge over which the fill's float shuts. */
    public static final double FILL_BAND_B = 0.5;

    @Override
    public void beforeFluidStep(double dt) {
        view.sync(vessel, mods.eln.sim.Ambient.kelvin(node.coordonate));
        // (and no more than the room left in one step, whatever the line's pressure: FluidEdge.openingFor)
        double room = Math.max(0, TankDescriptor.bucketsVolume(CHARGE_B) - vessel.liquidVolume());
        feedCheck.open = feedCheck.edge == null ? fillOpening() : Math.min(fillOpening(), feedCheck.edge.openingFor(room, dt));
        // the shell heats the contents through a heat link inside the fluid core's step (as the boiler's)
        vessel.wallT = thermal.Tc + PhysicalConstant.Tref;
        vessel.wallUA = heatingShare() * WALL_UA;
        // entrainment: the vapour's smoothed velocity out of the pot (last step's flow over the node's cross-section)
        double[] rho = densities(vessel);
        double flow = vapourOut();
        double u = rho[0] > 0 ? flow / rho[0] / AREA : 0;
        uSmooth += (u - uSmooth) * Math.min(1, dt / F_TAU);
        factor = rho[0] > 0 ? uSmooth * Math.sqrt(rho[0] / Math.max(1, rho[1] - rho[0])) : 0;
        // (by the smoothed vapour flow, the step's own swings; into the node the vapour port leads to, up to its room under
        // LINE_FULL, and half the pot's liquid)
        double kg = entrainment(factor) * uSmooth * rho[0] * AREA * dt;
        FluidNode line = null;
        for (FluidEdge e : vapourEdges()) line = e.a == vessel ? e.b : e.a;
        if (line == null || line.fixed) kg = 0;
        // (a pipe run carries it on while it has room under LINE_FULL; a vessel on the flange (a tank, a condenser) up to
        // its free volume)
        else kg = Math.min(kg, Math.max(0, (FluidWorld.isPipeRun(line) ? LINE_FULL : 1) * line.volume - line.liquidVolume()) * rho[1]);
        entrained = kg > 0 ? EaUnits.carryLiquid(vessel, line, kg) : 0;
        meterEntrain += entrained;
    }

    /** Liquid carried into the vapour line before the last step (kg). */
    double entrained;


    @Override
    public void afterFluidStep(double dt) {
        thermal.moveEnergyTo(-vessel.wallJ);
        // the meters: vapour off the pot by species, liquid carried over (each a second's mean)
        for (FluidEdge ve : vapourEdges()) {
            double sign = ve.a == vessel ? 1 : -1;
            if (sign * ve.flow <= 0 || ve.fSpecies == null) continue;
            meterVapour += sign * ve.flow * dt;
            if (meterSpecies.length < ve.fSpecies.length) meterSpecies = java.util.Arrays.copyOf(meterSpecies, ve.fSpecies.length);
            for (int i = 0; i < ve.fSpecies.length; i++) meterSpecies[i] += Math.max(0, sign * ve.fSpecies[i]) * dt;
        }
        // the safety valve: what it lets out, by species (its edge runs from the pot to the air)
        FluidEdge re = relief == null ? null : relief.edge;
        if (re != null && re.flow > 0 && re.fSpecies != null) {
            if (ventedKg.length < re.fSpecies.length) ventedKg = java.util.Arrays.copyOf(ventedKg, re.fSpecies.length);
            for (int i = 0; i < re.fSpecies.length; i++) ventedKg[i] += Math.max(0, re.fSpecies[i]) * dt;
            meterRelief += re.flow * dt;
        }
        meterTime += dt;
        if (meterTime >= 1) {
            reliefRate = meterRelief / meterTime;
            meterRelief = 0;
            vapourRate = meterVapour / meterTime;
            entrainRate = meterEntrain / meterTime;
            vapourSpecies = new double[meterSpecies.length];
            for (int i = 0; i < meterSpecies.length; i++) vapourSpecies[i] = meterSpecies[i] / meterTime;
            java.util.Arrays.fill(meterSpecies, 0);
            meterVapour = meterEntrain = meterTime = 0;
        }
        view.show(vessel);
        relieving(re);
        // past its rating it bursts, as a tank does: the contents' expansion energy (once its safety valve is in the network:
        // a pot filled brimful before the network is built isn't relieving yet)
        if (vessel.P() > BURST_PA && re != null) {
            var v = vessel;
            new Failure(this).pressureVessel(() -> v.P() * v.volume * Math.log(v.P() / Water.P_ATM)).fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, v.P());
        }
    }

    /** While it relieves: the pipe safety valve's jet and whistle (SteamJet), straight up off its crown. */
    void relieving(FluidEdge re) {
        if (re == null || re.flow <= 0 || !(node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        // (its crown: the middle of the pot's top, from the corner cells' centres)
        long a = at(0, 1, 0), b = at(1, 1, 1);
        var top = new net.minecraft.world.phys.Vec3((net.minecraft.core.BlockPos.getX(a) + net.minecraft.core.BlockPos.getX(b)) / 2.0 + 0.5, net.minecraft.core.BlockPos.getY(a) + 1.0,
            (net.minecraft.core.BlockPos.getZ(a) + net.minecraft.core.BlockPos.getZ(b)) / 2.0 + 0.5);
        double q = mods.eln.node.voxel.SteamJet.tick(sl, top, net.minecraft.core.Direction.UP, re, 1.0);
        if (q > 200 && mods.eln.node.voxel.SteamJet.gasShare(re) > 0.5 && sl.getGameTime() % 4 == 0)
            sl.playSound(null, net.minecraft.core.BlockPos.containing(top), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(), net.minecraft.sounds.SoundSource.BLOCKS,
                (float) Math.min(2, 0.4 + q / 10000), (float) Math.min(2, 1.4 + q / 40000));
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
        vessel = restored(new FluidNode(VOLUME, c.y + FLOOR, HEIGHT - FLOOR, false), loaded, amb);
        // (the stubs centred on their flanges, 4 px up the lower blocks)
        double stubZ = c.y + TankElement.LOW / 16.0 - 0.05;
        feed = restored(new FluidNode(STUB_VOLUME, stubZ, 0.1, false), loadedFeed, amb);
        sparge = restored(new FluidNode(STUB_VOLUME, stubZ, 0.1, false), loadedSparge, amb);
        loaded = loadedFeed = loadedSparge = null;
        for (FluidNode stub : new FluidNode[]{feed, sparge}) {
            stub.lumpable = true;   // (it merges into the pipe on its flange, as the boiler's feed stub)
            stub.resistance = 1 / PORT_K;
        }
        feedCheck = new Link(feed, 0, vessel, INLET_DRAW, PORT_K).kind(FluidEdge.Kind.CHECK);
        spargeCheck = new Link(sparge, 0, vessel, INLET_DRAW, PORT_K).kind(FluidEdge.Kind.CHECK);
        relief = new Vent(vessel, 1.0, RELIEF_K);
        relief.setPa = RELIEF_PA;
        relief.open = 1;
        int left = front.left().toEnumFacing().get3DDataValue(), right = front.right().toEnumFacing().get3DDataValue();
        int f = front.toEnumFacing().get3DDataValue();
        ports = List.of(new FluidPort(vessel, at(1, 1, 0), 1, 1.0, PORT_K, FluidPort.CENTRE, VAPOUR_SPAN),
            new FluidPort(vessel, at(0, 0, 1), f, 0, PORT_K, TankElement.sideSpot(f, TankElement.LOW)),
            new FluidPort(feed, at(1, 0, 0), left, 0.5, PORT_K, TankElement.sideSpot(left, TankElement.LOW)),
            new FluidPort(sparge, at(1, 0, 1), right, 0.5, PORT_K, TankElement.sideSpot(right, TankElement.LOW)));
        view.show(vessel);
        cells.make(this);
        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        cells.drop();
    }

    /** (Burst or burnt away: its ghosts go too.) */
    @Override
    public void onSelfDestruction() {
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
        mods.eln.fluid.FluidSaves.put(nbt, "feed", feed);
        mods.eln.fluid.FluidSaves.put(nbt, "sparge", sparge);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (nbt.contains("vesselW")) loaded = mods.eln.fluid.FluidSaves.array(nbt, "vessel");
        if (nbt.contains("feedW")) loadedFeed = mods.eln.fluid.FluidSaves.array(nbt, "feed");
        if (nbt.contains("spargeW")) loadedSparge = mods.eln.fluid.FluidSaves.array(nbt, "sparge");
    }

    // ---- what the player reads

    @Override
    public Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        if (vessel == null) return info;
        if (entrainRate > 1e-6) info.headline(I18N.tr("Boiling over"), String.format(I18N.tr("%s/s liquid carry-over"), EaUnits.grams(entrainRate * 1000)));
        if (onlyResidue()) info.headline(I18N.tr("Only residue left"), I18N.tr("Shell overheating"));
        if (reliefRate > RELIEVING_KG_S) info.headline(I18N.tr("Relieving"), I18N.tr("Outlet blocked"));
        if (vessel.P() < VACUUM_PA) info.headline(I18N.tr("Under vacuum"), String.format("%.2f bar", vessel.P() / 1e5));
        boolean highP = vessel.P() > 0.85 * BURST_PA;
        String pStr = String.format("%.2f bar%s", vessel.P() / 1e5, highP ? " (" + I18N.tr("near bursting") + ")" : "");
        if (highP) info.headline(I18N.tr("Pressure"), pStr);
        info.headline(I18N.tr("Temperature"), mods.eln.api.Meters.celsius(vessel.T() - Water.T0));
        Object[] b = boiling();
        if (b != null) info.headline(I18N.tr("Boiling now"), String.format(I18N.tr("mostly %s (%s)"), b[0], Utils.plotPercent((Double) b[1])));
        info.headline(I18N.tr("Liquid"), EaUnits.volume(vessel.liquidVolume() * Water.RHO_L / EaUnits.KG_PER_MB_WATER) + ", "
            + Utils.plotPercent(vessel.liquidVolume() / VOLUME));
        if (!highP) info.detail(I18N.tr("Pressure"), pStr);
        info.detail(I18N.tr("Shell"), mods.eln.api.Meters.celsius(thermal.Tc + PhysicalConstant.Tref - Water.T0));
        info.detail(I18N.tr("Contents"), EaUnits.composition(vessel));
        String visc = EaUnits.viscosity(vessel);
        if (visc != null) {
            if (visc.contains(I18N.tr("heat or dilute"))) info.headline(I18N.tr("Viscosity"), visc);
            else info.detail(I18N.tr("Viscosity"), visc);
        }
        info.detail(I18N.tr("Signal"), mods.eln.api.Meters.volts(tempOut.getU()));
        return info;
    }
}
