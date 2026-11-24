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

        if (drawScreen && view != null) drawView();

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
                    int y = stream.readInt(), stock = stream.readInt(), n = stream.readInt();
                    if (n < 0 || n > 1 << 20) break;
                    byte[] b = new byte[n];
                    stream.readFully(b);
                    var scan = mods.eln.transparentnode.groundradar.GroundScan.decode(b);
                    if (scan == null) break;
                    headY = y;
                    pipesLeft = stock;
                    view = scan;
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

    // ---- the monitor's view (AutoMinerElement.view): the drill head's Y, the pipes left, and the ground around the head,
    // its three layers side by side (above it, its own, under it), in the screen's left part (the 1.12 ore view's place)

    private mods.eln.transparentnode.groundradar.GroundScan view;
    private int headY, pipesLeft;
    private boolean viewDirty;
    private net.minecraft.client.renderer.texture.DynamicTexture viewTexture;
    private net.minecraft.resources.Identifier viewTex;
    private static int viewNext;
    /** The view's tiles: cells across, the gap between them (texels). */
    static final int TILE_GAP = 2;

    private void drawView() {
        var v = view;
        int n = v.n, L = v.layers.length, w = L * n + (L - 1) * TILE_GAP;
        if (viewDirty || viewTexture == null) {
            viewDirty = false;
            try {
                if (viewTexture == null || viewTexture.getPixels() == null || viewTexture.getPixels().getWidth() != w || viewTexture.getPixels().getHeight() != n) {
                    releaseView();
                    viewTexture = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "eln auto miner view", w, n, false);
                    viewTex = net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "dynamic/auto_miner_view_" + viewNext++);
                    net.minecraft.client.Minecraft.getInstance().getTextureManager().register(viewTex, viewTexture);
                }
                var img = viewTexture.getPixels();
                for (int y = 0; y < n; y++)
                    for (int x = 0; x < w; x++) {
                        int k = x / (n + TILE_GAP), i = x % (n + TILE_GAP);
                        img.setPixel(x, y, i < n ? mods.eln.transparentnode.groundradar.GroundScan.colour(v, k, i, y) : 0xFF000000);
                    }
                viewTexture.upload();
            } catch (IllegalStateException e) {
                return;
            }
        }
        UtilsClient.disableLight();
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 1, 0);
        GL11.glScalef(1 / 128f, -1 / 128f, 1);
        UtilsClient.drawString(mods.eln.i18n.I18N.tr("Y %1$", headY), 2, 1, 0xFF55FF55);
        UtilsClient.drawString(mods.eln.i18n.I18N.tr("Pipes %1$", pipesLeft), 2, 9, pipesLeft > 0 ? 0xFF55FF55 : 0xFFFF5555);
        float x0 = 2, y0 = 19, x1 = 78, y1 = y0 + (x1 - x0) * n / w;
        GL11.bindTexture(viewTex);
        GL11.setFullBright(true);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x0, y0);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x0, y1);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x1, y1);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x1, y0);
        GL11.glEnd();
        GL11.setFullBright(false);
        UtilsClient.drawString("+2", 8, (int) y1 + 2, 0xFF00AA00);
        UtilsClient.drawString("Y", 37, (int) y1 + 2, 0xFF00AA00);
        UtilsClient.drawString("-2", 62, (int) y1 + 2, 0xFF00AA00);
        GL11.glPopMatrix();
        UtilsClient.enableLight();
    }

    private void releaseView() {
        if (viewTex != null) net.minecraft.client.Minecraft.getInstance().getTextureManager().release(viewTex);
        viewTex = null;
        viewTexture = null;
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
