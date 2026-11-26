package mods.eln.transparentnode.autominer;

import java.awt.Color;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.entity.player.Player;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

public class AutoMinerRender extends TransparentNodeElementRender {
    private final AutoMinerDescriptor descriptor;
    private final float[] buttonsState;
    private final boolean[] ledsAState;
    private final boolean[] ledsPState;


    private final PhysicalInterpolatorNoRebound pipeLengthInterpol = new PhysicalInterpolatorNoRebound(0.4f, 2f, 0.8f);
    /** The drill pipe turning (its published speed in degrees a second, eased over a second). */
    private final mods.eln.anim.Spin pipe = new mods.eln.anim.Spin(1, 1);

    private final LinkedList<String> logs = new LinkedList<String>();

    private final int logSizeMax = 9;

    private boolean powerOk;


    private short pipeLength = 0;
    AutoMinerSlowProcess.jobType job;
    private boolean boot = true;
    public boolean silkTouch = false;

    public AutoMinerRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (AutoMinerDescriptor) descriptor;

        buttonsState = new float[this.descriptor.buttonsCount];
        for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
            buttonsState[idx] = (float) Math.random();
        }

        ledsAState = new boolean[this.descriptor.ledsACount];
        for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
            ledsAState[idx] = Math.random() > 0.5;
        }

        ledsPState = new boolean[this.descriptor.ledsPCount];
        for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
            ledsPState[idx] = Math.random() > 0.5;
        }

        addLoopedSound(new mods.eln.sound.LoopedSound("eln:autominer", coordonate(), mods.eln.sound.LoopedSound.Attenuation.LINEAR) {
            @Override
            public float getVolume() {
                if (powerOk &&
                    (job == AutoMinerSlowProcess.jobType.ore || job == AutoMinerSlowProcess.jobType.pipeAdd ||
                        job == AutoMinerSlowProcess.jobType.pipeRemove))
                    return 0.5f;   // (Baughn 2026-11-07: too loud; it was 2.13, which the engine played at 1)
                else
                    return 0f;
            }
        });
    }

    private void pushLog(String string) {
        logs.addFirst(string);
        if (logs.size() > logSizeMax)
            logs.removeLast();
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        GL11.glRotatef(pipe.deg(), 0, -1, 0);
        GL11.glPushMatrix();
        GL11.glScalef(0.99f, 0.99f, 0.99f);
        descriptor.pipe.draw();
        GL11.glPopMatrix();

        int len = (int) pipeLengthInterpol.get();
        GL11.glTranslatef(0, -(pipeLengthInterpol.get() - len), 0);
        for (int idx = len + 2; idx != 0; idx--) {
            if (idx != 1) {
                descriptor.pipe.draw();
            } else {
                descriptor.head.draw();
            }
            GL11.glTranslatef(0, -1f, 0);
        }
        GL11.glPopMatrix();

        for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
            buttonsState[idx] = idx == job.ordinal() && powerOk ? 1 : 0;
        }

        front.glRotateXnRef();

        boolean drawScreen = UtilsClient.clientDistanceTo(tileEntity) < 20 && powerOk;

        UtilsClient.disableCulling();

        GL11.glPushMatrix();
        GL11.glTranslatef(-1.57031f, 1.8125f - 1.5f - 0.02f, -0.3125f + 0.02f);
        GL11.glRotatef(90, 0, 1, 0);

        if (drawScreen) {
            UtilsClient.disableLight();
            GL11.glPushMatrix();
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glScalef(1 / 128f, -1 / 128f, 1);
            int idx = 0;
            for (String log : logs) {
                // 1.12 colour codes: bright green for the newest line, dark green for the rest
                UtilsClient.drawString(idx == 0 ? "> " + log.substring(2) : log, 80, 1 + idx, idx == 0 ? 0xFF55FF55 : 0xFF00AA00);
                idx += 8;
            }
            GL11.glPopMatrix();
            UtilsClient.enableLight();
        }

        if (drawScreen && viewSlice != null && viewCross != null) drawView();

        GL11.glPopMatrix();
        UtilsClient.enableCulling();

        descriptor.draw(false, buttonsState, ledsAState, ledsPState);
    }
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
            if (powerOk) {
                if (Math.random() < 0.2 * deltaT)
                    ledsAState[idx] = !ledsAState[idx];
            } else {
                ledsAState[idx] = true;
            }
        }

        for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
            if (powerOk) {
                if (Math.random() < 0.2 * deltaT)
                    ledsPState[idx] = !ledsPState[idx];
            } else {
                ledsPState[idx] = true;
            }
        }

        pipeLengthInterpol.step(deltaT);
        if (pipeLengthInterpol.get() < 0) pipeLengthInterpol.setPos(0);
        pipe.step(deltaT);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            pipeLength = stream.readShort();
            if (job != (job = AutoMinerSlowProcess.jobType.values()[stream.readByte()])) {
                switch (job) {
                    case ore:
                        pipe.speed(360);
                        break;
                    default:
                        pipe.speed(0);
                        break;
                }
            }
            powerOk = stream.readBoolean();
            silkTouch = stream.readBoolean();

            if (!powerOk) {
                logs.clear();
                pushLog("[i] Boot... Done.");
                pushLog("* Waiting opcode.");
            }

            if (boot) {
                boot = false;
                pipeLengthInterpol.setPos(pipeLength);
            }
            pipeLengthInterpol.setTarget(pipeLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            switch (stream.readByte()) {
                case AutoMinerElement.pushLogId:
                    pushLog(stream.readUTF());
                    break;
                case AutoMinerElement.viewId: {
                    int y = stream.readInt(), stock = stream.readInt();
                    var slice = readScan(stream);
                    var cross = readScan(stream);
                    if (slice == null || cross == null) break;
                    headY = y;
                    pipesLeft = stock;
                    viewSlice = slice;
                    viewCross = cross;
                    viewDirty = true;
                    break;
                }
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- the monitor's views (AutoMinerElement.view; Baughn 2026-11-24), in the screen's left part (where 1.12 drew an
    // ore scanner's picture), four quarters: its stats in a frame; XZ, the slice at its mining height; XY and YZ, the
    // cone's cuts from the miner down to bedrock along x and along z with the pipe and the drill head marked

    private mods.eln.transparentnode.groundradar.GroundScan viewSlice, viewCross;
    private int headY, pipesLeft;
    private boolean viewDirty;
    private final mods.eln.client.gui.PixelTexture texXZ = new mods.eln.client.gui.PixelTexture("auto_miner_xz"),
        texXY = new mods.eln.client.gui.PixelTexture("auto_miner_xy"), texYZ = new mods.eln.client.gui.PixelTexture("auto_miner_yz");
    /** The side views' size (texels; the cone drawn to scale in depth and width). */
    static final int CONE_PX = 64;
    /** The quarters (screen units, 1/128 block): x, y of each, and their size. */
    static final float Q = 34, X0 = 2, X1 = 41, Y0 = 1, Y1 = 37;
    static final int GREEN = 0xFF55FF55, DIM = 0xFF00AA00;
    /** Text scale in the views, and a label's backing (screen units). */
    static final float TEXT = 0.6f, LABEL_W = 9, LABEL_H = 6;
    /** How far the labels' backing stands off the views (blocks; the screen's z is unscaled). */
    static final float LIFT = 1 / 512f;

    private static void label(String s, float x, float y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x + 1, y + 1, 0);
        GL11.glScalef(TEXT, TEXT, 1);
        UtilsClient.drawString(s, 0, 0, GREEN);
        GL11.glPopMatrix();
    }

    /** A filled rectangle (x, y, w, h) in colour argb. */
    private static void rect(float x, float y, float w, float h, int argb) {
        GL11.bindTexture(UtilsClient.WHITE);
        GL11.glColor4f((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, 1);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static mods.eln.transparentnode.groundradar.GroundScan readScan(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 1 << 20) return null;
        byte[] b = new byte[n];
        in.readFully(b);
        return mods.eln.transparentnode.groundradar.GroundScan.decode(b);
    }

    private void drawView() {
        var slice = viewSlice;
        var cross = viewCross;
        if (viewDirty || texXZ.id() == null) {
            viewDirty = false;
            texXZ.paint(slice.n, slice.n, (x, y) -> mods.eln.transparentnode.groundradar.GroundScan.colour(slice, 0, x, y));
            int headDepth = node().y - 1 - headY;
            texXY.paint(CONE_PX, CONE_PX, (x, y) -> cone(cross, x, y, true, headDepth));
            texYZ.paint(CONE_PX, CONE_PX, (x, y) -> cone(cross, x, y, false, headDepth));
        }
        UtilsClient.disableLight();
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 1, 0);
        GL11.glScalef(1 / 128f, -1 / 128f, 1);
        GL11.setFullBright(true);
        frame(X0, Y0, Q, Q, DIM);
        quad(texXZ.id(), X1, Y0, Q, Q);
        quad(texXY.id(), X0, Y1, Q, Q);
        quad(texYZ.id(), X1, Y1, Q, Q);
        // the labels' backing, lifted off the views toward the viewer (+z here faces out of the screen), or it z-fights them
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, LIFT);
        for (float[] at : new float[][]{{X1, Y0}, {X0, Y1}, {X1, Y1}}) rect(at[0], at[1], LABEL_W, LABEL_H, 0xFF000000);
        GL11.glPopMatrix();
        GL11.setFullBright(false);
        // (the stats and labels at TEXT of the log's size: "Pipes 64" fits the frame)
        GL11.glPushMatrix();
        GL11.glTranslatef(X0 + 3, Y0 + 3, 0);
        GL11.glScalef(TEXT, TEXT, 1);
        UtilsClient.drawString(mods.eln.i18n.I18N.tr("Y %1$", headY), 0, 0, GREEN);
        UtilsClient.drawString(mods.eln.i18n.I18N.tr("Pipes %1$", pipesLeft), 0, 15, pipesLeft > 0 ? GREEN : 0xFFFF5555);
        UtilsClient.drawString(silkTouch ? mods.eln.i18n.I18N.tr("Silk on") : mods.eln.i18n.I18N.tr("Silk off"), 0, 30, GREEN);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 2 * LIFT);   // (and the labels off their backing)
        label("XZ", X1, Y0);
        label("XY", X0, Y1);
        label("YZ", X1, Y1);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
        UtilsClient.enableLight();
    }

    private mods.eln.misc.Coordonate node() {
        return coordonate();
    }

    /**
     * The cone's cut, a texel at (x, y) of CONE_PX x CONE_PX: depth to scale down from the miner to the floor, width to scale
     * across the bottom layer's; outside the cone black; the pipe light grey down to the head, the head red.
     */
    static int cone(mods.eln.transparentnode.groundradar.GroundScan p, int x, int y, boolean alongX, int headDepth) {
        var last = p.layers[p.layers.length - 1];
        double total = last.depth() + last.cell(), maxW = (double) p.n * last.cell();
        double d = (y + 0.5) / CONE_PX * total;
        int k = Math.max(0, p.layerAt((int) d));
        var l = p.layers[k];
        double w = (double) p.n * l.cell(), xw = ((x + 0.5) / CONE_PX - 0.5) * maxW;
        if (Math.abs(xw) >= w / 2) return 0xFF000000;
        int c = p.n / 2;
        boolean centre = x == CONE_PX / 2 || x == CONE_PX / 2 - 1;
        double rowBlocks = total / CONE_PX;
        if (centre && Math.abs(d - headDepth) <= rowBlocks) return 0xFFFF3030;
        if (centre && d < headDepth) return 0xFFC8C8C8;
        int i = Math.max(0, Math.min(p.n - 1, (int) Math.floor((xw + w / 2) / l.cell())));
        return alongX ? mods.eln.transparentnode.groundradar.GroundScan.colour(p, k, i, c) : mods.eln.transparentnode.groundradar.GroundScan.colour(p, k, c, i);
    }

    /** A texture over (x, y, w, h), screen units. */
    private static void quad(net.minecraft.resources.Identifier tex, float x, float y, float w, float h) {
        if (tex == null) return;
        GL11.bindTexture(tex);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }

    /** A one-unit outline around (x, y, w, h) in colour argb. */
    private static void frame(float x, float y, float w, float h, int argb) {
        rect(x, y, w, 1, argb);
        rect(x, y + h - 1, w, 1, argb);
        rect(x, y, 1, h, argb);
        rect(x + w - 1, y, 1, h, argb);
    }

    private void releaseView() {
        texXZ.release();
        texXY.release();
        texYZ.release();
    }

    @Override
    public void destructor() {
        super.destructor();
        releaseView();
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
