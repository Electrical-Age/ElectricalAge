package mods.eln.node.transparent;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.*;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import mods.eln.gl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client state and drawing of a transparent-node element (1.12, on the GL11 shim since P3). Server-safe on purpose
 * (see SixNodeElementRender). Looped sounds (R4) and GUI screens (P4b) are not here yet.
 */
public abstract class TransparentNodeElementRender {
    public TransparentNodeEntity tileEntity;
    public Direction front;
    public boolean grounded;
    public TransparentNodeDescriptor transparentNodedescriptor;

    public TransparentNodeElementRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        this.tileEntity = tileEntity;
        this.transparentNodedescriptor = descriptor;
    }

    protected ItemEntity unserializeItemStackToEntityItem(DataInputStream stream, ItemEntity old) throws IOException {
        return Utils.unserializeItemStackToEntityItem(stream, old, tileEntity);

    }

    public void drawEntityItem(ItemEntity entityItem, double x, double y, double z, float roty, float scale) {/*
        if(entityItem == null) return;
		


		entityItem.hoverStart = 0.0f;
		entityItem.setYRot(0.0f);
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ =0.0;
		
		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();	
		*/
        UtilsClient.drawEntityItem(entityItem, x, y, z, roty, scale);

    }

    public void glCableTransforme(Direction inverse) {
        inverse.glTranslate(0.5f);
        inverse.glRotateXnRef();
    }

    public abstract void draw();

    /** The element burnt out (see TransparentNodeElement.burn); TransparentNodeRender scorches it. */
    public boolean burnt;

    public void networkUnserialize(DataInputStream stream) {
        try {
            byte b = stream.readByte();
            front = Direction.fromInt(b & 0x7);
            grounded = (b & 8) != 0;
            burnt = (b & TransparentNodeElement.HEADER_BURNT) != 0;
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public Container getInventory() {
        return null;
    }

    public void preparePacketForServer(DataOutputStream stream) {
        tileEntity.preparePacketForServer(stream);
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        tileEntity.sendPacketToServer(bos);
    }


    public void clientSetGrounded(boolean value) {
        clientSendBoolean(TransparentNodeElement.unserializeGroundedId, value);
    }

    public void clientSendBoolean(Byte id, boolean value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeByte(value ? 1 : 0);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendId(Byte id) {
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

    public void clientSendString(Byte id, String str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeUTF(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendFloat(Byte id, float str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeFloat(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public void clientSendInt(Byte id, int str) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(id);
            stream.writeInt(str);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    public boolean cameraDrawOptimisation() {
        return true;
    }

    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {

        return null;
    }

    static final LRDUMask maskTempDraw = new LRDUMask();

    public CableRenderType drawCable(Direction side, CableRenderDescriptor render, LRDUMask connection, CableRenderType renderPreProcess) {
        if (render == null) return renderPreProcess;
        if (renderPreProcess == null) renderPreProcess = CableRender.connectionType(tileEntity, connection, side);
        GL11.glPushMatrix();
        glCableTransforme(side);
        render.bindCableTexture();

        for (LRDU lrdu : LRDU.values()) {
            Utils.setGlColorFromDye(renderPreProcess.otherdry[lrdu.toInt()]);
            if (connection.get(lrdu) == false) continue;
            maskTempDraw.set(1 << lrdu.toInt());
            CableRender.drawCable(render, maskTempDraw, renderPreProcess);
        }
        GL11.glPopMatrix();
        GL11.glColor3f(1f, 1f, 1f);
        return renderPreProcess;
    }

    public void notifyNeighborSpawn() {


    }

    public void serverPacketUnserialize(DataInputStream stream) {


    }

    protected Coordonate coordonate() {

        return new Coordonate(tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY(), tileEntity.getBlockPos().getZ(), tileEntity.getLevel());
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
    }

    /** Looped sounds of this render (1.12 LoopedSoundManager); the client plays them (R4). */
    public final java.util.List<mods.eln.sound.LoopedSound> loopedSounds = new java.util.ArrayList<>();

    protected void addLoopedSound(mods.eln.sound.LoopedSound sound) {
        loopedSounds.add(sound);
    }

    public void refresh(float deltaT) {
    }
}
