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

/**
 * With a chisel in hand: the outline is the brush box (size and shape: cube, flat layer or tunnel through the block)
 * the next left-click would take away, from the same geometry as ChiselItem.carve (ChiselBrush.box).
 */
public final class ChiselOutline {
    private ChiselOutline() {
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ChiselItem)) return;
        var level = e.getLevel();
        var hit = e.getHitResult();
        var stack = player.getMainHandItem();
        BlockPos at = ChiselItem.target(hit.getLocation(), hit.getDirection(), true);
        var st = level.getBlockState(at);
        boolean carvable = Mimic.carvable(st) || level.getBlockEntity(at) instanceof VoxelNodeEntity ve && ve.host() != null;
        if (!carvable) return;
        int[] b = ChiselItem.brushBox(hit.getLocation(), hit.getDirection(), ChiselItem.size(stack), ChiselItem.shape(stack), true);
        var shape = Shapes.box(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
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
