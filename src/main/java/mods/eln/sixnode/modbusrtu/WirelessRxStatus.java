package mods.eln.sixnode.modbusrtu;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WirelessRxStatus implements INBTTReady {

    String name;
    int id, uuid;
    boolean connected;

    public WirelessRxStatus(String name, int id, boolean connected, int uuid) {
        this.id = id;
        this.name = name;
        this.connected = connected;
        this.uuid = uuid;
    }

    public WirelessRxStatus() {
    }

    void setUUID(int uuid) {
        this.uuid = uuid;
    }

    public void writeTo(DataOutputStream packet) throws IOException {
        packet.writeInt(uuid);
        packet.writeInt(id);
        packet.writeUTF(name);
        packet.writeBoolean(connected);
    }

    public void readFrom(DataInputStream stream) throws IOException {
        uuid = stream.readInt();
        id = stream.readInt();
        name = stream.readUTF();
        connected = stream.readBoolean();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        name = nbt.getString(str + "name");
        id = nbt.getInteger(str + "id");
        connected = nbt.getBoolean(str + "connected");
        uuid = nbt.getInteger(str + "uuid");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setString(str + "name", name);
        nbt.setInteger(str + "id", id);
        nbt.setBoolean(str + "connected", connected);
        nbt.setInteger(str + "uuid", uuid);
        return nbt;
    }
}
