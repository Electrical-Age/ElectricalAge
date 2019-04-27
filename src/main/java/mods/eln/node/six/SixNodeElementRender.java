package mods.eln.node.six;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.misc.*;
import mods.eln.sound.LoopedSound;
import mods.eln.sound.LoopedSoundManager;
import mods.eln.sound.SoundCommand;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SixNodeElementRender {
    public SixNodeEntity tileEntity;
    public Direction side;
    // public SixNodeDescriptor descriptor;
    public LRDUMask connectedSide = new LRDUMask();
    int glList, cableList[];
    boolean cableListReady[] = {false, false, false, false};
    boolean glListReady = false;

    public SixNodeElementRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        this.sixNodeDescriptor = descriptor;
        this.tileEntity = tileEntity;
        this.side = side;
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
            if (drawCableAuto()) {
                for (int idx = 0; idx < 4; idx++) {
                    CableRenderDescriptor render = getCableRender(LRDU.fromInt(idx));
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
                UtilsClient.bindTexture(getCableRender(LRDU.fromInt(idx)).cableTexture);
                GL11.glCallList(cableList[idx]);
            }
        }

        GL11.glColor3f(1f, 1f, 1f);
        // Minecraft.getMinecraft().profiler.endSection();
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
        if (usedUuid())
            ClientProxy.uuidManager.kill(uuid);

        if (glListEnable()) {
            UtilsClient.glDeleteListsSafe(glList);
        }
        UtilsClient.glDeleteListsSafe(cableList[0]);
        UtilsClient.glDeleteListsSafe(cableList[1]);
        UtilsClient.glDeleteListsSafe(cableList[2]);
        UtilsClient.glDeleteListsSafe(cableList[3]);

        loopedSoundManager.dispose();
    }

    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return null;
    }

    public IInventory getInventory() {
        return null;
    }

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            tileEntity.preparePacketForServer(stream);

            stream.writeByte(side.getInt());
            stream.writeShort(tileEntity.elementRenderIdList[side.getInt()]);

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

    private LoopedSoundManager loopedSoundManager = new LoopedSoundManager();

    @SideOnly(Side.CLIENT)
    protected void addLoopedSound(final LoopedSound loopedSound) {
        loopedSoundManager.add(loopedSound);
    }

    public void refresh(float deltaT) {
        loopedSoundManager.process(deltaT);
    }
}
