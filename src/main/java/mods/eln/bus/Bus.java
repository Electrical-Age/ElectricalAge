package mods.eln.bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A field bus: the ports one connected run of bus cable reaches, and its named channels. Values are 0..1 (a signal's
 * share of its 50 V). Double-buffered: what devices write in a simulator tick is read in the next one, so the order
 * devices run in doesn't matter; several writers on one channel: the highest wins (a wired OR). Nothing here is in the
 * circuit solver.
 */
public final class Bus {
    /** Most channel names on one bus (Baughn: 64 is plenty). Checked when a player sets a name (BusNet.room). */
    public static final int MAX_CHANNELS = 64;
    /** A channel name: 1-16 of a-z, 0-9 and _. */
    static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,16}");

    public static boolean validName(String n) {
        return n != null && NAME.matcher(n).matches();
    }

    final List<BusPort> ports = new ArrayList<>();
    final Set<String> names = new LinkedHashSet<>();
    Map<String, Double> cur = new HashMap<>(), next = new HashMap<>();
    Map<String, Integer> writers = new HashMap<>(), nextWriters = new HashMap<>();

    double read(String name) {
        Double v = cur.get(name);
        return v == null ? 0 : v;
    }

    void write(String name, double v) {
        if (!names.contains(name)) return;
        double c = Math.max(0, Math.min(1, v));
        next.merge(name, c, Math::max);
        nextWriters.merge(name, 1, Integer::sum);
    }

    /** The claimed names again (a port's claims changed). */
    void reclaim() {
        names.clear();
        for (BusPort p : ports) names.addAll(p.claims);
    }

    /** Next tick's values become this tick's. */
    void swap() {
        cur = next;
        next = new HashMap<>();
        writers = nextWriters;
        nextWriters = new HashMap<>();
    }

    /** Its channel names, in the order ports claimed them. */
    public Set<String> names() {
        return java.util.Collections.unmodifiableSet(names);
    }

    /** A channel's level now (0..1). */
    public double value(String name) {
        return read(name);
    }

    /** How many writes a channel had last tick (two or more: a clash to show). */
    public int writers(String name) {
        Integer n = writers.get(name);
        return n == null ? 0 : n;
    }

    public int portCount() {
        return ports.size();
    }
}
