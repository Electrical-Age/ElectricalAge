package mods.eln.api.impl.lowlevel;

import mods.eln.api.impl.HostedMachines;
import mods.eln.api.impl.ServerThread;
import mods.eln.api.lowlevel.FaceConnection;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The block entities low-level circuits are hosted at, and their nodes' lifecycle, run by HostedMachines' tick
 * (ServerTickEvent.Pre, before the simulator's step): nodes of block entities that are gone (isRemoved, or moved to
 * another level) retire, nodes asked for (Circuit.hostAt, maybe mid chunk load) are placed, turned blocks reconnect.
 */
public final class CircuitHosts {
    public static final CircuitHosts INSTANCE = new CircuitHosts();
    private static final Logger LOG = LoggerFactory.getLogger("eln");

    private final Map<BlockEntity, CircuitNode> nodes = new LinkedHashMap<>();
    private final Map<BlockEntity, CircuitNode> queued = new LinkedHashMap<>();

    private CircuitHosts() {
    }

    /** Circuit.hostAt: checks be, then queues a node for it (placed at the next tick). */
    CircuitNode host(BlockEntity be, CircuitImpl circuit, FaceConnection[] faces) {
        ServerThread.check();
        if (!(be.getLevel() instanceof ServerLevel level))
            throw new IllegalStateException("hostAt needs a server block entity in a level (call it from onLoad or the server tick, not "
                + (be.getLevel() == null ? "before the block entity has a level)" : "on the client)"));
        if (be.isRemoved()) throw new IllegalStateException("hostAt on a removed block entity at " + be.getBlockPos());
        if (HostedMachines.INSTANCE.registered(be.getType()))
            throw new IllegalStateException("the block entity at " + be.getBlockPos() + " is a machine (ElnApi.machines()); "
                + "it can't host a circuit too (one EA node per block)");
        if (nodes.containsKey(be) || queued.containsKey(be))
            throw new IllegalStateException("the block entity at " + be.getBlockPos() + " already hosts a circuit");
        CircuitNode n = new CircuitNode(level, be, circuit, faces);
        queued.put(be, n);
        return n;
    }

    /** Before each step: retire the gone, place the queued, follow turned blocks. */
    public void tick() {
        if (NodeManager.instance == null) return;
        for (Iterator<CircuitNode> i = nodes.values().iterator(); i.hasNext(); ) {
            CircuitNode n = i.next();
            if (gone(n)) {
                i.remove();
                n.retire();
            }
        }
        for (CircuitNode n : new ArrayList<>(queued.values())) {
            queued.remove(n.be);
            if (gone(n)) {
                n.retire();
                continue;
            }
            NodeBase old = NodeManager.instance.getNodeFromCoordonate(new Coordonate(n.be.getBlockPos(), n.level));
            if (old != null) {
                LOG.error("Electrical Age: a circuit can't be hosted at {}: an EA node ({}) is already there", n.be.getBlockPos(),
                    old.getClass().getSimpleName());
                n.retire();
                continue;
            }
            try {
                n.place();
                nodes.put(n.be, n);
            } catch (RuntimeException e) {
                LOG.error("Electrical Age: hosting {} at {} failed; it stays out of the world", n.circuit, n.be.getBlockPos(), e);
                n.retire();
            }
        }
        for (CircuitNode n : new ArrayList<>(nodes.values())) n.followFacing();
    }

    private static boolean gone(CircuitNode n) {
        return n.be.isRemoved() || n.be.getLevel() != n.level;
    }

    /** The node hosting at be now, or null (tests). */
    public mods.eln.api.lowlevel.HostedNode node(BlockEntity be) {
        return nodes.get(be);
    }

    /** The world's start and stop: forget everything (the node manager and simulator are cleared with it). */
    public void clear() {
        for (CircuitNode n : nodes.values()) n.abandon();
        for (CircuitNode n : queued.values()) n.abandon();
        nodes.clear();
        queued.clear();
    }
}
