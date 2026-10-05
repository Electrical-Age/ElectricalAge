package mods.eln.client;

import mods.eln.Eln;
import mods.eln.PacketHandler;
import mods.eln.compat.WorldCompat;
import mods.eln.misc.IConfigSharing;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import mods.eln.sound.SoundClient;
import mods.eln.sound.SoundCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Client side of EA's byte protocol (server -> client packets), moved out of mods.eln.PacketHandler (1.12 port).
 * Packets are processed on the client thread (rule 5).
 */
public class ClientPacketHandler {

    public ClientPacketHandler() {
        //FMLCommonHandler.instance().bus().register(this);
        Eln.eventChannel.register(this);
    }

    @SubscribeEvent
    public void onClientPacket(ClientCustomPacketEvent event) {
        //Utils.println("onClientPacket");
        final byte[] data = PacketHandler.payloadBytes(event.getPacket().payload());
        final NetworkManager manager = event.getManager();
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                packetRx(data, manager);
            }
        });
    }

    /** Process one packet on the client thread (also used for the TE description data, see NodeBlockEntity). */
    public void packetRx(byte[] data, NetworkManager manager) {
        EntityPlayer player = Minecraft.getMinecraft().player; // EntityPlayerSP
        if (player == null) return;
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
        try {
            switch (stream.readByte()) {
                case Eln.packetNodeSingleSerialized:
                    packetNodeSingleSerialized(stream, manager, player);
                    break;
                case Eln.packetForClientNode:
                    packetForClientNode(stream, manager, player);
                    break;
                case Eln.packetOpenLocalGui:
                    packetOpenLocalGui(stream, manager, player);
                    break;
                case Eln.packetPlaySound:
                    packetPlaySound(stream, manager, player);
                    break;
                case Eln.packetDestroyUuid:
                    packetDestroyUuid(stream, manager, player);
                    break;
                case Eln.packetServerToClientInfo:
                    packetServerInfo(stream, manager, player);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void packetServerInfo(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        for (IConfigSharing c : Eln.instance.configShared) {
            try {
                c.deserialize(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void packetDestroyUuid(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            ClientProxy.uuidManager.kill(stream.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetPlaySound(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            if (stream.readInt() != player.dimension)
                return;
            SoundClient.play(SoundCommand.fromStream(stream, player.world));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void packetOpenLocalGui(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayer clientPlayer = (EntityPlayer) player;
        try {
            clientPlayer.openGui(Eln.instance, stream.readInt(),
                clientPlayer.world, stream.readInt(), stream.readInt(),
                stream.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetForClientNode(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayer clientPlayer = (EntityPlayer) player;
        int x, y, z, dimention;
        try {

            x = stream.readInt();
            y = stream.readInt();
            z = stream.readInt();
            dimention = stream.readInt();


            if (clientPlayer.dimension == dimention) {
                TileEntity entity = WorldCompat.getTileEntity(clientPlayer.world, x, y, z);
                if (entity != null && entity instanceof INodeEntity) {
                    INodeEntity node = (INodeEntity) entity;
                    if (node.getNodeUuid().equals(stream.readUTF())) {
                        node.serverPacketUnserialize(stream);
                        if (0 != stream.available()) {
                            Utils.println("0 != stream.available()");
                        }
                    } else {
                        Utils.println("Wrong node UUID warning");
                        int dataSkipLength = stream.readByte();
                        for (int idx = 0; idx < dataSkipLength; idx++) {
                            stream.readByte();
                        }
                    }
                }
            } else
                Utils.println("No node found for " + x + " " + y + " " + z);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetNodeSingleSerialized(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            EntityPlayer clientPlayer = player;
            int x, y, z, dimention;
            x = stream.readInt();
            y = stream.readInt();
            z = stream.readInt();
            dimention = stream.readInt();

            if (clientPlayer.dimension == dimention) {
                TileEntity entity = WorldCompat.getTileEntity(clientPlayer.world, x, y, z);
                if (entity != null && entity instanceof INodeEntity) {
                    INodeEntity node = (INodeEntity) entity;
                    if (node.getNodeUuid().equals(stream.readUTF())) {
                        node.serverPublishUnserialize(stream);
                        if (0 != stream.available()) {
                            Utils.println("0 != stream.available()");

                        }
                    } else {
                        Utils.println("Wrong node UUID warning");
                        int dataSkipLength = stream.readByte();
                        for (int idx = 0; idx < dataSkipLength; idx++) {
                            stream.readByte();
                        }
                    }
                } else
                    Utils.println("No node found for " + x + " " + y + " " + z);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
