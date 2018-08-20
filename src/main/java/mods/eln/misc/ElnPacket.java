package mods.eln.misc;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public class ElnPacket extends Packet {

    private String channel;
    private byte[] data;

    public ElnPacket(String channel, ByteBuffer data) {
        //  this(channel, data.array());
    }
      /*
	  	public ElnPacket(String channel, byte[] data) {
		  ByteBuf
	    this.channel = channel;
	    this.data = data;
	    if (data.length > 2097136) {
	      throw new IllegalArgumentException("Payload may not be larger than 2097136 (0x1ffff0) bytes");
	    }
	  }*/
	  /*
	@Override
	public void readPacketData(DataInput datainput) throws IOException {
	    this.channel = datainput.readUTF();
	    short size = datainput.readShort();
	    data = new byte[size];
	    for(int idx = 0; idx < size; idx++){
	    	data[idx] = datainput.readByte();
	    }
	}

	@Override
	public void writePacketData(DataOutput dataoutput) throws IOException {
		dataoutput.writeUTF(channel);
		dataoutput.writeShort(data.length);
		dataoutput.write(data);
	}

	@Override
	public void processPacket(NetHandler nethandler) {
		Eln.instance.
	}

	@Override
	public int getPacketSize() {
		return data.length;
	}*/

    @Override
    public void processPacket(INetHandler arg0) {
    }

    @Override
    public void readPacketData(PacketBuffer arg0) {
    }

    @Override
    public void writePacketData(PacketBuffer arg0) {
    }
}
