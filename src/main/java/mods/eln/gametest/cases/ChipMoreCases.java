package mods.eln.gametest.cases;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static mods.eln.gametest.cases.Wp9aCases.orient;
import static mods.eln.gametest.cases.Wp9cCases.*;

/**
 * Logic and analog chips without a case elsewhere: Schmitt trigger, D and JK flip-flops, OpAmp, sample and hold,
 * lowpass filter, sawtooth/sine VCOs. Port step T3 (notes/port26/PLAN.md). Lanes along +X at z=1, 4 blocks each:
 * Signal Source on the back pin (input 0), chip with its output +X into a signal cable, optional sources on the left
 * (input 1) and right (input 2) pins. Not here: the oscillator chip (its toggle rate depends on the process rate) and
 * the PID regulator (closed loop).
 */
public final class ChipMoreCases {
    private ChipMoreCases() {
    }

    /** One chip lane: inputs on the back/left/right pins (NaN = none), expected output as a function of run time t [s]. */
    static final class Lane {
        final String chip, what;
        final double back, left, right, tol;
        final DoubleUnaryOperator expected;

        Lane(String chip, String what, double back, double left, double right, DoubleUnaryOperator expected, double tol) {
            this.chip = chip;
            this.what = what;
            this.back = back;
            this.left = left;
            this.right = right;
            this.expected = expected;
            this.tol = tol;
        }
    }

    static Lane lane(String chip, String what, double back, double left, double right, double expected) {
        return new Lane(chip, what, back, left, right, t -> expected, 0.01 * SVU);
    }

    static final double N = Double.NaN;

    /** Sawtooth VCO at 0 V input: 1.12's 0.04 Hz, +2 V/s x SVU / 50 from 0, wrapping at SVU. */
    static double saw(double t) {
        return (2 * SVU / 50 * t) % SVU;
    }

    /**
     * Schmitt trigger (0 below 0.2 SVU, 1 from 0.6 SVU, else unchanged from 0). D flip-flop (D = input 0, clock =
     * input 1; a constant high clock is one rising edge at start). JK (clock = input 0, J = 1, K = 2; toggle from 0).
     * OpAmp 10000 * (in0 - in1), clamped to 0..SVU. Sample and hold (clock = input 1, high from the start: samples
     * once). Lowpass filter (5 Hz): settled. Sawtooth VCO at 0 V: 2 V/s x SVU / 50; sine VCO: centred on SVU / 2, full scale,
     * one period per sawtooth ramp.
     */
    static final Lane[] LANES = {
        lane("Schmitt Trigger Chip", "Schmitt(0.8 SVU)", 0.8 * SVU, N, N, SVU),
        lane("Schmitt Trigger Chip", "Schmitt(0.1 SVU)", 0.1 * SVU, N, N, 0),
        lane("Schmitt Trigger Chip", "Schmitt(0.5 SVU, from 0)", 0.5 * SVU, N, N, 0),
        lane("D Flip Flop Chip", "D flip-flop D=1", SVU, SVU, N, SVU),
        lane("D Flip Flop Chip", "D flip-flop D=0", 0, SVU, N, 0),
        lane("JK Flip Flop Chip", "JK J=1 K=0", SVU, SVU, 0, SVU),
        lane("JK Flip Flop Chip", "JK J=0 K=1", SVU, 0, SVU, 0),
        lane("JK Flip Flop Chip", "JK J=1 K=1 (toggle)", SVU, SVU, SVU, SVU),
        lane("OpAmp", "OpAmp(0.6 SVU, 0.4 SVU) (saturated)", 0.6 * SVU, 0.4 * SVU, N, SVU),
        lane("OpAmp", "OpAmp(0.4 SVU, 0.6 SVU)", 0.4 * SVU, 0.6 * SVU, N, 0),
        lane("Sample and hold", "sample and hold 0.6 SVU", 0.6 * SVU, SVU, N, 0.6 * SVU),
        lane("Lowpass filter", "lowpass filter 0.6 SVU (settled)", 0.6 * SVU, N, N, 0.6 * SVU),
        new Lane("Voltage controlled sawtooth oscillator", "sawtooth VCO at 0 V (SVU / 25 per s)", 0, N, N, ChipMoreCases::saw, 0.006 * SVU),
        new Lane("Voltage controlled sine oscillator", "sine VCO at 0 V", 0, N, N,
            t -> SVU / 2 + SVU / 2 * Math.sin(2 * Math.PI * saw(t) / SVU), 0.02 * SVU),
    };

    public static void addTo(List<TestCase> cases) {
        for (int i = 0; i < LANES.length; i += 4)
            cases.add(new LanesCase(i / 4 + 1, Arrays.copyOfRange(LANES, i, Math.min(LANES.length, i + 4))));
        cases.add(new PalCase());
    }

    /**
     * PAL: only truth-table entry 7 set (GUI packet TruthTablePositionClickedEvent = 1 + int). The table is indexed
     * in0*4 + in2*2 + (in2 xor in1) (Gray-code columns of PalGui's grid), so entry 7 is (in0, in1, in2) = (1, 0, 1).
     * Lanes: (1,0,1) -> 1; (1,1,1) -> entry 6 -> 0; (0,0,1) -> entry 3 -> 0; (1,0,0) -> entry 4 -> 0.
     */
    static final class PalCase implements TestCase {
        static final double[][] IN = {{SVU, 0, SVU}, {SVU, SVU, SVU}, {0, 0, SVU}, {SVU, 0, 0}};
        static final double[] EXPECT = {SVU, 0, 0, 0};
        final SixNodeElement[] pals = new SixNodeElement[4];

        public String name() {
            return "chips PAL";
        }

        public int width() {
            return 16;
        }

        public void build(TestContext ctx) {
            for (int i = 0; i < 4; i++) {
                pals[i] = chip(ctx, lane("PAL Chip", "PAL", IN[i][0], IN[i][1], IN[i][2], 0), 1 + 4 * i);
                pals[i].networkUnserialize(ctx.stream(o -> {
                    o.writeByte(1); // PalElement.TruthTablePositionClickedEvent
                    o.writeInt(7);
                }));
            }
        }

        public void measure(TestContext ctx) {
            for (int i = 0; i < 4; i++)
                within(ctx, String.format("chip PAL (only entry 7 set) inputs (%d, %d, %d) output [V]",
                    IN[i][0] > 0 ? 1 : 0, IN[i][1] > 0 ? 1 : 0, IN[i][2] > 0 ? 1 : 0), out(pals[i]), EXPECT[i], 0.01 * SVU);
        }
    }

    static SixNodeElement chip(TestContext ctx, Lane l, int x) {
        if (!Double.isNaN(l.back)) signal(ctx, ctx.at(x - 1, 1, 1), l.back);
        BlockPos p = ctx.at(x, 1, 1);
        SixNodeElement c = ctx.placeSix(Ids.six(l.chip), p);
        orient(c, f -> f, Direction.XP);
        if (!Double.isNaN(l.left)) signal(ctx, beside(c, p, c.front.left()), l.left);
        if (!Double.isNaN(l.right)) signal(ctx, beside(c, p, c.front.right()), l.right);
        ctx.placeSix(SIGNAL_CABLE, ctx.at(x + 1, 1, 1));
        return c;
    }

    /** Up to 4 lanes along +X at z=1, 4 blocks apart (the row is 16 long, 3 deep: left/right inputs at z=0/2). */
    static final class LanesCase implements TestCase {
        final int n;
        final Lane[] lanes;
        final SixNodeElement[] chips;
        long t0;

        LanesCase(int n, Lane[] lanes) {
            this.n = n;
            this.lanes = lanes;
            this.chips = new SixNodeElement[lanes.length];
        }

        public String name() {
            return "chips with state " + n;
        }

        public int width() {
            return 16;
        }

        public void build(TestContext ctx) {
            for (int i = 0; i < lanes.length; i++) chips[i] = chip(ctx, lanes[i], 1 + 4 * i);
            t0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            double t = elapsed(ctx, t0);
            for (int i = 0; i < lanes.length; i++)
                within(ctx, "chip " + lanes[i].what + " output [V]", out(chips[i]), lanes[i].expected.applyAsDouble(t), lanes[i].tol);
        }
    }
}
