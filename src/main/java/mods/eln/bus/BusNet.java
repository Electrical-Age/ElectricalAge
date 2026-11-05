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

    /**
     * Drop every link of these ports (their node is disconnecting). They keep their bus until the next rebuild, which
     * a reconnect usually rejoins them to: the rebuilt bus starts from it, so a node reconnecting (a part placed in its
     * block) never shows running logic a 0. A port the rebuild doesn't reach leaves its bus then.
     */
    public static void detach(Collection<BusPort> ports) {
        for (BusPort p : ports) {
            if (p == null) continue;
            for (BusPort o : p.links) {
                o.links.remove(p);
                if (o.links.isEmpty()) ALL.remove(o);
            }
            if (!p.links.isEmpty() || p.bus != null) dirty = true;
            p.links.clear();
            ALL.remove(p);
        }
    }

    /** Once a simulator tick, before anything else runs: find the buses again if links changed, then swap buffers. */
    public static void tick() {
        if (dirty) rebuild();
        for (Bus b : BUSES) b.swap();
    }

    /** Find the buses again now if links changed, without swapping (tests: a swap mid-tick would drop every bus's
     *  pending writes, the other tests' too). */
    public static void refresh() {
        if (dirty) rebuild();
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
            // seeded from the old buses: the highest per channel, now and pending
            for (Bus o : old)
                for (int c = 0; c < BusChannel.COUNT; c++) {
                    nb.cur[c] = Math.max(nb.cur[c], o.cur[c]);
                    nb.next[c] = Math.max(nb.next[c], o.next[c]);
                    nb.writers[c] += o.writers[c];
                    nb.nextWriters[c] += o.nextWriters[c];
                }
            for (Bus o : old) nb.droppedTotal += o.droppedTotal;
            made.add(nb);
        }
        // packets still queued go with their sender to its new bus (a sender no bus reaches now: dropped)
        for (Bus o : BUSES)
            for (Bus.Queued e : o.outbox)
                if (e.from() != null && seen.contains(e.from())) e.from().bus.outbox.add(e);
        // ports no bus reaches now (no links left) leave their old bus
        for (Bus o : BUSES) for (BusPort p : o.ports) if (!seen.contains(p)) p.bus = null;
        BUSES.clear();
        BUSES.addAll(made);
    }

    /** World start and stop: forget everything (the GameTest server and lab clones load several worlds in one JVM). */
    public static void clear() {
        Bus.sequence = 0;
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
