package mods.eln.packets

import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class SixNodeWailaRequestPacketHandler : IMessageHandler<SixNodeWailaRequestPacket, SixNodeWailaResponsePacket> {
    override fun onMessage(message: SixNodeWailaRequestPacket, ctx: MessageContext?): SixNodeWailaResponsePacket {
        val coord = message.coord
        val side = message.side
        val node = NodeManager.instance.getNodeFromCoordinate(coord) as? SixNode
        var stringMap: Map<String, String> = emptyMap()
        var itemStack: ItemStack? = null
        if (node != null) {
            val element = node.getElement(side)
            if (element != null) {
                stringMap = element.waila?.filter { it.value != null } ?: emptyMap()
                itemStack = element.sixNodeElementDescriptor.newItemStack()
            }
        }
        return SixNodeWailaResponsePacket(coord, side, itemStack, stringMap)
    }
}
