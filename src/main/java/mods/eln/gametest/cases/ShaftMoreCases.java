package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FuelRegistry;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

import static mods.eln.gametest.cases.Wp11Cases.*;

/**
 * Shaft parts without a case in Wp11: tachometer, joint hub, gas turbine. Port step T3 (notes/port26/PLAN.md).
 * Passive shaft parts have no drag, so a speed set at build time holds unless something adds or takes energy.
 */
public final class ShaftMoreCases {
    private ShaftMoreCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TachometerCase());
        cases.add(new JointHubCase());
        cases.add(new GasTurbineCase(800, true));
        cases.add(new GasTurbineCase(300, false));
        cases.add(new SplitKeepsSpeed());
    }

    /** Flywheel + tachometer at 500 rad/s: output = (rads - 0) / (1000 - 0) * SVU = SVU / 2 (default range). */
    static final class TachometerCase implements TestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "shaft tachometer";
        }

        public void build(TestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, Ids.transparent("Tachometer"));
            shaft(parts[0]).setRads(500.0);
        }

        public void measure(TestContext ctx) {
            TransparentNodeElement t = parts[1];
            double u = t.getElectricalLoad(t.front, LRDU.Down).getU();
            ctx.check("shaft tachometer on the flywheel's shaft", shaft(t) == shaft(parts[0]), "");
            Wp9cCases.within(ctx, "shaft tachometer output at 500 rad/s [V] (500/1000 * SVU)", u, 0.5 * Eln.SVU, 0.01 * Eln.SVU);
        }
    }

    /**
     * Cutting a spinning shaft in two: both halves keep turning at its speed (the half that got a new network used to
     * start at 0 rad/s; AGE's 4fd31c68 found it).
     */
    static final class SplitKeepsSpeed implements TestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "shaft cut in two keeps its speed";
        }

        public void build(TestContext ctx) {
            parts = shaftLine(ctx, JOINT, FLYWHEEL, FLYWHEEL);
            shaft(parts[0]).setRads(300.0);
            ctx.level().removeBlock(parts[0].node.coordonate.pos(), false);   // the joint in the middle
        }

        public void measure(TestContext ctx) {
            var a = shaft(parts[1]);
            var b = shaft(parts[2]);
            ctx.check("cut shaft: two networks", a != b, "");
            ctx.check("cut shaft: both halves still at 300 rad/s", Math.abs(a.getRads() - 300) < 1e-6 && Math.abs(b.getRads() - 300) < 1e-6,
                String.format("%.1f and %.1f rad/s", a.getRads(), b.getRads()));
        }
    }

    /** Flywheel + joint hub: one shaft network, so the hub turns at the flywheel's speed. */
    static final class JointHubCase implements TestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "shaft joint hub";
        }

        public void build(TestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, Ids.transparent("Joint hub"));
            shaft(parts[0]).setRads(200.0);
        }

        public void measure(TestContext ctx) {
            ctx.check("shaft joint hub joins the flywheel's shaft", shaft(parts[1]) == shaft(parts[0]),
                String.format("hub %.1f rad/s, flywheel %.1f rad/s", shaft(parts[1]).getRads(), shaft(parts[0]).getRads()));
        }
    }

    /**
     * Gas turbine + flywheel with a bucket of gasoline in its tank. At 800 rad/s it burns fuel (keeping the flywheel
     * near its speed); at 300 rad/s it is below its cutoff (400): no flame, no fuel used, it only slows (windage).
     */
    static final class GasTurbineCase implements TestCase {
        final double rads;
        final boolean burns;
        TransparentNodeElement[] parts;
        int filled;
        String fluid = "none";

        GasTurbineCase(double rads, boolean burns) {
            this.rads = rads;
            this.burns = burns;
        }

        public String name() {
            return "shaft gas turbine " + (int) rads + " rad/s";
        }

        public void build(TestContext ctx) {
            parts = shaftLine(ctx, Ids.transparent("Gas Turbine"), FLYWHEEL);
            shaft(parts[0]).setRads(rads);
            Fluid[] fuels = FuelRegistry.INSTANCE.fluidListToFluids(FuelRegistry.INSTANCE.getGasolineList());
            if (fuels.length == 0) return;
            fluid = FuelRegistry.INSTANCE.fluidName(fuels[0]);
            filled = ((mods.eln.mechanical.GasTurbineElement) parts[0]).tank.fill(new FluidStack(fuels[0], 1000), true);
        }

        public void measure(TestContext ctx) {
            String what = "shaft gas turbine at " + (int) rads + " rad/s";
            if (filled == 0) {
                ctx.check(what + ": tank takes gasoline", false, "fluid " + fluid + ", filled " + filled + " mB");
                return;
            }
            FluidStack left = ((mods.eln.mechanical.GasTurbineElement) parts[0]).tank.drain(Integer.MAX_VALUE, false);
            int mb = left == null ? 0 : left.getAmount();
            double now = shaft(parts[0]).getRads();
            if (burns) {
                ctx.check(what + ": burns fuel", mb < filled, String.format("%s %d -> %d mB", fluid, filled, mb));
                ctx.check(what + ": holds the shaft near its speed (over 790 rad/s)", now > 790, String.format("%.2f rad/s", now));
            } else {
                ctx.check(what + ": below cutoff, no fuel used", mb == filled, String.format("%s %d -> %d mB", fluid, filled, mb));
                ctx.check(what + ": no power: it only slows (windage)", now <= rads, String.format("%.4f rad/s", now));
            }
        }
    }
}
