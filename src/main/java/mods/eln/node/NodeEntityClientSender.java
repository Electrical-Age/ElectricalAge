package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.UtilsClient;
import net.minecraft.tileentity.TileEntity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NodeEntityClientSender {

    private TileEntity e;
    private String nodeUuid;

    public NodeEntityClientSender(TileEntity e, String nodeUuid) {
        this.e = e;
        this.nodeUuid = nodeUuid;
    }

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetPublishForNode);

            stream.writeInt(e.xCoord);
            stream.writeInt(e.yCoord);
            stream.writeInt(e.zCoord);

            stream.writeByte(e.getWorldObj().provider.dimensionId);

            stream.writeUTF(nodeUuid);


        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        UtilsClient.sendPacketToServer(bos);
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
}
