package mods.eln.network;

import java.io.InputStream;
import java.util.Properties;

/**
 * The commit this jar was built from (eln-build.properties, written by build.gradle). It versions EA's network
 * channel: NeoForge refuses a client whose channel version differs, so a pack built from another commit than the
 * server's gets "mod version mismatch" at login instead of silently mismatched block states (Baughn, 2026-10-25).
 */
public final class BuildInfo {
    private BuildInfo() {
    }

    public static final String COMMIT = read();

    private static String read() {
        try (InputStream in = BuildInfo.class.getResourceAsStream("/eln-build.properties")) {
            if (in == null) return "unknown";
            Properties p = new Properties();
            p.load(in);
            String c = p.getProperty("commit", "unknown").trim();
            return c.isEmpty() ? "unknown" : c;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** The channel version: the protocol number and the build commit. */
    public static String channelVersion(String protocol) {
        return protocol + "+" + COMMIT;
    }
}
