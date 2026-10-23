package mods.eln.node.voxel;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sixnode.electricalcable.ElectricalCableElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Six-node cables to voxel cables (notes/cables/PLAN.md step 6; Baughn: generic, first on blocks holding only
 * cables). For each cable element: which of its four edges it is connected through, by EA's own rules (the same
 * masks as connectJob: same-plane neighbour, outer-corner wrap through a wrappable block, inner corner to another
 * element of the block), then arms from the face's centre to those edges (CablePart.arms), and a knuckle in the
 * wrap block for each outer corner. The block becomes a voxel node holding the pieces, with the elements' voltage,
 * temperature and burn state; the six-node NBT is kept in it (VoxelNode.legacy) for a revert.
 * <p>
 * A block is skipped (stays six-node, and keeps working beside voxel cables) if it holds anything but electrical
 * cables of the known tiers, or a wrap goes through a block that isn't air or a voxel node (a torch, a multiblock's
 * ghost block), or a block it needs to look at isn't loaded.
 */
public final class CableConverter {
    private CableConverter() {
    }

    /** What converting a six-node block would do: its pieces, the knuckles to add next door, or why not. */
    public record Plan(List<CablePart> parts, Map<BlockPos, List<CablePart>> knuckles, List<Integer> cableFaces, String skip) {
        static Plan skip(String why) {
            return new Plan(List.of(), Map.of(), List.of(), why);
        }
    }

    public static Plan plan(SixNode s) {
        if (s instanceof VoxelNode) return Plan.skip("already a voxel block");
        Level level = s.coordonate.world();
        BlockPos pos = s.coordonate.pos();
        if (level == null) return Plan.skip("no level");
        for (int dx = -2; dx <= 2; dx += 2)
            for (int dz = -2; dz <= 2; dz += 2)
                if (!level.isLoaded(pos.offset(dx, 0, dz))) return Plan.skip("next to an unloaded chunk");
        List<CablePart> parts = new ArrayList<>();
        Map<BlockPos, List<CablePart>> knuckles = new LinkedHashMap<>();
        List<Integer> cableFaces = new ArrayList<>();
        boolean any = false;
        for (Direction f : Direction.values()) {
            SixNodeElement e = s.getElement(f);
            if (e == null) continue;
            // cables become cable pieces; any other device moves over as it is (hosted), its edges as ports
            boolean cable = (e instanceof ElectricalCableElement || e instanceof mods.eln.sixnode.thermalcable.ThermalCableElement)
                && CablePart.tierOf(e.sixNodeElementDescriptor.name) >= 0;
            SixNodeElement c = cable ? e : null;
            int tier = c == null ? CablePart.tierOf("Low Voltage Cable") : CablePart.tierOf(e.sixNodeElementDescriptor.name);
            if (c != null) cableFaces.add(f.getInt());
            any = true;
            List<Integer> dirs = new ArrayList<>();
            for (Direction d : Direction.values()) {
                if (d == f || d == f.getInverse()) continue;
                LRDU edge = f.getLRDUGoingTo(d);
                boolean linked = false;
                // inner corner: another element of this block on face d
                SixNodeElement o = s.getElement(d);
                if (o != null && NodeBase.compareConnectionMask(e.getConnectionMask(edge), o.getConnectionMask(d.getLRDUGoingTo(f))))
                    linked = true;
                // same plane: the node next door toward d
                LRDU lrdu = d.getLRDUGoingTo(f);
                NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.relative(d.toEnumFacing()), level));
                if (n != null && NodeBase.compareConnectionMask(s.getSideConnectionMask(d, lrdu), n.getSideConnectionMask(d.getInverse(), lrdu.inverseIfLR())))
                    linked = true;
                // outer corner: round the edge, through the block toward d (air, or a voxel node holding another
                // corner's knuckle), onto face -f of the block beyond
                if (!linked && (n == null ? s.isBlockWrappable(d) : n instanceof VoxelNode)) {
                    BlockPos w = pos.relative(d.toEnumFacing());
                    NodeBase p = NodeManager.instance.getNodeFromCoordonate(new Coordonate(w.relative(f.toEnumFacing()), level));
                    Direction od = f.getInverse();
                    if (p != null && NodeBase.compareConnectionMask(s.getSideConnectionMask(d, lrdu),
                        p.getSideConnectionMask(od, od.getLRDUGoingTo(d).inverse()))) {
                        var ws = level.getBlockState(w);
                        if (!ws.isAir() && !(ws.getBlock() instanceof VoxelNodeBlock))
                            return Plan.skip("an outer corner goes through " + ws.getBlock().getName().getString());
                        int kt = tier;
                        if (c == null) {   // (a device's corner: a signal or power knuckle by its edge; no thermal knuckles)
                            int m = e.getConnectionMask(edge);
                            if ((m & (NodeBase.maskElectricalPower | NodeBase.maskElectricalGate)) == 0)
                                return Plan.skip("a non-electrical outer corner (" + e.getClass().getSimpleName() + ")");
                            kt = (m & NodeBase.maskElectricalPower) == 0 ? CablePart.tierOf("Signal Cable") : CablePart.tierOf("Low Voltage Cable");
                        }
                        int[] k = knucklePoint(d, f);
                        CablePart kn = CablePart.knuckle(kt, k[0], k[1], k[2]);
                        kn.byConverter = true;
                        knuckles.computeIfAbsent(w, x -> new ArrayList<>()).add(kn);   // (a block may hold several corners)
                        linked = true;
                    }
                }
                if (linked) dirs.add(d.toEnumFacing().get3DDataValue());
            }
            if (c == null) continue;   // (a device: knuckles for its outer corners only)
            int face = f.toEnumFacing().get3DDataValue();
            List<CablePart> arms = dirs.isEmpty() ? CablePart.arms(tier, face, otherPair(face)) : CablePart.arms(tier, face, dirs.stream().mapToInt(i -> i).toArray());
            double u = c instanceof ElectricalCableElement ec ? ec.electricalLoad.state : 0;
            double t = c instanceof ElectricalCableElement ec ? ec.thermalTc() : ((mods.eln.sixnode.thermalcable.ThermalCableElement) c).thermalTc();
            for (int i = 0; i < arms.size(); i++) {
                CablePart a = arms.get(i);
                a.paid = 16 / arms.size() + (i == 0 ? 16 % arms.size() : 0);   // (the one cable item it was)
                a.u = u;
                a.t = t;
                a.burnt = c.getBurnt();
            }
            parts.addAll(arms);
        }
        if (!any) return Plan.skip("empty");
        return new Plan(parts, knuckles, cableFaces, null);
    }

    /** A knuckle's point in the wrap block: on the side toward the cable's block (-d), and toward f. */
    static int[] knucklePoint(Direction d, Direction f) {
        int[] k = {8, 8, 8};
        int dv = d.toEnumFacing().get3DDataValue(), fv = f.toEnumFacing().get3DDataValue();
        k[CablePart.axis(dv)] = dv % 2 == 0 ? 16 : 0;
        k[CablePart.axis(fv)] = fv % 2 == 0 ? 0 : 16;
        return k;
    }

    /** An unconnected cable: a straight piece across the face. */
    private static int[] otherPair(int face) {
        return switch (face / 2) {
            case 0 -> new int[]{4, 5};
            case 1 -> new int[]{4, 5};
            default -> new int[]{2, 3};
        };
    }

    /**
     * Convert the six-node block: returns null if done, else why not. The node leaves the simulation and the node
     * list before its block is replaced (the old block entity would otherwise break the new node: Gemini).
     */
    public static String convert(SixNode s) {
        Plan p = plan(s);
        if (p.skip != null) return p.skip;
        Level level = s.coordonate.world();
        BlockPos pos = s.coordonate.pos();
        boolean wet = level.getBlockState(pos).getOptionalValue(mods.eln.node.six.SixNodeBlock.WATERLOGGED).orElse(false);
        CompoundTag legacy = new CompoundTag();
        s.writeToNBT(legacy);
        // the voxel node is built from the six-node NBT without the cables (the devices move over as they are)
        CompoundTag host = legacy.copy();
        for (int f : p.cableFaces) {
            host.putShort("EID" + f, (short) 0);
            host.remove("ED" + f);
        }
        s.disconnect();
        s.unload();   // (wireless channels, lamp supplies: the hosted copies register again in globalBoot)
        NodeManager.instance.removeNode(s);
        level.setBlock(pos, ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState().setValue(VoxelNodeBlock.WATERLOGGED, wet), 3);
        VoxelNode v = new VoxelNode();
        v.readFromNBT(host);
        NodeManager.instance.addNode(v);
        v.neighborBlockRead();
        v.initializeFromNBT();
        v.globalBoot();
        v.setNeedPublish(true);
        v.legacy = legacy;
        for (CablePart part : p.parts) {
            String err = v.addPart(part);
            if (err != null) mods.eln.misc.Utils.println("CableConverter: " + pos + ": " + err);
        }
        for (var k : p.knuckles.entrySet())
            for (CablePart kn : k.getValue()) {
                if (NodeManager.instance.getNodeFromCoordonate(new Coordonate(k.getKey(), level)) instanceof VoxelNode w
                    && w.parts.stream().anyMatch(q -> q instanceof CablePart c && c.isKnuckle() && java.util.Arrays.equals(c.points, kn.points)))
                    continue;   // (laid by the block round the corner, converted first)
                String err = PipeKitItem.place(level, k.getKey(), kn, null);
                if (err != null) mods.eln.misc.Utils.println("CableConverter: knuckle at " + k.getKey() + ": " + err);
            }
        return null;
    }

    /**
     * Undo a conversion: the voxel node goes, the six-node block comes back from the NBT kept in it (its state as at
     * the conversion). Returns null if done, else why not. Knuckles: see revertKnuckles.
     */
    public static String revert(VoxelNode v) {
        if (v.legacy == null) return "not converted";
        Level level = v.coordonate.world();
        BlockPos pos = v.coordonate.pos();
        // the devices as they are now; the cables as they were (from the six-node NBT kept at the conversion)
        CompoundTag nbt = new CompoundTag();
        v.writeToNBT(nbt);
        for (int f = 0; f < 6; f++)
            if (nbt.getShortOr("EID" + f, (short) 0) == 0 && v.legacy.getShortOr("EID" + f, (short) 0) != 0) {
                nbt.putShort("EID" + f, v.legacy.getShortOr("EID" + f, (short) 0));
                nbt.put("ED" + f, v.legacy.getCompoundOrEmpty("ED" + f));
            }
        v.disconnect();
        v.unload();
        NodeManager.instance.removeNode(v);
        boolean wet = level.getBlockState(pos).getOptionalValue(VoxelNodeBlock.WATERLOGGED).orElse(false);
        level.setBlock(pos, mods.eln.Eln.sixNodeBlock.defaultBlockState().setValue(mods.eln.node.six.SixNodeBlock.WATERLOGGED, wet), 3);
        SixNode s = new SixNode();
        s.readFromNBT(nbt);
        NodeManager.instance.addNode(s);
        s.initializeFromNBT();
        s.globalBoot();
        s.setNeedPublish(true);
        return null;
    }

    /** Take out the knuckles the converter laid in this node; the block goes if nothing is left. */
    public static int revertKnuckles(VoxelNode v) {
        int n = 0;
        for (int i = v.parts.size() - 1; i >= 0; i--)
            if (v.parts.get(i) instanceof CablePart c && c.byConverter) {
                v.removePart(i, false);
                n++;
            }
        if (n > 0 && v.isEmptyNode()) {
            Level level = v.coordonate.world();
            BlockPos pos = v.coordonate.pos();
            v.onBreakBlock();
            level.removeBlock(pos, false);
        }
        return n;
    }

    // ---- automatic conversion as chunks load (ElnConfig [cables] convertOnLoad)

    public static volatile boolean convertOnLoad = false;
    /** At most this many blocks a tick (the rest wait for the next). */
    static final int PER_TICK = 2000;
    private static final Map<net.minecraft.server.level.ServerLevel, java.util.Set<Long>> PENDING = new java.util.WeakHashMap<>();
    public static long converted, kept;

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.level.ChunkEvent.Load.class, e -> {
            if (!convertOnLoad || !(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
            var cp = e.getChunk().getPos();
            // (and its neighbours: a block at an edge waited for this chunk to load)
            var set = PENDING.computeIfAbsent(level, l -> new java.util.HashSet<>());
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) set.add(net.minecraft.world.level.ChunkPos.pack(cp.x() + dx, cp.z() + dz));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class, e -> {
            if (PENDING.isEmpty()) return;
            for (var en : new ArrayList<>(PENDING.entrySet())) {
                var level = en.getKey();
                var chunks = en.getValue();
                PENDING.remove(level);
                int done = 0;
                java.util.Set<Long> later = new java.util.HashSet<>();
                for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
                    if (!(n instanceof SixNode s) || n instanceof VoxelNode || n.coordonate.world() != level) continue;
                    long cp = net.minecraft.world.level.ChunkPos.pack(n.coordonate.x >> 4, n.coordonate.z >> 4);
                    if (!chunks.contains(cp)) continue;
                    if (done >= PER_TICK) {
                        later.add(cp);
                        continue;
                    }
                    if (!level.isLoaded(n.coordonate.pos())) continue;
                    String why = convert(s);
                    if (why == null) {
                        converted++;
                        done++;
                    } else kept++;
                }
                if (!later.isEmpty()) PENDING.computeIfAbsent(level, l -> new java.util.HashSet<>()).addAll(later);
                if (done > 0) mods.eln.Eln.LOG.info("Cables converted to voxel cables: " + done + " block(s) (" + converted + " so far)");
            }
        });
    }
}
