package mods.eln.gametest;

import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Save/load round trip of one node, the way a world load does it (NodeManager.loadFromNbt): save the node, unload and
 * forget it, build a fresh node of the same class from that tag, then save the fresh one. The two tags, and the
 * elements' client packets (networkSerialize), should be equal. Ported from the 1.12 selftest (step T3); the 26.x harness runs it on every node of every case after measure().
 */
public final class Reload {
    public final CompoundTag before, after;
    public final byte[] publishBefore, publishAfter;
    public final NodeBase fresh;

    private Reload(CompoundTag before, CompoundTag after, byte[] pb, byte[] pa, NodeBase fresh) {
        this.before = before;
        this.after = after;
        this.publishBefore = pb;
        this.publishAfter = pa;
        this.fresh = fresh;
    }

    public static Reload node(ServerLevel w, BlockPos p) throws ReflectiveOperationException {
        NodeBase old = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p, w));
        if (old == null) throw new IllegalStateException("no node at " + p);
        CompoundTag before = save(old);
        byte[] pb = publish(old);
        old.unload();
        NodeManager.instance.removeNode(old);

        NodeBase fresh = old.getClass().getConstructor().newInstance();
        fresh.readFromNBT(before.copy());
        NodeManager.instance.addNode(fresh);
        fresh.initializeFromNBT();
        fresh.globalBoot();
        BlockEntity te = w.getBlockEntity(p);
        if (te instanceof NodeBlockEntity) ((NodeBlockEntity) te).forgetNode();
        return new Reload(before, save(fresh), pb, publish(fresh), fresh);
    }

    static CompoundTag save(NodeBase n) {
        CompoundTag t = new CompoundTag();
        t.putString("tag", n.getNodeUuid());
        n.writeToNBT(t);
        return t;
    }

    /** The elements' client state as the server would send it. */
    static byte[] publish(NodeBase n) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        if (n instanceof SixNode) {
            for (SixNodeElement e : ((SixNode) n).sideElementList) if (e != null) e.networkSerialize(out);
        } else if (n instanceof TransparentNode) {
            ((TransparentNode) n).element.networkSerialize(out);
        }
        try {
            out.flush();
        } catch (IOException ignored) {
        }
        return bos.toByteArray();
    }

    public boolean tagsEqual() {
        return before.equals(after);
    }

    public boolean publishEqual() {
        return Arrays.equals(publishBefore, publishAfter);
    }

    /** Paths of the keys that differ between the two tags (at most 8). */
    public List<String> differences() {
        List<String> out = new ArrayList<>();
        diff("", before, after, out);
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private static void diff(String path, CompoundTag a, CompoundTag b, List<String> out) {
        TreeSet<String> keys = new TreeSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (String k : keys) {
            Tag x = a.get(k), y = b.get(k);
            if (x == null || y == null) {
                out.add(path + k + (x == null ? " (only after)" : " (only before)"));
            } else if (x instanceof CompoundTag && y instanceof CompoundTag) {
                diff(path + k + "/", (CompoundTag) x, (CompoundTag) y, out);
            } else if (!x.equals(y)) {
                out.add(path + k + ": " + x + " -> " + y);
            }
        }
    }

    /**
     * Live simulation state the simulator recomputes on the next tick, so a difference right after a load is not lost
     * data: node voltages (...Uc) and switch-resistor states (...State).
     */
    public static boolean isTransient(String difference) {
        String key = difference.split(":")[0].trim();
        key = key.substring(key.lastIndexOf('/') + 1);
        return key.endsWith("Uc") || key.endsWith("State");
    }

    public int firstPublishDifference() {
        int n = Math.min(publishBefore.length, publishAfter.length);
        for (int i = 0; i < n; i++) if (publishBefore[i] != publishAfter[i]) return i;
        return n;
    }

    public static String describe(NodeBase n) {
        if (n instanceof SixNode) {
            StringBuilder b = new StringBuilder("SixNode[");
            for (SixNodeElement e : ((SixNode) n).sideElementList)
                if (e != null) b.append(e.sixNodeElementDescriptor.name).append(' ');
            return b.toString().trim() + "]";
        }
        if (n instanceof TransparentNode) return ((TransparentNode) n).element.getDescriptor().name;
        return n.getClass().getSimpleName();
    }
}
