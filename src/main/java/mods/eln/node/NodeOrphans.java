package mods.eln.node;

import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes whose block is gone (incognito 2026-10-17: three batteries stayed in the simulation on air after a blow-up,
 * invisible, and survived restarts). Every 10 s, a six-node or transparent node whose chunk is loaded but whose block
 * isn't a node block anymore leaves the simulation and the node list, without drops (its block already went). Also
 * `/elnlab remove x y z` for one position.
 */
public final class NodeOrphans {
    private NodeOrphans() {
    }

    private static int ticks;

    public static void tick(MinecraftServer server) {
        if (++ticks % 200 != 0) return;
        List<NodeBase> orphans = new ArrayList<>();
        for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
            if (!(n instanceof SixNode) && !(n instanceof TransparentNode)) continue;   // (other mods' nodes keep their own blocks)
            if (isOrphan(n)) orphans.add(n);
        }
        for (NodeBase n : orphans) {
            Utils.println("removing orphaned node " + n.getClass().getSimpleName() + " at " + n.coordonate + " (its block is gone)");
            remove(n);
        }
    }

    /** Its chunk is loaded and the block there isn't a node block. */
    public static boolean isOrphan(NodeBase n) {
        if (n.coordonate == null) return false;
        if (!(n.coordonate.world() instanceof ServerLevel level)) return false;
        BlockPos p = n.coordonate.pos();
        if (!level.isLoaded(p)) return false;
        return !(level.getBlockState(p).getBlock() instanceof NodeBlock);
    }

    /** Take a node out of the simulation and the node list (no drops). */
    public static void remove(NodeBase n) {
        try {
            n.disconnect();
        } catch (RuntimeException e) {
            Utils.println("orphan disconnect failed: " + e);
        }
        NodeManager.instance.removeNode(n);
    }
}
