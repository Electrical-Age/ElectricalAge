package mods.eln.packets

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import mods.eln.integration.waila.WailaCache

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
class NodeReturnPacketHandler : IMessageHandler<NodeReturnPacket, IMessage> {
    override fun onMessage(message: NodeReturnPacket?, ctx: MessageContext?): IMessage? {
        val map = message!!.map
        val coord = message.coord
        WailaCache.nodes.put(coord, map)
        return null
    }
}