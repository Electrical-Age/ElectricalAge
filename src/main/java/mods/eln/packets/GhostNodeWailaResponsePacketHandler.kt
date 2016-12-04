package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.integration.waila.GhostNodeWailaData
import mods.eln.integration.waila.WailaCache
import mods.eln.misc.Coordonate

class GhostNodeWailaResponsePacketHandler : IMessageHandler<GhostNodeWailaResponsePacket, IMessage> {

    private fun Coordonate.isNull() = this.x == 0 && this.y == 0 && this.z == 0 && this.dimention == 0

    override fun onMessage(message: GhostNodeWailaResponsePacket, ctx: MessageContext?): IMessage? {
        if (!message.realCoord.isNull()) {
            WailaCache.ghostNodes.put(message.coord, GhostNodeWailaData(message.realCoord, message.itemStack,
                    message.type, message.realSide))
        }

        return null
    }
}
