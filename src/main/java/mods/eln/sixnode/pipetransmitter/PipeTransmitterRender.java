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

/** The Voltage Probe's body, its pale signal pin, and an iron stem from the body's top up into the port. */
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
        box(1.8f / 16, 2.4f / 16, 1.2f / 16);
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** A box from x0 to x1 off the face, 2h across, both windings (no culling trouble for a box this small). */
    static void box(float x0, float x1, float h) {
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) v[i] = new float[]{(i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? -h : h, (i & 4) == 0 ? -h : h};
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
