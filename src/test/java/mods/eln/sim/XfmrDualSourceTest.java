package mods.eln.sim;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.DroopLaw;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Baughn 2026-10-09: 200V heat turbine -> two 1:4 DC/DC transformers -> generator used as a motor. With the
 * transformer next to the generator isolating it motors at ~400 W; with it ideal (one SubSystem) the generator
 * sits at ~16 x the turbine's no-load voltage and the shaft slows down.
 *
 * Topology and values as in game (Wp10bContent subId 8, Wp11Content subId 10, ElnDeviceRegistry cables, Rs =
 * P*drop/I^2/2): turbine positiveLoad --0.15 ohm-- inputLoad (Rs 0.005) | MV cable | xfmr1 1:4 (MV/HV windings) |
 * HV cable | xfmr2 1:4 (HV/VHV windings) | VHV cable | generator inputLoad (HV Rs 0.1) --0.1 ohm-- positiveLoad.
 * Adjacent loads are joined by ElectricalConnection (R = Rs1 + Rs2), as NodeBase does.
 */
public class XfmrDualSourceTest {
    static final double MV_RS = 0.05, HV_RS = 0.1, VHV_RS = 2000.0 / 3 / 10000; // 0.0667
    static final double TURB_RS = MV_RS * 0.10, TURB_K = 500 / (200 / 25.0), TURB_PMAX = 500 * 1.5;
    static final double GEN_K = 4000 / (3200 / 25.0), GEN_NOMP = 4000, GEN_U_PER_RAD = 3200 / 800.0;
    static final double JOULE_PER_RAD = 10 * 10 * 0.5 / 2; // generator 5 + gas turbine 5, shapeFactor 0.5

    SimHarness h;

    ElectricalLoad load(double rs) {
        ElectricalLoad l = new ElectricalLoad();
        l.setRs(rs);
        h.sim.addElectricalLoad(l);
        return l;
    }

    void connect(ElectricalLoad a, ElectricalLoad b) {
        h.sim.addElectricalComponent(new ElectricalConnection(a, b));
    }

    /** TurbineElectricalProcess, verbatim (targetU fixed: the turbine's deltaT is steady). */
    class Turbine implements IRootSystemPreStepProcess {
        ElectricalLoad pos;
        VoltageSource src;
        double targetU;

        public void rootSystemPreStepProcess() {
            SubSystem.Th th = pos.getSubSystem().getTh(pos, src);
            double Ut;
            if (targetU < th.U) {
                Ut = th.U;
            } else if (th.isHighImpedance()) {
                Ut = targetU;
            } else {
                double a = 1 / th.R;
                double b = TURB_K - th.U / th.R;
                double c = -TURB_K * targetU;
                Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            }
            double i = (Ut - th.U) / th.R;
            double p = i * Ut;
            if (p > TURB_PMAX) {
                Ut = (Math.sqrt(th.U * th.U + 4 * TURB_PMAX * th.R) + th.U) / 2;
                Ut = Math.min(Ut, targetU);
                if (Double.isNaN(Ut)) Ut = 0;
                if (Ut < th.U) Ut = th.U;
            }
            src.setU(Ut);
        }
    }

    /**
     * GeneratorElectricalProcess + GeneratorShaftProcess (Generator.kt), shaft energy only. legacy = the 1.7.10
     * motoring law (copied); otherwise the law Generator.kt uses now, through the real DroopLaw.
     */
    class Generator implements IRootSystemPreStepProcess, IProcess {
        ElectricalLoad pos;
        VoltageSource src;
        double rads;
        boolean legacy;
        SubSystem.Th lastTh;

        public void rootSystemPreStepProcess() {
            double targetU = rads * GEN_U_PER_RAD;
            SubSystem.Th th = pos.getSubSystem().getTh(pos, src);
            lastTh = th;
            double Ut;
            if (!legacy) {
                Ut = DroopLaw.terminalU(th, targetU, GEN_K);
                if (targetU < th.U) Ut = DroopLaw.limitAbsorbed(th, Ut, GEN_NOMP * 0.5);
            } else if (targetU < th.U) {
                Ut = th.U * 0.999 + targetU * 0.001;
            } else if (th.isHighImpedance()) {
                Ut = targetU;
            } else {
                double a = 1 / th.R;
                double b = GEN_K - th.U / th.R;
                double c = -GEN_K * targetU;
                Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
            }
            src.setU(Ut);
        }

        public void process(double time) {
            double p = src.getP();
            double E = p * time;
            if (E < 0) E *= 0.9;
            double eff = 0.95;
            double shaftE = E >= 0 ? E / eff : E * eff;
            double drag = 0.02 * Math.max(rads, 10.0) * time * 20;
            double energy = JOULE_PER_RAD * rads - (shaftE + drag * eff);
            rads = Math.max(0, energy / JOULE_PER_RAD);
        }
    }

    static class Result {
        double genU, genP, turbU, turbP, rads, thU, thR;

        public String toString() {
            return String.format("gen U=%.1f P=%.1f | turbine U=%.2f P=%.1f | shaft %.2f rad/s | gen sees Th %.1f V %.3f ohm",
                genU, genP, turbU, turbP, rads, thU, thR);
        }
    }

    static class Cfg {
        boolean legacy, iso2, iso1, isoFirst, genFirst = true;
        double core = 1, rads0 = 189, seconds = 1, turbineTargetU = 237;

        Cfg legacy(boolean v) { legacy = v; return this; }
        Cfg iso2(boolean v) { iso2 = v; return this; }
        Cfg iso1(boolean v) { iso1 = v; return this; }
        Cfg isoFirst(boolean v) { isoFirst = v; return this; }
        Cfg genFirst(boolean v) { genFirst = v; return this; }
        Cfg core(double v) { core = v; return this; }
        Cfg rads0(double v) { rads0 = v; return this; }
        Cfg seconds(double v) { seconds = v; return this; }

        public String toString() {
            return (legacy ? "legacy " : "fixed  ") + (iso1 ? "iso1 " : "") + (iso2 ? "iso2 " : "ideal ")
                + (isoFirst ? "isoFirst " : "") + (genFirst ? "gen-first " : "turb-first ") + "core " + core
                + " rads0 " + rads0 + " t=" + seconds + "s";
        }
    }

    final java.util.List<IRootSystemPreStepProcess> isoProcs = new java.util.ArrayList<>();

    /** One 1:4 transformer, ideal or isolating (TransformerElement: winding loads Rs = cable Rs * core). */
    void transformer(ElectricalLoad p, ElectricalLoad s, boolean isolating) {
        if (isolating) {
            VoltageSource a = new VoltageSource("a", p, null), b = new VoltageSource("b", s, null);
            h.sim.addElectricalComponent(a);
            h.sim.addElectricalComponent(b);
            TransformerInterSystemProcess iso = new TransformerInterSystemProcess(p, s, a, b);
            iso.setRatio(4);
            isoProcs.add(iso);
        } else {
            Transformer x = new Transformer(p, s);
            x.setRatio(4);
            h.sim.addElectricalComponent(x);
        }
    }

    Result run(Cfg cfg) {
        h = new SimHarness();
        isoProcs.clear();
        Turbine t = new Turbine();
        t.targetU = cfg.turbineTargetU;
        t.pos = load(TURB_RS);
        t.src = new VoltageSource("turbine", t.pos, null);
        h.sim.addElectricalComponent(t.src);
        ElectricalLoad tIn = load(TURB_RS);
        h.sim.addElectricalComponent(new Resistor(t.pos, tIn).setR(TURB_RS * 30));

        ElectricalLoad c1 = load(MV_RS);
        connect(tIn, c1);
        ElectricalLoad x1p = load(MV_RS * cfg.core), x1s = load(HV_RS * cfg.core);
        connect(c1, x1p);
        transformer(x1p, x1s, cfg.iso1);
        ElectricalLoad c2 = load(HV_RS);
        connect(x1s, c2);
        ElectricalLoad x2p = load(HV_RS * cfg.core), x2s = load(VHV_RS * cfg.core);
        connect(c2, x2p);
        transformer(x2p, x2s, cfg.iso2);
        ElectricalLoad c3 = load(VHV_RS);
        connect(x2s, c3);

        Generator g = new Generator();
        g.legacy = cfg.legacy;
        g.rads = cfg.rads0;
        ElectricalLoad gIn = load(HV_RS);
        connect(c3, gIn);
        g.pos = load(HV_RS);
        h.sim.addElectricalComponent(new Resistor(gIn, g.pos).setR(HV_RS));
        g.src = new VoltageSource("generator", g.pos, null);
        h.sim.addElectricalComponent(g.src);

        if (cfg.isoFirst) for (IRootSystemPreStepProcess p : isoProcs) h.sim.mna.addProcess(p);
        if (cfg.genFirst) h.sim.mna.addProcess(g);
        h.sim.mna.addProcess(t);
        if (!cfg.genFirst) h.sim.mna.addProcess(g);
        if (!cfg.isoFirst) for (IRootSystemPreStepProcess p : isoProcs) h.sim.mna.addProcess(p);
        h.sim.addElectricalProcess(g);

        h.seconds(cfg.seconds);
        Result r = new Result();
        r.genU = g.src.getU();
        r.genP = g.src.getP();
        r.turbU = t.src.getU();
        r.turbP = t.src.getP();
        r.rads = g.rads;
        r.thU = g.lastTh.U;
        r.thR = g.lastTh.R;
        return r;
    }

    Result show(Cfg c) {
        Result r = run(c);
        System.out.println(c + ": " + r);
        return r;
    }

    /** The bug, with the 1.7.10 motoring law: ideal mode stalls near the turbine's no-load voltage x 16. */
    @Test
    public void legacyLawReproducesTheStall() {
        for (double core : new double[]{1, 4})
            for (boolean genFirst : new boolean[]{true, false})
                for (boolean isoFirst : new boolean[]{false, true})
                    for (boolean iso : new boolean[]{true, false})
                        for (double s : new double[]{0.05, 1, 10})
                            show(new Cfg().legacy(true).core(core).genFirst(genFirst).isoFirst(isoFirst).iso2(iso).seconds(s));
        show(new Cfg().legacy(true).iso1(true).iso2(true).seconds(10));
        show(new Cfg().legacy(true).iso2(true).rads0(600).seconds(1));
        show(new Cfg().legacy(true).iso2(false).rads0(600).seconds(1));

        Result ideal = run(new Cfg().legacy(true).seconds(10));
        assertTrue("legacy ideal sits near 16 x no-load: " + ideal, ideal.genU > 3700);
        assertTrue("legacy ideal motors with almost nothing: " + ideal, -ideal.genP < 200);
        // the motoring current is 0.001 (th.U - E) / th.R, with th.R ~ n^2 x the turbine side's R
        double predicted = ideal.genU * 0.001 * (ideal.thU - 4 * ideal.rads) / ideal.thR;
        assertEquals(predicted, -ideal.genP, 0.05 * predicted);
    }

    /** With the droop law both ways, ideal and isolating agree and the shaft spins up. */
    @Test
    public void fixedLawIdealMatchesIsolating() {
        for (double core : new double[]{1, 4})
            for (boolean genFirst : new boolean[]{true, false})
                for (double s : new double[]{1, 10}) { // first tick: transient, the 50x pre-step loops differ
                    Result iso = show(new Cfg().core(core).genFirst(genFirst).iso2(true).seconds(s));
                    Result ideal = show(new Cfg().core(core).genFirst(genFirst).iso2(false).seconds(s));
                    Result both = show(new Cfg().core(core).genFirst(genFirst).iso1(true).iso2(true).seconds(s));
                    assertEquals(iso.genP, ideal.genP, 0.03 * Math.abs(iso.genP));
                    assertEquals(iso.genU, ideal.genU, 0.03 * iso.genU);
                    assertEquals(both.genP, ideal.genP, 0.03 * Math.abs(both.genP));
                    assertTrue("motoring: " + ideal, ideal.genP < -500);
                    if (s >= 10) assertTrue("spins up: " + ideal, ideal.rads > 300);
                }
    }

    /** Generating into a resistor (the usual single-source case): the new law is the old one, bit for bit. */
    @Test
    public void generatingUnchanged() {
        for (double rLoad : new double[]{1, 100, 2560, 1e5}) {
            for (double rads : new double[]{100, 800, 1000}) {
                double[] u = new double[2];
                for (int legacy = 0; legacy < 2; legacy++) {
                    h = new SimHarness();
                    Generator g = new Generator();
                    g.legacy = legacy == 1;
                    g.rads = rads;
                    g.pos = load(HV_RS);
                    g.src = new VoltageSource("generator", g.pos, null);
                    h.sim.addElectricalComponent(g.src);
                    ElectricalLoad out = load(HV_RS);
                    h.sim.addElectricalComponent(new Resistor(g.pos, out).setR(HV_RS));
                    h.sim.addElectricalComponent(new Resistor(out, null).setR(rLoad));
                    h.sim.mna.addProcess(g);
                    h.ticks(3);
                    u[legacy] = g.src.getU();
                }
                assertEquals(u[1], u[0], 1e-12 * Math.abs(u[1])); // same formula; last-ulp differences only
            }
        }
    }

    /** Motoring from a stiff 3.2 kV supply: legacy vs fixed (fixed is capped at 0.5 nominalP). */
    @Test
    public void stiffSupplyMotoring() {
        for (double rSupply : new double[]{0.01, 1, 10, 100}) {
            for (int legacy = 1; legacy >= 0; legacy--) {
                h = new SimHarness();
                ElectricalLoad emf = load(0);
                h.sim.addElectricalComponent(new VoltageSource("supply", emf, null).setU(3200));
                Generator g = new Generator();
                g.legacy = legacy == 1;
                g.rads = 189;
                g.pos = load(HV_RS);
                h.sim.addElectricalComponent(new Resistor(emf, g.pos).setR(rSupply));
                g.src = new VoltageSource("generator", g.pos, null);
                h.sim.addElectricalComponent(g.src);
                h.sim.mna.addProcess(g);
                h.ticks(1);
                System.out.println(String.format("stiff supply R=%.2f %s: gen U=%.1f P=%.1f", rSupply,
                    legacy == 1 ? "legacy" : "fixed ", g.src.getU(), g.src.getP()));
                if (legacy == 0) assertEquals(-2000, g.src.getP(), 1);
            }
        }
    }

    /** Other getTh users: two constant-power sources, one behind an ideal 1:16, both into one load. */
    @Test
    public void twoPowerSourcesThroughIdealTransformer() {
        h = new SimHarness();
        ElectricalLoad a = load(0.05), as = load(0.05), b = load(0.1), load = load(0.1);
        PowerSource pa = new PowerSource("pa", a);
        pa.setP(500); pa.setUmax(300); pa.setImax(100);
        h.sim.addElectricalComponent(pa);
        connect(a, as);
        ElectricalLoad hv = load(0.1);
        Transformer x = new Transformer(as, hv);
        x.setRatio(16);
        h.sim.addElectricalComponent(x);
        connect(hv, load);
        PowerSource pb = new PowerSource("pb", b);
        pb.setP(1000); pb.setUmax(5000); pb.setImax(100);
        h.sim.addElectricalComponent(pb);
        connect(b, load);
        h.sim.addElectricalComponent(new Resistor(load, null).setR(3200.0 * 3200 / 4000));
        h.seconds(2);
        System.out.println("two PowerSources: pa P=" + pa.getP() + " eff=" + pa.getEffectiveP() + " pb eff=" + pb.getEffectiveP()
            + " load U=" + load.getU());
        assertEquals(500, pa.getEffectiveP(), 5);
        assertEquals(1000, pb.getEffectiveP(), 10);
    }

    /** Two generators in one SubSystem (one generating, one motoring, e.g. a flywheel), through an ideal 1:4 and
     *  through an isolating one: both obey P = k (E - U) and agree. Shafts held at fixed speed. */
    @Test
    public void generatorDrivesGenerator() {
        double[][] res = new double[2][];
        for (int iso = 0; iso < 2; iso++) {
            h = new SimHarness();
            isoProcs.clear();
            Generator a = new Generator(), b = new Generator();
            a.rads = 200; b.rads = 787.5; // E 800 V (3200 V reflected) and 3150 V
            a.pos = load(HV_RS); b.pos = load(HV_RS);
            a.src = new VoltageSource("a", a.pos, null); b.src = new VoltageSource("b", b.pos, null);
            h.sim.addElectricalComponent(a.src);
            h.sim.addElectricalComponent(b.src);
            ElectricalLoad ap = load(HV_RS), bs = load(HV_RS);
            h.sim.addElectricalComponent(new Resistor(a.pos, ap).setR(HV_RS));
            h.sim.addElectricalComponent(new Resistor(b.pos, bs).setR(HV_RS));
            // a is on the 1:4 primary, b on the secondary: a drives b as a motor (below the 2 kW motoring limit).
            ElectricalLoad xp = load(HV_RS), xs = load(HV_RS);
            connect(ap, xp);
            connect(xs, bs);
            transformer(xp, xs, iso == 1);
            h.sim.mna.addProcess(a);
            h.sim.mna.addProcess(b);
            for (IRootSystemPreStepProcess p : isoProcs) h.sim.mna.addProcess(p);
            h.seconds(10); // ideal settles within 1 s; the isolating coupling needs a few (6.9 kW on tick 1)
            double pa = a.src.getP(), pb = b.src.getP();
            System.out.println((iso == 1 ? "isolating" : "ideal    ") + " gen->gen: a U=" + a.src.getU() + " P=" + pa
                + " | b U=" + b.src.getU() + " P=" + pb);
            assertEquals(GEN_K * (800 - a.src.getU()), pa, 0.01 * Math.abs(pa));
            assertEquals(GEN_K * (3150 - b.src.getU()), pb, 0.01 * Math.abs(pb));
            assertTrue(pa > 0 && pb < 0 && pb > -2000);
            res[iso] = new double[]{pa, pb};
        }
        assertEquals(res[1][0], res[0][0], 0.02 * Math.abs(res[0][0]));
        assertEquals(res[1][1], res[0][1], 0.02 * Math.abs(res[0][1]));
    }
}
