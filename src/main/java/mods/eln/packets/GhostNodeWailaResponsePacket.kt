package mods.eln.packets

import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

class GhostNodeWailaResponsePacket(var coord: Coordinate = Coordinate(0, 0, 0, 0),
                                   var realCoord: Coordinate = Coordinate(0, 0, 0, 0),
                                   var itemStack: ItemStack? = null,
                                   var type: Byte = UNKNOWN_TYPE,
                                   var realSide: Direction = Direction.XN) : IMessage {

    companion object {
        val UNKNOWN_TYPE: Byte = 0
        val TRANSPARENT_BLOCK_TYPE: Byte = 1
        val SIXNODE_TYPE: Byte = 2
    }

    private fun Coordinate.write(buf: ByteBuf?) {
        if (buf != null) {
            ByteBufUtils.writeVarInt(buf, this.pos.x, 5)
            ByteBufUtils.writeVarInt(buf, this.pos.y, 5)
            ByteBufUtils.writeVarInt(buf, this.pos.z, 5)
            ByteBufUtils.writeVarInt(buf, this.dimension, 5)
        }
    }

    private fun Coordinate.read(buf: ByteBuf?) {
        if (buf != null) {
            val x = ByteBufUtils.readVarInt(buf, 5)
            val y = ByteBufUtils.readVarInt(buf, 5)
            val z = ByteBufUtils.readVarInt(buf, 5)
            this.pos.setPos(x, y, z)
            this.dimension = ByteBufUtils.readVarInt(buf, 5)
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
