package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.BatteryProcess;
import mods.eln.transparentnode.battery.BatteryElement;

import java.util.List;

/**
 * The lead-acid curve (Baughn 2026-11-22: "a more accurate lead-acid voltage curve ... float/absorption included"). A
 * Cost Oriented Battery (48 V) on a held line: at the float voltage (54 V) it settles near 97 % and takes almost
 * nothing; at the absorption voltage (57.6 V) it fills; over that, charge past full gasses as heat instead of storing:
 * held at 60 V it heats fast (~330 W) and burns of heat within seconds, not on the spot. A 12 V Capacity Oriented Battery on a 48 V line burns at once.
 */
public final class BatteryCurveCases {
    private BatteryCurveCases() {
    }

    static final int COST = 16 << 6, CAPACITY = 1 + (16 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new Held("battery_floats_at_54v", COST, BatteryProcess.FLOAT * 48, 0.95, 2400));
        cases.add(new Held("battery_fills_at_57v", COST, 1.2 * 48, 0.95, 2400));
        cases.add(new Held("battery_gasses_past_full", COST, 60, 0.999, 300));
        cases.add(new Held("battery_12v_on_48v_burns", CAPACITY, 48, 0.5, 3));
    }

    static final class Held implements TestCase {
        final String id;
        final int damage;
        final double volts, start;
        final int ticks;
        BatteryElement battery;
        double t0;

        Held(String id, int damage, double volts, double start, int ticks) {
            this.id = id;
            this.damage = damage;
            this.volts = volts;
            this.start = start;
            this.ticks = ticks;
        }

        public String id() {
            return id;
        }

        public int ticks() {
            return ticks;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            battery = (BatteryElement) l.device(damage, 3, 1);
            battery.batteryProcess.setCharge(start);
            t0 = battery.thermalLoad.Tc;
            SixNodeElement[] plus = l.feed(ctx.at(3, 1, 1), battery.front.left());
            ctx.setVolts(plus[0], volts);
            SixNodeElement[] minus = l.feed(ctx.at(3, 1, 1), battery.front.right());
            ctx.setVolts(minus[0], 0f);
        }

        public void measure(TestContext ctx) {
            double q = battery.batteryProcess.getCharge(), i = -battery.batteryProcess.getDischargeCurrent();
            double rated = battery.descriptor.electricalStdI, dT = battery.thermalLoad.Tc - t0;
            ctx.line(String.format("%s: %.1f V held, charge %.4f -> %.4f, charging %.3f A (rated %.2f), EMF %.2f V, +%.1f K, burnt %s",
                id, volts, start, q, i, rated, battery.batteryProcess.getU(), dT, battery.isBurnt()));
            switch (id) {
                case "battery_floats_at_54v" -> {
                    ctx.check("float: it settles near 97 % and no higher", q > 0.965 && q < 0.977, String.format("%.4f", q));
                    ctx.check("float: it takes almost nothing once settled", i < 0.1 * rated, String.format("%.3f A", i));
                    ctx.check("float: unharmed", !battery.isBurnt(), "");
                }
                case "battery_fills_at_57v" -> {
                    ctx.check("absorption: it fills", q > 0.99, String.format("%.4f", q));
                    ctx.check("absorption: the current tapers as it fills", i < 0.25 * rated, String.format("%.3f A", i));
                    ctx.check("absorption: unharmed", !battery.isBurnt(), "");
                }
                case "battery_gasses_past_full" -> {
                    ctx.check("gassing: charge past full doesn't store", q <= 1.0 + 1e-9, String.format("%.6f", q));
                    ctx.check("gassing: a small current, against the gas's overpotential", i > 0.005 * rated && i < 0.05 * rated, String.format("%.3f A", i));
                    ctx.check("gassing: the overpotential holds the EMF over absorption", battery.batteryProcess.getU() > 1.2 * 48 + 0.5, String.format("%.2f V", battery.batteryProcess.getU()));
                    ctx.check("gassing: it warms", dT > 0.5, String.format("+%.2f K", dT));
                    ctx.check("gassing: 15 s in, it hasn't burnt", !battery.isBurnt(), "");
                }
                default -> ctx.check("a 12 V battery on a 48 V line burns at once (3 ticks, before its heat could)", battery.isBurnt(), "");
            }
        }
    }
}
