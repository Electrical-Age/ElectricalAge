package mods.eln.client.render;

import mods.eln.node.voxel.ChiselItem;
import mods.eln.node.voxel.Mimic;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/** With a chisel in hand: the outline is the brush cube the next left-click would take away (ChiselItem.carve). */
public final class ChiselOutline {
    private ChiselOutline() {
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ChiselItem)) return;
        var level = e.getLevel();
        var hit = e.getHitResult();
        int size = ChiselItem.size(player.getMainHandItem());
        var face = hit.getDirection();
        Vec3 v = hit.getLocation().add(face.getStepX() * -0.001, face.getStepY() * -0.001, face.getStepZ() * -0.001);
        BlockPos at = BlockPos.containing(v);
        var st = level.getBlockState(at);
        boolean carvable = Mimic.carvable(st) || level.getBlockEntity(at) instanceof VoxelNodeEntity ve && ve.host() != null;
        if (!carvable) return;
        int vx = (int) Math.floor((v.x - at.getX()) * 16), vy = (int) Math.floor((v.y - at.getY()) * 16), vz = (int) Math.floor((v.z - at.getZ()) * 16);
        int x0 = vx / size * size, y0 = vy / size * size, z0 = vz / size * size;
        var shape = Shapes.box(x0 / 16.0, y0 / 16.0, z0 / 16.0, (x0 + size) / 16.0, (y0 + size) / 16.0, (z0 + size) / 16.0);
        e.addCustomRenderer((state, collector, pose, lrs) -> {
            Vec3 cam = lrs.cameraRenderState.pos;
            pose.pushPose();
            pose.translate(at.getX() - cam.x, at.getY() - cam.y, at.getZ() - cam.z);
            collector.submitShapeOutline(pose, shape, RenderTypes.linesTranslucent(), 0xC0FF9020, 2.5f, false);
            pose.popPose();
            return true;
        });
    }
}
