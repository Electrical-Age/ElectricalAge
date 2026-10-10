package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FuelRegistry;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.mechanical.TurbineElement;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

import static mods.eln.selftest.cases.Wp11Cases.*;

/**
 * Shaft parts without a case in Wp11: tachometer, joint hub, gas turbine. Port step T3 (notes/port26/PLAN.md).
 * Passive shaft parts have no drag, so a speed set at build time holds unless something adds or takes energy.
 */
public final class ShaftMoreCases {
    private ShaftMoreCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new TachometerCase());
        cases.add(new JointHubCase());
        cases.add(new GasTurbineCase(800, true));
        cases.add(new GasTurbineCase(300, false));
    }

    /** Flywheel + tachometer at 500 rad/s: output = (rads - 0) / (1000 - 0) * SVU = SVU / 2 (default range). */
    static final class TachometerCase implements SelfTestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "shaft tachometer";
        }

        public void build(SelfTestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, Ids.transparent("Tachometer"));
            shaft(parts[0]).setRads(500.0);
        }

        public void measure(SelfTestContext ctx) {
            TransparentNodeElement t = parts[1];
            double u = t.getElectricalLoad(t.front, LRDU.Down).getU();
            ctx.check("shaft tachometer on the flywheel's shaft", shaft(t) == shaft(parts[0]), "");
            Wp9cCases.within(ctx, "shaft tachometer output at 500 rad/s [V] (500/1000 * SVU)", u, 0.5 * Eln.SVU, 0.01 * Eln.SVU);
        }
    }

    /** Flywheel + joint hub: one shaft network, so the hub turns at the flywheel's speed. */
    static final class JointHubCase implements SelfTestCase {
        TransparentNodeElement[] parts;

        public String name() {
            return "shaft joint hub";
        }

        public void build(SelfTestContext ctx) {
            parts = shaftLine(ctx, FLYWHEEL, Ids.transparent("Joint hub"));
            shaft(parts[0]).setRads(200.0);
        }

        public void measure(SelfTestContext ctx) {
            ctx.check("shaft joint hub joins the flywheel's shaft", shaft(parts[1]) == shaft(parts[0]),
                String.format("hub %.1f rad/s, flywheel %.1f rad/s", shaft(parts[1]).getRads(), shaft(parts[0]).getRads()));
        }
    }

    /**
     * Gas turbine + flywheel with its 4 mB tank full of gasoline. At 800 rad/s (optimal) it burns fuel and speeds the
     * shaft up; at 300 rad/s it is below its efficiency cutoff (0.5): no fuel used, speed unchanged.
     */
    static final class GasTurbineCase implements SelfTestCase {
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

        public void build(SelfTestContext ctx) {
            parts = shaftLine(ctx, Ids.transparent("Gas Turbine"), FLYWHEEL);
            shaft(parts[0]).setRads(rads);
            Fluid[] fuels = FuelRegistry.INSTANCE.fluidListToFluids(FuelRegistry.INSTANCE.getGasolineList());
            if (fuels.length == 0) return;
            fluid = fuels[0].getName();
            filled = ((TurbineElement) parts[0]).getTank().fill(new FluidStack(fuels[0], 4), true);
        }

        public void measure(SelfTestContext ctx) {
            String what = "shaft gas turbine at " + (int) rads + " rad/s";
            if (filled == 0) {
                ctx.check(what + ": tank takes gasoline", false, "fluid " + fluid + ", filled " + filled + " mB");
                return;
            }
            FluidStack left = ((TurbineElement) parts[0]).getTank().drain(Integer.MAX_VALUE, false);
            int mb = left == null ? 0 : left.amount;
            double now = shaft(parts[0]).getRads();
            if (burns) {
                ctx.check(what + ": burns fuel", mb < filled, String.format("%s %d -> %d mB", fluid, filled, mb));
                ctx.check(what + ": speeds the shaft up", now > rads, String.format("%.2f rad/s", now));
            } else {
                ctx.check(what + ": below cutoff, no fuel used", mb == filled, String.format("%s %d -> %d mB", fluid, filled, mb));
                ctx.check(what + ": speed unchanged", Math.abs(now - rads) < 1e-6, String.format("%.4f rad/s", now));
            }
        }
    }
}
