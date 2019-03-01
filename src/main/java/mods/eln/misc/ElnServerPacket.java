package mods.eln.misc;

import mods.eln.Eln;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class ElnServerPacket implements Packet {

    private String channelName;
    private byte[] buf;
    private static final String __OBFID = "CL_00001297";

    public ElnServerPacket(ByteBuf buf) {
        this(Eln.channelName, buf);
    }

    public ElnServerPacket(byte[] buf) {
        this(Eln.channelName, buf);
    }

    public ElnServerPacket(String channelName, ByteBuf buf) {
        this(channelName, buf.array());
    }

    public ElnServerPacket(String channelName, byte[] buf) {
        this.channelName = channelName;
        this.buf = buf;
        if (buf.length > 2097136) {
            throw new IllegalArgumentException("Payload may not be larger than 2097136 (0x1ffff0) bytes");
        }
    }

    public void readPacketData(PacketBuffer buf) {
        try {
            this.channelName = buf.readString(20);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.buf = new byte[ByteBufUtils.readVarShort(buf)];
        buf.readBytes(this.buf);
    }

    public void writePacketData(PacketBuffer buf) {
        try {
            buf.writeString(this.channelName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ByteBufUtils.writeVarShort(buf, this.buf.length);
        buf.writeBytes(this.buf);
    }

    @Override
    public void processPacket(INetHandler arg0) {
    }
}
