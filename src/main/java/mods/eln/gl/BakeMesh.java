package mods.eln.gl;

import mods.eln.misc.UtilsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import java.util.Map;

/** The chunk geometry of settled {@link Bake} calls: what a section's mesh gets in place of the renderer's draw. */
public final class BakeMesh {
    private BakeMesh() {
    }

    /** Where a texture is in the block atlas: (u, v) of the texture maps to (u0 + u * du, v0 + v * dv). holes = has discarded texels. */
    public record Spr(float u0, float v0, float du, float dv, boolean holes) {
    }

    /** Where {@link #write} puts a vertex: the cutout layer (the texture has holes) or the solid one. */
    public interface Sink {
        void vertex(boolean cutout, float x, float y, float z, int color, float u, float v, int light);
    }

    private static final float L0X, L0Y, L0Z, L1X, L1Y, L1Z;

    static {
        float n = (float) (1 / Math.sqrt(0.2 * 0.2 + 1 + 0.7 * 0.7));
        L0X = 0.2f * n;
        L0Y = n;
        L0Z = -0.7f * n;
        L1X = -0.2f * n;
        L1Y = n;
        L1Z = 0.7f * n;
    }

    /**
     * Write the calls' quads into their section's buffers (meshing worker; reads only the arguments). Vertices are
     * relative to the section's origin (ox, oy, oz) and may lie outside the section. light[i] is the packed light of
     * calls[i], but for what is drawn at full light. The colour carries the entity shader's directional shading for
     * the vertex normal (two lights, 0.6 each, over 0.4 ambient), so the result equals the block entity renderer's.
     */
    public static int write(Sink out, Bake.Call[] calls, int[] lights, Map<Identifier, Spr> sprites, int ox, int oy, int oz, boolean nether) {
        int written = 0;
        final int S = GlFrame.STRIDE;
        float l1y = nether ? -L1Y : L1Y;
        float[] vx = new float[4], vy = new float[4], vz = new float[4], vu = new float[4], vv = new float[4];
        int[] vc = new int[4];
        for (int ci = 0; ci < calls.length; ci++) {
            Bake.Call c = calls[ci];
            Spr sp = sprites.get(c.texture == null ? UtilsClient.WHITE : c.texture);
            if (sp == null) throw new IllegalStateException("no sprite for " + c.texture);
            boolean cutout = sp.holes();
            float m00 = c.m00, m01 = c.m01, m02 = c.m02, m10 = c.m10, m11 = c.m11, m12 = c.m12, m20 = c.m20, m21 = c.m21, m22 = c.m22;
            float tx = c.m30 + (BlockPos.getX(c.pos) - ox), ty = c.m31 + (BlockPos.getY(c.pos) - oy), tz = c.m32 + (BlockPos.getZ(c.pos) - oz);
            // normals transform by the cofactor matrix (the inverse transpose up to scale; they are normalised below)
            float n00 = m11 * m22 - m12 * m21, n01 = m12 * m20 - m10 * m22, n02 = m10 * m21 - m11 * m20;
            float n10 = m02 * m21 - m01 * m22, n11 = m00 * m22 - m02 * m20, n12 = m01 * m20 - m00 * m21;
            float n20 = m01 * m12 - m02 * m11, n21 = m02 * m10 - m00 * m12, n22 = m00 * m11 - m01 * m10;
            float det = m00 * n00 + m01 * n01 + m02 * n02;
            float sign = det < 0 ? -1 : 1;
            int light = c.fullBright ? LightCoordsUtil.FULL_BRIGHT : lights[ci];
            float[] q = c.quads;
            int base = c.color;
            for (int i = 0; i + 4 * S <= c.floats; i += 4 * S) {
                for (int k = 0; k < 4; k++) {
                    int j = i + k * S;
                    float x = q[j], y = q[j + 1], z = q[j + 2];
                    vx[k] = m00 * x + m10 * y + m20 * z + tx;
                    vy[k] = m01 * x + m11 * y + m21 * z + ty;
                    vz[k] = m02 * x + m12 * y + m22 * z + tz;
                    vu[k] = sp.u0() + q[j + 3] * sp.du();
                    vv[k] = sp.v0() + q[j + 4] * sp.dv();
                    int col = c.colored ? Float.floatToRawIntBits(q[j + 5]) : base;
                    float shade = 1;
                    if (c.lit) {
                        float a = q[j + 6], b = q[j + 7], d = q[j + 8];
                        float nx = (n00 * a + n10 * b + n20 * d) * sign, ny = (n01 * a + n11 * b + n21 * d) * sign, nz = (n02 * a + n12 * b + n22 * d) * sign;
                        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (len > 0) {
                            nx /= len;
                            ny /= len;
                            nz /= len;
                        }
                        // as the renderer's vertex format stores a normal: a signed byte per axis
                        nx = (int) (nx * 127f) / 127f;
                        ny = (int) (ny * 127f) / 127f;
                        nz = (int) (nz * 127f) / 127f;
                        shade = Math.min(1f, (Math.max(0f, L0X * nx + L0Y * ny + L0Z * nz) + Math.max(0f, L1X * nx + l1y * ny + L1Z * nz)) * 0.6f + 0.4f);
                    }
                    int r = Math.round(((col >> 16) & 255) * shade), g = Math.round(((col >> 8) & 255) * shade), bl = Math.round((col & 255) * shade);
                    vc[k] = 0xFF000000 | (r << 16) | (g << 8) | bl;
                }
                for (int k = 0; k < 4; k++) out.vertex(cutout, vx[k], vy[k], vz[k], vc[k], vu[k], vv[k], light);
                written += 4;
                if (!c.cull) {
                    for (int k = 3; k >= 0; k--) out.vertex(cutout, vx[k], vy[k], vz[k], vc[k], vu[k], vv[k], light);
                    written += 4;
                }
            }
        }
        return written;
    }
}
