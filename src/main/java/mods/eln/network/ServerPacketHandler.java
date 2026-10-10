package mods.eln.network;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Server side of EA's byte protocol, client -> server (1.12 PacketHandler): GUI actions for a node
 * (packetPublishForNode: x, y, z, dimension, node uuid, then the node's own bytes). Runs on the server thread.
 * 26.x hardening: the player must be in the node's dimension and within 16 blocks (1.12 trusted any coordinates).
 */
public final class ServerPacketHandler {
    private ServerPacketHandler() {
    }

    static final double MAX_DISTANCE_SQ = 16 * 16;

    public static void register() {
        ElnNet.serverHandler = (bytes, player) -> player.level().getServer().execute(() -> handle(bytes, player));
    }

    public static void handle(byte[] bytes, ServerPlayer player) {
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            byte type = stream.readByte();
            if (type == Eln.packetPlayerKey) {
                mods.eln.server.PlayerKeys.set(player, stream.readByte(), stream.readBoolean());
                return;
            }
            if (type != Eln.packetPublishForNode) return;
            Coordonate c = new Coordonate(stream.readInt(), stream.readInt(), stream.readInt(), stream.readInt());
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(c);
            if (node == null || !node.getNodeUuid().equals(stream.readUTF())) {
                Utils.println("packetForNode: no node at " + c);
                return;
            }
            if (c.world() != player.level()
                || player.distanceToSqr(c.x + 0.5, c.y + 0.5, c.z + 0.5) > MAX_DISTANCE_SQ) return;
            node.networkUnserialize(stream, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
