package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.integration.waila.WailaCache
import mods.eln.misc.Coordonate

class GhostNodeResponsePacketHandler : IMessageHandler<GhostNodeResponsePacket, IMessage> {

    private fun Coordonate.isNull() = this.x == 0 && this.y == 0 && this.z == 0 && this.dimention == 0

    override fun onMessage(message: GhostNodeResponsePacket?, ctx: MessageContext?): IMessage? {
        if (message != null && !message.realCoord.isNull()) {
            WailaCache.ghostNodes.put(message.coord, message.realCoord)
        }

        return null
    }
}
