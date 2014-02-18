package mods.eln.modbusrtu;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;

public class WirelessRxStatus implements INBTTReady{
	public WirelessRxStatus(String name,int id,boolean connected,int uuid) {
		this.id = id;
		this.name = name;
		this.connected = connected;
		this.uuid = uuid;
	}
	public WirelessRxStatus() {
		// TODO Auto-generated constructor stub
	}
	String name;
	int id,uuid;
	boolean connected;
	
	void setUUID(int uuid){
		this.uuid = uuid;
	}
	public void writeTo(DataOutputStream packet) throws IOException {
		// TODO Auto-generated method stub
		packet.writeInt(uuid);
		packet.writeInt(id);
		packet.writeUTF(name);
		packet.writeBoolean(connected);
	}
	
	public void readFrom(DataInputStream stream) throws IOException {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		name = nbt.getString(str + "name" );
		id = nbt.getInteger(str + "id");
		connected = nbt.getBoolean(str + "connected");
		uuid = nbt.getInteger(str + "uuid");		
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setString(str + "name", name);
		nbt.setInteger(str + "id", id);
		nbt.setBoolean(str + "connected", connected);
		nbt.setInteger(str + "uuid", uuid);
	}
}
