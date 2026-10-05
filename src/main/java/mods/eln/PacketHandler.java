package mods.eln;


import io.netty.buffer.ByteBuf;
import mods.eln.client.ClientKeyHandler;
import mods.eln.misc.Coordonate;
import mods.eln.misc.IConfigSharing;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.server.PlayerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;

import java.io.*;

/**
 * Server side of EA's byte protocol (client -> server packets). Client-bound packets are handled by
 * mods.eln.client.ClientPacketHandler (1.12 port: split by direction, rule 4).
 * Packets are copied off the netty buffer and processed on the server thread (rule 5; known bug: EA mutated
 * world state on the netty thread).
 */
public class PacketHandler {

    public PacketHandler() {
        Eln.eventChannel.register(this);
    }

    /** Copy the readable bytes of a payload (the buffer may be pooled, sliced or direct). */
    public static byte[] payloadBytes(ByteBuf payload) {
        byte[] data = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), data);
        return data;
    }

    @SubscribeEvent
    public void onServerPacket(ServerCustomPacketEvent event) {
        final byte[] data = payloadBytes(event.getPacket().payload());
        final NetworkManager manager = event.getManager();
        final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                packetRx(new DataInputStream(new ByteArrayInputStream(data)), manager, player);
            }
        });
    }


    public void packetRx(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            switch (stream.readByte()) {
                case Eln.packetPlayerKey:
                    packetPlayerKey(stream, manager, player);
                    break;
                case Eln.packetPublishForNode:
                    packetForNode(stream, manager, player);
                    break;
                case Eln.packetClientToServerConnection:
                    packetNewClient(manager, player);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void packetNewClient(NetworkManager manager, EntityPlayer player) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.packetServerToClientInfo);
            for (IConfigSharing c : Eln.instance.configShared) {
                c.serializeConfig(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.sendPacketToClient(bos, (EntityPlayerMP) player);
    }

    void packetForNode(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            Coordonate coordonate = new Coordonate(stream.readInt(),
                stream.readInt(), stream.readInt(), stream.readInt());

            NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordonate);
            if (node != null && node.getNodeUuid().equals(stream.readUTF())) {
                node.networkUnserialize(stream, (EntityPlayerMP) player);
            } else {
                Utils.println("packetForNode node found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetPlayerKey(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        byte id;
        try {
            id = stream.readByte();
            boolean state = stream.readBoolean();

            if (id == ClientKeyHandler.wrenchId) {
                PlayerManager.PlayerMetadata metadata = Eln.playerManager.get(playerMP);
                metadata.setInteractEnable(state);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
