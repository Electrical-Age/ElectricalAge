package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.six.SixNodeElement;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.GROUND;
import static mods.eln.gametest.cases.Wp9aCases.LV_CABLE;
import static mods.eln.gametest.cases.Wp9aCases.SOURCE;
import static mods.eln.gametest.cases.Wp9aCases.source;

/**
 * Failure of a cable run (26.x, Baughn's playtest): a fault through a chain of identical cables burns one of them, the
 * weakest link, not the whole chain. Electrical Source -> 6 Low Voltage Cables -> Ground Cable along +X.
 * - overheat: 60 V (under the voltage limit) straight into ground: all cables carry the same current and heat alike;
 *   one burns (Simulator: one failure per network per tick, the rest relieved).
 * - 1000 V on LV cable (limit 65 V): during the short the run sits far below 65 V, so one cable burns from overheat as
 *   above; then the cables between it and the source sit at 1000 V with no current and burn one by one from
 *   overvoltage (the 1.12 rule for cables). Reported, not checked, until Baughn decides whether that rule stays.
 */
public final class BurnChainCases {
    private BurnChainCases() {
    }

    static final int N = 6;

    public static void addTo(List<TestCase> cases) {
        cases.add(new ChainCase("burn chain overvoltage", 1000, 400, false));
        cases.add(new ChainCase("burn chain overheat", 60, 1200, true));
    }

    static final class ChainCase implements TestCase {
        final String name;
        final double u;
        final int ticks;
        final boolean check;
        final List<SixNodeElement> cables = new ArrayList<>();

        ChainCase(String name, double u, int ticks, boolean check) {
            this.name = name;
            this.u = u;
            this.ticks = ticks;
            this.check = check;
        }

        public String name() {
            return name;
        }

        public int ticks() {
            return ticks;
        }

        public void build(TestContext ctx) {
            source(ctx, SOURCE, 0, 1, u);
            for (int i = 1; i <= N; i++) cables.add(ctx.placeSix(LV_CABLE, ctx.at(i, 1, 1)));
            ctx.placeSix(GROUND, ctx.at(N + 1, 1, 1));
        }

        public void measure(TestContext ctx) {
            StringBuilder s = new StringBuilder();
            int burnt = 0;
            for (SixNodeElement c : cables) {
                boolean b = c.isBurnt();
                if (b) burnt++;
                s.append(b ? 'X' : '-');
            }
            ctx.line(name + ": cables (source side first): " + s);
            if (check) ctx.check(name + ": exactly one cable of " + N + " burns", burnt == 1, "cables (source side first): " + s);
        }
    }
}
