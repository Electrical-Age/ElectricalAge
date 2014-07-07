package mods.eln.client;

import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;

public class ConnectionListener {

	public ConnectionListener() {
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void onConnectedToServerEvent(ClientConnectedToServerEvent event) {
		Utils.println("Connected to server " + FMLCommonHandler.instance().getEffectiveSide());
	}

	@SubscribeEvent
	public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
		Utils.println("Disconnected from server " + FMLCommonHandler.instance().getEffectiveSide());
		UtilsClient.glDeleteListsAllSafe();
	}
}
