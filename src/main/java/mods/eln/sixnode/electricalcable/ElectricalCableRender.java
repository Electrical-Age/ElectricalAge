package mods.eln.sixnode.electricalcable;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalCableRender extends SixNodeElementRender {

    ElectricalCableDescriptor descriptor;

    int color = 0;

    public ElectricalCableRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalCableDescriptor) descriptor;
    }

//	double voltage = 0, current = 0, temperature = 0;

    public boolean drawCableAuto() {
        return false;
    }

    @Override
    public void draw() {
        Utils.setGlColorFromDye(color, 1.0f);
        UtilsClient.bindTexture(descriptor.render.cableTexture);
        glListCall();
        GL11.glColor3f(1f, 1f, 1f);
    }

    /**
     * Burnt through (26.x): each connected arm keeps only its outer part, charred at the cut, with the copper core
     * sticking out a little; the middle and the node are gone.
     */
    @Override
    public boolean drawBurnt() {
        mods.eln.cable.CableRenderType ct = CableRender.connectionType(this, side);
        float w = descriptor.render.widthDiv2, h = descriptor.render.height / 16f;
        for (int idx = 0; idx < 4; idx++) {
            if ((connectedSide.mask & (1 << idx)) == 0) continue;
            LRDU lrdu = LRDU.fromInt(idx);
            // arm axis and the block edge it reaches
            boolean alongZ = lrdu == LRDU.Left || lrdu == LRDU.Right;
            float edge = (lrdu == LRDU.Left || lrdu == LRDU.Down) ? -0.5f : 0.5f;
            float keep = 0.45f, cut = edge * (1 - keep);
            GL11.glPushMatrix();
            if (alongZ) {
                GL11.glTranslatef(0, 0, edge);
                GL11.glScalef(1, 1, keep);
                GL11.glTranslatef(0, 0, -edge);
            } else {
                GL11.glTranslatef(0, edge, 0);
                GL11.glScalef(1, keep, 1);
                GL11.glTranslatef(0, -edge, 0);
            }
            Utils.setGlColorFromDye(color, 1.0f);
            UtilsClient.bindTexture(descriptor.render.cableTexture);
            CableRender.drawCable(descriptor.render, new mods.eln.misc.LRDUMask(1 << idx), ct);
            GL11.glPopMatrix();
            // charred end and exposed core at the cut
            float dir = Math.signum(edge), c0 = cut, c1 = cut - dir * 0.04f, core = cut - dir * 0.09f;
            box(alongZ, 0.0f, h + 0.002f, -w - 0.002f, w + 0.002f, c0 + dir * 0.06f, c0, 0.12f, 0.10f, 0.09f);
            box(alongZ, h * 0.3f, h * 0.7f, -w * 0.35f, w * 0.35f, c0, core, 0.85f, 0.45f, 0.20f);
        }
        GL11.glColor3f(1f, 1f, 1f);
        return true;
    }

    /** An untextured box: x (height) from x0 to x1, across from a0 to a1, along the arm from b0 to b1. */
    static void box(boolean alongZ, float x0, float x1, float a0, float a1, float b0, float b1, float r, float g, float b) {
        UtilsClient.disableTexture();
        UtilsClient.disableCulling();
        GL11.glColor3f(r, g, b);
        float[][] v = new float[8][];
        int i = 0;
        for (float x : new float[]{x0, x1})
            for (float a : new float[]{a0, a1})
                for (float bb : new float[]{b0, b1}) v[i++] = alongZ ? new float[]{x, a, bb} : new float[]{x, bb, a};
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        float[][] normals = {{-1, 0, 0}, {1, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        GL11.glBegin(GL11.GL_QUADS);
        for (int f = 0; f < 6; f++) {
            float[] n = normals[f];
            if (f >= 2) {
                // the four sides: across or along the arm, in the axis order of v
                float[] p0 = v[faces[f][0]], p2 = v[faces[f][2]];
                n = new float[]{0, p0[1] == p2[1] ? Math.signum(p0[1]) : 0, p0[2] == p2[2] ? Math.signum(p0[2]) : 0};
            }
            GL11.glNormal3f(n[0], n[1], n[2]);
            for (int k : faces[f]) GL11.glVertex3f(v[k][0], v[k][1], v[k][2]);
        }
        GL11.glEnd();
        UtilsClient.enableCulling();
        UtilsClient.enableTexture();
    }

    @Override
    public void glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side));
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side));
    }

    @Override
    public boolean glListEnable() {
        return true;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            color = (b >> 4) & 0xF;
        /*	voltage = stream.readShort() / NodeBase.networkSerializeUFactor;
			current = stream.readShort() / NodeBase.networkSerializeIFactor;
			temperature = stream.readShort() / NodeBase.networkSerializeTFactor;*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.render;
    }

    @Override
    public int getCableDry(LRDU lrdu) {
        return color;
    }
}
