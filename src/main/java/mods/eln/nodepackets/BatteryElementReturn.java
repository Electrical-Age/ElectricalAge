package mods.eln.nodepackets;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mods.eln.integration.waila.WailaCache;
import mods.eln.misc.Coordonate;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class BatteryElementReturn implements IMessage{
	
	public Map<String, String> map;
	public Coordonate coord;
	
	public BatteryElementReturn() {};
	
	public BatteryElementReturn(Map<String, String> m, Coordonate c) {
		this.map = m;
		this.coord = c;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		List<String> keys = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		int length1 = ByteBufUtils.readVarInt(buf, 5);
		for(int i = 0; i < length1; i++){
			keys.add(ByteBufUtils.readUTF8String(buf));
		}
		int length2 = ByteBufUtils.readVarInt(buf, 5);
		for(int i = 0; i < length2; i++){
			values.add(ByteBufUtils.readUTF8String(buf));
		}
		int x = ByteBufUtils.readVarInt(buf, 5);
		int y = ByteBufUtils.readVarInt(buf, 5);
		int z = ByteBufUtils.readVarInt(buf, 5);
		int w = ByteBufUtils.readVarInt(buf, 5);
		coord = new Coordonate(x, y, z, w);
		map = new HashMap<String, String>();
		Iterator<String> i1 = keys.iterator();
		Iterator<String> i2 = values.iterator();
		while (i1.hasNext() && i2.hasNext()) {
		    map.put(i1.next(), i2.next());
		}
	}

	@Override
	public void toBytes(ByteBuf buf){
		List<String> keys = new ArrayList<String>(map.keySet());
		List<String> values = new ArrayList<String>(map.values());
		ByteBufUtils.writeVarInt(buf, keys.size(), ByteBufUtils.varIntByteCount(keys.size()));
		for(String element : keys){
			ByteBufUtils.writeUTF8String(buf, element);
		}
		ByteBufUtils.writeVarInt(buf, values.size(), ByteBufUtils.varIntByteCount(values.size()));
		for(String element : values){
			ByteBufUtils.writeUTF8String(buf, element);
		}
		ByteBufUtils.writeVarInt(buf, coord.x, 5);
		ByteBufUtils.writeVarInt(buf, coord.y, 5);
		ByteBufUtils.writeVarInt(buf, coord.z, 5);
		ByteBufUtils.writeVarInt(buf, coord.dimention, 5);
		
	}

}
