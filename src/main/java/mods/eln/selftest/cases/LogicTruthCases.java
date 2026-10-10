package mods.eln.selftest.cases;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.BinaryOperator;

import static mods.eln.selftest.cases.Wp9aCases.orient;
import static mods.eln.selftest.cases.Wp9cCases.*;

/**
 * Full truth tables of the two-input logic chips (Wp9c's LogicCase checks one row of AND, XOR and NOT). Per chip, four
 * lanes along +X at z=1: Signal Source (back input) -> chip (output +X) -> signal cable, second Signal Source on the
 * chip's left pin. Inputs 0 V / SVU; output SVU for 1, 0 V for 0. Port step T3 (notes/port26/PLAN.md).
 */
public final class LogicTruthCases {
    private LogicTruthCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new TruthCase("AND Chip", (a, b) -> a && b));
        cases.add(new TruthCase("NAND Chip", (a, b) -> !(a && b)));
        cases.add(new TruthCase("OR Chip", (a, b) -> a || b));
        cases.add(new TruthCase("NOR Chip", (a, b) -> !(a || b)));
        cases.add(new TruthCase("XOR Chip", (a, b) -> a ^ b));
        cases.add(new TruthCase("XNOR Chip", (a, b) -> !(a ^ b)));
        cases.add(new NotCase());
    }

    static SixNodeElement gate(SelfTestContext ctx, int damage, int x, double back, double left) {
        signal(ctx, ctx.at(x - 1, 1, 1), back);
        BlockPos p = ctx.at(x, 1, 1);
        SixNodeElement g = ctx.placeSix(damage, p);
        orient(g, l -> l, Direction.XP);
        if (left >= 0) signal(ctx, beside(g, p, g.front.left()), left);
        ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
        return g;
    }

    static final class TruthCase implements SelfTestCase {
        final String chip;
        final BinaryOperator<Boolean> fn;
        final SixNodeElement[] lanes = new SixNodeElement[4];

        TruthCase(String chip, BinaryOperator<Boolean> fn) {
            this.chip = chip;
            this.fn = fn;
        }

        public String name() {
            return "logic truth table " + chip;
        }

        public int width() {
            return 16;
        }

        public void build(SelfTestContext ctx) {
            int id = Ids.six(chip);
            for (int i = 0; i < 4; i++) lanes[i] = gate(ctx, id, 1 + 4 * i, (i & 1) != 0 ? SVU : 0, (i & 2) != 0 ? SVU : 0);
        }

        public void measure(SelfTestContext ctx) {
            for (int i = 0; i < 4; i++) {
                boolean a = (i & 1) != 0, b = (i & 2) != 0;
                double expected = fn.apply(a, b) ? SVU : 0;
                within(ctx, String.format("logic %s(%d, %d) output [V]", chip, a ? 1 : 0, b ? 1 : 0), out(lanes[i]), expected, 0.01 * SVU);
            }
        }
    }

    static final class NotCase implements SelfTestCase {
        SixNodeElement lo, hi;

        public String name() {
            return "logic truth table NOT Chip";
        }

        public int width() {
            return 8;
        }

        public void build(SelfTestContext ctx) {
            int id = Ids.six("NOT Chip");
            lo = gate(ctx, id, 1, 0, -1);
            hi = gate(ctx, id, 5, SVU, -1);
        }

        public void measure(SelfTestContext ctx) {
            within(ctx, "logic NOT Chip(0) output [V]", out(lo), SVU, 0.01 * SVU);
            within(ctx, "logic NOT Chip(1) output [V]", out(hi), 0, 0.01 * SVU);
        }
    }
}
