package mods.eln.packets

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordonate
import java.util.*

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
open class TransparentNodeResponsePacket : IMessage {

    lateinit var map: Map<String, String>
    lateinit var coord: Coordonate

    constructor() {

    }

    constructor(m: Map<String, String>, c: Coordonate) {
        map = m
        coord = c
    }

    override fun fromBytes(buf: ByteBuf?) {
        var keys = listOf<String>()
        var values = listOf<String>()
        val length1 = ByteBufUtils.readVarInt(buf, 5)
        for (i in 1..length1) {
            keys += ByteBufUtils.readUTF8String(buf)
        }
        for (k in 1..length1) {
            values += ByteBufUtils.readUTF8String(buf)
        }
        val x = ByteBufUtils.readVarInt(buf, 5)
        val y = ByteBufUtils.readVarInt(buf, 5)
        val z = ByteBufUtils.readVarInt(buf, 5)
        val w = ByteBufUtils.readVarInt(buf, 5)
        coord = Coordonate(x, y, z, w)
        val i1 = keys.iterator()
        val i2 = values.iterator()
        var localmap = HashMap<String, String>()
        while (i1.hasNext() && i2.hasNext()) {
            localmap.put(i1.next(), i2.next())
        }
        map = localmap
    }

    override fun toBytes(buf: ByteBuf?) {
        ByteBufUtils.writeVarInt(buf, map.size, 5)
        for (element: String in map.keys.iterator()) {
            ByteBufUtils.writeUTF8String(buf, element)
        }
        for (element: String in map.values.iterator()) {
            ByteBufUtils.writeUTF8String(buf, element)
        }
        ByteBufUtils.writeVarInt(buf, coord.x, 5)
        ByteBufUtils.writeVarInt(buf, coord.y, 5)
        ByteBufUtils.writeVarInt(buf, coord.z, 5)
        ByteBufUtils.writeVarInt(buf, coord.dimention, 5)
    }
}
