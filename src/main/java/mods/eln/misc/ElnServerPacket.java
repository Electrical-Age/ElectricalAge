package mods.eln.misc;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class ElnServerPacket extends Packet {

	private String field_149172_a;
    private byte[] field_149171_b;
    private static final String __OBFID = "CL_00001297";
	  
    public ElnServerPacket() {}
	  
    public ElnServerPacket(String p_i45189_1_, ByteBuf p_i45189_2_)
	  {
	    this(p_i45189_1_, p_i45189_2_.array());
	  }
	  
    public ElnServerPacket(String p_i45190_1_, byte[] p_i45190_2_) {
	    this.field_149172_a = p_i45190_1_;
	    this.field_149171_b = p_i45190_2_;
	    if (p_i45190_2_.length > 2097136) {
	        throw new IllegalArgumentException("Payload may not be larger than 2097136 (0x1ffff0) bytes");
	    }
    }
	  
    public void readPacketData(PacketBuffer p_148837_1_) {
	    try {
			this.field_149172_a = p_148837_1_.readStringFromBuffer(20);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    this.field_149171_b = new byte[ByteBufUtils.readVarShort(p_148837_1_)];
	    p_148837_1_.readBytes(this.field_149171_b);
    }
	  
    public void writePacketData(PacketBuffer p_148840_1_) {
	    try {
			p_148840_1_.writeStringToBuffer(this.field_149172_a);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    ByteBufUtils.writeVarShort(p_148840_1_, this.field_149171_b.length);
	    p_148840_1_.writeBytes(this.field_149171_b);
    }

	@Override
	public void processPacket(INetHandler arg0) {
	}
}

/*
public class ElnServerPacket
  extends Packet
{
  private String field_149172_a;
  private byte[] field_149171_b;
  private static final String __OBFID = "CL_00001297";
  
  public ElnServerPacket() {}
  
  public ElnServerPacket(String p_i45189_1_, ByteBuf p_i45189_2_)
  {
    this(p_i45189_1_, p_i45189_2_.array());
  }
  
  public ElnServerPacket(String p_i45190_1_, byte[] p_i45190_2_)
  {
    this.field_149172_a = p_i45190_1_;
    this.field_149171_b = p_i45190_2_;
    if (p_i45190_2_.length > 2097136) {
      throw new IllegalArgumentException("Payload may not be larger than 2097136 (0x1ffff0) bytes");
    }
  }
  
  public void readPacketData(PacketBuffer p_148837_1_) 
  {
    this.field_149172_a = p_148837_1_.readStringFromBuffer(20);
    this.field_149171_b = new byte[ByteBufUtils.readVarShort(p_148837_1_)];
    p_148837_1_.readBytes(this.field_149171_b);
  }
  
  public void writePacketData(PacketBuffer p_148840_1_)
  {
    p_148840_1_.writeStringToBuffer(this.field_149172_a);
    ByteBufUtils.writeVarShort(p_148840_1_, this.field_149171_b.length);
    p_148840_1_.writeBytes(this.field_149171_b);
  }
  
  public void processPacket(INetHandlerPlayClient p_149170_1_)
  {
	 
    //p_149170_1_.handleCustomPayload(this);
  }
  
  public void processPacket(INetHandler p_148833_1_)
  {
    processPacket((INetHandlerPlayClient)p_148833_1_);
  }
  
  @SideOnly(Side.CLIENT)
  public String func_149169_c()
  {
    return this.field_149172_a;
  }
  
  @SideOnly(Side.CLIENT)
  public byte[] func_149168_d()
  {
    return this.field_149171_b;
  }
}*/
