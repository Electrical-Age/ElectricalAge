package mods.eln.packets

import io.netty.buffer.ByteBuf
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction

class SixNodeWailaRequestPacket : TransparentNodeRequestPacket {
    lateinit var side: Direction

    constructor() {}

    constructor(coord: Coordinate, side: Direction) : super(coord) {
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
