package mods.eln.oil;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * An underground oil lake (design v2 §3): one start, one piece covering the lake; each chunk carves its part
 * (OilLakePiece). Placement (spacing, biomes) is data: data/eln/worldgen/structure_set/oil_lakes.json, so admins tune
 * it with a datapack. Size: radius 28-56 x 0.7-1.3, oil depth up to ~6: about 5-20k buckets.
 */
public class OilLakeStructure extends Structure {
    public static final MapCodec<OilLakeStructure> CODEC = simpleCodec(OilLakeStructure::new);

    public OilLakeStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext ctx) {
        var rnd = ctx.random();
        int cx = ctx.chunkPos().getMiddleBlockX(), cz = ctx.chunkPos().getMiddleBlockZ();
        int surface = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int baseY = Math.min(-20 + rnd.nextInt(51), surface - 24);
        if (baseY < ctx.heightAccessor().getMinY() + 12) return Optional.empty();
        int r = 28 + rnd.nextInt(29);
        float aspect = 0.7f + 0.6f * rnd.nextFloat();
        LakeShape shape = new LakeShape(((long) ctx.chunkPos().x() << 32) ^ (ctx.chunkPos().z() & 0xffffffffL), cx, cz, baseY,
            Math.round(r * aspect), Math.round(r / aspect), rnd.nextInt());
        return Optional.of(new Structure.GenerationStub(new BlockPos(cx, baseY, cz), b -> b.addPiece(new OilLakePiece(shape, surface))));
    }

    @Override
    public StructureType<?> type() {
        return OilWorldgen.LAKE_TYPE.get();
    }
}
