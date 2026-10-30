package mods.eln.sixnode.electricaldatalogger;

/**
 * The client side of {@link LogWatchers}: while the graph can be seen, ask the server to keep sending it, once a second.
 */
public final class LogWatch {
    long last;

    /** Call while the graph is wanted; true when an ask is due (send it). */
    public boolean due() {
        long now = net.minecraft.util.Util.getMillis();
        if (now - last < 1000) return false;
        last = now;
        return true;
    }
}
