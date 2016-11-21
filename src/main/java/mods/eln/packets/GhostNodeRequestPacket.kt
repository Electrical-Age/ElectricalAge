package mods.eln.packets

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordonate

open class GhostNodeRequestPacket: IMessage {
    lateinit var coord: Coordonate

    constructor() {}

    constructor(coord: Coordonate) {
        this.coord = coord
    }

    override fun fromBytes(buf: ByteBuf?) {
        val x = ByteBufUtils.readVarInt(buf, 5)
        val y = ByteBufUtils.readVarInt(buf, 5)
        val z = ByteBufUtils.readVarInt(buf, 5)
        val w = ByteBufUtils.readVarInt(buf, 5)
        coord = Coordonate(x, y, z, w)
    }

    override fun toBytes(buf: ByteBuf?) {
        ByteBufUtils.writeVarInt(buf, coord.x, 5)
        ByteBufUtils.writeVarInt(buf, coord.y, 5)
        ByteBufUtils.writeVarInt(buf, coord.z, 5)
        ByteBufUtils.writeVarInt(buf, coord.dimention, 5)
    }
}
