package mods.eln.node.voxel;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.eln.client.render.ElnRenderErrors;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws a voxel node's parts (phase 3: plain boxes per straight pipe piece, with a flange ring where a pipe meets the
 * block's face; a baked model comes with phase 5). GL11 shim in a GlFrame, submitted as custom geometry, like
 * SixNodeRender.
 */
public class VoxelNodeRender implements BlockEntityRenderer<VoxelNodeEntity, VoxelNodeRender.State> {
    static final Identifier[] TEX = {Identifier.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png"),
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/polished_deepslate.png")};

    public static class State extends BlockEntityRenderState {
        List<VoxelPart> parts = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VoxelNodeEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.parts = be.clientParts;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        GlFrame frame = GlSubmit.open(poseStack);
        try {
            for (VoxelPart part : state.parts) {
                if (part instanceof ValvePart v) {
                    UtilsClient.glDefaultColor();
                    UtilsClient.bindTexture(TEX[0]);
                    var bs = v.boxes();
                    GL11.glColor4f(0.45f, 0.45f, 0.5f, 1f);
                    box(bs.get(0)[0], bs.get(0)[1], bs.get(0)[2], bs.get(0)[3], bs.get(0)[4], bs.get(0)[5]);
                    // the handwheel: red when shut, green when open
                    if (v.open) GL11.glColor4f(0.3f, 0.75f, 0.3f, 1f);
                    else GL11.glColor4f(0.8f, 0.2f, 0.15f, 1f);
                    int[] s = bs.get(1);
                    box(s[0], s[1], s[2], s[3], s[4], s[5]);
                    if (v.axis == 1) box(s[3] - 1, s[1] - 2, s[2] - 2, s[3], s[4] + 2, s[5] + 2);
                    else box(s[0] - 2, s[4] - 1, s[2] - 2, s[3] + 2, s[4], s[5] + 2);
                    continue;
                }
                if (part instanceof AdapterPart a) {
                    UtilsClient.glDefaultColor();
                    UtilsClient.bindTexture(TEX[0]);
                    GL11.glColor4f(0.75f, 0.55f, 0.3f, 1f);   // (brass)
                    int[] b = a.boxes().get(0);
                    box(b[0], b[1], b[2], b[3], b[4], b[5]);
                    continue;
                }
                if (!(part instanceof PipePart pipe)) continue;
                UtilsClient.glDefaultColor();
                if (pipe.burst) GL11.glColor4f(0.55f, 0.35f, 0.25f, 1f);   // (burst: scorched)
                UtilsClient.bindTexture(TEX[pipe.tier]);
                for (int[] b : pipe.boxes()) box(b[0], b[1], b[2], b[3], b[4], b[5]);
                // flanges where it meets a face: a ring one voxel wider than the bore, half a voxel deep
                int h = pipe.bore() / 2 + 1;
                for (int end = 0; end < 2; end++) {
                    int i = end == 0 ? 0 : pipe.points.length - 3;
                    int x = pipe.points[i], y = pipe.points[i + 1], z = pipe.points[i + 2];
                    switch (PipePart.faceOf(x, y, z)) {
                        case 0 -> box(x - h, 0, z - h, x + h, 1, z + h);
                        case 1 -> box(x - h, 15, z - h, x + h, 16, z + h);
                        case 2 -> box(x - h, y - h, 0, x + h, y + h, 1);
                        case 3 -> box(x - h, y - h, 15, x + h, y + h, 16);
                        case 4 -> box(0, y - h, z - h, 1, y + h, z + h);
                        case 5 -> box(15, y - h, z - h, 16, y + h, z + h);
                        default -> {
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            ElnRenderErrors.report("voxel-node", e);
        } finally {
            GlFrame.close();
        }
        GlSubmit.submit(frame, poseStack, collector, state.lightCoords);
    }

    /** A box in voxels (0..16), every face wound to face outwards, texture mapped at vanilla density. */
    static void box(double x0, double y0, double z0, double x1, double y1, double z1) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++)
            c[i] = new double[]{((i & 1) == 0 ? x0 : x1) / 16, ((i & 2) == 0 ? y0 : y1) / 16, ((i & 4) == 0 ? z0 : z1) / 16};
        quad(c[0], c[4], c[6], c[2], -1, 0, 0);   // x-
        quad(c[1], c[3], c[7], c[5], 1, 0, 0);    // x+
        quad(c[0], c[1], c[5], c[4], 0, -1, 0);   // y-
        quad(c[2], c[6], c[7], c[3], 0, 1, 0);    // y+
        quad(c[0], c[2], c[3], c[1], 0, 0, -1);   // z-
        quad(c[4], c[5], c[7], c[6], 0, 0, 1);    // z+
    }

    private static void quad(double[] a, double[] b, double[] c, double[] d, float nx, float ny, float nz) {
        // wind so the normal (b-a)x(c-a) points out
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
        double[][] q = cx * nx + cy * ny + cz * nz >= 0 ? new double[][]{a, b, c, d} : new double[][]{a, d, c, b};
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(nx, ny, nz);
        for (double[] p : q) {
            // planar UVs on the face's two other axes
            double u = nx != 0 ? p[2] : p[0], v = ny != 0 ? p[2] : p[1];
            GL11.glTexCoord2d(u, 1 - v);
            GL11.glVertex3d(p[0], p[1], p[2]);
        }
        GL11.glEnd();
    }
}
