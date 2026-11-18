package mods.eln.gametest;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.voxel.VoxelNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routing phase 3b's neutrality check: the voxel blocks' links (electrical, thermal, bus) and connection flags (what
 * clients draw: lrduCubeMask, lrduElementMask, off-centre devices' slotMask) as a sorted list of lines, so the same
 * blocks connected with `[cables] linksBuild` off (the rules make the links) and on (the joints do) can be compared.
 * Conductors are named by their block and piece ids (their load objects are new at every connect); anything else by
 * identity (devices keep their loads across a reconnect).
 */
public final class LinksDiff {
    private LinksDiff() {
    }

    /** The canonical lines of these blocks (and the counts: parallel links count). */
    public static List<String> canonical(Collection<VoxelNode> nodes) {
        Map<Object, String> names = new IdentityHashMap<>();
        for (VoxelNode n : nodes) names.putAll(n.conductorNames());
        List<String> out = new ArrayList<>();
        for (VoxelNode n : nodes) {
            String at = n.coordonate.x + "," + n.coordonate.y + "," + n.coordonate.z;
            n.liveLinksForDiff(l -> {
                String a = name(l[1], names), b = name(l[2], names);
                out.add(l[0] + " " + (a.compareTo(b) < 0 ? a + " -- " + b : b + " -- " + a) + " (" + l[3] + " of " + at + ")");
            });
            StringBuilder m = new StringBuilder("masks " + at + ":");
            for (Direction d : Direction.values())
                for (LRDU lr : LRDU.values()) {
                    if (n.lrduCubeMask.get(d, lr)) m.append(" cube ").append(d).append('/').append(lr);
                    if (n.lrduElementMask.get(d, lr)) m.append(" el ").append(d).append('/').append(lr);
                }
            for (int i = 6; i < mods.eln.node.six.SixNode.SLOTS; i++) {
                var e = n.sideElementList[i];
                if (e == null) continue;
                for (LRDU lr : LRDU.values()) if (e.slotMask.get(lr)) m.append(" slot ").append(i).append('/').append(lr);
            }
            out.add(m.toString());
        }
        java.util.Collections.sort(out);
        return out;
    }

    static String name(Object o, Map<Object, String> names) {
        if (o instanceof String str) return str;
        String s = names.get(o);
        return s != null ? s : o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /** Lines in one list and not the other (multisets), at most `max` of each. */
    public static String difference(List<String> a, List<String> b, int max) {
        Map<String, Integer> count = new java.util.HashMap<>();
        for (String s : a) count.merge(s, 1, Integer::sum);
        for (String s : b) count.merge(s, -1, Integer::sum);
        StringBuilder only = new StringBuilder();
        int shown = 0;
        for (var e : new java.util.TreeMap<>(count).entrySet()) {
            if (e.getValue() == 0) continue;
            if (shown++ >= max) break;
            only.append(e.getValue() > 0 ? "off only x" + e.getValue() + ": " : "on only x" + (-e.getValue()) + ": ").append(e.getKey()).append("; ");
        }
        return only.toString();
    }

    /** Reconnect every one of these blocks (in order) with linksBuild set to `on`, and return the canonical lines. */
    public static List<String> connectedWith(Collection<VoxelNode> nodes, boolean on) {
        boolean was = VoxelNode.linksBuild;
        try {
            VoxelNode.linksBuild = on;
            for (VoxelNode n : nodes) if (n.isAdded()) n.reconnect();
        } finally {
            VoxelNode.linksBuild = was;
        }
        return canonical(nodes);
    }

    /** Keep only blocks that are in the simulation (frozen ones have no links to compare). */
    public static List<VoxelNode> added(Collection<? extends NodeBase> nodes) {
        List<VoxelNode> out = new ArrayList<>();
        for (NodeBase n : nodes) if (n instanceof VoxelNode v && v.isAdded() && !v.isDestructing()) out.add(v);
        return out;
    }
}
