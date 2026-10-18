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
    public record Plan(List<CablePart> parts, Map<BlockPos, CablePart> knuckles, String skip) {
        static Plan skip(String why) {
            return new Plan(List.of(), Map.of(), why);
        }
    }

    public static Plan plan(SixNode s) {
        Level level = s.coordonate.world();
        BlockPos pos = s.coordonate.pos();
        if (level == null) return Plan.skip("no level");
        for (int dx = -2; dx <= 2; dx += 2)
            for (int dz = -2; dz <= 2; dz += 2)
                if (!level.isLoaded(pos.offset(dx, 0, dz))) return Plan.skip("next to an unloaded chunk");
        List<CablePart> parts = new ArrayList<>();
        Map<BlockPos, CablePart> knuckles = new LinkedHashMap<>();
        boolean any = false;
        for (Direction f : Direction.values()) {
            SixNodeElement e = s.getElement(f);
            if (e == null) continue;
            if (!(e instanceof ElectricalCableElement c)) return Plan.skip("holds a " + e.getClass().getSimpleName());
            int tier = CablePart.tierOf(c.descriptor.name);
            if (tier < 0) return Plan.skip("holds a " + c.descriptor.name);
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
                if (n != null) {
                    if (NodeBase.compareConnectionMask(s.getSideConnectionMask(d, lrdu), n.getSideConnectionMask(d.getInverse(), lrdu.inverseIfLR())))
                        linked = true;
                } else if (s.isBlockWrappable(d)) {
                    // outer corner: round the edge, through the block toward d, onto face -f of the block beyond
                    BlockPos w = pos.relative(d.toEnumFacing());
                    NodeBase p = NodeManager.instance.getNodeFromCoordonate(new Coordonate(w.relative(f.toEnumFacing()), level));
                    Direction od = f.getInverse();
                    if (p != null && NodeBase.compareConnectionMask(s.getSideConnectionMask(d, lrdu),
                        p.getSideConnectionMask(od, od.getLRDUGoingTo(d).inverse()))) {
                        var ws = level.getBlockState(w);
                        if (!ws.isAir() && ws.getBlock() != ElnRegistry.VOXEL_NODE_BLOCK.get())
                            return Plan.skip("an outer corner goes through " + ws.getBlock().getName().getString());
                        int[] k = knucklePoint(d, f);
                        knuckles.put(w, CablePart.knuckle(tier, k[0], k[1], k[2]));
                        linked = true;
                    }
                }
                if (linked) dirs.add(d.toEnumFacing().get3DDataValue());
            }
            int face = f.toEnumFacing().get3DDataValue();
            List<CablePart> arms = dirs.isEmpty() ? CablePart.arms(tier, face, otherPair(face)) : CablePart.arms(tier, face, dirs.stream().mapToInt(i -> i).toArray());
            double u = c.electricalLoad.state, t = c.thermalTc();
            for (CablePart a : arms) {
                a.u = u;
                a.t = t;
                a.burnt = c.getBurnt();
            }
            parts.addAll(arms);
        }
        if (!any) return Plan.skip("empty");
        return new Plan(parts, knuckles, null);
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
        CompoundTag legacy = new CompoundTag();
        s.writeToNBT(legacy);
        s.disconnect();
        NodeManager.instance.removeNode(s);
        level.setBlock(pos, ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
        VoxelNode v = new VoxelNode();
        v.onBlockPlacedBy(new Coordonate(pos, level), Direction.YN, null, ItemStack.EMPTY);
        v.legacy = legacy;
        for (CablePart part : p.parts) {
            String err = v.addPart(part);
            if (err != null) mods.eln.misc.Utils.println("CableConverter: " + pos + ": " + err);
        }
        for (var k : p.knuckles.entrySet()) {
            if (NodeManager.instance.getNodeFromCoordonate(new Coordonate(k.getKey(), level)) instanceof VoxelNode w
                && w.parts.stream().anyMatch(q -> q instanceof CablePart c && c.isKnuckle() && java.util.Arrays.equals(c.points, k.getValue().points)))
                continue;   // (laid by the block round the corner, converted first)
            String err = PipeKitItem.place(level, k.getKey(), k.getValue(), null);
            if (err != null) mods.eln.misc.Utils.println("CableConverter: knuckle at " + k.getKey() + ": " + err);
        }
        return null;
    }
}
