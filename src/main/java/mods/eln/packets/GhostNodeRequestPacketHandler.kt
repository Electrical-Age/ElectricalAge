package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Eln
import mods.eln.node.NodeManager

class GhostNodeRequestPacketHandler : IMessageHandler<GhostNodeRequestPacket, NodeReturnPacket> {

    override fun onMessage(message: GhostNodeRequestPacket?, ctx: MessageContext?): NodeReturnPacket? {
        val coord = message!!.coord
        val wailaData = mutableMapOf<String, String>()

        Eln.ghostManager.getGhost(coord)?.let { ghostElement ->
            ghostElement.observatorCoordonate?.let { obsersverCoordonate ->
                NodeManager.instance.getTransparentNodeFromCoordinate(obsersverCoordonate)?.let { element ->
                    wailaData.putAll(element.waila)
                }
            }
        }

        return NodeReturnPacket(wailaData, coord, true)
    }
}
