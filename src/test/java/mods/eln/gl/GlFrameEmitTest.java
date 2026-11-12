package mods.eln.gl;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Display-list replay (GlFrame.emit) gives the same vertices as drawing the same quads directly, under rotations,
 * mirrors, uniform and non-uniform scales; and a transformer coil's compiled list matches its shape (render cost,
 * 2026-11-12).
 */
class GlFrameEmitTest {
    static final int QUADS = 500;

    /** Random quads: positions in a block, normals of random length (some zero), colours and uvs. */
    static float[][] quads(Random r) {
        float[][] v = new float[QUADS * 4][];
        for (int i = 0; i < v.length; i++) {
            float s = r.nextInt(10) == 0 ? 0 : 0.2f + 3 * r.nextFloat();
            float nx = (float) r.nextGaussian(), ny = (float) r.nextGaussian(), nz = (float) r.nextGaussian();
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            v[i] = new float[]{r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(), s * nx / l, s * ny / l, s * nz / l};
        }
        return v;
    }

    static void draw(float[][] v) {
        GL11.glBegin(GL11.GL_QUADS);
        for (float[] p : v) {
            GL11.glNormal3f(p[5], p[6], p[7]);
            GL11.glTexCoord2f(p[3], p[4]);
            GL11.glVertex3f(p[0], p[1], p[2]);
        }
        GL11.glEnd();
    }

    static Matrix4f matrix(Random r, int kind) {
        Matrix4f m = new Matrix4f().translate(r.nextFloat() * 10, r.nextFloat() * 10, r.nextFloat() * 10)
            .rotate((float) (r.nextFloat() * 2 * Math.PI), r.nextFloat() + 0.1f, r.nextFloat(), r.nextFloat()).normalize3x3();
        m.translate(r.nextFloat(), r.nextFloat(), r.nextFloat()).rotate(r.nextFloat() * 3, 0, 1, 0);
        return switch (kind) {
            case 0 -> m;
            case 1 -> m.scale(0.0625f);
            case 2 -> m.scale(1, 1, -1);
            default -> m.scale(1, 0.3f + r.nextFloat(), 2);
        };
    }

    static float[] frame(Matrix4f m, Runnable body) {
        return frame(m, body, true);
    }

    /** One frame of body at base m; its batch's floats (copied only if asked: the timing leaves the copy out). */
    static float[] frame(Matrix4f m, Runnable body, boolean copy) {
        GlFrame f = GlFrame.open(m, new org.joml.Matrix3f(m).invert().transpose());
        try {
            body.run();
            var b = f.batches().values().iterator().next();
            return copy ? java.util.Arrays.copyOf(b.data, b.floats) : null;
        } finally {
            GlFrame.close();
            GlFrame.beginRenderFrame();
        }
    }

    @Test
    void listReplayMatchesDirectDrawing() {
        Random r = new Random(7);
        float[][] v = quads(r);
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        draw(v);
        GL11.glEndList();
        for (int k = 0; k < 40; k++) {
            Matrix4f m = matrix(r, k % 4);
            float[] direct = frame(m, () -> draw(v)), replayed = frame(m, () -> GL11.glCallList(list));
            assertEquals(direct.length, replayed.length);
            for (int i = 0; i < direct.length; i++)
                if (i % GlFrame.STRIDE == 5) assertEquals(Float.floatToRawIntBits(direct[i]), Float.floatToRawIntBits(replayed[i]), "colour " + i);
                else assertEquals(direct[i], replayed[i], 2e-5f * Math.max(1, Math.abs(direct[i])), "matrix kind " + (k % 4) + ", float " + i);
        }
        GL11.glDeleteLists(list, 1);
    }

    /** A transformer coil drawn from its compiled list gives the vertices of drawing its shape directly. */
    @Test
    void coilListMatchesItsShape() {
        Random r = new Random(11);
        for (float build : new float[]{0.3f, 1.25f, 2.9f}) {
            Matrix4f m = matrix(r, 1);
            int list = mods.eln.transparentnode.transformer.TransformerDescriptor.coilList(build);
            float[] direct = frame(m, () -> mods.eln.transparentnode.transformer.TransformerDescriptor.coilShape(build));
            float[] replayed = frame(m, () -> GL11.glCallList(list));
            assertEquals(direct.length, replayed.length);
            for (int i = 0; i < direct.length; i++)
                if (i % GlFrame.STRIDE == 5) assertEquals(Float.floatToRawIntBits(direct[i]), Float.floatToRawIntBits(replayed[i]));
                else assertEquals(direct[i], replayed[i], 2e-5f * Math.max(1, Math.abs(direct[i])));
        }
    }
}
