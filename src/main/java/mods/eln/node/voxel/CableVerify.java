package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Do the cable pieces' stored joints say what the live network has? Over a set of voxel blocks (the scope):
 * <ul>
 * <li><b>Groups:</b> in each block, the pieces joined by same-block CONT and SPLICE joints (same cable, same face, no
 * knuckles) must be exactly its live conductors.</li>
 * <li><b>Edges:</b> the set of links between a conductor of the scope and anything else (another conductor, a device's
 * load, a fitting's signal), per layer (electrical, thermal, bus), taken from the blocks' own lists (CableNet's
 * internal links, element links, node connections, bus links), must equal the set the joints give.</li>
 * </ul>
 * Burnt pieces and blocks out of the simulation (frozen, unloaded) are skipped and counted. A conductor pair joined by
 * more than one joint is listed as parallel (informational: each joint would be its own link once joints build the
 * network).
 */
public final class CableVerify {
    private CableVerify() {
    }

    /** The outcome: counts, and one line per problem (capped in text by the caller). */
    public record Report(int blocks, int pieces, int burnt, int outside, int liveEdges, int jointEdges, int[] counts,
                         List<String> problems, List<String> parallel, int excludedBlocks, int excludedPieces, int unrecorded,
                         int unjoinedToAbsent) {
        public boolean ok() {
            return problems.isEmpty();
        }

        public String summary() {
            StringBuilder b = new StringBuilder();
            b.append(blocks).append(" block(s), ").append(pieces).append(" piece(s) checked (").append(excludedBlocks)
                .append(" block(s) with ").append(excludedPieces).append(" piece(s) out of the simulation not checked; ")
                .append(unrecorded).append(" block(s) never connected under stored links; ").append(unjoinedToAbsent)
                .append(" end(s) meeting a block out of the simulation with no joint; ").append(burnt).append(" burnt, ")
                .append(outside).append(" joint(s) to blocks out of the simulation skipped); ").append(liveEdges).append(" live link(s), ")
                .append(jointEdges).append(" from joints; ").append(problems.size()).append(" problem(s), ").append(parallel.size())
                .append(" parallel pair(s). Joints:");
            for (int k = 1; k < 8; k++)
                if (counts[k] + counts[k + 8] > 0)
                    b.append(' ').append(k < CableLinks.NAMES.length ? CableLinks.NAMES[k] : "kind" + k).append(' ').append(counts[k] + counts[k + 8])
                        .append(" (").append(counts[k + 8]).append(" implied)");
            return b.toString();
        }
    }

    public static Report verify(Collection<VoxelNode> scope) {
        List<VoxelNode> nodes = new ArrayList<>();
        int excludedBlocks = 0, excludedPieces = 0, unrecorded = 0, unjoined = 0;
        for (VoxelNode n : scope) {
            if (n.isDestructing()) continue;
            if (!n.linksRecorded) unrecorded++;
            if (n.isAdded()) {
                nodes.add(n);
                continue;
            }
            excludedBlocks++;
            for (VoxelPart p : n.parts) if (p instanceof CablePart) excludedPieces++;
        }
        Set<VoxelNode> in = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        in.addAll(nodes);
        // the blocks looked at: the scope and its voxel neighbours (joints and links across the scope's edge)
        Set<VoxelNode> near = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (VoxelNode n : nodes) {
            near.add(n);
            for (int f = 0; f < 6; f++) {
                int[] d = CableRouter.DV[f];
                VoxelNode v = n.at(d[0], d[1], d[2]);
                if (v != null && v.isAdded()) near.add(v);
            }
        }
        // what each live conductor's loads are, and which are the scope's
        Map<Object, String> label = new IdentityHashMap<>();
        Set<Object> scoped = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (VoxelNode n : near)
            for (CableNet.Conductor k : n.conductors()) {
                if (!k.valid() || k.isBurntNow()) continue;
                String name = n.coordonate.x + "," + n.coordonate.y + "," + n.coordonate.z + " " + describe(k);
                for (Object o : new Object[]{k.load, k.thermal, k.port}) {
                    if (o == null) continue;
                    label.put(o, name);
                    if (in.contains(n)) scoped.add(o);
                }
            }
        Map<Object, Integer> ids = new IdentityHashMap<>();
        // live links touching a scope conductor
        Map<String, String> live = new TreeMap<>();
        for (VoxelNode n : near)
            n.liveLinks(l -> {
                if (l[1] == null || l[2] == null || l[1] == l[2] || !scoped.contains(l[1]) && !scoped.contains(l[2])) return;
                live.put(key((char) l[0], l[1], l[2], ids), text((char) l[0], l[1], l[2], label));
            });
        // links from joints
        List<String> problems = new ArrayList<>();
        Map<String, String> fromJoints = new TreeMap<>();
        Map<String, Set<String>> jointsPerEdge = new HashMap<>();
        int pieces = 0, burnt = 0, outside = 0;
        List<CablePart> all = new ArrayList<>();
        for (VoxelNode n : near) {
            boolean mine = in.contains(n);
            for (VoxelPart p : n.parts) {
                if (!(p instanceof CablePart c)) continue;
                if (mine) {
                    pieces++;
                    all.add(c);
                }
                if (c.burnt != null) {
                    if (mine) burnt++;
                    continue;
                }
                CableNet.Conductor k = n.conductorOf(c);
                if (k == null || !k.valid() || k.isBurntNow()) continue;
                for (int[] j : c.joints) {
                    String where = n.coordonate.x + "," + n.coordonate.y + "," + n.coordonate.z + " piece " + c.id + " " + CableLinks.describe(j);
                    Object[] other;
                    String jkey;
                    int kind = CableLinks.kind(j);
                    if (kind == CableLinks.CONT || kind == CableLinks.SPLICE || kind == CableLinks.TOUCH) {
                        VoxelNode t = n.at(j[2], j[3], j[4]);
                        if (t == null || !t.isAdded()) {
                            if (mine) outside++;
                            continue;
                        }
                        if (!(t.partById(j[5]) instanceof CablePart tc)) {
                            if (mine) problems.add(where + ": no such piece");
                            continue;
                        }
                        if (tc.burnt != null) continue;
                        CableNet.Conductor tk = t.conductorOf(tc);
                        if (tk == null || !tk.valid() || tk.isBurntNow()) continue;
                        if (tk == k) continue;   // (a joint inside a conductor: the group check)
                        other = new Object[]{tk.load, tk.thermal, tk.port};
                        String a = ref(n, c, kind == CableLinks.TOUCH ? -1 : j[1]), b = ref(t, tc, kind == CableLinks.CONT ? j[6] : -1);
                        jkey = kind == CableLinks.SPLICE ? a + ">" + b : a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                    } else {
                        other = n.terminal(j);
                        if (other == null) {
                            if (mine) problems.add(where + ": its device is gone");
                            continue;
                        }
                        jkey = ref(n, c, j[1]) + ">" + java.util.Arrays.toString(j);
                    }
                    Object[] own = {k.load, k.thermal, k.port};
                    char[] layers = {'E', 'T', 'B'};
                    for (int i = 0; i < 3; i++) {
                        if (own[i] == null || other[i] == null || own[i] == other[i]) continue;
                        if (!scoped.contains(own[i]) && !scoped.contains(other[i])) continue;
                        String ek = key(layers[i], own[i], other[i], ids);
                        fromJoints.put(ek, text(layers[i], own[i], other[i], label) + "  (" + where + ")");
                        jointsPerEdge.computeIfAbsent(ek, x -> new HashSet<>()).add(jkey);
                    }
                }
            }
        }
        for (var e : live.entrySet()) if (!fromJoints.containsKey(e.getKey())) problems.add("live link with no joint: " + e.getValue());
        for (var e : fromJoints.entrySet()) if (!live.containsKey(e.getKey())) problems.add("joint with no live link: " + e.getValue());
        List<String> parallel = new ArrayList<>();
        for (var e : jointsPerEdge.entrySet())
            if (e.getValue().size() > 1 && e.getKey().startsWith("E")) parallel.add(fromJoints.get(e.getKey()) + " x" + e.getValue().size());
        // groups
        for (VoxelNode n : nodes) groups(n, problems);
        // ends meeting an end in a block out of the simulation (frozen, unloaded) that no joint records: links that were
        // never seen with both blocks connected
        for (VoxelNode n : nodes)
            for (int f = 0; f < 6; f++) {
                int[] d = CableRouter.DV[f];
                VoxelNode v = n.at(d[0], d[1], d[2]);
                if (v == null || v.isAdded()) continue;
                for (VoxelPart p : n.parts) {
                    if (!(p instanceof CablePart c)) continue;
                    for (CablePart.Port port : c.cablePorts()) {
                        if (port.face() != f) continue;
                        CablePart.Port m = port.mirrored();
                        boolean meets = false, joined = false;
                        for (VoxelPart q : v.parts)
                            if (q instanceof CablePart o && o.cablePorts().contains(m)) {
                                meets = true;
                                int e = CableLinks.endOf(c, port);
                                for (int[] j : c.joints)
                                    joined |= CableLinks.kind(j) == CableLinks.CONT && j[1] == e && j[2] == d[0] && j[3] == d[1] && j[4] == d[2] && j[5] == o.id;
                            }
                        if (meets && !joined) unjoined++;
                    }
                }
            }
        return new Report(nodes.size(), pieces, burnt, outside, live.size(), fromJoints.size(), CableLinks.counts(all), problems, parallel,
            excludedBlocks, excludedPieces, unrecorded, unjoined);
    }

    /** The same-block joints of n's pieces must group them exactly as its live conductors. */
    private static void groups(VoxelNode n, List<String> problems) {
        List<CablePart> cs = new ArrayList<>();
        for (VoxelPart p : n.parts) if (p instanceof CablePart c && c.burnt == null) cs.add(c);
        Map<CablePart, Integer> idx = new IdentityHashMap<>();
        for (int i = 0; i < cs.size(); i++) idx.put(cs.get(i), i);
        int[] root = new int[cs.size()];
        for (int i = 0; i < root.length; i++) root[i] = i;
        for (CablePart c : cs) {
            if (c.isKnuckle()) continue;
            for (int[] j : c.joints) {
                int kind = CableLinks.kind(j);
                if ((kind != CableLinks.CONT && kind != CableLinks.SPLICE) || j[2] != 0 || j[3] != 0 || j[4] != 0) continue;
                if (!(n.partById(j[5]) instanceof CablePart t) || !idx.containsKey(t) || t.isKnuckle() || !t.sameCable(c) || t.face != c.face) continue;
                int a = find(root, idx.get(c)), b = find(root, idx.get(t));
                if (a != b) root[a] = b;
            }
        }
        for (int i = 0; i < cs.size(); i++)
            for (int j = i + 1; j < cs.size(); j++) {
                CableNet.Conductor ki = n.conductorOf(cs.get(i)), kj = n.conductorOf(cs.get(j));
                boolean liveSame = ki != null && ki == kj;
                boolean jointSame = find(root, i) == find(root, j);
                if (liveSame != jointSame)
                    problems.add(n.coordonate.x + "," + n.coordonate.y + "," + n.coordonate.z + " pieces " + cs.get(i).id + " and " + cs.get(j).id
                        + (liveSame ? ": one conductor, but no joints join them" : ": joints join them, but they are separate conductors"));
            }
    }

    private static int find(int[] root, int i) {
        while (root[i] != i) i = root[i] = root[root[i]];
        return i;
    }

    private static String ref(VoxelNode n, CablePart c, int end) {
        return n.coordonate.x + "," + n.coordonate.y + "," + n.coordonate.z + "#" + c.id + ":" + end;
    }

    private static String describe(CableNet.Conductor k) {
        StringBuilder b = new StringBuilder("conductor[");
        for (int i = 0; i < k.parts.size(); i++) b.append(i == 0 ? "" : ",").append(k.parts.get(i).id);
        return b.append(']').toString();
    }

    private static String key(char layer, Object a, Object b, Map<Object, Integer> ids) {
        int x = ids.computeIfAbsent(a, o -> ids.size()), y = ids.computeIfAbsent(b, o -> ids.size());
        return layer + ":" + Math.min(x, y) + "-" + Math.max(x, y);
    }

    private static String text(char layer, Object a, Object b, Map<Object, String> label) {
        return layer + " " + label.getOrDefault(a, a.getClass().getSimpleName()) + " -- " + label.getOrDefault(b, b.getClass().getSimpleName());
    }
}
