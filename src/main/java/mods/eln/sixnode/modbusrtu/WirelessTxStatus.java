package mods.eln.sixnode.modbusrtu;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WirelessTxStatus implements INBTTReady {

    String name;
    int id;
    double value;
    int uuid;

    WirelessTxStatus() {
    }

    public WirelessTxStatus(String name, int id, double value, int uuid) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.uuid = uuid;
    }

    public void setUUID(int uuid) {
        this.uuid = uuid;
    }

    public void writeTo(DataOutputStream packet) throws IOException {
        packet.writeInt(uuid);
        packet.writeInt(id);
        packet.writeUTF(name);
        packet.writeDouble(value);
    }

    public void readFrom(DataInputStream stream) throws IOException {
        uuid = stream.readInt();
        id = stream.readInt();
        name = stream.readUTF();
        value = stream.readDouble();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        name = nbt.getStringOr(str + "name", "");
        id = nbt.getIntOr(str + "id", 0);
        value = nbt.getDoubleOr(str + "value", 0.0);
        uuid = nbt.getIntOr(str + "uuid", 0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putString(str + "name", name);
        nbt.putInt(str + "id", id);
        nbt.putDouble(str + "value", value);
        nbt.putInt(str + "uuid", uuid);
    }
}
