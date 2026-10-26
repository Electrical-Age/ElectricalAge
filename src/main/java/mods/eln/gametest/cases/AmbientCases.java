package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;

import java.util.List;

/** Ambient temperature by biome, height and depth (Baughn 2026-10-26; mods.eln.sim.Ambient). */
public final class AmbientCases {
    private AmbientCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestWorld());
        cases.add(new Curve());
        cases.add(new DesertAndSnow());
    }

    /** The GameTests hold the ambient at 20 C (their world's biomes vary), so every other case keeps its numbers. */
    static final class TestWorld implements TestCase {
        public String id() {
            return "ambient_test_world";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var pos = ctx.at(1, 1, 1);
            var level = ctx.level();
            var climate = level.getBiome(pos).value().getModifiedClimateSettings();
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
            double c = mods.eln.sim.Ambient.celsius(level, pos);
            mods.eln.sim.Ambient.fixedC = null;
            double real = mods.eln.sim.Ambient.celsius(level, pos);
            mods.eln.sim.Ambient.fixedC = 20.0;
            ctx.line(String.format("ambient: %s (unfixed %s), biome %s t=%.2f, y %d, surface %d", c, real, level.getBiome(pos).getRegisteredName(), climate.temperature(), pos.getY(), surface));
            ctx.check("ambient: 20 C in the test world", Math.abs(c - 20) < 1e-9, c + " C");
        }
    }

    /** The mapping (Ambient): 25 C per unit of biome temperature, cooler above y 80, blending into the depth curve. */
    static final class Curve implements TestCase {
        public String id() {
            return "ambient_curve";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ctx.checkValue("ambient: plains 0.8 at sea level [C]", mods.eln.sim.Ambient.surfaceC(0.8, 64), 20);
            ctx.checkValue("ambient: desert 2.0 [C]", mods.eln.sim.Ambient.surfaceC(2.0, 64), 50);
            ctx.checkValue("ambient: snowy plains 0.0 [C]", mods.eln.sim.Ambient.surfaceC(0.0, 64), 0);
            ctx.checkValue("ambient: jagged peaks -0.7 at y 80 [C]", mods.eln.sim.Ambient.surfaceC(-0.7, 80), -17.5);
            ctx.checkValue("ambient: plains 100 blocks above y 80 [C] (20 - 25 x 100 x 0.05/40)", mods.eln.sim.Ambient.surfaceC(0.8, 180), 20 - 3.125);
            ctx.checkValue("ambient: 3 blocks down, still the surface [C]", mods.eln.sim.Ambient.blend(50, 3), 50);
            ctx.checkValue("ambient: under a desert, 18 blocks down: halfway to the depth curve [C]", mods.eln.sim.Ambient.blend(50, 18),
                (50 + 15 + 1.8) / 2);
            ctx.checkValue("ambient: under snow, 40 blocks down: the depth curve [C] (15 + 4)", mods.eln.sim.Ambient.blend(0, 40), 19);
            ctx.checkValue("ambient: 100 blocks down [C] (15 + 10)", mods.eln.sim.Ambient.blend(50, 100), 25);
            var nether = ctx.level().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            mods.eln.sim.Ambient.fixedC = null;
            double n = nether == null ? Double.NaN : mods.eln.sim.Ambient.celsius(nether, new net.minecraft.core.BlockPos(0, 64, 0));
            mods.eln.sim.Ambient.fixedC = 20.0;
            ctx.checkValue("ambient: the Nether [C]", n, 110);
        }
    }

    /**
     * Devices cool towards the ambient where they stand: two passive dissipators (time constant 10 s) placed at 20 C,
     * one in a desert (50 C), one on snowy plains (0 C), relax towards +30 K and -20 K on the 20 C scale; their loads
     * carry the stamped ambient.
     */
    static final class DesertAndSnow implements TestCase {
        static final int DISSIPATOR_PASSIVE = 0 + (64 << 6);
        mods.eln.node.transparent.TransparentNodeElement hot, cold;
        long t0;

        public String id() {
            return "ambient_desert_and_snow";
        }

        public int width() {
            return 8;
        }

        public int ticks() {
            return 200;
        }

        static mods.eln.sim.ThermalLoad load(mods.eln.node.transparent.TransparentNodeElement e) {
            return e.thermalLoadList.get(0);
        }

        public void build(TestContext ctx) {
            var server = ctx.level().getServer();
            var src = server.createCommandSourceStack().withSuppressedOutput().withLevel(ctx.level());
            var a = ctx.at(0, 0, 0);
            var b = ctx.at(7, 3, 2);
            server.getCommands().performPrefixedCommand(src, String.format("fillbiome %d %d %d %d %d %d minecraft:desert",
                Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ())));
            var p1 = ctx.at(1, 1, 1);
            var p2 = ctx.at(6, 1, 1);
            server.getCommands().performPrefixedCommand(src, String.format("fillbiome %d %d %d %d %d %d minecraft:snowy_plains",
                p2.getX() - 1, p2.getY() - 1, p2.getZ() - 1, p2.getX() + 1, p2.getY() + 1, p2.getZ() + 1));
            mods.eln.sim.Ambient.fixedC = null;
            try {
                hot = ctx.placeTransparent(DISSIPATOR_PASSIVE, p1);
                cold = ctx.placeTransparent(DISSIPATOR_PASSIVE, p2);
            } finally {
                mods.eln.sim.Ambient.fixedC = 20.0;
            }
            t0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            double t = (ctx.world().getGameTime() - t0) / 20.0, f = 1 - Math.exp(-t / 10);
            var h = load(hot);
            var c = load(cold);
            ctx.line(String.format("ambient: desert %s / %.2f K, snow %s / %.2f K after %.2f s", h.ambient, h.Tc, c.ambient, c.Tc, t));
            ctx.check("ambient: stamped from the biome (desert +30 K, snowy plains -20 K on the 20 C scale)",
                Math.abs(h.ambient - 30) < 1e-6 && Math.abs(c.ambient + 20) < 1e-6, h.ambient + " / " + c.ambient);
            ctx.check("ambient: the desert dissipator warms towards 50 C", Math.abs(h.Tc - 30 * f) < 0.05 * 30 * f + 0.2,
                String.format("%.2f K, expected %.2f", h.Tc, 30 * f));
            ctx.check("ambient: the snowy one cools towards 0 C", Math.abs(c.Tc + 20 * f) < 0.05 * 20 * f + 0.2,
                String.format("%.2f K, expected %.2f", c.Tc, -20 * f));
        }
    }
}
