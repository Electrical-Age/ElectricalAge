package mods.eln.sim.process.destruct;

import mods.eln.node.NodeBase;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.IProcess;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The nodes connected to a failing device, for Simulator's one-failure-per-network rule (26.x, Baughn's playtest:
 * a fault through a run of cables burned every cable, not the weakest one).
 */
public final class FaultNetwork {
    private FaultNetwork() {
    }

    /** Every node reachable from start through node connections, start included. */
    public static Set<NodeBase> of(NodeBase start) {
        Set<NodeBase> seen = new HashSet<>();
        ArrayDeque<NodeBase> todo = new ArrayDeque<>();
        seen.add(start);
        todo.add(start);
        while (!todo.isEmpty()) {
            NodeBase n = todo.poll();
            for (NodeConnection c : n.nodeConnectionList) {
                NodeBase o = c.other(n);
                if (o != null && seen.add(o)) todo.add(o);
            }
        }
        return seen;
    }

    /**
     * A device of this network failed and opened the circuit: the rest got no further than their limits. Every
     * watchdog over its limit is set back to it (temperature) and re-armed, so it fails later only if the fault goes on.
     */
    public static void relieve(Collection<NodeBase> nodes) {
        for (NodeBase n : nodes) {
            if (n instanceof SixNode six) {
                for (SixNodeElement e : six.sideElementList) if (e != null) relieve(e.slowProcessList);
            } else if (n instanceof TransparentNode t && t.element != null) {
                relieve(t.element.slowProcessList);
            }
        }
    }

    private static void relieve(List<IProcess> processes) {
        for (IProcess p : processes) if (p instanceof ValueWatchdog w) w.relieve();
    }
}
