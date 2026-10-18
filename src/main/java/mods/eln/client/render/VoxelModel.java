package mods.eln.client.render;

import com.mojang.serialization.MapCodec;
import mods.eln.node.voxel.VoxelGeometry;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.node.voxel.VoxelPart;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The voxel node's block model (notes/cables/PLAN.md step 8): its parts (from the block entity's model data) baked
 * into the chunk mesh by VoxelGeometry, instead of drawn every frame. Blockstate: {"type": "eln:voxel_node"}.
 */
public final class VoxelModel implements DynamicBlockStateModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("eln", "voxel_node");

    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            // every sprite the geometry uses, resolved now (the mesher runs on worker threads)
            Map<Identifier, Material.Baked> sprites = new HashMap<>();
            for (Identifier id : VoxelGeometry.PIPE) sprites.put(id, baker.materials().get(new Material(id), () -> "eln:voxel_node"));
            for (int t = 0; t < mods.eln.node.voxel.CablePart.TIERS.length; t++) {
                Identifier id = VoxelGeometry.cableSprite(t);
                sprites.computeIfAbsent(id, k -> baker.materials().get(new Material(k), () -> "eln:voxel_node"));
            }
            return new VoxelModel(sprites, sprites.get(VoxelGeometry.PIPE[0]));
        }
    }

    private final Map<Identifier, Material.Baked> sprites;
    private final Material.Baked particle;

    VoxelModel(Map<Identifier, Material.Baked> sprites, Material.Baked particle) {
        this.sprites = sprites;
        this.particle = particle;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        var data = level.getModelData(pos);
        // the imitated block (a stair a cable runs over: Mimic), with its own model
        BlockState host = data.get(VoxelNodeEntity.HOST);
        List<int[]> carved = data.get(VoxelNodeEntity.CARVED);
        if (host != null && carved != null) carved(level, pos, host, random, carved, parts);
        else if (host != null)
            net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(host).collectParts(level, pos, host, random, parts);
        List<VoxelPart> ps = data.get(VoxelNodeEntity.PARTS);
        if (ps == null || ps.isEmpty()) return;
        QuadCollection.Builder qb = new QuadCollection.Builder();
        MutableQuad q = new MutableQuad();
        for (VoxelGeometry.Quad g : VoxelGeometry.of(ps)) {
            Material.Baked mat = sprites.getOrDefault(g.sprite(), particle);
            q.reset().setSprite(mat, com.mojang.blaze3d.platform.Transparency.NONE).setColor(g.argb()).setAmbientOcclusion(true);
            for (int i = 0; i < 4; i++) {
                q.setPosition(i, g.xyz()[3 * i], g.xyz()[3 * i + 1], g.xyz()[3 * i + 2]);
                q.setUvFromSprite(i, g.uv()[2 * i], g.uv()[2 * i + 1]);
            }
            q.recomputeNormals(true);
            if (g.face() >= 0) qb.addCulledFace(Direction.from3DDataValue(g.face()), q.toBakedQuad());
            else qb.addUnculledFace(q.toBakedQuad());
        }
        parts.add(new SimpleModelWrapper(qb.build(), true, particle));
    }

    /** What the chisel left of a block: its remaining voxels as boxes, each face in the block's own face texture. */
    private void carved(BlockAndTintGetter level, BlockPos pos, BlockState host, RandomSource random, List<int[]> boxes, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> hp = new java.util.ArrayList<>();
        net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(host).collectParts(level, pos, host, random, hp);
        net.minecraft.client.resources.model.geometry.BakedQuad[] face = new net.minecraft.client.resources.model.geometry.BakedQuad[6];
        for (Direction d : Direction.values())
            for (BlockStateModelPart p : hp) {
                var qs = p.getQuads(d);
                if (qs.isEmpty()) qs = p.getQuads(null);
                if (!qs.isEmpty()) {
                    face[d.get3DDataValue()] = qs.get(0);
                    break;
                }
            }
        QuadCollection.Builder qb = new QuadCollection.Builder();
        MutableQuad q = new MutableQuad();
        for (int[] b : boxes)
            for (Direction d : Direction.values()) {
                var src = face[d.get3DDataValue()];
                if (src == null) continue;
                q.reset().setFrom(src);
                q.setCubeFace(d, b[0] / 16f, b[1] / 16f, b[2] / 16f, b[3] / 16f, b[4] / 16f, b[5] / 16f);
                q.bakeUvsFromPosition();
                q.setAmbientOcclusion(true);
                boolean onFace = switch (d) {
                    case DOWN -> b[1] == 0;
                    case UP -> b[4] == 16;
                    case NORTH -> b[2] == 0;
                    case SOUTH -> b[5] == 16;
                    case WEST -> b[0] == 0;
                    case EAST -> b[3] == 16;
                };
                if (onFace) qb.addCulledFace(d, q.toBakedQuad());
                else qb.addUnculledFace(q.toBakedQuad());
            }
        parts.add(new SimpleModelWrapper(qb.build(), true, particle));
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    /** An imitated block may have translucent faces (stained glass panes). */
    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockState host = level.getModelData(pos).get(VoxelNodeEntity.HOST);
        return host == null ? 0 : net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(host).materialFlags(level, pos, host);
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockState host = level.getModelData(pos).get(VoxelNodeEntity.HOST);
        return host == null ? particle : net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(host).particleMaterial(level, pos, host);
    }
}
