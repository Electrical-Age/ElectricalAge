package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.mna.process.PowerSourceBipole;
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
        return List.of(new SmallSolarCase(), new SolarGhostCase());
    }

    /**
     * Small Solar Panel: P = Pmax * light, light from the panel's own sun/sky/obstruction function (1.12 SolarCase).
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
            SolarPannelSlowProcess process = Batch1Cases.field(panel, "slowProcess");
            double light = process.getSolarLight();
            double pmax = Batch1Cases.<Double>field(Batch1Cases.field(panel, "descriptor"), "electricalPmax");
            PowerSourceBipole src = Batch1Cases.field(panel, "powerSource");
            double tol = 0.01 * pmax * light + 0.005 * pmax;
            ctx.check(String.format("solar power [W] (Pmax %.1f * light %.4f)", pmax, light),
                Math.abs(src.getP() - pmax * light) <= tol, String.format("measured %.4f, expected %.4f +- %.3f", src.getP(), pmax * light, tol));
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
            } finally {
                mods.eln.misc.Utils.weatherSource = saved;
            }
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
