package mods.eln.packets

import mods.eln.integration.waila.SixNodeCoordinate
import mods.eln.integration.waila.SixNodeWailaData
import mods.eln.integration.waila.WailaCache
import mods.eln.misc.Coordinate
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class SixNodeWailaResponsePacketHandler : IMessageHandler<SixNodeWailaResponsePacket, IMessage> {

    private fun Coordinate.isNull() = this.pos.x == 0 && this.pos.y == 0 && this.pos.z == 0 && this.dimension == 0

    override fun onMessage(message: SixNodeWailaResponsePacket, ctx: MessageContext?): IMessage? {
        if (!message.coord.isNull()) {
            WailaCache.sixNodes.put(SixNodeCoordinate(message.coord, message.side),
                SixNodeWailaData(message.itemStack, message.map))
        }

        return null
    }
}
