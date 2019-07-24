package mods.eln.node

import mods.eln.sim.IProcess
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer

/*
 * Each tick, publishes all nodes that ask for it.
 * Additionally, publish all nodes with an inventory that are opened by a player, to that player.
 */
class NodePublishProcess : IProcess {
    var counter = 0

    override fun process(time: Double) {
        val server = FMLCommonHandler.instance().minecraftServerInstance

        if (server != null) {
            for (node in NodeManager.instance.nodeList) {
                if (node.needPublish) {
                    node.publishToAllPlayer()
                }
            }

            for (player in server.playerList.players) {
                var openContainerNode: NodeBase? = null
                var container: INodeContainer? = null
                if (player.openContainer is INodeContainer) {
                    container = player.openContainer as INodeContainer
                    openContainerNode = container.node
                }

                for (node in NodeManager.instance.nodeList) {
                    if (node === openContainerNode) {
                        if (counter % (1 + container!!.refreshRateDivider) == 0)
                            node.publishToPlayer(player)
                    }
                }
            }

            counter++
        }
    }
}
