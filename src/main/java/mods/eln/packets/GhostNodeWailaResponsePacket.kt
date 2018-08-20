package mods.eln.packets

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import net.minecraft.item.ItemStack

class GhostNodeWailaResponsePacket(var coord: Coordonate = Coordonate(0, 0, 0, 0),
                                   var realCoord: Coordonate = Coordonate(0, 0, 0, 0),
                                   var itemStack: ItemStack? = null,
                                   var type: Byte = UNKNOWN_TYPE,
                                   var realSide: Direction = Direction.XN) : IMessage {

    companion object {
        val UNKNOWN_TYPE: Byte = 0
        val TRANSPARENT_BLOCK_TYPE: Byte = 1
        val SIXNODE_TYPE: Byte = 2
    }

    private fun Coordonate.write(buf: ByteBuf?) {
        if (buf != null) {
            ByteBufUtils.writeVarInt(buf, this.x, 5)
            ByteBufUtils.writeVarInt(buf, this.y, 5)
            ByteBufUtils.writeVarInt(buf, this.z, 5)
            ByteBufUtils.writeVarInt(buf, this.dimention, 5)
        }
    }

    private fun Coordonate.read(buf: ByteBuf?) {
        if (buf != null) {
            this.x = ByteBufUtils.readVarInt(buf, 5)
            this.y = ByteBufUtils.readVarInt(buf, 5)
            this.z = ByteBufUtils.readVarInt(buf, 5)
            this.dimention = ByteBufUtils.readVarInt(buf, 5)
        }
    }

    override fun fromBytes(buf: ByteBuf?) {
        coord.read(buf)
        realCoord.read(buf)
        itemStack = ByteBufUtils.readItemStack(buf)
        type = buf?.readByte() ?: UNKNOWN_TYPE
        realSide = Direction.fromInt(buf?.readInt() ?: 0)
    }

    override fun toBytes(buf: ByteBuf?) {
        coord.write(buf)
        realCoord.write(buf)
        ByteBufUtils.writeItemStack(buf, itemStack)
        buf?.writeByte(type.toInt())
        buf?.writeInt(realSide.int)
    }
}
