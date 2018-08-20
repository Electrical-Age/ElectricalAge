package mods.eln.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.channel.ChannelHandler.Sharable;
import mods.eln.Eln;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

@Sharable
public class ClientPacketHandler {

    public ClientPacketHandler() {
        //FMLCommonHandler.instance().bus().register(this);
        Eln.eventChannel.register(this);
    }

    @SubscribeEvent
    public void onClientPacket(ClientCustomPacketEvent event) {
        //Utils.println("onClientPacket");
        FMLProxyPacket packet = event.getPacket();
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet.payload().array()));
        NetworkManager manager = event.getManager();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer; // EntityClientPlayerMP

        Eln.packetHandler.packetRx(stream, manager, player);
    }
}
