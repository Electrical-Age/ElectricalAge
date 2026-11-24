package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Refinery M2 step 0 (todo #591): a pot still on the real fluid core at the pinned game scale, before the Still Pot and
 * Condenser Coil exist. Pot (32 B, a crude charge, heated through its wall link from a shell a burner heats), a neck at
 * its vapour port, a vertical condenser node (vapour in at the top, liquid out at its floor, gas offtake at its top,
 * cooled through its wall link), receivers below it, each vented by a breather (gas out above the air's pressure, air
 * in below it, as FlareStackElement's tip). Cuts are taken by switching receivers by the pot's temperature.
 * Entrainment is prototyped outside the fluid core: a CHECK link from the pot's floor to the neck, metered by an
 * orifice, its opening set before each step from the smoothed vapour velocity out of the pot (Souders-Brown).
 *
 * A sketch, slow (minutes): run with {@code ./gradlew :test --tests mods.eln.sim.fluid.StillSketchTest -Psketch=still}
 * (-PsketchOnly=prefix runs the cases whose label starts with it). The tables go to build/still-sketch.txt.
 */
@EnabledIfSystemProperty(named = "eln.sketch", matches = ".*still.*")
class StillSketchTest {
    /** A bucket's volume (m^3). */
    static final double B = mods.eln.oil.TankDescriptor.bucketsVolume(1);
    static final double DT = 0.05;
    static int HEAVY, DIESEL, GASOLINE, RESIDUE, GAS, CRUDE;
    static final double RHO_CRUDE = 850;

    static final StringBuilder OUT = new StringBuilder();

    static void out(String f, Object... a) {
        String s = String.format(Locale.ROOT, f, a);
        System.out.println(s);
        OUT.append(s).append('\n');
    }

    static void defineCrude() {
        TestSpecies.defineAll();
        HEAVY = TestSpecies.id("heavy_oil");
        DIESEL = TestSpecies.id("diesel");
        GASOLINE = TestSpecies.id("gasoline");
        RESIDUE = TestSpecies.id("residue");
        GAS = TestSpecies.id("natural_gas");
        CRUDE = TestSpecies.id("crude_oil");
        // (TestSpecies parses the JSON but doesn't register crude's mixture; the game's loader does)
        SpeciesRegistry.get().setMixture("eln:crude_oil", java.util.Map.of("eln:heavy_oil", 0.35, "eln:diesel", 0.25,
            "eln:gasoline", 0.20, "eln:residue", 0.15, "eln:natural_gas", 0.05));
    }

    /** One batch's settings. */
    static final class Cfg {
        String label;
        double chargeB = 24, potB = 32;
        boolean live;
        /** The burner (W) and its thermostat on the shell (K; NaN: none, full power). */
        double burnerW = 5000, tsetK = Double.NaN;
        double uaPot = 60, uaCoil = 40, coilB = 2;
        /** Pot temperatures (K) at which the next receiver takes over (null: one receiver). */
        double[] cuts;
        /** Stop when the pot reaches this (K), or after maxS. */
        double stopK = Double.POSITIVE_INFINITY, maxS = 2400, stopShellK = Double.POSITIVE_INFINITY;
        /** Entrainment: on, its onset factor (m/s) and the liquid it carries per kg of vapour at twice the onset. */
        boolean entrain;
        double f0 = ONSET_F, eAt2 = E_AT_2F0;
        /** Print the time-resolved overhead every window (s); 0: not. */
        double window = 0;

        Cfg(String label) {
            this.label = label;
        }
    }

    /** Proposed entrainment constants (from the runs below): onset Souders-Brown factor (m/s), liquid/vapour at 2x. */
    static final double ONSET_F = 0.1, E_AT_2F0 = 2, E_MAX = 5;
    /** Smoothing of the vapour velocity (s). */
    static final double F_TAU = 2.0;

    /** One batch's results. */
    static final class Res {
        Cfg c;
        double[][] recv;   // per receiver: liquid kg by species index (heavy, diesel, gasoline, residue)
        double[] vented = new double[5], potLeft = new double[4], coilLeft = new double[4], neckLeft = new double[4];
        double charged[] = new double[5];
        double airIn, fMax, fMaxBoil, entrainedKg, msPerStep, endPotK, endShellK, maxShellK, maxCoilK, endS, minPotP = 1e9, maxPotP;
        long failures, cut, substeps;
        double balanceErr, gasFed;
        double shellRiseKPerMin = Double.NaN, shellLimitAt = Double.NaN, residueOnlyAt = Double.NaN;
    }

    static final int[] IDX = new int[5];

    static void ids() {
        IDX[0] = HEAVY;
        IDX[1] = DIESEL;
        IDX[2] = GASOLINE;
        IDX[3] = RESIDUE;
        IDX[4] = GAS;
    }

    static double coking(double[] liq) {
        double m = 0, c = 0;
        for (int j = 0; j < 4; j++) {
            m += liq[j];
            c += liq[j] * SpeciesRegistry.get().byId(IDX[j]).coking;
        }
        return m > 0 ? c / m : 0;
    }

    static double sum4(double[] liq) {
        return liq[0] + liq[1] + liq[2] + liq[3];
    }

    /** "123 g: gasoline 95 %, diesel 5 %". */
    static String comp(double[] liq) {
        double m = sum4(liq);
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "%.0f g", 1000 * m));
        if (m > 1e-6) {
            b.append(":");
            String[] name = {"heavy", "diesel", "gasoline", "residue"};
            for (int j : new int[]{2, 1, 0, 3}) if (liq[j] / m >= 0.005) b.append(String.format(Locale.ROOT, " %s %.0f%%", name[j], 100 * liq[j] / m));
        }
        return b.toString();
    }

    static double[] liquids(FluidNode n) {
        double[] l = new double[4];
        for (int j = 0; j < 4; j++) l[j] = n.liquid(IDX[j]);
        return l;
    }

    static double[] masses(FluidNode n) {
        double[] l = new double[5];
        for (int j = 0; j < 5; j++) l[j] = n.mass(IDX[j]);
        return l;
    }

    static double gasRho(FluidNode n) {
        double mg = 0;
        for (int i = 0; i < n.state.n; i++) mg += n.state.vap[i];
        return n.state.vGas > 0 ? mg / n.state.vGas : 0;
    }

    static double liqRho(FluidNode n) {
        double ml = 0;
        for (int i = 0; i < n.state.n; i++) ml += n.state.liq[i];
        return n.state.vLiquid > 0 ? ml / n.state.vLiquid : RHO_CRUDE;
    }

    /** A breather on node r (FluidWorld's vent pair): gas out, float shut; air in. */
    static final class Breather {
        final FluidEdge out, in;
        final FluidNode r;

        Breather(FluidNetwork net, FluidNode r, FluidNode atm) {
            this.r = r;
            out = net.connect(r, 1.0, atm, 0, VENT_K).kind(FluidEdge.Kind.CHECK);
            out.gasOnly = true;
            in = net.connect(atm, 0, r, 1.0, VENT_K).kind(FluidEdge.Kind.CHECK);
            set();
        }

        void set() {
            double dp = r.P() - Water.P_ATM;
            double fl = Math.max(0, Math.min(1, (0.97 - r.liquidVolume() / r.volume) / 0.02));
            out.open = fl * Math.max(0, Math.min(1, (dp - DEADBAND) / DEADBAND));
            in.open = Math.max(0, Math.min(1, (-dp - DEADBAND) / DEADBAND));
        }
    }

    /** The purge valve's wide-open conductance (the breather is a third item on that part) and FlareStack's deadband. */
    static final double VENT_K = 2e-11, DEADBAND = 300;
    /** Line conductances (m^3): the vapour line and gas offtake, the condensate line. */
    static final double K_VAP = 1e-10, K_LIQ = 1e-10;
    /** The entrainment link: orifice area (m^2) and the drive (Pa) its boost leaves over the lift. */
    static final double ENT_AREA = 2e-5, ENT_DP = 5000;
    /** The shell: heat capacity (J/K, the Large Boiler's), loss to the air (W/K). */
    static final double SHELL_C = 2800, SHELL_LOSS = 2.5;
    /** The proposed shell limit (K): over heavy oil's 700 K and M5's ~720 K cracking, with the wall's drop over the pot. */
    static final double SHELL_LIMIT_K = 850;

    static Res run(Cfg c) {
        defineCrude();
        ids();
        Res r = new Res();
        r.c = c;
        FluidNetwork net = new FluidNetwork();
        double potV = c.potB * B, potH = 2;
        FluidNode pot = net.add(new FluidNode(potV, 0, potH, false));
        double crudeKg = c.chargeB * B * RHO_CRUDE;
        // dead crude in at once, air at 1 atm over it; a live charge's gas comes in over GAS_FEED_S as the oil pump would
        // bring it (a prescribed flow: a lump written between steps sub-steps the network)
        EaUnits.add(pot, CRUDE, crudeKg * (c.live ? 0.95 : 1), Water.T_AMBIENT, false, false);
        pot.mAir = Water.P_ATM * (potV - pot.liquidVolume()) / (Water.R_AIR * Water.T_AMBIENT);
        pot.energy = pot.energyAt(Water.T_AMBIENT);
        pot.update();
        assertEquals(0, pot.mass(CRUDE), 1e-12, "crude went in as its cuts");
        assertTrue(pot.mass(GASOLINE) > 0);
        double gasFeed = c.live ? crudeKg * 0.05 : 0;
        r.charged = masses(pot);
        r.charged[4] += gasFeed;
        FluidNode neck = net.add(FluidNode.air(0.5 * B, potH, 0.25));
        FluidNode coil = net.add(FluidNode.air(c.coilB * B, 0, 2));
        int nr = c.cuts == null ? 1 : c.cuts.length + 1;
        FluidNode[] recv = new FluidNode[nr];
        FluidNode atm = net.add(FluidNode.atmosphere(0));
        FluidEdge vap = net.connect(pot, 1.0, neck, 1.0, K_VAP).kind(FluidEdge.Kind.CHECK);
        FluidEdge ent = net.connect(pot, 0, neck, 1.0, 0).kind(FluidEdge.Kind.CHECK);
        ent.area = ENT_AREA;
        ent.open = 0;
        FluidEdge line = net.connect(neck, 0, coil, 1.0, K_VAP);
        FluidEdge feed = net.connect(atm, 0, pot, 0, 0).kind(FluidEdge.Kind.FLOW);
        feed.shares = new double[GAS + 1];
        feed.shares[GAS] = 1;
        feed.kelvin = Water.T_AMBIENT;
        FluidEdge[] liq = new FluidEdge[nr], gas = new FluidEdge[nr];
        Breather[] br = new Breather[nr];
        for (int i = 0; i < nr; i++) {
            recv[i] = net.add(FluidNode.air(32 * B, -2, 2));
            recv[i].lossWPerK = 0.5;
            liq[i] = net.connect(coil, 0, recv[i], 1.0, K_LIQ);
            gas[i] = net.connect(coil, 1.0, recv[i], 1.0, K_VAP);
            br[i] = new Breather(net, recv[i], atm);
        }
        double shellT = Water.T_AMBIENT, uSmooth = 0, entTarget = 0;
        int active = 0;
        int steps = (int) Math.round(c.maxS / DT), win = c.window > 0 ? (int) Math.round(c.window / DT) : 0;
        double[] winOver = new double[5];
        double winHeat = 0, winEnt = 0;
        long nanos = 0, warm = 200;
        double residueOnlyAt = Double.NaN, shellAtResidue = Double.NaN;
        if (win > 0) out("  %s: t s | pot K bar | shell K | heat in kW | F m/s | coil K, holdup g | overhead in the window | receiver", c.label);
        int s;
        for (s = 0; s < steps; s++) {
            double t = s * DT;
            // the live charge's gas
            feed.rate = t < GAS_FEED_S ? gasFeed / GAS_FEED_S : 0;
            // the burner and the shell's wall link
            double burner = Double.isNaN(c.tsetK) ? c.burnerW : c.burnerW * Math.max(0, Math.min(1, (c.tsetK - shellT) / 20));
            pot.wallUA = c.uaPot;
            pot.wallT = shellT;
            coil.wallUA = c.uaCoil;
            coil.wallT = 300;
            // cuts by the pot's temperature: the active receiver's lines open, the others shut
            if (c.cuts != null) while (active < c.cuts.length && pot.T() >= c.cuts[active]) active++;
            for (int i = 0; i < nr; i++) liq[i].open = gas[i].open = i == active ? 1 : 0;
            for (Breather b : br) b.set();
            // entrainment: the smoothed vapour velocity out of the pot, its Souders-Brown factor
            double rhoG = gasRho(pot), rhoL = liqRho(pot);
            double fac = 0;
            if (rhoG > 0) {
                double u = Math.max(0, vap.flow) / rhoG / (potV / potH);
                uSmooth += (u - uSmooth) * Math.min(1, DT / F_TAU);
                fac = uSmooth * Math.sqrt(rhoG / Math.max(1, rhoL - rhoG));
            }
            r.fMax = Math.max(r.fMax, fac);
            double eRatio = c.entrain && fac > c.f0 ? Math.min(E_MAX, c.eAt2 * (fac / c.f0 - 1)) : 0;
            entTarget = eRatio * Math.max(0, vap.flow);
            double lift = (neck.z + neck.height) - (pot.z + pot.level() * potH);
            ent.boost = rhoL * FluidNetwork.G * lift + ENT_DP;
            ent.open = entTarget <= 0 || pot.liquidVolume() <= 0 ? 0 : Math.min(1, entTarget / (FluidEdge.CD * ENT_AREA * Math.sqrt(2 * rhoL * Math.max(1000, pot.P() - neck.P() + ENT_DP))));
            long t0 = System.nanoTime();
            net.step(DT);
            if (s >= warm) nanos += System.nanoTime() - t0;
            double heatJ = pot.wallJ;
            shellT += (burner * DT - heatJ - SHELL_LOSS * (shellT - Water.T_AMBIENT) * DT) / SHELL_C;
            r.maxShellK = Math.max(r.maxShellK, shellT);
            r.maxCoilK = Math.max(r.maxCoilK, coil.T());
            r.minPotP = Math.min(r.minPotP, pot.P());
            r.maxPotP = Math.max(r.maxPotP, pot.P());
            if (vap.flow > 1e-4) r.fMaxBoil = Math.max(r.fMaxBoil, fac);
            for (int i = 0; i < nr; i++) {
                if (br[i].out.fSpecies != null) for (int j = 0; j < 5; j++) r.vented[j] += at(br[i].out.fSpecies, IDX[j]) * DT;
                r.airIn += br[i].in.flow * DT;
            }
            if (ent.fSpecies != null) for (int j = 0; j < 4; j++) {
                double m = at(ent.fSpecies, IDX[j]) * DT;
                r.entrainedKg += m;
                winEnt += m;
            }
            r.gasFed += feed.flow * DT;
            if (line.fSpecies != null) for (int j = 0; j < 5; j++) winOver[j] += at(line.fSpecies, IDX[j]) * DT;
            winHeat += heatJ;
            // boiled down to residue: what volatile liquid is left
            double volatileLeft = pot.liquid(GASOLINE) + pot.liquid(DIESEL) + pot.liquid(HEAVY);
            if (Double.isNaN(residueOnlyAt) && volatileLeft < 0.02 * (r.charged[0] + r.charged[1] + r.charged[2])) {
                residueOnlyAt = t;
                shellAtResidue = shellT;
            }
            if (win > 0 && (s + 1) % win == 0) {
                out("  %5.0f | %4.0f %5.3f | %4.0f | %5.2f | %4.2f | %4.0f %4.0f | %s%s | %d: %s", t + DT, pot.T(), pot.P() / 1e5, shellT,
                    winHeat / (win * DT) / 1000, fac, coil.T(), 1000 * sum4(liquids(coil)), comp(winOver),
                    winOver[4] > 1e-6 ? String.format(Locale.ROOT, " +gas %.1f g", 1000 * winOver[4]) : "",
                    active, comp(liquids(recv[active])));
                winOver = new double[5];
                winHeat = 0;
                winEnt = 0;
            }
            if (Double.isNaN(r.shellLimitAt) && shellT > SHELL_LIMIT_K) r.shellLimitAt = t;
            if (pot.T() >= c.stopK || shellT > c.stopShellK) {
                s++;
                break;
            }
        }
        r.endS = s * DT;
        r.endPotK = pot.T();
        r.endShellK = shellT;
        r.residueOnlyAt = residueOnlyAt;
        if (!Double.isNaN(residueOnlyAt) && r.endS - residueOnlyAt > 60)
            r.shellRiseKPerMin = (shellT - shellAtResidue) / ((r.endS - residueOnlyAt) / 60);
        r.recv = new double[nr][];
        for (int i = 0; i < nr; i++) r.recv[i] = liquids(recv[i]);
        r.potLeft = liquids(pot);
        r.coilLeft = liquids(coil);
        r.neckLeft = liquids(neck);
        r.msPerStep = nanos / 1e6 / Math.max(1, s - warm);
        var st = net.coreStats();
        r.failures = st.failures;
        r.cut = st.cut;
        r.substeps = st.substeps;
        // the books: every species charged is in a node or went out of a breather (the gas: what the feed brought before the end)
        r.charged[4] = r.gasFed;
        double worst = 0;
        for (int j = 0; j < 5; j++) {
            double held = pot.mass(IDX[j]) + neck.mass(IDX[j]) + coil.mass(IDX[j]);
            for (FluidNode n : recv) held += n.mass(IDX[j]);
            double err = Math.abs(held + r.vented[j] - r.charged[j]) / Math.max(1e-6, r.charged[j]);
            worst = Math.max(worst, err);
        }
        r.balanceErr = worst;
        assertTrue(worst < 1e-6, c.label + ": the books are off by " + worst);
        assertEquals(0, r.failures, c.label + ": solver failures");
        assertEquals(0, r.cut, c.label + ": steps cut short");
        return r;
    }

    /** Over the first GAS_FEED_S of a live batch, its gas comes in (as the oil pump's line from the well would bring it). */
    static final double GAS_FEED_S = 120;

    static double at(double[] a, int id) {
        return id < a.length ? a[id] : 0;
    }

    static void summary(Res r) {
        Cfg c = r.c;
        double gasIn = r.charged[2];
        out("%s: %.0f B %s crude (%.0f g), burner %.0f W%s, pot UA %.0f, coil UA %.0f; %.0f s, pot end %.0f K, shell end %.0f K (max %.0f), coil max %.0f K, pot %.2f-%.2f bar",
            c.label, c.chargeB, c.live ? "live" : "dead", 1000 * (sum4(r.charged) + r.charged[4]), c.burnerW,
            Double.isNaN(c.tsetK) ? "" : String.format(Locale.ROOT, " (thermostat %.0f K)", c.tsetK), c.uaPot, c.uaCoil, r.endS, r.endPotK,
            r.endShellK, r.maxShellK, r.maxCoilK, r.minPotP / 1e5, r.maxPotP / 1e5);
        for (int i = 0; i < r.recv.length; i++)
            out("  receiver %d: %s; coking %.3f; gasoline recovery %.0f%%", i, comp(r.recv[i]), coking(r.recv[i]), 100 * r.recv[i][2] / gasIn);
        out("  breather out: gasoline %.1f g (%.1f%% of the charge's), diesel %.2f g, natural gas %.1f g; air in %.1f g",
            1000 * r.vented[2], 100 * r.vented[2] / gasIn, 1000 * r.vented[1], 1000 * r.vented[4], 1000 * r.airIn);
        out("  left: pot %s, coil %s, neck %s; entrained %.1f g; F max %.3f (boiling %.3f) m/s; shell rise after residue-only %s K/min",
            comp(r.potLeft), comp(r.coilLeft), comp(r.neckLeft), 1000 * r.entrainedKg, r.fMax, r.fMaxBoil,
            Double.isNaN(r.shellRiseKPerMin) ? "-" : String.format(Locale.ROOT, "%.0f", r.shellRiseKPerMin));
        if (!Double.isNaN(r.residueOnlyAt) || !Double.isNaN(r.shellLimitAt))
            out("  only residue left (2%% of the volatile charge) at %.0f s; shell past %.0f K at %s s", r.residueOnlyAt, SHELL_LIMIT_K,
                Double.isNaN(r.shellLimitAt) ? "-" : String.format(Locale.ROOT, "%.0f", r.shellLimitAt));
        out("  cost %.3f ms/step; failures %d, cut short %d, sub-steps %d; books %.1e", r.msPerStep, r.failures, r.cut, r.substeps, r.balanceErr);
    }

    static boolean wanted(String label) {
        String only = System.getProperty("eln.sketch.only", "");
        return only.isEmpty() || label.startsWith(only);
    }

    static Res go(Cfg c) {
        if (!wanted(c.label)) return null;
        Res r = run(c);
        summary(r);
        return r;
    }

    /** The entrainment link alone: does it lift and meter liquid, and keep the books (mass and energy)? */
    static void probe() {
        defineCrude();
        ids();
        FluidNetwork net = new FluidNetwork();
        FluidNode pot = net.add(new FluidNode(32 * B, 0, 2, false));
        EaUnits.add(pot, CRUDE, 20 * B * RHO_CRUDE, 300, false, false);
        pot.mAir = Water.P_ATM * (pot.volume - pot.liquidVolume()) / (Water.R_AIR * 300);
        pot.energy = pot.energyAt(300);
        pot.update();
        FluidNode neck = net.add(FluidNode.air(0.5 * B, 2, 0.25));
        FluidEdge ent = net.connect(pot, 0, neck, 1.0, 0).kind(FluidEdge.Kind.CHECK);
        ent.area = ENT_AREA;
        // (as in the still: the pot's vapour port into the neck, the neck's floor on to a vessel at the air's pressure)
        FluidNode below = net.add(FluidNode.air(32 * B, 0, 2));
        net.connect(pot, 1.0, neck, 1.0, K_VAP).kind(FluidEdge.Kind.CHECK);
        net.connect(neck, 0, below, 1.0, K_VAP);
        net.connect(below, 1.0, pot, 1.0, K_VAP);
        double m0 = pot.mass() + neck.mass() + below.mass(), u0 = pot.energy + neck.energy + below.energy;
        double target = 0.004, moved = 0;
        for (int s = 0; s < 100; s++) {
            double rhoL = liqRho(pot);
            ent.boost = rhoL * FluidNetwork.G * ((neck.z + neck.height) - pot.level() * 2) + ENT_DP;
            ent.open = Math.min(1, target / (FluidEdge.CD * ENT_AREA * Math.sqrt(2 * rhoL * Math.max(1000, pot.P() - neck.P() + ENT_DP))));
            if (s >= 50) ent.open = 0;
            net.step(DT);
            moved += ent.flow * DT;
        }
        out("probe: entrainment link asked %.1f g/s for 2.5 s (%.1f g), moved %.1f g; mass %.1e, energy %.1e (relative)", 1000 * target,
            1000 * target * 2.5, 1000 * moved, (pot.mass() + neck.mass() + below.mass() - m0) / m0, (pot.energy + neck.energy + below.energy - u0) / Math.abs(u0));
    }

    @Test
    void sketch() throws Exception {
        if (wanted("probe")) probe();
        // the cuts by the pot's temperature (K), chosen from the overhead run: gasoline to 455, slop to 550, diesel to 640, heavy oil
        double[] cuts = {Double.parseDouble(System.getProperty("eln.sketch.t1", "455")), Double.parseDouble(System.getProperty("eln.sketch.t2", "550")),
            Double.parseDouble(System.getProperty("eln.sketch.t3", "640"))};
        double stop = Double.parseDouble(System.getProperty("eln.sketch.stop", "700"));
        // the time-resolved overhead of a sensible batch (one receiver, so the overhead is the whole story)
        Cfg o = new Cfg("overhead 24B dead 5kW");
        o.burnerW = 5000;
        o.tsetK = 760;
        o.window = 30;
        o.stopK = 700;
        go(o);
        // sensible: cuts switched by the pot's temperature (gasoline | slop | diesel | heavy oil)
        for (double b : new double[]{16, 24})
            for (boolean live : new boolean[]{false, true})
                for (double w : new double[]{2000, 5000}) {
                    Cfg c = new Cfg(String.format(Locale.ROOT, "sensible %.0fB %s %.0fkW", b, live ? "live" : "dead", w / 1000));
                    c.chargeB = b;
                    c.live = live;
                    c.burnerW = w;
                    c.tsetK = 760;
                    c.cuts = cuts;
                    c.stopK = stop;
                    c.entrain = true;
                    go(c);
                }
        // uncontrolled: one receiver, full heat (a big burner flat out, no thermostat), with and without entrainment
        for (double w : new double[]{10000, 25000})
            for (boolean e : new boolean[]{false, true}) {
                Cfg c = new Cfg(String.format(Locale.ROOT, "uncontrolled 24B live %.0fkW%s", w / 1000, e ? " entrain" : ""));
                c.live = true;
                c.burnerW = w;
                c.entrain = e;
                c.maxS = 1500;
                c.stopShellK = 1100;
                go(c);
            }
        // the entrainment constants: onset and strength against the overfired product's coking (to the shell limit)
        for (double f0 : new double[]{0.10, 0.12})
            for (double e2 : new double[]{1, 2, 4})
                for (double w : new double[]{10000, 25000}) {
                    Cfg c = new Cfg(String.format(Locale.ROOT, "scan f0 %.2f e2 %.0f %.0fkW", f0, e2, w / 1000));
                    c.live = true;
                    c.burnerW = w;
                    c.entrain = true;
                    c.f0 = f0;
                    c.eAt2 = e2;
                    c.stopShellK = SHELL_LIMIT_K;
                    go(c);
                }
        // the pot's wall: UA 40 and 100 against the 60 above (the shell's lead over the pot); a big burner on the thermostat
        for (double ua : new double[]{40, 100}) {
            Cfg c = new Cfg(String.format(Locale.ROOT, "pot UA%.0f 24B dead 5kW", ua));
            c.uaPot = ua;
            c.tsetK = 760;
            c.cuts = cuts;
            c.stopK = stop;
            c.entrain = true;
            go(c);
        }
        for (double ua : new double[]{40, 60, 100}) {
            Cfg c = new Cfg(String.format(Locale.ROOT, "pot UA%.0f 24B live 10kW thermostat", ua));
            c.uaPot = ua;
            c.live = true;
            c.burnerW = 10000;
            c.tsetK = 760;
            c.cuts = cuts;
            c.stopK = stop;
            c.entrain = true;
            go(c);
        }
        // deeper: heavy oil too, the thermostat at 840 K, to a 740 K pot
        Cfg d = new Cfg("deep 24B dead 5kW");
        d.tsetK = 840;
        d.cuts = cuts;
        d.stopK = 740;
        d.entrain = true;
        go(d);
        // the condenser: gasoline out of the breather against the coil's UA and the burner
        for (double w : new double[]{2000, 5000, 10000})
            for (double ua : new double[]{5, 10, 20, 40, 80}) {
                Cfg c = new Cfg(String.format(Locale.ROOT, "coil 24B dead %.0fkW UA%.0f", w / 1000, ua));
                c.burnerW = w;
                c.tsetK = 760;
                c.uaCoil = ua;
                c.stopK = 520;
                go(c);
            }
        // live crude: its natural gas leaves through the breather saturated with gasoline at the coil's temperature
        for (double w : new double[]{2000, 5000})
            for (double ua : new double[]{40, 80, 160}) {
                Cfg c = new Cfg(String.format(Locale.ROOT, "coil 24B live %.0fkW UA%.0f", w / 1000, ua));
                c.live = true;
                c.burnerW = w;
                c.tsetK = 760;
                c.uaCoil = ua;
                c.stopK = 520;
                go(c);
            }
        java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("eln.sketch.out", "still-sketch.txt")), OUT.toString());
    }
}
