package mods.eln.packets

import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
class TransparentNodeRequestPacketHandler : IMessageHandler<TransparentNodeRequestPacket, TransparentNodeResponsePacket> {
    override fun onMessage(message: TransparentNodeRequestPacket?, ctx: MessageContext?): TransparentNodeResponsePacket? {
        val c = message!!.coord
        val node = NodeManager.instance.getNodeFromCoordinate(c) as? TransparentNode
        var stringMap: Map<String, String> = emptyMap()
        if (node != null) {
            try {
                stringMap = node.element.waila
            } catch (e: NullPointerException) {
                Utils.print("Attempted to get WAILA info for an invalid node!")
                e.printStackTrace()
                return null
            }
        }
        return TransparentNodeResponsePacket(stringMap, c)
    }
}
