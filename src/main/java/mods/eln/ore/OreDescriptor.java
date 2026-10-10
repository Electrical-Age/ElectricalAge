package mods.eln.ore;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

/**
 * An EA ore (1.12: a metadata variant of eln:ore). 26.x: one block per ore; world generation is data
 * (data/eln/worldgen, made by script/gen_ore_data.py from these numbers): spawnRate veins per chunk, size
 * spawnSizeMin..spawnSizeMax-1, y spawnHeightMin..spawnHeightMax-1, replacing plain stone (as 1.12's WorldGenMinable).
 */
public class OreDescriptor extends GenericItemBlockUsingDamageDescriptor {

    int metadata;

    int spawnRate, spawnSizeMax, spawnSizeMin, spawnHeightMin, spawnHeightMax;

    /** The ore's block (set at registration). */
    public Block block;

    public OreDescriptor(String name, int metadata,
                         int spawnRate, int spawnSizeMin, int spawnSizeMax, int spawnHeightMin, int spawnHeightMax) {
        super(name);
        this.metadata = metadata;
        this.spawnHeightMax = spawnHeightMax;
        this.spawnHeightMin = spawnHeightMin;
        this.spawnRate = spawnRate;
        this.spawnSizeMin = spawnSizeMin;
        this.spawnSizeMax = spawnSizeMax;
    }

    /** One vein (1.12 WorldGenMinable on plain stone); used by the selftest and the regeneration. */
    public void generateVein(ServerLevel w, RandomSource random, int posX, int posY, int posZ, int size) {
        new LiveOreFeature(block, size).placeHere(w, random, new BlockPos(posX, posY, posZ));
    }

    /**
     * The vanilla ore feature for a live level: OreFeature.place first checks the world-generation heightmap, which
     * only exists while chunks generate (a live level logs "Unprimed heightmap"); this skips that check.
     */
    static final class LiveOreFeature extends OreFeature {
        private final int veinSize;

        LiveOreFeature(Block block, int size) {
            super(new BlockMatchTest(Blocks.STONE), block.defaultBlockState(), size);
            veinSize = size;
        }

        boolean placeHere(ServerLevel level, RandomSource random, BlockPos origin) {
            // as OreFeature.place, without the OCEAN_FLOOR_WG test
            float dir = random.nextFloat() * (float) Math.PI;
            float spreadXY = veinSize / 8.0F;
            int maxRadius = net.minecraft.util.Mth.ceil((veinSize / 16.0F * 2.0F + 1.0F) / 2.0F);
            double x0 = origin.getX() + Math.sin(dir) * spreadXY;
            double x1 = origin.getX() - Math.sin(dir) * spreadXY;
            double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
            double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
            double y0 = origin.getY() + random.nextInt(3) - 2;
            double y1 = origin.getY() + random.nextInt(3) - 2;
            int xStart = origin.getX() - net.minecraft.util.Mth.ceil(spreadXY) - maxRadius;
            int yStart = origin.getY() - 2 - maxRadius;
            int zStart = origin.getZ() - net.minecraft.util.Mth.ceil(spreadXY) - maxRadius;
            int sizeXZ = 2 * (net.minecraft.util.Mth.ceil(spreadXY) + maxRadius);
            int sizeY = 2 * (2 + maxRadius);
            return doPlace(level, random, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, sizeY);
        }
    }

    public int getMetadata() {
        return metadata;
    }

    public int getSpawnRate() {
        return spawnRate;
    }
}
