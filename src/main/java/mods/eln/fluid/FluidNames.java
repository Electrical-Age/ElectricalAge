package mods.eln.fluid;

import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * Stable names for the fluid nodes of a set of devices, for canonical topology dumps (FluidWorld.Plan.dump, the live
 * networks' FluidWorld.liveTopology) that don't depend on the order the devices joined in: the device's level and block
 * (FluidDevice.fluidPos), then the node's id in it (a voxel part's id) or its place among the device's nodes. A node
 * several devices share (a tank box's vessel) takes the least of its names, by block and then id (a node a group
 * presents through one of its members, a heat exchanger stack's slices, is named by that member).
 */
final class FluidNames {
    /** A node's name: its device's level and block, then its id in the device (kind 0) or its place there (kind 1). */
    private record Key(String level, long pos, int kind, int num) implements Comparable<Key> {
        // (by number, not text: which of two names is the lesser doesn't depend on where in the world they are)
        public int compareTo(Key o) {
            int c = level.compareTo(o.level);
            if (c != 0) return c;
            if ((pos == Long.MIN_VALUE) != (o.pos == Long.MIN_VALUE)) return pos == Long.MIN_VALUE ? 1 : -1;
            BlockPos a = BlockPos.of(pos), b = BlockPos.of(o.pos);
            c = pos == Long.MIN_VALUE ? 0 : a.getX() != b.getX() ? Integer.compare(a.getX(), b.getX())
                : a.getY() != b.getY() ? Integer.compare(a.getY(), b.getY()) : Integer.compare(a.getZ(), b.getZ());
            if (c != 0) return c;
            return kind != o.kind ? Integer.compare(kind, o.kind) : Integer.compare(num, o.num);
        }

        @Override
        public String toString() {
            return level + " " + xyz(pos) + (kind == 0 ? " #" : " n") + num;
        }
    }

    private final Map<FluidNode, Key> key = new IdentityHashMap<>();

    FluidNames(Collection<FluidDevice> devices) {
        for (FluidDevice d : devices) {
            long pos = blockOf(d);
            String level = String.valueOf(d.fluidLevel());
            List<FluidNode> own = new ArrayList<>();
            for (FluidNode n : FluidWorld.nodesOf(d)) if (!own.contains(n)) own.add(n);
            for (int i = 0; i < own.size(); i++) {
                FluidNode n = own.get(i);
                int id = d.fluidNodeId(n);
                Key k = id >= 0 ? new Key(level, pos, 0, id) : new Key(level, pos, 1, i);
                Key was = key.get(n);
                if (was == null || k.compareTo(was) < 0) key.put(n, k);
            }
        }
    }

    /** A node's name ("?" for a node of none of the devices). */
    String of(FluidNode n) {
        Key k = key.get(n);
        return k == null ? "?" : k.toString();
    }

    /** The name of the least of some nodes (a run by its least piece), "?" if none of them is named. */
    String least(Collection<FluidNode> ns) {
        Key min = null;
        for (FluidNode n : ns) {
            Key k = key.get(n);
            if (k != null && (min == null || k.compareTo(min) < 0)) min = k;
        }
        return min == null ? "?" : min.toString();
    }

    /** Is n a node of one of the devices? */
    boolean named(FluidNode n) {
        return key.containsKey(n);
    }

    /** Is the block naming n kept by the filter (a node of none of the devices: no)? */
    boolean in(FluidNode n, LongPredicate keep) {
        Key k = key.get(n);
        return k != null && k.pos != Long.MIN_VALUE && keep.test(k.pos);
    }

    /** The block naming a device: its own (fluidPos), its node's for a machine, else its lowest port's (Long.MIN_VALUE: none). */
    static long blockOf(FluidDevice d) {
        long pos = d.fluidPos();
        if (pos != Long.MIN_VALUE) return pos;
        if (d instanceof mods.eln.node.transparent.TransparentNodeElement e && e.node != null) return e.node.coordonate.pos().asLong();
        for (FluidDevice.FluidPort p : d.fluidPorts()) if (pos == Long.MIN_VALUE || p.pos() < pos) pos = p.pos();
        return pos;
    }

    static String xyz(long pos) {
        if (pos == Long.MIN_VALUE) return "?";
        BlockPos b = BlockPos.of(pos);
        return b.getX() + "," + b.getY() + "," + b.getZ();
    }
}
