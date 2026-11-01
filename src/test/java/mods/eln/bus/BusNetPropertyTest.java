package mods.eln.bus;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Random links, breaks and rejoins of bus ports against a plain reference graph (Baughn: one property test for the
 * scenarios, rejoining broken buses among them). After each tick: two linked ports share a bus exactly when the graph
 * connects them, a port without links is on no bus, and a value written on one port is read by exactly the ports its
 * bus reaches.
 */
class BusNetPropertyTest {
    @Property(tries = 300)
    void busesFollowTheGraph(@ForAll long seed, @ForAll @IntRange(min = 2, max = 14) int n) {
        BusNet.clear();
        try {
            Random r = new Random(seed);
            List<BusPort> ports = new ArrayList<>();
            List<Set<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BusPort p = new BusPort();
                p.claim(1 << 5);
                ports.add(p);
                adj.add(new HashSet<>());
            }
            for (int step = 0; step < 60; step++) {
                int ops = 1 + r.nextInt(4);
                for (int o = 0; o < ops; o++) {
                    int i = r.nextInt(n), j = r.nextInt(n);
                    if (r.nextInt(3) > 0) {   // link (a cable or pin joined), often rejoining what a break split
                        BusNet.link(ports.get(i), ports.get(j));
                        if (i != j) {
                            adj.get(i).add(j);
                            adj.get(j).add(i);
                        }
                    } else {                  // a node disconnecting: its port loses every link
                        BusNet.detach(List.of(ports.get(i)));
                        for (int k : adj.get(i)) adj.get(k).remove(i);
                        adj.get(i).clear();
                    }
                }
                BusNet.tick();
                int[] comp = components(adj);
                for (int i = 0; i < n; i++) {
                    Bus bi = ports.get(i).bus();
                    if (adj.get(i).isEmpty()) {
                        assertNull(bi, "port " + i + " has no links, so no bus (step " + step + ")");
                        continue;
                    }
                    assertNotNull(bi, "port " + i + " is linked, so on a bus");
                    for (int j = 0; j < n; j++) {
                        if (adj.get(j).isEmpty()) continue;
                        assertEquals(comp[i] == comp[j], bi == ports.get(j).bus(), "ports " + i + ", " + j + " (step " + step + ")");
                    }
                }
                // one port writes; next tick exactly its bus reads it
                int w = r.nextInt(n);
                double v = 0.1 + 0.8 * r.nextDouble();
                ports.get(w).write(5, v);
                BusNet.tick();
                for (int i = 0; i < n; i++) {
                    boolean reach = !adj.get(w).isEmpty() && !adj.get(i).isEmpty() && comp[i] == comp[w];
                    assertEquals(reach ? v : 0, ports.get(i).read(5), 1e-9, "port " + i + " after port " + w + " wrote (step " + step + ")");
                }
            }
        } finally {
            BusNet.clear();
        }
    }

    static int[] components(List<Set<Integer>> adj) {
        int n = adj.size();
        int[] c = new int[n];
        java.util.Arrays.fill(c, -1);
        int k = 0;
        for (int s = 0; s < n; s++) {
            if (c[s] >= 0) continue;
            java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
            q.add(s);
            c[s] = k;
            while (!q.isEmpty()) for (int o : adj.get(q.poll())) if (c[o] < 0) {
                c[o] = k;
                q.add(o);
            }
            k++;
        }
        return c;
    }
}
