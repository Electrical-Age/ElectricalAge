package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNode
import net.minecraft.item.ItemStack

class GhostNodeWailaRequestPacketHandler : IMessageHandler<GhostNodeWailaRequestPacket, GhostNodeWailaResponsePacket> {
    override fun onMessage(message: GhostNodeWailaRequestPacket, ctx: MessageContext?): GhostNodeWailaResponsePacket {
        val realCoord = Eln.ghostManager.getGhost(message.coord)?.observatorCoordonate
        var itemStack: ItemStack? = null
        var type: Byte = GhostNodeWailaResponsePacket.UNKNOWN_TYPE
        var realSide = Direction.XN

        if (realCoord != null) {
            val node = NodeManager.instance.getNodeFromCoordonate(realCoord) as? TransparentNode
            if (node != null) {
                itemStack = node.element.descriptor.newItemStack()
                type = GhostNodeWailaResponsePacket.TRANSPARENT_BLOCK_TYPE
            }

            val element = Eln.ghostManager.getObserver(realCoord) as? SixNodeElement
            if (element != null) {
                itemStack = element.sixNodeElementDescriptor.newItemStack()
                type = GhostNodeWailaResponsePacket.SIXNODE_TYPE
                realSide = element.side
            }
        }

        return GhostNodeWailaResponsePacket(message.coord, realCoord ?: Coordonate(0, 0, 0 ,0), itemStack, type,
                realSide)
    }
}
