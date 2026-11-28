package mods.eln.client.render;

import mods.eln.node.voxel.KitItem;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.List;

/**
 * In a voxel block the outline is the one thing aimed at (a device, or a pipe or cable), not everything in the block
 * (AdventurAgent: one outline round every chip of a block). Picked by VoxelNodeEntity.aimed, as Jade's tooltip and the
 * server's clicks and breaking pick it (VoxelPick). The imitated block keeps its own outline.
 */
public final class VoxelOutline {
    private VoxelOutline() {
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.getMainHandItem().getItem() instanceof KitItem || player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ToolbagItem
            || player.getMainHandItem().getItem() instanceof mods.eln.node.voxel.ChiselItem) return;
        BlockPos at = e.getBlockPos();
        if (!(e.getLevel().getBlockEntity(at) instanceof VoxelNodeEntity v)) return;
        List<int[]> boxes = v.outline(aimed(v, e.getHitResult().getLocation(), false));
        if (boxes == null || boxes.isEmpty()) return;
        VoxelShape s = Shapes.empty();
        for (int[] b : boxes)
            s = Shapes.or(s, Shapes.box(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0));
        VoxelShape shape = s;
        e.addCustomRenderer((state, collector, pose, lrs) -> {
            Vec3 cam = lrs.cameraRenderState.pos;
            pose.pushPose();
            pose.translate(at.getX() - cam.x, at.getY() - cam.y, at.getZ() - cam.z);
            collector.submitShapeOutline(pose, shape, lines(), 0x66000000, 2f, false);
            pose.popPose();
            return true;
        });
    }

    /**
     * What the crosshair is on in v, its ray stopped at hit: VoxelNodeEntity.aimed from the camera entity's eye at this
     * frame's partial tick, as the game traced the ray (Minecraft.pick), not the eye at the tick's end: while the
     * player moves that is up to a tick's walk away, and the line swung between neighbouring cables. runsNormally: as
     * the tracer asked the frame's time (the game's own pick false, Jade's true; they differ only while ticks are frozen).
     */
    public static mods.eln.node.voxel.VoxelNode.Aimed aimed(VoxelNodeEntity v, Vec3 hit, boolean runsNormally) {
        Minecraft mc = Minecraft.getInstance();
        var viewer = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        return v.aimed(viewer == null ? null : viewer.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(runsNormally)), hit);
    }

    /**
     * The line type for EA's outlines, as vanilla picks its own block outline's: under Fabulous ("improved
     * transparency") the translucent lines go through the order-independent transparency pass, and only the no-depth-
     * write variant has pipelines there (Baughn's client crashed on login: "Render type lines_translucent does not have
     * OIT pipelines set up", 2026-11-12).
     */
    public static net.minecraft.client.renderer.rendertype.RenderType lines() {
        return net.minecraft.client.Minecraft.getInstance().gameRenderer.useImprovedTransparency()
            ? RenderTypes.linesTranslucentNoDepthWrite() : RenderTypes.linesTranslucent();
    }
}
