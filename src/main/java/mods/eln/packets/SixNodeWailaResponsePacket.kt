package mods.eln.packets

import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction

class SixNodeWailaResponsePacket: TransparentNodeResponsePacket {
    lateinit var side: Direction

    constructor() {}

    constructor(coord: Coordonate, side: Direction, data: Map<String,String>): super(data, coord) {
        this.side = side
    }

    override fun fromBytes(buf: ByteBuf?) {
        super.fromBytes(buf)
        side = Direction.fromInt(buf?.readInt() ?: 0)
    }

    override fun toBytes(buf: ByteBuf?) {
        super.toBytes(buf)
        buf?.writeInt(side.int)
    }
}
