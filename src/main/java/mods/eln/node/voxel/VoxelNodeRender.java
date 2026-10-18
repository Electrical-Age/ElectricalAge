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
    /** Iron (dark grey) and alloy steel (blue-grey): tools/icons/pipekit.py. */
    static final Identifier[] TEX = {Identifier.fromNamespaceAndPath("eln", "textures/block/pipe_iron.png"),
        Identifier.fromNamespaceAndPath("eln", "textures/block/pipe_alloy.png")};
    static final int SIDES = 12;

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
                drawPipe(pipe);
            }
        } catch (RuntimeException e) {
            ElnRenderErrors.report("voxel-node", e);
        } finally {
            GlFrame.close();
        }
        GlSubmit.submit(frame, poseStack, collector, state.lightCoords);
    }

    /**
     * A pipe: a round tube along each straight piece (Baughn: not square), a fitting ring at each bend, and a flange
     * where it meets a block face, the same size on both sides of the face so neighbours' flanges meet.
     */
    static void drawPipe(PipePart pipe) {
        double r = pipe.bore() / 2.0;
        int[] p = pipe.points;
        int n = p.length / 3;
        for (int i = 1; i < n; i++) {
            int[] a = {p[3 * i - 3], p[3 * i - 2], p[3 * i - 1]}, b = {p[3 * i], p[3 * i + 1], p[3 * i + 2]};
            int axis = a[0] != b[0] ? 0 : a[1] != b[1] ? 1 : 2;
            double lo = Math.min(a[axis], b[axis]), hi = Math.max(a[axis], b[axis]);
            // run into the bend a little so the tubes meet without a gap
            boolean aBend = i > 1, bBend = i < n - 1;
            if (a[axis] < b[axis]) { if (aBend) lo -= r * 0.7; if (bBend) hi += r * 0.7; }
            else { if (bBend) lo -= r * 0.7; if (aBend) hi += r * 0.7; }
            int[] o = others(axis);
            tube(axis, a[o[0]], a[o[1]], r, lo, hi);
        }
        // fittings at the bends
        for (int i = 1; i < n - 1; i++) {
            int[] c = {p[3 * i], p[3 * i + 1], p[3 * i + 2]};
            int axis = p[3 * i - 3] != c[0] ? 0 : p[3 * i - 2] != c[1] ? 1 : 2;
            int[] o = others(axis);
            tube(axis, c[o[0]], c[o[1]], r + 0.35, c[axis] - r - 0.3, c[axis] + r + 0.3);
        }
        // flanges at the face ends: a slim ring 0.75 voxels proud of the pipe, 1 voxel deep
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            int x = p[3 * i], y = p[3 * i + 1], z = p[3 * i + 2];
            int face = PipePart.faceOf(x, y, z);
            if (face < 0) continue;
            int axis = face <= 1 ? 1 : face <= 3 ? 2 : 0;
            int[] c = {x, y, z};
            int[] o = others(axis);
            double at = c[axis];
            tube(axis, c[o[0]], c[o[1]], r + 0.75, at == 0 ? 0 : 15, at == 0 ? 1 : 16);
        }
    }

    static int[] others(int axis) {
        return axis == 0 ? new int[]{1, 2} : axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
    }

    /** A capped tube along axis (0 x, 1 y, 2 z), centred at (c1, c2) on the other two axes, from a0 to a1 (voxels). */
    static void tube(int axis, double c1, double c2, double r, double a0, double a1) {
        int[] o = others(axis);
        double[][] ring0 = new double[SIDES][], ring1 = new double[SIDES][];
        for (int k = 0; k < SIDES; k++) {
            double ang = 2 * Math.PI * (k + 0.5) / SIDES;
            double[] v0 = new double[3], v1 = new double[3];
            v0[o[0]] = v1[o[0]] = c1 + r * Math.cos(ang);
            v0[o[1]] = v1[o[1]] = c2 + r * Math.sin(ang);
            v0[axis] = a0;
            v1[axis] = a1;
            ring0[k] = v0;
            ring1[k] = v1;
        }
        for (int k = 0; k < SIDES; k++) {
            int k1 = (k + 1) % SIDES;
            double ang = 2 * Math.PI * (k + 1) / SIDES;
            float[] nrm = new float[3];
            nrm[o[0]] = (float) Math.cos(ang);
            nrm[o[1]] = (float) Math.sin(ang);
            quad16(ring0[k], ring0[k1], ring1[k1], ring1[k], nrm[0], nrm[1], nrm[2], axis, k);
        }
        double[] c0 = new double[3], cc1 = new double[3];
        c0[o[0]] = cc1[o[0]] = c1;
        c0[o[1]] = cc1[o[1]] = c2;
        c0[axis] = a0;
        cc1[axis] = a1;
        for (int k = 0; k < SIDES; k++) {
            int k1 = (k + 1) % SIDES;
            float[] n0 = new float[3], n1 = new float[3];
            n0[axis] = -1;
            n1[axis] = 1;
            quad16(c0, c0, ring0[k1], ring0[k], n0[0], n0[1], n0[2], -1, k);
            quad16(cc1, cc1, ring1[k], ring1[k1], n1[0], n1[1], n1[2], -1, k);
        }
    }

    /** A quad in voxels, wound to face along the normal; UVs: around the tube (u) and along it (v), or planar for caps. */
    private static void quad16(double[] a, double[] b, double[] c, double[] d, float nx, float ny, float nz, int axis, int k) {
        double[][] q = {a, b, c, d};
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        if (ux * ux + uy * uy + uz * uz < 1e-12) { ux = c[0] - b[0]; uy = c[1] - b[1]; uz = c[2] - b[2]; vx = d[0] - b[0]; vy = d[1] - b[1]; vz = d[2] - b[2]; }
        double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
        if (cx * nx + cy * ny + cz * nz < 0) q = new double[][]{a, d, c, b};
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(nx, ny, nz);
        for (double[] v : q) {
            double tu, tv;
            if (axis >= 0) {
                tu = (k + (v == q[1] || v == q[2] ? 1 : 0)) / (double) SIDES;
                tv = v[axis] / 16;
            } else {
                tu = v[0] / 16 + v[2] / 16;
                tv = v[1] / 16;
            }
            GL11.glTexCoord2d(tu, tv);
            GL11.glVertex3d(v[0] / 16, v[1] / 16, v[2] / 16);
        }
        GL11.glEnd();
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
