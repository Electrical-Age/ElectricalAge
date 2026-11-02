package mods.eln.sixnode.pipetransmitter;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.gl.GL11;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.registry.ElnDeviceRegistry;

/**
 * The Voltage Probe's body, its pale signal pin, an iron stem from the body's top up into the port, and its kind on the
 * body's top: a coloured plate with a raised white letter, P, T or Q (the letter for colourblind players: a lab test
 * found the three kinds looked the same).
 */
public class PipeTransmitterRender extends SixNodeElementRender {
    final PipeTransmitterDescriptor descriptor;

    public PipeTransmitterRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (PipeTransmitterDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        front.glRotateOnX();
        descriptor.draw();
        drawStem();
        drawMark(descriptor.sensing);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return lrdu == front ? ElnDeviceRegistry.signalCableDescriptor.render : null;
    }

    /** The stem (element frame: x off the face): from inside the body up to the port's centre, a little wider than an iron
     *  pipe is drawn, with a flange where it leaves the body. */
    static void drawStem() {
        float top = PipeTransmitterDescriptor.PORT_HEIGHT / 16f;
        GL11.glColor3f(0.42f, 0.42f, 0.45f);
        box(1 / 16f, top + 0.6f / 16, 0.7f / 16);
        box(2f / 16, 2.6f / 16, 0.9f / 16);
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** The letters, 3 x 5 cells, rows from the top: P, T, Q. */
    static final String[][] GLYPHS = {
        {"111", "101", "111", "100", "100"},
        {"111", "010", "010", "010", "010"},
        {"111", "101", "101", "111", "001"}};
    /** The plates' colours: pressure blue, temperature red, steam quality green. */
    static final float[][] COLOURS = {{0.20f, 0.42f, 0.90f}, {0.88f, 0.25f, 0.15f}, {0.25f, 0.72f, 0.35f}};

    /** Its kind on the body's top (element frame: the body is 2 px high, 4 across along y and 6 along z, -3..3). */
    static void drawMark(int sensing) {
        int k = Math.max(0, Math.min(2, sensing));
        float top = 2f / 16, cell = 1.1f / 16;
        float[] c = COLOURS[k];
        GL11.glColor3f(c[0], c[1], c[2]);
        box(top, top + 0.1f / 16, -1.9f / 16, 1.9f / 16, -2.9f / 16, 2.9f / 16);
        GL11.glColor3f(0.95f, 0.95f, 0.92f);
        for (int r = 0; r < 5; r++)
            for (int col = 0; col < 3; col++) {
                if (GLYPHS[k][r].charAt(col) != '1') continue;
                // (seen from off the face: +z up the letter, +y to its right)
                float y0 = (col - 1.5f) * cell, z1 = (2.5f - r) * cell;
                // raised past the stem's flange (top + 0.6 px), so the port's square doesn't cut the letter's
                // middle row (AdventurAgent: P read like a ∩)
                box(top + 0.65f / 16, top + 0.9f / 16, y0, y0 + cell, z1 - cell, z1);
            }
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** A box from x0 to x1 off the face, 2h across, both windings (no culling trouble for a box this small). */
    static void box(float x0, float x1, float h) {
        box(x0, x1, -h, h, -h, h);
    }

    static void box(float x0, float x1, float y0, float y1, float z0, float z1) {
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) v[i] = new float[]{(i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1};
        int[][] faces = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
        UtilsClient.disableTexture();
        GL11.glBegin(GL11.GL_QUADS);
        for (int[] f : faces) {
            for (int k = 0; k < 4; k++) GL11.glVertex3f(v[f[k]][0], v[f[k]][1], v[f[k]][2]);
            for (int k = 3; k >= 0; k--) GL11.glVertex3f(v[f[k]][0], v[f[k]][1], v[f[k]][2]);
        }
        GL11.glEnd();
        UtilsClient.enableTexture();
    }
}
