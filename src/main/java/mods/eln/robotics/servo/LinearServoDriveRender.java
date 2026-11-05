package mods.eln.robotics.servo;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Linear Servo Drive (LinearServoDriveDescriptor): the motor, its drum, and the status lamp. */
public class LinearServoDriveRender extends TransparentNodeElementRender implements mods.eln.introspect.ClientDescribable {
    final LinearServoDriveDescriptor desc;
    public byte status;
    /** Its bus address (0: none), and whether its bus pin is on a bus (its screen). */
    public int address;
    public boolean onBus;

    public LinearServoDriveRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (LinearServoDriveDescriptor) d;
    }

    @Override
    public void draw() {
        drawPins();
        front.glRotateXnRef();
        desc.draw(status);
    }

    /**
     * Its floor pins, where cables meet them (LinearServoDriveDescriptor.spec): on each flank, three small tabs on the
     * floor edge a quarter block apart, white for signals and violet for the bus (in block coordinates, before the
     * model's turn; left as seen facing the flank: up x (into the flank)).
     */
    void drawPins() {
        if (front == null) return;
        mods.eln.misc.UtilsClient.disableTexture();
        for (int flank = 0; flank < 2; flank++) {
            var out = (flank == 0 ? front.left() : front.right()).toEnumFacing();
            float ox = out.getStepX(), oz = out.getStepZ();
            float lx = -oz, lz = ox;   // (left = up x forward, forward = -out)
            for (int pin = -1; pin <= 1; pin++) {
                boolean bus = flank == 1 && pin == -1;   // (the right flank's right pin)
                if (bus) mods.eln.gl.GL11.glColor3f(0.62f, 0.38f, 0.80f);
                else mods.eln.gl.GL11.glColor3f(0.9f, 0.9f, 0.86f);
                float cx = 0.5f * ox + pin * 0.25f * lx, cz = 0.5f * oz + pin * 0.25f * lz;
                box(cx, cz, ox, oz, lx, lz);
            }
        }
        mods.eln.misc.UtilsClient.enableTexture();
        mods.eln.gl.GL11.glColor3f(1f, 1f, 1f);
    }

    /** A tab 2/16 wide, 1/16 high and 1/32 proud of the face, its centre on the face at (cx, cz), on the floor. */
    static void box(float cx, float cz, float ox, float oz, float lx, float lz) {
        float w = 1 / 16f, d = 1 / 32f, y0 = -0.5f, y1 = -0.5f + 1 / 16f;
        float[][] c = new float[4][];   // the footprint: inner left, outer left, outer right, inner right
        c[0] = new float[]{cx + lx * w, cz + lz * w};
        c[1] = new float[]{cx + lx * w + ox * d, cz + lz * w + oz * d};
        c[2] = new float[]{cx - lx * w + ox * d, cz - lz * w + oz * d};
        c[3] = new float[]{cx - lx * w, cz - lz * w};
        mods.eln.gl.GL11.glBegin(mods.eln.gl.GL11.GL_QUADS);
        for (float y : new float[]{y1}) for (int i = 0; i < 4; i++) mods.eln.gl.GL11.glVertex3f(c[i][0], y, c[i][1]);
        for (int i = 0; i < 4; i++) {   // the sides
            float[] a = c[i], b = c[(i + 1) % 4];
            mods.eln.gl.GL11.glVertex3f(a[0], y0, a[1]);
            mods.eln.gl.GL11.glVertex3f(a[0], y1, a[1]);
            mods.eln.gl.GL11.glVertex3f(b[0], y1, b[1]);
            mods.eln.gl.GL11.glVertex3f(b[0], y0, b[1]);
        }
        mods.eln.gl.GL11.glEnd();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            status = stream.readByte();
            address = stream.readByte() & 0xFF;
            onBus = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void describeClient(mods.eln.introspect.Report r) {
        var names = LinearServoDriveElement.STATUS_NAMES;
        r.sync().choice(mods.eln.introspect.Keys.STATUS, status >= 0 && status < names.size() ? names.get(status) : names.get(0), names);
    }
}
