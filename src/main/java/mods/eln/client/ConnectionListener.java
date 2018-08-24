package mods.eln.client;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Type;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConnectionListener {

    public ConnectionListener() {
        FMLCommonHandler.instance().bus().register(this);
    }

    static boolean newConnection = false;
    static int timer = 0;

    @SubscribeEvent
    public void onConnectedToServerEvent(ClientConnectedToServerEvent event) {
        Utils.println("Connected to server " + FMLCommonHandler.instance().getEffectiveSide());
        Eln.instance.regenOreScannerFactors();

        timer = 20;
        newConnection = true;
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
        Utils.println("Disconnected from server " + FMLCommonHandler.instance().getEffectiveSide());
        UtilsClient.glDeleteListsAllSafe();
    }

    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.type != Type.CLIENT) return;

        if (newConnection) {
            if (timer-- != 0) return;

            newConnection = false;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
            DataOutputStream stream = new DataOutputStream(bos);

            try {
                stream.writeByte(Eln.packetClientToServerConnection);
            } catch (IOException e) {

                e.printStackTrace();
            }

            UtilsClient.sendPacketToServer(bos);
        }
    }
}
