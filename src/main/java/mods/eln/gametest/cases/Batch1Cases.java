package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.current;
import static mods.eln.gametest.cases.Wp9aCases.link;
import static mods.eln.gametest.cases.Wp9aCases.load;
import static mods.eln.gametest.cases.Wp9aCases.orient;

/**
 * P1 batch 1 (ground, electrical source, LV cable, power resistor), ported from the 1.12 selftest
 * (legacy selftest/cases/Wp9aCases.java: ResistorCase and its helpers). Same circuit, same checks.
 */
public final class Batch1Cases {
    private Batch1Cases() {
    }

    static final int GROUND = 0 + (2 << 6);
    static final int SOURCE = 0 + (3 << 6);
    static final int LV_CABLE = 4 + (32 << 6);
    static final int POWER_RESISTOR = 36 + (96 << 6);
    static final double U = 50.0;

    public static List<TestCase> cases() {
        return List.of(new ResistorCase(), new CableChainCase(), new BatteryCase());
    }

    // ---- helpers (1.12 Wp9aCases; orient, load, link and current are Wp9aCases') ----

    /** Power Resistor with n items in the core slot, terminals along X. */
    static ResistorElement resistor(TestContext ctx, int x, int z, int n) {
        return Wp9aCases.resistor(ctx, POWER_RESISTOR, x, z, n);
    }

    /** Source 50 V -> Power Resistor (48 core items = 100 ohm, E12) -> ground: I = U / R_total, plus heating. */
    static final class ResistorCase implements TestCase {
        ElectricalSourceElement src;
        ResistorElement r;
        SixNodeElement gnd;
        long t0;

        public String id() {
            return "batch1_power_resistor";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            src = ctx.source(SOURCE, ctx.at(0, 1, 1), U);
            r = resistor(ctx, 1, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(2, 1, 1));
            t0 = ctx.level().getGameTime();
        }

        public void measure(TestContext ctx) {
            ResistorDescriptor d = (ResistorDescriptor) Eln.sixNodeItem.getDescriptor(POWER_RESISTOR);
            ctx.checkValue("resistor R from 48 core items [ohm] (E12: 0.01*10^4)", r.nominalRs, 100.0);
            double rt = link(src, r) + r.nominalRs + link(r, gnd);
            double i = U / rt;
            double iMeas = current(r);
            ctx.checkValue("resistor current [A]", iMeas, i);
            ctx.checkValue("resistor voltage drop [V]", load(r, Direction.XN).getU() - load(r, Direction.XP).getU(), i * r.nominalRs);
            double p = iMeas * iMeas * r.nominalRs;
            double t = (ctx.level().getGameTime() - t0) * 0.05;
            double c = d.thermalMaximalPowerDissipated * d.thermalNominalHeatTime / d.thermalWarmLimit;
            double tc = r.getThermalLoad(LRDU.Up).Tc;
            double bound = p * t / c;
            ctx.check("resistor heating", tc > 0 && tc <= bound * 1.01 + 1e-6,
                String.format("T = %.4f C above ambient, expected 0 < T <= %.4f C", tc, bound));
        }
    }

    /**
     * 1.12 base selftest circuit, without lamp and battery (not ported yet): source 50 V -> LV cable -> LV cable ->
     * Power Resistor 100 ohm -> ground. Every node voltage on the chain: U drops by I * (link resistances) along it.
     */
    static final class CableChainCase implements TestCase {
        ElectricalSourceElement src;
        SixNodeElement c1, c2, gnd;
        ResistorElement r;

        public String id() {
            return "batch1_cable_chain";
        }

        public int width() {
            return 8;
        }

        public void build(TestContext ctx) {
            src = ctx.source(SOURCE, ctx.at(0, 1, 1), U);
            c1 = ctx.placeSix(LV_CABLE, ctx.at(1, 1, 1));
            c2 = ctx.placeSix(LV_CABLE, ctx.at(2, 1, 1));
            r = resistor(ctx, 3, 1, 48);
            gnd = ctx.placeSix(GROUND, ctx.at(4, 1, 1));
        }

        public void measure(TestContext ctx) {
            double rt = link(src, c1) + link(c1, c2) + link(c2, r) + r.nominalRs + link(r, gnd);
            double i = U / rt;
            ctx.checkValue("cable chain current [A]", current(r), i);
            ctx.checkValue("cable 1 voltage [V]", load(c1, Direction.XN).getU(), U - i * (load(src, Direction.XP).getRs() + load(c1, Direction.XN).getRs()));
            ctx.checkValue("cable 2 voltage [V]", load(c2, Direction.XN).getU(), U - i * (link(src, c1) + link(c1, c2)));
        }
    }

    static final int BATTERY = 0 + (16 << 6); // Cost Oriented Battery

    /** 1.12 base selftest, battery part: open circuit U = voltageFunction(startCharge) * U nominal; charge = start. */
    static final class BatteryCase implements TestCase {
        mods.eln.transparentnode.battery.BatteryElement battery;

        public String id() {
            return "batch1_battery";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            battery = (mods.eln.transparentnode.battery.BatteryElement) ctx.placeTransparent(BATTERY, ctx.at(1, 1, 1));
        }

        public void measure(TestContext ctx) {
            mods.eln.transparentnode.battery.BatteryDescriptor bd = battery.descriptor;
            double expectedU = battery.batteryProcess.voltageFunction.getValue(bd.startCharge) * battery.batteryProcess.uNominal;
            ctx.checkValue("battery open-circuit voltage [V]", battery.positiveLoad.getU() - battery.negativeLoad.getU(), expectedU);
            ctx.check("battery charge", Math.abs(battery.batteryProcess.getCharge() - bd.startCharge) < 0.01,
                String.format("%.4f (item start charge %.4f)", battery.batteryProcess.getCharge(), bd.startCharge));
        }
    }

}
