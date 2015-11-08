package mods.eln.nodepackets;

import mods.eln.misc.Coordonate;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class NodePacket implements IMessage{

	Coordonate coord;
	
	public NodePacket() {}
	
	public NodePacket(Coordonate c){
		this.coord = c;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int x = ByteBufUtils.readVarInt(buf, 5);
		int y = ByteBufUtils.readVarInt(buf, 5);
		int z = ByteBufUtils.readVarInt(buf, 5);
		int w = ByteBufUtils.readVarInt(buf, 5);
		coord = new Coordonate(x, y, z, w);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeVarInt(buf, coord.x, 5);
		ByteBufUtils.writeVarInt(buf, coord.y, 5);
		ByteBufUtils.writeVarInt(buf, coord.z, 5);
		ByteBufUtils.writeVarInt(buf, coord.dimention, 5);
	}

}
