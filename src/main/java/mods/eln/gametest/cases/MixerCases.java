package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.oil.PipeMixerElement;
import mods.eln.oil.TankElement;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;

import java.util.List;

/**
 * The Pipe Mixer (Baughn 2026-11-11: a signal-controlled pipe T of two steerable check valves, not molar valves): water
 * mains on its inlets A (left) and B (right), a drain (a big vessel of air) on its outlet.
 * <ul>
 * <li>a_only: signal 1: only A flows;</li>
 * <li>b_only: signal 0: only B flows;</li>
 * <li>half: signal 0.5, both mains at 2 bar: about half each (within 15 %);</li>
 * <li>b_stronger: signal 0.5, B at 4 bar and A at 1 bar: more comes from B (the mix follows the pressures);</li>
 * <li>no_backflow: signal 1, A at 1 bar, the outlet a 3 bar main: nothing goes back into A.</li>
 * </ul>
 */
public final class MixerCases {
    private MixerCases() {
    }

    public static void addTo(List<TestCase> cases) {
        for (String v : Mix.VARIANTS) cases.add(new Mix(v));
    }

    static final int PIPE_MIXER = 60 + (82 << 6);

    static final class Mix implements TestCase {
        static final String[] VARIANTS = {"a_only", "b_only", "half", "b_stronger", "no_backflow"};
        final String variant;
        PipeMixerElement m;
        FluidDevice fx;
        FluidNode mainA, mainB, outlet;
        double a0, b0, o0, aW, bW;
        mods.eln.sim.IProcess clock;
        int tick;

        Mix(String variant) {
            this.variant = variant;
        }

        public String id() {
            return "pipe_mixer_" + variant;
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 300;
        }

        /** The port on face f (the mixer's) as a fixture's: the neighbour's face back, the same spot. */
        static Object[] on(PipeMixerElement m, Direction f, FluidNode n) {
            var pos = m.node.coordonate.pos();
            int face = f.toEnumFacing().get3DDataValue();
            return new Object[]{pos.relative(f.toEnumFacing()), f.toEnumFacing().getOpposite().get3DDataValue(), n, TankElement.sideSpot(face, PipeMixerElement.LINE_Y)};
        }

        public void build(TestContext ctx) {
            m = (PipeMixerElement) ctx.placeTransparent(PIPE_MIXER, ctx.at(2, 1, 2));
            m.front = Direction.XN;
            m.reconnect();
            int y = ctx.at(2, 1, 2).getY();
            double pa = 0, pb = 0, po = -1;
            switch (variant) {
                case "a_only" -> { m.testSignal = 1; pa = pb = 2e5; }
                case "b_only" -> { m.testSignal = 0; pa = pb = 2e5; }
                case "half" -> { m.testSignal = 0.5; pa = pb = 2e5; }
                case "b_stronger" -> { m.testSignal = 0.5; pa = 1e5; pb = 4e5; }
                case "no_backflow" -> { m.testSignal = 1; pa = 1e5; pb = 1e5; po = 3e5; }
            }
            mainA = GasPlantCases.water(y, Water.P_ATM + pa, 293.15);
            mainB = GasPlantCases.water(y, Water.P_ATM + pb, 293.15);
            outlet = po < 0 ? GasPlantCases.air(y) : GasPlantCases.water(y, Water.P_ATM + po, 293.15);
            fx = GasPlantCases.fixtures(m.fluidLevel(), on(m, m.front.left(), mainA), on(m, m.front.right(), mainB), on(m, m.front.back(), outlet));
            mods.eln.Eln.simulator.addSlowProcess(clock = dt -> {
                if (++tick == 100) {
                    a0 = mainA.mWater;
                    b0 = mainB.mWater;
                    o0 = outlet.mWater;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(clock);
            mods.eln.fluid.FluidWorld.remove(fx);
            double secs = (tick - 100) / 20.0, fa = (a0 - mainA.mWater) / secs, fb = (b0 - mainB.mWater) / secs;
            String what = String.format("from A %.2f g/s, from B %.2f g/s, outlet %+.2f g/s; %s", fa * 1000, fb * 1000, (outlet.mWater - o0) / secs * 1000, m.getWaila());
            ctx.line(variant + ": " + what);
            switch (variant) {
                case "a_only" -> ctx.check("pipe mixer: signal 1 lets only A through", fa > 1e-3 && Math.abs(fb) < 0.01 * fa, what);
                case "b_only" -> ctx.check("pipe mixer: signal 0 lets only B through", fb > 1e-3 && Math.abs(fa) < 0.01 * fb, what);
                case "half" -> ctx.check("pipe mixer: signal 0.5, equal mains: about half each (within 15 %)", fa > 1e-3 && Math.abs(fa - fb) < 0.15 * (fa + fb) / 2, what);
                case "b_stronger" -> ctx.check("pipe mixer: signal 0.5, B's main stronger: more from B", fb > 1.3 * fa && fa > 0, what);
                case "no_backflow" -> ctx.check("pipe mixer: a stronger outlet sends nothing back into A (a check valve)", fa >= -1e-6 && Math.abs(fb) < 1e-6, what);
            }
        }
    }
}
