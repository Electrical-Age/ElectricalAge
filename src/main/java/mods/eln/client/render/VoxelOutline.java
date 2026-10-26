package mods.eln.client.render;

import mods.eln.node.voxel.KitItem;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.node.voxel.VoxelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * In a voxel block the outline is the one thing aimed at (a device, or a pipe or cable), not everything in the block
 * (AdventurAgent: one outline round every chip of a block). Picked as VoxelNode.elementAt and partAt pick it on the
 * server: a part's own box, then a device, then a part's fattened box. The imitated block keeps its own outline.
 */
public final class VoxelOutline {
    private VoxelOutline() {
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.getMainHandItem().getItem() instanceof KitItem
            || player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ChiselItem) return;
        BlockPos at = e.getBlockPos();
        if (!(e.getLevel().getBlockEntity(at) instanceof VoxelNodeEntity v)) return;
        Vec3 h = e.getHitResult().getLocation().subtract(at.getX(), at.getY(), at.getZ());
        List<int[]> boxes = aimed(v, h.x, h.y, h.z);
        if (boxes == null) return;
        VoxelShape s = Shapes.empty();
        for (int[] b : boxes)
            s = Shapes.or(s, Shapes.box(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0));
        VoxelShape shape = s;
        e.addCustomRenderer((state, collector, pose, lrs) -> {
            Vec3 cam = lrs.cameraRenderState.pos;
            pose.pushPose();
            pose.translate(at.getX() - cam.x, at.getY() - cam.y, at.getZ() - cam.z);
            collector.submitShapeOutline(pose, shape, RenderTypes.linesTranslucent(), 0x66000000, 2f, false);
            pose.popPose();
            return true;
        });
    }

    /** The boxes of what the block-local point is on, or null (the imitated block, or nothing of ours). */
    static List<int[]> aimed(VoxelNodeEntity v, double x, double y, double z) {
        List<VoxelPart> parts = v.parts();
        for (VoxelPart p : parts) for (int[] b : p.boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return grown(p);
        for (int f = 0; f < mods.eln.node.six.SixNode.SLOTS; f++) {
            List<int[]> bs = v.slotBoxes(f);
            for (int[] b : bs) if (VoxelPart.contains(b, x, y, z, 0.05)) return bs;
        }
        for (VoxelPart p : parts) for (int[] b : p.boxes()) if (VoxelPart.contains(b, x, y, z, 2.05)) return grown(p);
        return null;
    }

    /** A part's boxes, fattened as its outline is (2 voxels, not past the block). */
    static List<int[]> grown(VoxelPart p) {
        List<int[]> out = new ArrayList<>();
        for (int[] b : p.boxes())
            out.add(new int[]{Math.max(0, b[0] - 2), Math.max(0, b[1] - 2), Math.max(0, b[2] - 2), Math.min(16, b[3] + 2), Math.min(16, b[4] + 2), Math.min(16, b[5] + 2)});
        return out;
    }
}
