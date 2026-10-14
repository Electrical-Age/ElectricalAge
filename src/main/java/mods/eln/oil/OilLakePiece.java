package mods.eln.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Carves one chunk of an oil lake (LakeShape): the cavern, the oil (full; the lake's record then sets the level on the
 * chunk's first load), islands, pillars, gravel/clay/tar-sand beaches; seals fluids at the cavern's edges; and lays
 * tar sand under the surface above the lake (exposed in deserts). Registers the lake's record (Reservoirs.ensure).
 */
public class OilLakePiece extends StructurePiece {
    final LakeShape shape;
    final int surface;

    public OilLakePiece(LakeShape shape, int surface) {
        super(OilWorldgen.LAKE_PIECE.get(), 0, new BoundingBox(shape.minX(), shape.baseY() - 34, shape.minZ(),
            shape.minX() + shape.sizeX() - 1, Math.max(surface + 4, shape.baseY() + 12), shape.minZ() + shape.sizeZ() - 1));
        this.shape = shape;
        this.surface = surface;
    }

    public OilLakePiece(CompoundTag tag) {
        super(OilWorldgen.LAKE_PIECE.get(), tag);
        this.shape = new LakeShape(tag.getLongOr("key", 0), tag.getIntOr("cx", 0), tag.getIntOr("cz", 0), tag.getIntOr("base_y", 0),
            tag.getIntOr("rx", 32), tag.getIntOr("rz", 32), tag.getIntOr("seed", 0));
        this.surface = tag.getIntOr("surface", 64);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("key", shape.key());
        tag.putInt("cx", shape.cx());
        tag.putInt("cz", shape.cz());
        tag.putInt("base_y", shape.baseY());
        tag.putInt("rx", shape.rx());
        tag.putInt("rz", shape.rz());
        tag.putInt("seed", shape.seed());
        tag.putInt("surface", surface);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
                            ChunkPos chunkPos, BlockPos reference) {
        Reservoirs.ensure(level.getLevel(), shape);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState(), stone = Blocks.STONE.defaultBlockState();
        BlockState oil = Oil.RESERVOIR_BLOCK.get().defaultBlockState();
        BlockState tar = OilWorldgen.TAR_SAND.get().defaultBlockState();
        int base = shape.baseY();
        for (int x = Math.max(chunkBox.minX(), shape.minX()); x <= Math.min(chunkBox.maxX(), shape.minX() + shape.sizeX() - 1); x++)
            for (int z = Math.max(chunkBox.minZ(), shape.minZ()); z <= Math.min(chunkBox.maxZ(), shape.minZ() + shape.sizeZ() - 1); z++) {
                LakeShape.Kind kind = shape.kind(x, z);
                if (kind == LakeShape.Kind.OUTSIDE) {
                    // just outside the cavern: no water or lava flowing in at its height
                    if (shape.dist(x, z) < 1.3f)
                        for (int y = base - 8; y <= base + 9; y++) {
                            p.set(x, y, z);
                            if (!level.getBlockState(p).getFluidState().isEmpty()) level.setBlock(p, stone, 2);
                        }
                    tarSand(level, p, x, z, tar);
                    continue;
                }
                int ceil = shape.ceiling(x, z);
                int floorTop = switch (kind) {
                    case OIL -> base - shape.depth(x, z);
                    case ISLAND -> base + 1;
                    default -> base;
                };
                if (kind == LakeShape.Kind.PILLAR) {
                    for (int y = base - 2; y < ceil; y++) level.setBlock(p.set(x, y, z), stone, 2);
                } else {
                    for (int y = floorTop; y < ceil; y++)
                        level.setBlock(p.set(x, y, z), kind == LakeShape.Kind.OIL && y < base ? oil : air, 2);
                    // a solid floor and ceiling (no fluid, no hole into a cave below the oil)
                    p.set(x, floorTop - 1, z);
                    BlockState under = level.getBlockState(p);
                    if (kind == LakeShape.Kind.OIL) {
                        if (under.isAir() || !under.getFluidState().isEmpty()) level.setBlock(p, stone, 2);
                    } else {
                        float h = LakeShape.hash(x, floorTop, z, shape.seed());
                        level.setBlock(p, h < 0.3f ? tar : h < 0.6f ? Blocks.GRAVEL.defaultBlockState()
                            : h < 0.8f ? Blocks.CLAY.defaultBlockState() : stone, 2);
                    }
                    p.set(x, ceil, z);
                    if (!level.getBlockState(p).getFluidState().isEmpty() || level.getBlockState(p).is(BlockTags.SAND)) level.setBlock(p, stone, 2);
                }
                tarSand(level, p, x, z, tar);
            }
    }

    /** Tar sand under the surface above the lake, in patches; in deserts at the top, exposed. */
    private void tarSand(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, BlockState tar) {
        if (shape.dist(x, z) > 0.9f || LakeShape.noise(x / 7f, z / 7f, shape.seed() ^ 0x7A2) < 0.55f) return;
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        p.set(x, top, z);
        boolean desert = level.getBiome(p).is(net.neoforged.neoforge.common.Tags.Biomes.IS_DESERT);
        for (int y = desert ? top : top - 5; y <= (desert ? top : top - 2); y++) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.SANDSTONE) || s.is(Blocks.GRAVEL) || s.is(BlockTags.BASE_STONE_OVERWORLD))
                level.setBlock(p, tar, 2);
        }
    }
}
