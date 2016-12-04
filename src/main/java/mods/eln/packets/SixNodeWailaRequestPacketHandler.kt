package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode

class SixNodeWailaRequestPacketHandler : IMessageHandler<SixNodeWailaRequestPacket, SixNodeWailaResponsePacket> {
    override fun onMessage(message: SixNodeWailaRequestPacket, ctx: MessageContext?): SixNodeWailaResponsePacket {
        val coord = message.coord
        val side = message.side
        val node = NodeManager.instance.getNodeFromCoordonate(coord) as? SixNode
        var stringMap: Map<String, String> = emptyMap()
        if (node != null) {
            val element = node.getElement(side)
            if (element != null) {
                stringMap = element.waila.filter { it.value != null }
            }
        }
        return SixNodeWailaResponsePacket(coord, side, stringMap)
    }
}
