package mods.eln.packets

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordonate

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
open class TransparentNodeRequestPacket : IMessage {

    lateinit var coord: Coordonate

    constructor() {

    }

    constructor(c: Coordonate) {
        coord = c
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
