package mods.eln.node;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server tick (start): publish every node that needs it to the players tracking its chunk, and the node behind a
 * player's open EA container at the container's refresh rate (1.12: same, on the FML bus).
 */
public class NodeServer {

    public NodeServer() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e -> tick(e.getServer()));
    }

    public void init() {
    }

    public void stop() {
    }

    public int counter = 0;

    public void tick(MinecraftServer server) {
        for (NodeBase node : NodeManager.instance.getNodeList()) {
            if (node.getNeedPublish()) {
                node.publishToAllPlayer();
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof INodeContainer) {
                INodeContainer container = (INodeContainer) player.containerMenu;
                NodeBase node = container.getNode();
                if (node != null && (counter % (1 + container.getRefreshRateDivider())) == 0)
                    node.publishToPlayer(player);
            }
        }

        counter++;
    }
}
