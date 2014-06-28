package mods.eln.client;

import mods.eln.misc.Color;
import mods.eln.misc.I18N;
import mods.eln.misc.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Type;

/**
 * Check version when the map is loaded by the client.
 */
public class VersionCheckerHandler {

	private static VersionCheckerHandler instance;
	private boolean boot = true;

	public static VersionCheckerHandler getInstance() {
		if (instance == null)
			instance = new VersionCheckerHandler();
		return instance;
	}

	private VersionCheckerHandler() {
		FMLCommonHandler.instance().bus().register(this);
	}

	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if (!boot || event.phase == Phase.START)
			return;

		final Minecraft m = FMLClientHandler.instance().getClient();
		final WorldClient world = m.theWorld;
		if (m == null || world == null)
			return;

		// Print the current version when the client start a map
		m.thePlayer.addChatMessage(new ChatComponentText(Version.printColor()));

		// TODO: check online if a new version is available (some work must be
		// done on server side first)

		boot = false; // Single-shot event
	}

}
