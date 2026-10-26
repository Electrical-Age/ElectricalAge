package mods.eln.sim;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The ambient temperature where a device stands (Baughn 2026-10-26: by biome, not a constant 20 C). Thermal loads cool
 * towards it (ThermalLoad.ambient, stamped when a device joins the simulation); fluid nodes fill at it.
 * <ul>
 * <li>Surface: 25 C per unit of the biome's temperature (with its frozen patches): plains 0.8 is 20 C, desert 50 C,
 * snowy plains 0 C, jagged peaks -17.5 C; above y 80 (the overworld's sea level + 17, whatever the world type) it cools
 * at vanilla's rate (0.05 / 40 of a unit per block, without its noise).</li>
 * <li>Underground (by depth under the column's surface, so a roof doesn't count) it blends from that towards the
 * depth curve (cooler under a desert, warmer under snow; Baughn): from BLEND_FROM blocks down, fully at BLEND_FULL;
 * the curve is {@link #depthC} plus {@link #depthPerBlock} per block of depth.</li>
 * <li>Where water evaporates (the Nether): {@link #netherC} (Baughn: 90 C, so things just barely work; air conditioning later).</li>
 * </ul>
 * Neighbouring blocks (lava, ice) don't count (Baughn: too exploitable); neither does freezing, for now.
 */
public final class Ambient {
    private Ambient() {
    }

    /** Config (ElnConfig, section ambient). */
    public static volatile double depthC = 15, depthPerBlock = 0.1, netherC = 90;

    public static final double C_PER_UNIT = 25, COOL_ABOVE_Y = 80, COOL_PER_BLOCK = 0.05 / 40, BLEND_FROM = 4, BLEND_FULL = 32;

    /** The GameTests' fixed ambient (their world's biomes vary from test to test); null in a game. */
    public static volatile Double fixedC;

    /** Ambient at pos, in C. */
    public static double celsius(Level level, BlockPos pos) {
        Double fixed = fixedC;
        if (fixed != null) return fixed;
        if (level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos)) return netherC;
        var climate = level.getBiome(pos).value().getModifiedClimateSettings();
        double t = climate.temperatureModifier().modifyTemperature(pos, climate.temperature());
        return blend(surfaceC(t, pos.getY()), surfaceY(level, pos) - 1 - pos.getY());
    }

    /**
     * The column's surface (one above its top block). EA simulates nodes in chunks nobody has loaded (and connects them
     * at startup): there, the terrain the generator would make, without loading the chunk (getBiome likewise falls back
     * to the generator's biome).
     */
    static int surfaceY(Level level, BlockPos pos) {
        int x = pos.getX(), z = pos.getZ();
        if (level.hasChunk(x >> 4, z >> 4) || !(level instanceof net.minecraft.server.level.ServerLevel sl))
            return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        var src = sl.getChunkSource();
        return src.getGenerator().getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, sl, src.randomState());
    }

    /** The surface value for a biome temperature t at height y (C). */
    public static double surfaceC(double t, double y) {
        return C_PER_UNIT * (t - Math.max(0, y - COOL_ABOVE_Y) * COOL_PER_BLOCK);
    }

    /** The surface value blended towards the depth curve, depth blocks under the surface (C). */
    public static double blend(double surface, double depth) {
        double w = Math.max(0, Math.min(1, (depth - BLEND_FROM) / (BLEND_FULL - BLEND_FROM)));
        return surface * (1 - w) + depthCurve(depth) * w;
    }

    public static double depthCurve(double depth) {
        return depthC + Math.max(0, depth) * depthPerBlock;
    }

    /** Ambient at pos as an EA thermal temperature (K above PhysicalConstant.Tamb, the Tc scale). */
    public static double relative(Level level, BlockPos pos) {
        return celsius(level, pos) - PhysicalConstant.Tamb;
    }

    /** Ambient at pos in kelvin (the fluid sim's scale). */
    public static double kelvin(Level level, BlockPos pos) {
        return celsius(level, pos) + PhysicalConstant.TCelsius;
    }

    /** At a node's coordinate; 0 (20 C) if its level isn't loaded. */
    public static double relative(mods.eln.misc.Coordonate c) {
        Level l = c == null ? null : c.world();
        return l == null ? 0 : relative(l, c.pos());
    }

    public static double kelvin(mods.eln.misc.Coordonate c) {
        return relative(c) + PhysicalConstant.Tref;
    }

    /** Set the ambient of every load (when a device joins the simulation; its biome doesn't change while it runs). */
    public static void stamp(Iterable<? extends ThermalLoad> loads, double ambient) {
        for (ThermalLoad l : loads) l.ambient = ambient;
    }
}
