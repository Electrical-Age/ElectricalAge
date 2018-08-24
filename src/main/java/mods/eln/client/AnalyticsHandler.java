package mods.eln.client;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

/**
 * Sent analytics information about the mod and the game configuration.<br>
 * Singleton class. Uses the {@link net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent} and must be registered by
 * the caller on the {@link net.minecraftforge.fml.common.FMLCommonHandler} bus.
 *
 * @author metc
 */
public class AnalyticsHandler {

    private final static String URL = "http://mc.electrical-age.net/version.php?id=%s&v=%s&l=%s";

    private static AnalyticsHandler instance;

    private boolean ready = false;

    public static AnalyticsHandler getInstance() {
        if (instance == null)
            instance = new AnalyticsHandler();
        return instance;
    }

    private AnalyticsHandler() {
        // Send analytics data.
        Thread analyticsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Prepare get parameters
                    final String version = Version.getVersionName().replaceAll("\\s+", "");
                    final String lang = I18N.getCurrentLanguage();
                    final String url = String.format(URL, Eln.playerUUID, version, lang);

                    // Send HTTP get request
                    CloseableHttpClient client = HttpClientBuilder.create().build();
                    CloseableHttpResponse response = client.execute(new HttpGet(url));

                    final int repCode = response.getStatusLine().getStatusCode();
                    if (repCode != HttpStatus.SC_OK)
                        throw new IOException("HTTP error " + repCode);

                    // Utils.println("URL: " + url);

                    response.close();
                    client.close();

                } catch (Exception e) {
                    String error = "Unable to send analytics data: " + e.getMessage() + ".";
                    System.err.println(error);
                }
                AnalyticsHandler.getInstance().ready = true;
            }
        });

        analyticsThread.start();
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

        FMLCommonHandler.instance().bus().unregister(this);
        ready = false;
    }
}
