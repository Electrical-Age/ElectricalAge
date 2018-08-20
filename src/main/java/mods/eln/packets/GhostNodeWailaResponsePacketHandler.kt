package mods.eln.packets

import mods.eln.integration.waila.GhostNodeWailaData
import mods.eln.integration.waila.WailaCache
import mods.eln.misc.Coordinate
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class GhostNodeWailaResponsePacketHandler : IMessageHandler<GhostNodeWailaResponsePacket, IMessage> {

    private fun Coordinate.isNull() = this.pos.x == 0 && this.pos.y == 0 && this.pos.z == 0 && this.dimension == 0

    override fun onMessage(message: GhostNodeWailaResponsePacket, ctx: MessageContext?): IMessage? {
        if (!message.realCoord.isNull()) {
            WailaCache.ghostNodes.put(message.coord, GhostNodeWailaData(message.realCoord, message.itemStack,
                message.type, message.realSide))
        }

        return null
    }
}
