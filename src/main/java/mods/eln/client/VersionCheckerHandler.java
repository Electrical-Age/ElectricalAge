package mods.eln.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import mods.eln.Eln;
import mods.eln.misc.Color;
import mods.eln.misc.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Check the current mod version with the last stable version when the map is loaded
 * by the client.<br>
 * Singleton class. Uses the {@link ClientTickEvent} and must be registered by
 * the caller on the {@link FMLCommonHandler} bus.
 *
 * @author metc
 */
public class VersionCheckerHandler {

    // Current mod version file hosted on Github
    private final static String URL = "http://electrical-age.net/modinfo/modinfo.json";

    private static VersionCheckerHandler instance;

    private boolean ready = false;
    private String versionMsg = null;

    public static VersionCheckerHandler getInstance() {
        if (instance == null)
            instance = new VersionCheckerHandler();
        return instance;
    }

    private VersionCheckerHandler() {
        // Check online if a new mod version if available (asynchronous HTTP request).
        Thread versionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    // Get the mod info Json file
                    final String urlSrc = IOUtils.toString(new URL(URL));
                    JsonObject j = new JsonParser().parse(urlSrc).getAsJsonObject();
                    int manifestVersion = j.get("manifest_version").getAsInt();
                    if (manifestVersion != 1)
                        throw new IOException();

                    // Read the last stable version
                    JsonObject stable = j.get("stable").getAsJsonObject();
                    int uniqueVersion = 1000000 * stable.get("version_major").getAsInt() +
                        1000 * stable.get("version_minor").getAsInt() + stable.get("version_revision").getAsInt();
                    int currentUniqueVersion = Version.UNIQUE_VERSION;

                    // New stable version
                    if (uniqueVersion > currentUniqueVersion) {
                        int major = stable.get("version_major").getAsInt();
                        int minor = stable.get("version_minor").getAsInt();
                        int revision = stable.get("version_revision").getAsInt();
                        msg = String.format(Color.GREEN + "> New stable version available: %d.%d.%d" + " - please upgrade !",
                            major, minor, revision);
                    }
                    // No update
                    else if (uniqueVersion == currentUniqueVersion) {
                        msg = "> No update available (last stable version)";
                    }
                    // DEV version (not stable)
                    else {
                        msg = Color.RED + "> Warning: this is a version under test !";
                    }

                } catch (Exception e) {
                    final String error = "Unable to check the latest available version.";
                    System.err.println(error);
                    msg = Color.RED + "> " + error;

                    e.printStackTrace();
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
        if (Eln.versionCheckEnabled) {
            m.thePlayer.addChatMessage(new ChatComponentText(Version.printColor()));
            m.thePlayer.addChatMessage(new ChatComponentText(versionMsg));
        }

        FMLCommonHandler.instance().bus().unregister(this);
        ready = false;
    }
}
