package mods.eln.node;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class NodeServer {

    public NodeServer() {
        FMLCommonHandler.instance().bus().register(this);

    }

    public void init() {
        //	NodeBlockEntity.nodeAddedList.clear();
    }

    public void stop() {
        //	NodeBlockEntity.nodeAddedList.clear();
    }

    public int counter = 0;

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase != Phase.START) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (server != null) {

            for (NodeBase node : NodeManager.instance.getNodeList()) {
                if (node.getNeedPublish()) {
                    node.publishToAllPlayer();
                }
            }

            for (Object obj : server.getConfigurationManager().playerEntityList) {
                EntityPlayerMP player = (EntityPlayerMP) obj;

                NodeBase openContainerNode = null;
                INodeContainer container = null;
                if (player.openContainer != null && player.openContainer instanceof INodeContainer) {
                    container = ((INodeContainer) player.openContainer);
                    openContainerNode = container.getNode();
                }

                for (NodeBase node : NodeManager.instance.getNodeList()) {

                    if (node == openContainerNode) {
                        if ((counter % (1 + container.getRefreshRateDivider())) == 0)
                            node.publishToPlayer(player);
                    }
                }
            }

            counter++;
        }

    }


}
