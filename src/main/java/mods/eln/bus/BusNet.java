package mods.eln.bus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The field buses: links between ports, and the buses they make. Nodes link their ports as they connect and detach
 * them as they disconnect (never a NodeConnection: that would merge unrelated circuits' fault networks). The buses are
 * found again by a flood at the start of the next simulator tick (Simulator.tick calls tick()), a rebuilt bus starting
 * from its members' old values, so a cable placed nearby never shows running logic a 0. Server thread only.
 */
public final class BusNet {
    private BusNet() {
    }

    /** Ports with links (a port without any is on no bus). */
    private static final Set<BusPort> ALL = new LinkedHashSet<>();
    private static final Set<Bus> BUSES = new LinkedHashSet<>();
    private static boolean dirty;

    public static void link(BusPort a, BusPort b) {
        if (a == null || b == null || a == b) return;
        if (a.links.add(b) | b.links.add(a)) {
            ALL.add(a);
            ALL.add(b);
            dirty = true;
        }
    }

    /** Drop every link of these ports (their node is disconnecting); they leave their buses at the next rebuild. */
    public static void detach(Collection<BusPort> ports) {
        for (BusPort p : ports) {
            if (p == null) continue;
            for (BusPort o : p.links) {
                o.links.remove(p);
                if (o.links.isEmpty()) {
                    ALL.remove(o);
                    o.bus = null;
                }
            }
            if (!p.links.isEmpty()) dirty = true;
            p.links.clear();
            ALL.remove(p);
            p.bus = null;
        }
    }

    /** Once a simulator tick, before anything else runs: find the buses again if links changed, then swap buffers. */
    public static void tick() {
        if (dirty) rebuild();
        for (Bus b : BUSES) b.swap();
    }

    static void rebuild() {
        dirty = false;
        Set<BusPort> seen = new HashSet<>();
        List<Bus> made = new ArrayList<>();
        for (BusPort start : ALL) {
            if (!seen.add(start)) continue;
            Bus nb = new Bus();
            Set<Bus> old = new LinkedHashSet<>();
            ArrayDeque<BusPort> q = new ArrayDeque<>();
            q.add(start);
            while (!q.isEmpty()) {
                BusPort p = q.poll();
                nb.ports.add(p);
                if (p.bus != null) old.add(p.bus);
                for (BusPort o : p.links) if (seen.add(o)) q.add(o);
            }
            for (BusPort p : nb.ports) p.bus = nb;
            nb.reclaim();
            // seeded from the old buses: the highest per name, now and pending
            for (Bus o : old) {
                o.cur.forEach((k, v) -> nb.cur.merge(k, v, Math::max));
                o.next.forEach((k, v) -> nb.next.merge(k, v, Math::max));
                o.writers.forEach((k, v) -> nb.writers.merge(k, v, Integer::sum));
                o.nextWriters.forEach((k, v) -> nb.nextWriters.merge(k, v, Integer::sum));
            }
            made.add(nb);
        }
        BUSES.clear();
        BUSES.addAll(made);
    }

    /** Can a port on this bus claim these names without passing the bus's limit? A port on no bus: always. */
    public static boolean room(BusPort port, Collection<String> names) {
        Bus b = port.bus;
        if (b == null) return true;
        // the port's own old claims leave; another port's claim of the same name costs nothing
        Set<String> others = new HashSet<>();
        for (BusPort p : b.ports) if (p != port) others.addAll(p.claims);
        Set<String> after = new HashSet<>(others);
        after.addAll(names);
        return after.size() <= Bus.MAX_CHANNELS;
    }

    /** World start and stop: forget everything (the GameTest server and lab clones load several worlds in one JVM). */
    public static void clear() {
        ALL.clear();
        BUSES.clear();
        dirty = false;
    }

    /** Buses now (tests, debug). */
    public static Collection<Bus> buses() {
        return java.util.Collections.unmodifiableSet(BUSES);
    }

    static Map<String, Integer> debug() {
        Map<String, Integer> m = new HashMap<>();
        m.put("ports", ALL.size());
        m.put("buses", BUSES.size());
        return m;
    }
}
