package mods.eln.client;

import java.io.IOException;
import java.net.URL;

import mods.eln.misc.Color;
import mods.eln.misc.I18N;
import mods.eln.misc.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/**
 * Check the current version with the last stable version when the map is loaded
 * by the client.<br>
 * Singleton class. Uses the {@link ClientTickEvent} and must be registered by
 * the caller on the {@link FMLCommonHandler} bus.
 * 
 * @author metc
 */
public class VersionCheckerHandler {

	// Official website link
	// Now use a PHP page with Google Analytics tracking
	private final static String URL = "http://mc.electrical-age.net/version.php?v=%s&l=%s";

	private static VersionCheckerHandler instance;

	private boolean ready = false;
	private String versionMsg = null;
	private Thread versionThread;

	public static VersionCheckerHandler getInstance() {
		if (instance == null)
			instance = new VersionCheckerHandler();
		return instance;
	}

	private VersionCheckerHandler() {
		// Must be done by the caller !
		// FMLCommonHandler.instance().bus().register(this);

		// Check online if a new mod version if available (asynchronous HTTP
		// request).
		versionThread = new Thread(new Runnable() {
			@Override
			public void run() {
				String msg = null;
				try {
					// HTTP GET request with the current mod version and language. Yes, you are tracked !
					final String v = Version.getVersionName().replaceAll("\\s+", "");
					final String url = String.format(URL, v, I18N.getCurrentLanguage());
					final String urlSrc = IOUtils.toString(new URL(url));
					JsonObject j = new JsonParser().parse(urlSrc)
							.getAsJsonObject();
					int manifestVersion = j.get("manifest_version").getAsInt();
					if (manifestVersion != 1)
						throw new IOException();

					// Read the last stable version
					JsonObject stable = j.get("stable").getAsJsonObject();
					int rev = stable.get("version_revision").getAsInt();
					int currentRev = Integer.valueOf(Version.REVISION);

					// New stable version
					if (rev > currentRev) {
						int major = stable.get("version_major").getAsInt();
						int minor = stable.get("version_minor").getAsInt();
						msg = String
								.format(Color.GREEN
										+ "> New stable version avaiable: BETA-%d.%d r%d - please upgrade !",
										major, minor, rev);
					}
					// No update
					else if (rev == currentRev) {
						msg = "> No update available (last stable version)";
					}
					// DEV version (not stable)
					else {
						msg = Color.RED
								+ "> Warning: this is a version under test !";
					}

				} catch (Exception e) {
					String error = "Unable to check the lastest available version.";
					System.err.println(error);
					msg = Color.RED + "> " + error;
				}

				// Ready. Display the message on the client chat.
				VersionCheckerHandler.getInstance().versionMsg = msg;
				VersionCheckerHandler.getInstance().ready = true;
			}
		});

		versionThread.start();
	}

	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if (!ready || event.phase == Phase.START)
			return;

		final Minecraft m = FMLClientHandler.instance().getClient();
		final WorldClient world = m.theWorld;
		if (m == null || world == null)
			return;

		if (!ready)
			return;

		// Print the current version when the client start a map
		m.thePlayer.addChatMessage(new ChatComponentText(Version.printColor()));
		m.thePlayer.addChatMessage(new ChatComponentText(versionMsg));

		FMLCommonHandler.instance().bus().unregister(this);
		ready = false;
	}
}
