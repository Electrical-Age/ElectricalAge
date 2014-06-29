package mods.eln.modbusrtu;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.Coordonate;
import mods.eln.wirelesssignal.IWirelessSignalTx;

public class WirelessTxStatus implements INBTTReady{
	WirelessTxStatus(){
		
	}
	public WirelessTxStatus(String name,int id,double value,int uuid) {
		this.id = id;
		this.name = name;
		this.value = value;
		this.uuid = uuid;
	}
	String name;
	int id;
	double value;
	int uuid;
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
		// TODO Auto-generated method stub
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
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		name = nbt.getString(str + "name" );
		id = nbt.getInteger(str + "id");
		value = nbt.getDouble(str + "value");
		uuid = nbt.getInteger(str + "uuid");		
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setString(str + "name", name);
		nbt.setInteger(str + "id", id);
		nbt.setDouble(str + "value", value);
		nbt.setInteger(str + "uuid", uuid);
	}


}
