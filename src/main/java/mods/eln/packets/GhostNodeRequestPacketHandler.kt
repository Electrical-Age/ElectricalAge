package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Eln
import mods.eln.misc.Coordonate

class GhostNodeRequestPacketHandler : IMessageHandler<GhostNodeRequestPacket, GhostNodeResponsePacket> {
    override fun onMessage(message: GhostNodeRequestPacket?, ctx: MessageContext?): GhostNodeResponsePacket? =
            GhostNodeResponsePacket(message!!.coord,
                    Eln.ghostManager.getGhost(message.coord)?.observatorCoordonate ?: Coordonate(0, 0, 0 ,0))
}
