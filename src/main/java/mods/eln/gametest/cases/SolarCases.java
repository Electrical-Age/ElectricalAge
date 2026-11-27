package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.PvCurve;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.process.PvSourceBipole;
import mods.eln.transparentnode.solarpanel.SolarPanelElement;
import mods.eln.transparentnode.solarpanel.SolarPannelSlowProcess;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Solar panels: the first multiblock on 26.x (ghost blocks), ported from 1.12 Wp10bCases.SolarCase plus a ghost check
 * like the 1.12 teleporter/wind turbine ones.
 */
public final class SolarCases {
    private SolarCases() {
    }

    static int transparent(String name) {
        for (var e : Eln.transparentNodeItem.all().entrySet()) if (name.equals(e.getValue().name)) return e.getKey();
        throw new IllegalStateException("no transparent device " + name);
    }

    public static List<TestCase> cases() {
        return List.of(new SmallSolarCase(), new LoadedSolarCase(), new SolarGhostCase());
    }

    /**
     * The panel's diode curve follows its light and temperature (Baughn's option B+, board #79): the photocurrent is
     * the curve's short-circuit current at the panel's temperature x the light the panel's own sun/sky/obstruction
     * function gives (1.12 SolarCase's light). Also used by Wp10bCases.SolarCase.
     */
    static void checkCurve(TestContext ctx, SolarPanelElement panel, String label) {
        SolarPannelSlowProcess process = TestContext.field(panel, "slowProcess");
        PvSourceBipole pv = TestContext.field(panel, "pv");
        PvCurve c = pv.curve();
        double light = process.getSolarLight();
        double iph = c.iscRef * light * (1 + PvCurve.ISC_PER_K * (panel.panelCelsius() - PvCurve.REF_C));
        // + 1% of Isc: the light is sampled every 0.1..0.2 s, and at dawn/dusk it moves that much in between
        ctx.check(String.format("%s: photocurrent = Isc(T) x light %.4f", label, light),
            Math.abs(c.iph() - iph) <= 0.01 * c.iscRef, String.format("Iph %.4f A, expected %.4f A (panel %.1f C)", c.iph(), iph, panel.panelCelsius()));
    }

    /**
     * Small Solar Panel, open: the curve follows the light, the terminals sit at the open-circuit voltage, no power.
     * Plus an independent sanity check: at noon in clear weather the light is high (the 26.x sun angle source).
     */
    static final class SmallSolarCase implements TestCase {
        SolarPanelElement panel;

        public String id() {
            return "solar_small";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            var server = ctx.level().getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set noon");
            server.setWeatherParameters(100000, 0, false, false);
            panel = (SolarPanelElement) ctx.placeTransparent(transparent("Small Solar Panel"), ctx.at(1, 1, 1));
        }

        public void measure(TestContext ctx) {
            SolarPannelSlowProcess process = TestContext.field(panel, "slowProcess");
            double light = process.getSolarLight();
            checkCurve(ctx, panel, "solar small");
            PvSourceBipole pv = TestContext.field(panel, "pv");
            ctx.check("solar small, open: at Voc, no current", Math.abs(pv.getU() - pv.curve().voc()) <= 0.01 * pv.curve().voc()
                && Math.abs(pv.getI()) < 1e-6, String.format("U %.3f V (Voc %.3f), I %.2e A", pv.getU(), pv.curve().voc(), pv.getI()));
            ctx.check("solar: noon, clear sky: light > 0.5 (sun angle source on 26.x)", light > 0.5, String.format("light %.4f", light));
            // local weather (Watershed's showers, by IMC): asked at the panel; rain halves the light, thunder halves it
            // again; where the source has none (null), the world's weather (clear here)
            var saved = mods.eln.misc.Utils.weatherSource;
            BlockPos at = panel.node.coordonate.pos();
            try {
                double[][] cases = {{1, 0, 0.5}, {1, 1, 0.25}, {0.4, 0, 0.8}};
                for (double[] c : cases) {
                    mods.eln.misc.Utils.weatherSource = (lvl, pos) -> pos.equals(at) ? new double[]{c[0], c[1]} : new double[]{0, 0};
                    double l = process.getSolarLight();
                    ctx.check(String.format("solar: rain %.1f, thunder %.1f at the panel: light x %.2f", c[0], c[1], c[2]),
                        Math.abs(l - light * c[2]) < 1e-9, String.format("light %.4f, clear %.4f", l, light));
                }
                mods.eln.misc.Utils.weatherSource = (lvl, pos) -> null;
                double l = process.getSolarLight();
                ctx.check("solar: no local weather there: the world's (clear)", Math.abs(l - light) < 1e-9, String.format("light %.4f", l));
                // (the source is asked once per 16-block column for a second: every panel asks twice a slow tick)
                int[] asked = {0};
                mods.eln.misc.Utils.weatherSource = (lvl, pos) -> {
                    asked[0]++;
                    return new double[]{1, 0};
                };
                var lvl = panel.node.coordonate.world();
                mods.eln.misc.Utils.weatherAt(lvl, at);
                mods.eln.misc.Utils.weatherAt(lvl, at);
                int same = asked[0];
                mods.eln.misc.Utils.weatherAt(lvl, at.east(40));
                ctx.check("solar: the weather source is asked once per column, again for another column", same == 1 && asked[0] == 2,
                    same + " then " + asked[0]);
            } finally {
                mods.eln.misc.Utils.weatherSource = saved;
            }
        }
    }

    /**
     * Small Solar Panel on a resistor of its hot-noon Vmp^2/Pmp: the panel sits where its curve meets the load line, and
     * the sunlight warms it above the air (25 K at full sun in a minute; a little after 2 s).
     */
    static final class LoadedSolarCase implements TestCase {
        SolarPanelElement panel;
        Resistor load;
        double r;

        public String id() {
            return "solar_small_loaded";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            var server = ctx.level().getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set noon");
            server.setWeatherParameters(100000, 0, false, false);
            panel = (SolarPanelElement) ctx.placeTransparent(transparent("Small Solar Panel"), ctx.at(1, 1, 1));
            PvSourceBipole pv = TestContext.field(panel, "pv");
            double[] best = new mods.eln.sim.PvCurve(pv.curve().ratedU, pv.curve().ratedP).maxPowerPoint();   // (at a hot noon)
            r = best[0] * best[0] / best[1];
            load = new Resistor(TestContext.field(panel, "positiveLoad"), TestContext.field(panel, "negativeLoad")).setR(r);
            Eln.simulator.addElectricalComponent(load);
        }

        public void measure(TestContext ctx) {
            PvSourceBipole pv = TestContext.field(panel, "pv");
            PvCurve c = pv.curve();
            checkCurve(ctx, panel, "solar small loaded");
            double lo = 0, hi = c.voc();   // where the curve meets U/R
            double rt = r;
            for (int k = 0; k < 100; k++) { double m = (lo + hi) / 2; if (m / rt > c.current(m)) hi = m; else lo = m; }
            double u = (lo + hi) / 2;
            ctx.check("solar small loaded: on the curve", Math.abs(pv.getU() - u) <= 0.01 * u + 0.01,
                String.format("U %.3f V, expected %.3f V (R %.2f ohm)", pv.getU(), u, r));
            ctx.check("solar small loaded: delivers", load.getP() > 0.5 * c.ratedP * Math.min(1, c.iph() / c.iscRef),
                String.format("load %.2f W of a %.1f W panel", load.getP(), c.ratedP));
            ctx.check("solar small loaded: the sun warms the panel", panel.panelCelsius() > 20.2 && panel.panelCelsius() < 30,
                String.format("%.2f C", panel.panelCelsius()));
            Eln.simulator.removeElectricalComponent(load);
        }
    }

    /** 2x3 Solar Panel: its ghost blocks appear around the node, and go when the node's block is removed. */
    static final class SolarGhostCase implements TestCase {
        TransparentNodeElement panel;
        BlockPos node;
        int expected;

        public String id() {
            return "solar_2x3_ghosts";
        }

        public int width() {
            return 8;
        }

        public int depth() {
            return 3;
        }

        public void build(TestContext ctx) {
            TransparentNodeDescriptor d = Eln.transparentNodeItem.getDescriptor(transparent("2x3 Solar Panel"));
            panel = ctx.placeTransparent(transparent("2x3 Solar Panel"), ctx.at(3, 1, 1));
            node = panel.node.coordonate.pos();
            expected = d.getGhostGroup(panel.front).size();
        }

        int ghosts(TestContext ctx) {
            int n = 0;
            for (BlockPos p : BlockPos.betweenClosed(node.offset(-4, -2, -4), node.offset(4, 4, 4)))
                if (ctx.level().getBlockState(p).getBlock() == ElnRegistry.GHOST_BLOCK.get()) n++;
            return n;
        }

        public void measure(TestContext ctx) {
            int g = ghosts(ctx);
            ctx.check("solar 2x3: ghost blocks placed (descriptor's ghost group)", g == expected && expected > 0, g + " / " + expected);
            ctx.level().removeBlock(node, false);
            int left = ghosts(ctx);
            ctx.check("solar 2x3 removed: ghosts and node gone", left == 0
                && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new Coordonate(node, ctx.level())) == null, left + " ghost(s) left");
        }
    }
}
