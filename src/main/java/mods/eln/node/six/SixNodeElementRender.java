package mods.eln.node.six;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.*;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import mods.eln.gl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client state and drawing of one six-node element (1.12, on the GL11 shim since P3). Server-safe on purpose: the
 * block entity holds these, so no client classes here; device subclasses are created only on the client (ElnRenders).
 * Looped sounds (R4) and GUI screens (P4b) are not here yet.
 */
public abstract class SixNodeElementRender {
    public SixNodeEntity tileEntity;
    public Direction side;
    // public SixNodeDescriptor descriptor;
    public LRDUMask connectedSide = new LRDUMask();
    /** The element burnt out (see SixNodeElement.burn); SixNodeRender scorches it. */
    public boolean burnt;
    int glList, cableList[];
    boolean cableListReady[] = {false, false, false, false};
    boolean glListReady = false;

    /** Its slot in the block (SixNodeElement.slot) and, past the six faces, its offset from the face's centre (world
     *  voxels); set by SixNodeEntity after construction. */
    public int slot;
    public int ox, oy, oz;
    /** Past the faces: the edges it connects on (bit LRDU.toInt()), where its pins are. */
    public int pinMask;

    public SixNodeElementRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        this.sixNodeDescriptor = descriptor;
        this.tileEntity = tileEntity;
        this.side = side;
        this.slot = side.getInt();
        cableList = new int[4];
        // this.descriptor = descriptor;
        if (glListEnable()) {
            glList = UtilsClient.glGenListsSafe();

        }
        cableList[0] = UtilsClient.glGenListsSafe();
        cableList[1] = UtilsClient.glGenListsSafe();
        cableList[2] = UtilsClient.glGenListsSafe();
        cableList[3] = UtilsClient.glGenListsSafe();
    }

    public void needRedrawCable() {
        needRedraw = true;
    }

    public void drawPowerPin(float d[]) {
        drawPowerPin(front, d);
    }

    public void drawPowerPin(LRDU front, float d[]) {
        if (UtilsClient.distanceFromClientPlayer(tileEntity) > 20) return;
        GL11.glColor3f(0, 0, 0);
        UtilsClient.drawConnectionPinSixNode(front, d, 1.8f, 0.9f);
        GL11.glColor3f(1, 1, 1);
    }

    public void drawPowerPinWhite(LRDU front, float d[]) {
        if (UtilsClient.distanceFromClientPlayer(tileEntity) > 20) return;
        UtilsClient.drawConnectionPinSixNode(front, d, 1.8f, 0.9f);
    }


    public void drawSignalPin(float d[]) {
        drawSignalPin(front, d);
    }

    public void drawSignalPin(LRDU front, float d[]) {
        if (UtilsClient.distanceFromClientPlayer(tileEntity) > 20) return;
        GL11.glColor3f(0, 0, 0);
        UtilsClient.drawConnectionPinSixNode(front, d, 0.9f, 0.9f);
        GL11.glColor3f(1, 1, 1);
    }

    boolean needRedraw;

    public void newConnectionType(CableRenderType connectionType) {

    }

    public void draw() {
        // Minecraft.getMinecraft().profiler.startSection("SixNodeRender");
        if (needRedraw) {
            needRedraw = false;
            connectionType = CableRender.connectionType(this, side);
            newConnectionType(connectionType);
            if (drawCableAuto() && slot < 6) {   // (a device off centre has pins, no stubs to the block's edges)
                for (int idx = 0; idx < 4; idx++) {
                    CableRenderDescriptor render = CableRender.stub(getCableRender(LRDU.fromInt(idx)), connectionType, LRDU.fromInt(idx));
                    cableListReady[idx] = false;
                    if (render != null && (connectedSide.mask & (1 << idx)) != 0) {
                        GL11.glNewList(cableList[idx], GL11.GL_COMPILE);
                        CableRender.drawCable(render, new LRDUMask(1 << idx), connectionType);
                        GL11.glEndList();
                        cableListReady[idx] = true;
                    }
                }
            }
        }

        for (int idx = 0; idx < 4; idx++) {
            Utils.setGlColorFromDye(connectionType.otherdry[idx]);
            if (cableListReady[idx]) {
                UtilsClient.bindTexture(CableRender.stub(getCableRender(LRDU.fromInt(idx)), connectionType, LRDU.fromInt(idx)).cableTexture);
                GL11.glCallList(cableList[idx]);
            }
        }

        if (slot >= 6) drawPins();

        GL11.glColor3f(1f, 1f, 1f);
        // Minecraft.getMinecraft().profiler.endSection();
    }

    /** Off centre: a pin past its body on every connecting edge, in the element frame (before the render's own turn).
     *  draw() does it; a render that doesn't call super.draw() calls this (the in-line parts). */
    protected void drawPins() {
        if (slot < 6 || sixNodeDescriptor.drawsOwnPins()) return;
        // from the model's edge (a lopsided model: SixNodeDescriptor.bodyBounds) to one voxel past its body
        int[] ext = sixNodeDescriptor.bodyExtents(side, front);
        float[] reach = sixNodeDescriptor.modelReach(side, front);
        for (LRDU l : LRDU.values())
            if ((pinMask & (1 << l.toInt())) != 0) {
                float to = (ext[l.toInt()] + 1) / 16f;
                float from = reach == null ? to - 1 / 16f : Math.min(reach[l.toInt()] / 16f, to - 1 / 16f);
                pinColour(l);
                drawPinBox(l, from, to, 1.8f / 16, 1 / 16f);
            }
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** Is the pin toward l a field bus pin (mods.eln.bus)? It takes no six-node cable, so getCableRender can't say. */
    protected boolean busPin(LRDU l) {
        return false;
    }

    /** The colour of the pin toward l: violet for a bus pin, as the bus cable is drawn; else by the cable it takes. */
    protected void pinColour(LRDU l) {
        if (busPin(l)) busColour();
        else pinColour(getCableRender(l));
    }

    /** The bus cable's violet, for a bus pin. */
    protected static void busColour() {
        GL11.glColor3f(0.62f, 0.38f, 0.80f);
    }

    /** A bus pin's width and height: wider and taller than the bus cable (3 x 1 voxels), so the cable's end goes into it. */
    public static final float BUS_PIN_W = 3.4f / 16, BUS_PIN_H = 1.6f / 16;
    /** Where a bus pin ends: just past the block's edge, over the end of a bus cable there (as the models' terminals). */
    public static final float BUS_PIN_TO = 8.1f / 16;

    /** A bus pin in the current frame: on the face toward edge l, offset voxels along it toward l.left() (as VoxelNode
     *  places edge pins; element frame: Left/Right along z, Down/Up along y), from `from` off the centre (inside the
     *  body, so it comes out of it) to just past the block's edge. */
    protected static void drawBusPin(LRDU l, int offset, float from) {
        LRDU a = l.left();
        float s = (a == LRDU.Left || a == LRDU.Down ? -1 : 1) * offset / 16f;
        GL11.glPushMatrix();
        if (a == LRDU.Left || a == LRDU.Right) GL11.glTranslatef(0, 0, s);
        else GL11.glTranslatef(0, s, 0);
        busColour();
        drawPinBox(l, from, BUS_PIN_TO, BUS_PIN_W, BUS_PIN_H);
        GL11.glPopMatrix();
        GL11.glColor3f(1f, 1f, 1f);
    }

    /** A pin's colour by the cable it takes (AdventurAgent couldn't tell a thermal probe's pins apart): copper for
     *  thermal cable, pale for signal cable, dark grey for the power cables. */
    protected static void pinColour(CableRenderDescriptor r) {
        if (r != null && r == mods.eln.registry.ElnDeviceRegistry.copperThermalCableDescriptor.render) GL11.glColor3f(0.85f, 0.48f, 0.22f);
        else if (r != null && r == mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render) GL11.glColor3f(0.9f, 0.9f, 0.86f);
        else GL11.glColor3f(0.35f, 0.35f, 0.38f);
    }

    /** A small box (in the current colour) on the face toward edge l, from `from` to `to` off the centre (element frame: x off the face,
     *  Left/Right along z, Down/Up along y), w wide and h high. */
    protected static void drawPinBox(LRDU l, float from, float to, float w, float h) {
        UtilsClient.disableTexture();
        GL11.glBegin(GL11.GL_QUADS);
        for (float[] q : PinBox.quads(l, from, to, w, h))
            for (int i = 0; i < 12; i += 3) GL11.glVertex3f(q[i], q[i + 1], q[i + 2]);
        GL11.glEnd();
        UtilsClient.enableTexture();
        GL11.glColor3f(1f, 1f, 1f);
    }

    /**
     * Draw the burnt look yourself (true), or let SixNodeRender draw the soot overlay (false). Cables: burnt through,
     * since they can be any colour (Baughn).
     */
    public boolean drawBurnt() {
        return false;
    }

    public boolean drawCableAuto() {
        return true;
    }

    public boolean glListEnable() {
        return true;
    }

    public void glListCall() {
        if (!glListEnable()) return;
        if (!glListReady) {
            GL11.glNewList(glList, GL11.GL_COMPILE);
            glListDraw();
            GL11.glEndList();

            glListReady = true;
        }
        GL11.glCallList(glList);
    }

    public void glListDraw() {

    }

    /** Rebuild the display list (glListDraw) on the next draw. */
    protected void invalidateGlList() {
        glListReady = false;
    }

    public LRDU front;
    CableRenderType connectionType;
    public SixNodeDescriptor sixNodeDescriptor;

    public int isProvidingWeakPower(Direction side) {
        return 0;
    }

    public void publishUnserialize(DataInputStream stream) {
        try {
            byte b = stream.readByte();
            connectedSide.set(b & 0xF);
            front = front.fromInt((b >> 4) & 0x3);
            burnt = (b & SixNodeElement.HEADER_BURNT) != 0;

            needRedraw = true;

        } catch (IOException e) {

            e.printStackTrace();
        }
        glListReady = false;
    }

    public void singleUnserialize(DataInputStream stream) {

    }

    private int uuid = 0;

    public int getUuid() {
        if (uuid == 0) {
            uuid = UtilsClient.getUuid();
        }
        return uuid;
    }

    public boolean usedUuid() {
        return uuid != 0;
    }

    public void play(SoundCommand s) {
        s.addUuid(getUuid());
        s.set(tileEntity);
        s.play();
    }

    public void destructor() {
        for (mods.eln.sound.LoopedSound l : loopedSounds) l.setActive(false);
        if (glListEnable()) {
            UtilsClient.glDeleteListsSafe(glList);
        }
        UtilsClient.glDeleteListsSafe(cableList[0]);
        UtilsClient.glDeleteListsSafe(cableList[1]);
        UtilsClient.glDeleteListsSafe(cableList[2]);
        UtilsClient.glDeleteListsSafe(cableList[3]);

    }

    public Container getInventory() {
        return null;
    }

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            tileEntity.preparePacketForServer(stream);

            stream.writeByte(slot);
            stream.writeShort(tileEntity.elementRenderIdList[slot]);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        tileEntity.sendPacketToServer(bos);
    }

    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return null;
    }

    public int getCableDry(LRDU lrdu) {
        return 0;
    }

    public void clientSetFloat(int id, float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSetFloat(int id, float value1, float value2) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeFloat(value1);
            stream.writeFloat(value2);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSetDouble(byte id, double value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeDouble(value);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSetString(byte id, String text) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeUTF(text);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSetInt(byte id, int value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeInt(value);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void clientSetByte(byte id, byte value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeByte(value);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSend(int id) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public boolean cameraDrawOptimisation() {

        return true;
    }

    public void serverPacketUnserialize(DataInputStream stream) throws IOException {

    }

    public void notifyNeighborSpawn() {
        needRedraw = true;
    }

    /** Looped sounds of this render (1.12 LoopedSoundManager); the client plays them (R4). */
    public final java.util.List<mods.eln.sound.LoopedSound> loopedSounds = new java.util.ArrayList<>();

    protected void addLoopedSound(mods.eln.sound.LoopedSound sound) {
        loopedSounds.add(sound);
    }

    public void refresh(float deltaT) {
    }

    /** Blocks this device's model reaches out of its block (0: it stays inside), for SixNodeRender's bounding box. */
    public int renderReach() {
        return 0;
    }
}
