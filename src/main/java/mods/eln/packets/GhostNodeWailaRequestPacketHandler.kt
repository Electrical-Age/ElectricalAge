package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import net.minecraft.item.ItemStack

class GhostNodeWailaRequestPacketHandler : IMessageHandler<GhostNodeWailaRequestPacket, GhostNodeWailaResponsePacket> {
    override fun onMessage(message: GhostNodeWailaRequestPacket, ctx: MessageContext?): GhostNodeWailaResponsePacket {
        val realCoord = Eln.ghostManager.getGhost(message.coord)?.observatorCoordonate
        var itemStack: ItemStack? = null
        var type: Byte = GhostNodeWailaResponsePacket.UNKNOWN_TYPE

        if (realCoord != null) {
            val node = NodeManager.instance.getNodeFromCoordonate(realCoord) as? TransparentNode
            if (node != null) {
                itemStack = node.element.descriptor.newItemStack()
                type = GhostNodeWailaResponsePacket.TRANSPARENT_BLOCK_TYPE
            }

            val sixnode = NodeManager.instance.getNodeFromCoordonate(realCoord) as? SixNode
            if (sixnode != null) {
                type = GhostNodeWailaResponsePacket.SIXNODE_TYPE
            }
        }

        return GhostNodeWailaResponsePacket(message.coord, realCoord ?: Coordonate(0, 0, 0 ,0), itemStack, type)
    }
}
