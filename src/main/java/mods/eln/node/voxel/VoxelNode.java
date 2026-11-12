package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.fluid.FluidWorld;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A block that tracks which of its 16x16x16 voxels are used (notes/pipes/DESIGN.md section 2): a list of parts (pipes
 * now), none overlapping. One per block (NodeManager), uuid "v". Its pipes are fluid devices: their ends on the block's
 * faces couple with whatever is on the other side at the same spot (FluidWorld). No electrical or thermal side yet.
 */
public class VoxelNode extends mods.eln.node.six.SixNode implements FluidDevice {
    public static final String UUID = "v";

    public final List<VoxelPart> parts = new ArrayList<>();
    private final BitSet used = new BitSet(4096);
    /** The six-node block this was converted from (its NBT), kept for a revert; null if built as a voxel node. */
    public CompoundTag legacy;
    /** The decorative block this one imitates (a stair a cable runs over...: Mimic), or null. */
    public net.minecraft.world.level.block.state.BlockState host;
    /** The imitated block's voxels carved away (the chisel), or null. */
    public BitSet carved;

    /** The voxels of the parts and hosted elements (not the imitated block). */
    public BitSet partsVoxels() {
        BitSet s = new BitSet(4096);
        for (VoxelPart p : parts) for (int[] b : p.boxes()) fill(s, b);
        for (int f = 0; f < 6; f++) if (sideElementList[f] != null) fill(s, plate(f));
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] != null) for (int[] b : elementBoxes(i)) fill(s, b);
        return s;
    }

    /** What's left of the imitated block, as boxes (none without one). */
    public List<int[]> hostBoxes() {
        if (host == null || coordonate.world() == null) return List.of();
        List<int[]> boxes = Mimic.boxes(host, coordonate.world(), coordonate.pos());
        if (carved == null || carved.isEmpty()) return boxes;
        BitSet s = Mimic.voxels(boxes);
        s.andNot(carved);
        return Mimic.boxes(s);
    }

    /** The cables' conductors and their loads (server). */
    final CableNet cables = new CableNet(this);

    @Override
    public String getNodeUuid() {
        return UUID;
    }

    static int voxel(int x, int y, int z) {
        return (x * 16 + y) * 16 + z;
    }

    /** Bumped whenever the parts, sides, host or carving may have changed: VoxelNodeEntity's shape cache. */
    volatile int stamp;

    private void markUsed() {
        stamp++;
        used.clear();
        for (VoxelPart p : parts) for (int[] b : p.boxes()) fill(used, b);
        for (int f = 0; f < 6; f++) if (sideElementList[f] != null || isSideKept(f)) fill(used, plate(f));
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] != null) for (int[] b : elementBoxes(i)) fill(used, b);
        for (int[] b : hostBoxes()) fill(used, b);
    }

    /** The imitated block was mined: it drops, the parts stay (or the block goes if nothing is left). */
    public void clearHost(net.minecraft.world.entity.player.Player player) {
        if (host == null) return;
        var level = coordonate.world();
        var pos = coordonate.pos();
        if (level instanceof net.minecraft.server.level.ServerLevel sl && (player == null || !player.isCreative()))
            net.minecraft.world.level.block.Block.dropResources(host, sl, pos, null, player, player == null ? ItemStack.EMPTY : player.getMainHandItem());
        host = null;
        carved = null;
        markUsed();
        setNeedPublish(true);
        if (isEmptyNode()) removeEmptyBlock();
    }

    /** Nothing of ours is left: the block becomes what it imitated again, or air. */
    public void removeEmptyBlock() {
        var level = coordonate.world();
        var pos = coordonate.pos();
        var h = host;
        onBreakBlock();
        if (h != null) level.setBlock(pos, h, 3);
        else level.removeBlock(pos, false);
    }

    /**
     * The voxels a six-node element on face f (EA Direction int) takes: a plate 3 voxels thick on that face, inset 2
     * from its edges, so the edge midpoints (its ports) and cables round the inner corners stay free.
     */
    public static int[] plate(int f) {
        int face = Direction.fromInt(f).toEnumFacing().get3DDataValue();
        int n = CablePart.normalAxis(face);
        int[] b = {2, 2, 2, 14, 14, 14};
        b[n] = CablePart.plane(face) == 0 ? 0 : 13;
        b[n + 3] = CablePart.plane(face) == 0 ? 3 : 16;
        return b;
    }

    /** The voxels the model of the element on face f (EA Direction int) takes: where it is clicked and outlined. Its
     *  room (what else may go in the block) is still its plate. */
    public int[] elementBox(int slot) {
        var e = sideElementList[slot];
        if (e == null || e.sixNodeElementDescriptor == null) return slot < 6 ? plate(slot) : new int[6];
        return slotBox(e.sixNodeElementDescriptor, e.side.getInt(), e.ox, e.oy, e.oz);
    }

    /** The boxes of the element in a slot: on a face, its model box; off centre, its body and its pins' boxes. */
    public List<int[]> elementBoxes(int slot) {
        var e = sideElementList[slot];
        if (slot < 6 || e == null || e.sixNodeElementDescriptor == null) return List.of(elementBox(slot));
        List<int[]> out = new ArrayList<>();
        out.add(bodyBox(e.sixNodeElementDescriptor, e.side, e.front, e.ox, e.oy, e.oz));
        for (Pin p : pins(e)) out.add(p.box());
        return out;
    }

    /** The body of a device off centre (its model without the pins: SixNodeDescriptor.bodyExtents), clipped to the block. */
    public static int[] bodyBox(mods.eln.node.six.SixNodeDescriptor d, Direction side, LRDU front, int ox, int oy, int oz) {
        int[] r = rawBodyBox(d, side, front, ox, oy, oz);
        for (int k = 0; k < 6; k++) r[k] = Math.max(0, Math.min(16, r[k]));
        return r;
    }

    static int[] rawBodyBox(mods.eln.node.six.SixNodeDescriptor d, Direction side, LRDU front, int ox, int oy, int oz) {
        int face = side.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(face);
        int[] ext = d.bodyExtents(side, front);
        int[] c = {8 + ox, 8 + oy, 8 + oz};
        int[] r = {c[0], c[1], c[2], c[0], c[1], c[2]};
        for (LRDU l : LRDU.values()) {
            int dv = side.applyLRDU(l).toEnumFacing().get3DDataValue(), a = CablePart.normalAxis(dv);
            if (dv % 2 == 1) r[a + 3] = c[a] + ext[l.toInt()];
            else r[a] = c[a] - ext[l.toInt()];
        }
        int h = d.footprint()[1];
        r[n] = CablePart.plane(face) == 0 ? 0 : 16 - h;
        r[n + 3] = CablePart.plane(face) == 0 ? h : 16;
        return r;
    }

    /** The pin boxes of a device off centre on face f: one per edge `has` (client: the published pin mask). */
    public static List<int[]> pinBoxes(mods.eln.node.six.SixNodeDescriptor d, Direction side, LRDU front, int ox, int oy, int oz, java.util.function.Predicate<LRDU> has) {
        List<int[]> out = new ArrayList<>();
        for (LRDU l : LRDU.values()) if (has.test(l)) out.add(pinGeometry(d, side, front, ox, oy, oz, l)[1]);
        return out;
    }

    /** {tip (x, y, z), box, contact} of a pin toward edge l of a device off centre (block voxels): one voxel past its body. */
    static int[][] pinGeometry(mods.eln.node.six.SixNodeDescriptor d, Direction side, LRDU front, int ox, int oy, int oz, LRDU l) {
        int af = side.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(af), bh = d.bodyExtents(side, front)[l.toInt()];
        int[] c = {8 + ox, 8 + oy, 8 + oz};
        c[n] = CablePart.plane(af);
        int dv = side.applyLRDU(l).toEnumFacing().get3DDataValue(), a = CablePart.normalAxis(dv), b = 3 - n - a;
        boolean pos = dv % 2 == 1;
        int[] tip = c.clone();
        tip[a] += pos ? bh + 1 : -bh - 1;
        int[] box = new int[6], contact = new int[6];
        box[a] = pos ? c[a] + bh : c[a] - bh - 1;
        box[a + 3] = box[a] + 1;
        contact[a] = pos ? tip[a] : tip[a] - 1;
        contact[a + 3] = contact[a] + 1;
        for (int[] x : new int[][]{box, contact}) {
            x[b] = c[b] - 1;
            x[b + 3] = c[b] + 1;
            x[n] = CablePart.plane(af) == 0 ? 0 : 15;
            x[n + 3] = x[n] + 1;
        }
        return new int[][]{tip, box, contact};
    }

    /** A device's model box on face f (EA Direction int), its centre (ox, oy, oz) world voxels off the face's centre;
     *  clipped to the block. */
    public static int[] slotBox(mods.eln.node.six.SixNodeDescriptor d, int f, int ox, int oy, int oz) {
        int[] b = d.footprintBox(f);
        int[] o = {ox, oy, oz};
        int[] r = new int[6];
        for (int k = 0; k < 6; k++) r[k] = Math.max(0, Math.min(16, b[k] + o[k % 3]));   // (not past the block)
        return r;
    }

    /** Elements changed (placed, broken): the plates in the grid. */
    public void elementsChanged() {
        markUsed();
        setNeedPublish(true);
    }

    private static void fill(BitSet s, int[] b) {
        for (int x = b[0]; x < b[3]; x++)
            for (int y = b[1]; y < b[4]; y++)
                for (int z = b[2]; z < b[5]; z++) s.set(voxel(x, y, z));
    }

    /** Is any voxel of the box (local voxels, [x0, x1) etc.) used? */
    /** A copy of the used voxels (for a route snapshot, RouteView). */
    public BitSet usedCopy() {
        return (BitSet) used.clone();   // (routes keep a voxel of air from it themselves: CableRouter.CLEARANCE)
    }

    public boolean usedIn(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.max(0, x0); x < Math.min(16, x1); x++)
            for (int y = Math.max(0, y0); y < Math.min(16, y1); y++)
                for (int z = Math.max(0, z0); z < Math.min(16, z1); z++) if (used.get(voxel(x, y, z))) return true;
        return false;
    }

    /** Would this part fit (no voxel already used)? */
    public boolean fits(VoxelPart p) {
        BitSet want = new BitSet(4096);
        for (int[] b : p.boxes()) fill(want, b);
        return !want.intersects(used);
    }

    /** Could the part go in (null), else why not. */
    public String canAdd(VoxelPart p) {
        if (p instanceof PipePart pipe && !pipe.valid()) return "not a straight-pieced pipe inside the block";
        if (p instanceof CablePart c && !c.valid()) return "not a cable lying on a face of the block";
        if (p instanceof AdapterPart a) {   // (its flange sits on its own stub's end)
            BitSet others = new BitSet(4096), want = new BitSet(4096);
            PipePart stub = stubOf(a);
            for (VoxelPart q : parts) if (q != stub) for (int[] b : q.boxes()) fill(others, b);
            for (int[] b : a.boxes()) fill(want, b);
            if (stub == null) return "no pipe ends there";
            if (want.intersects(others)) return "something is in the way";
        } else if (p instanceof CablePart c) {   // (cables joined to it may overlap it where they meet)
            BitSet others = new BitSet(4096), want = new BitSet(4096);
            for (VoxelPart q : parts) if (!(q instanceof CablePart o && o.touches(c))) for (int[] b : q.boxes()) fill(others, b);
            for (int[] b : c.boxes()) fill(want, b);
            if (want.intersects(others)) return "something is in the way";
        } else if (!fits(p)) return "something is in the way";
        return null;
    }

    /** Add a part (a pipe laid here): null if done, else why not. */
    public String addPart(VoxelPart p) {
        String why = canAdd(p);
        if (why != null) return why;
        if (p instanceof PipePart pipe && pipe.fluid == null) pipe.makeNode(coordonate.y, mods.eln.sim.Ambient.kelvin(coordonate), null);
        parts.add(p);
        markUsed();
        changed();
        if (p instanceof AdapterPart) showAdapters();
        return null;
    }

    /** Take a part out (it drops if a player broke it). Returns true if the block is now empty. */
    public boolean removePart(int index, boolean drop) {
        if (index < 0 || index >= parts.size()) return parts.isEmpty();
        VoxelPart p = parts.remove(index);
        if (p instanceof PipePart) pruneJunctions();
        markUsed();
        if (drop && !partItem(p).isEmpty()) dropItem(partItem(p));
        changed();
        return isEmptyNode();
    }

    /** The item a part was made from (what it drops), or empty: a burst pipe, a burnt cable. */
    static ItemStack partItem(VoxelPart p) {
        if (p instanceof PipePart pipe) return pipe.burst ? ItemStack.EMPTY : PipeKitItem.kit(pipe.tier, pipe.length());
        if (p instanceof CablePart c) return c.burnt != null ? ItemStack.EMPTY : CableKitItem.kit(c.tier, c.spec, c.dropLength());
        if (p instanceof ValvePart v) return valveItem(v);
        if (p instanceof AdapterPart) return new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get());
        if (p instanceof SafetyValvePart sv) return new ItemStack(sv.purge ? mods.eln.registry.ElnRegistry.PIPE_PURGE_VALVE.get() : mods.eln.registry.ElnRegistry.PIPE_SAFETY_VALVE.get());
        if (p instanceof PipeSensorPart s) return sensorItem(s);
        return ItemStack.EMPTY;
    }

    /** Middle-click: the device, cable, pipe or fitting under the crosshair, else the imitated block. */
    @Override
    public ItemStack pickItem(net.minecraft.world.phys.BlockHitResult hit, net.minecraft.world.level.LevelReader level) {
        if (hit != null) {
            var l = hit.getLocation();
            Aimed a = aimed(l.x - coordonate.x, l.y - coordonate.y, l.z - coordonate.z);
            if (a.element() >= 0 && getElement(a.element()) != null) return getElement(a.element()).getDropItemStack();
            if (a.part() >= 0 && !pickPart(parts.get(a.part())).isEmpty()) return pickPart(parts.get(a.part()));
        }
        if (host != null) return host.getCloneItemStack(level, coordonate.pos(), false);
        for (VoxelPart p : parts) if (!pickPart(p).isEmpty()) return pickPart(p);
        return super.pickItem(hit, level);
    }

    /** A part's item for a middle-click: a cable or pipe as a one-block kit of its tier (burnt or burst too). */
    static ItemStack pickPart(VoxelPart p) {
        if (p instanceof PipePart pipe) return PipeKitItem.kit(pipe.tier, 16);
        if (p instanceof CablePart c) return CableKitItem.kit(c.tier, c.spec, 16);
        return partItem(p);
    }

    /** The item a pipe sensor was fitted from. */
    static ItemStack sensorItem(PipeSensorPart s) {
        return new ItemStack(s.sensing == PipeSensorPart.PRESSURE ? mods.eln.registry.ElnRegistry.PIPE_PRESSURE_SENSOR.get()
            : s.sensing == PipeSensorPart.TEMPERATURE ? mods.eln.registry.ElnRegistry.PIPE_TEMPERATURE_SENSOR.get()
            : mods.eln.registry.ElnRegistry.PIPE_QUALITY_SENSOR.get());
    }

    /** The item a valve was fitted from. */
    static ItemStack valveItem(ValvePart v) {
        return new ItemStack(switch (v.mode) {
            case ValvePart.CONTROL -> mods.eln.registry.ElnRegistry.PIPE_CONTROL_VALVE.get();
            case ValvePart.THROTTLE -> mods.eln.registry.ElnRegistry.PIPE_THROTTLE_VALVE.get();
            case ValvePart.PRESSURE -> mods.eln.registry.ElnRegistry.PIPE_PRESSURE_VALVE.get();
            default -> mods.eln.registry.ElnRegistry.PIPE_VALVE.get();
        });
    }

    /**
     * A carved block stays a voxel block when its last part or device goes: turning back into what it imitated would
     * fill the carving in (Baughn 2026-11-08: removing a cable took the chisel's cut-out with it).
     */
    public boolean keepsCarving() {
        return host != null && carved != null && !carved.isEmpty();
    }

    /**
     * The last part or device went from an imitating block: it becomes the stair (or whatever it imitated) again, unless
     * it's carved, when it stays as it is. True if it imitates something (the caller mustn't remove the block then).
     */
    public boolean lastPartGone() {
        if (host == null) return false;
        if (!keepsCarving()) removeEmptyBlock();
        return true;
    }

    /** What this node put into the simulation and the fluid world (GameTests: none of it may stay after the node goes):
     *  its conductors' loads and processes, its pipes' fluid nodes, its node connections. */
    public List<Object> simObjectsForTests() {
        List<Object> out = new ArrayList<>();
        for (CableNet.Conductor k : cables.conductors) {
            if (k.load != null) out.add(k.load);
            if (k.thermal != null) out.add(k.thermal);
            out.addAll(k.slow);
            out.addAll(k.thermalSlow);
            out.addAll(k.watchdogs);
        }
        for (VoxelPart p : parts) if (p instanceof PipePart pp && pp.fluid != null) out.add(pp.fluid);
        for (VoxelPart p : parts) if (p instanceof JunctionPart j && j.fluid != null) out.add(j.fluid);
        out.addAll(nodeConnectionList);
        return out;
    }

    /** Is part in a burnt conductor here? */
    boolean burntCable(CablePart part) {
        for (CableNet.Conductor k : cables.conductors) if (k.parts.contains(part)) return k.getBurnt() != null;
        return false;
    }

    /** The conductor holding part, as a burnable (Jade's burnt-out lines), or null. */
    public mods.eln.sim.process.destruct.IBurnable burnableOf(CablePart part) {
        for (CableNet.Conductor k : cables.conductors) if (k.parts.contains(part)) return k;
        return null;
    }

    /** Mend the burnt conductor holding part (a burnt cable repaired with a kit: Baughn 2026-11-11). */
    void repairCable(CablePart part) {
        for (CableNet.Conductor k : cables.conductors) if (k.parts.contains(part)) k.repair();
    }

    /** Nothing left: no parts, no elements (the block goes). */
    public boolean isEmptyNode() {
        return parts.isEmpty() && !getIfSideRemain();
    }

    /** The part whose boxes hold the block-local point (0..1), or -1. */
    public int partAt(double x, double y, double z) {
        // (a junction is never aimed at on its own: the arm under it is; it goes when its arms do)
        for (int i = parts.size() - 1; i >= 0; i--)
            if (!(parts.get(i) instanceof JunctionPart)) for (int[] b : parts.get(i).boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return i;
        for (int i = parts.size() - 1; i >= 0; i--)   // (the outline is 2 voxels fatter than the parts)
            if (!(parts.get(i) instanceof JunctionPart)) for (int[] b : parts.get(i).boxes()) if (VoxelPart.contains(b, x, y, z, 2.05)) return i;
        for (int i = parts.size() - 1; i >= 0; i--)
            if (parts.get(i) instanceof JunctionPart j) for (int[] b : j.boxes()) if (VoxelPart.contains(b, x, y, z, 2.05)) return armNear(j, x, y, z);
        return -1;
    }

    /** The index of the pipe ending at junction j nearest the point (block-local voxels), or -1. */
    int armNear(JunctionPart j, double x, double y, double z) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i) instanceof PipePart pipe && endsAt(pipe, j.x, j.y, j.z)) {
                int[] pts = pipe.points;
                int k = pts[0] == j.x && pts[1] == j.y && pts[2] == j.z ? 3 : pts.length - 6;   // (the point next to its end)
                double d = sq(pts[k] - x) + sq(pts[k + 1] - y) + sq(pts[k + 2] - z);
                if (d < bd) {
                    bd = d;
                    best = i;
                }
            }
        return best;
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Does a pipe have an inner end at (x, y, z)? */
    static boolean endsAt(PipePart pipe, int x, int y, int z) {
        for (int[] e : pipe.innerEnds()) if (e[0] == x && e[1] == y && e[2] == z) return true;
        return false;
    }

    /** The junction at (x, y, z), or null. */
    JunctionPart junctionAt(int x, int y, int z) {
        for (VoxelPart p : parts) if (p instanceof JunctionPart j && j.x == x && j.y == y && j.z == z) return j;
        return null;
    }

    /** The pipes with an inner end at (x, y, z). */
    List<PipePart> armsAt(int x, int y, int z) {
        List<PipePart> out = new ArrayList<>();
        for (VoxelPart p : parts) if (p instanceof PipePart pipe && endsAt(pipe, x, y, z)) out.add(pipe);
        return out;
    }

    /** Junctions left with fewer than two arms go; their contents go into the arm that is left (if any). */
    private void pruneJunctions() {
        for (int i = parts.size() - 1; i >= 0; i--)
            if (parts.get(i) instanceof JunctionPart j) {
                List<PipePart> arms = armsAt(j.x, j.y, j.z);
                if (arms.size() >= 2) continue;
                if (!arms.isEmpty() && arms.get(0).fluid != null && j.fluid != null) {
                    arms.get(0).fluid.addShareOf(j.fluid, 1);
                    arms.get(0).fluid.update();
                }
                parts.remove(i);
            }
    }

    /**
     * A brush on a power cable paints its insulation (Baughn 2026-11-06: colour is only its look). EA's brushes count
     * colours as the old dye damage (0 black .. 15 white), a spec as DyeColor ids (0 white .. 15 black).
     */
    boolean recolour(Player player, int index, CablePart c) {
        ItemStack held = player.getMainHandItem();
        if (!(mods.eln.item.BrushDescriptor.getDescriptor(held) instanceof mods.eln.item.BrushDescriptor brush)) return false;
        int colour = 15 - brush.getColor(held);
        if (colour == c.spec.colour() || !brush.use(held, player)) return true;
        CablePart n = new CablePart(c.tier, new CableSpec(c.spec.mm2(), colour), c.face, c.points);
        n.burnt = c.burnt;
        n.paid = c.paid;
        n.byConverter = c.byConverter;
        n.u = c.u;
        n.t = c.t;
        parts.set(index, n);
        changed();
        return true;
    }

    private void changed() {
        vents = null;
        valveLinks = null;
        junctionLinks = null;
        if (isAdded()) {
            if (hasFluid()) FluidWorld.add(this);
            FluidWorld.changed(this);
            // cables: rebuild the conductors and their links (reconnect keeps their state through the pieces)
            boolean cable = false;
            for (VoxelPart p : parts) if (p instanceof CablePart) cable = true;
            if (cable || cablesBuilt) reconnect();
        }
        setNeedPublish(true);
        invalidateCapabilities();   // (an adapter added, removed or its pipe changed: pipes holding a cache look again)
    }

    /** Were there conductors at the last connect (a removed last cable must reconnect too)? */
    private boolean cablesBuilt;

    // ---- fluid

    @Override
    public Object fluidLevel() {
        return coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        List<FluidPort> out = new ArrayList<>();
        long pos = coordonate.pos().asLong();
        for (VoxelPart p : parts) out.addAll(p.ports(pos));
        return out;
    }

    /** Pipes whose inner ends meet at the same point are joined (a junction). */
    @Override
    public List<List<mods.eln.sim.fluid.FluidNode>> fluidJoins() {
        java.util.Map<Integer, List<mods.eln.sim.fluid.FluidNode>> at = new java.util.HashMap<>();
        java.util.Set<Integer> valves = new java.util.HashSet<>();
        for (VoxelPart p : parts) if (p instanceof ValvePart v) valves.add(voxel(v.x, v.y, v.z));
        // (nor at a junction: its arms link to its own node, fluidLinks; one rule, whatever else meets there)
        for (VoxelPart p : parts) if (p instanceof JunctionPart j) valves.add(voxel(j.x, j.y, j.z));
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null)
                for (int[] e : pipe.innerEnds())
                    if (!valves.contains(voxel(e[0], e[1], e[2])))   // (a valve's two pipes meet through it, not joined)
                        at.computeIfAbsent(voxel(e[0], e[1], e[2]), k -> new ArrayList<>()).add(pipe.fluid);
        List<List<mods.eln.sim.fluid.FluidNode>> out = new ArrayList<>();
        for (var g : at.values()) if (g.size() > 1) out.add(g);
        return out;
    }

    /** Each valve links the two pipes that end at it, as far open as it is (kept while the parts don't change: the
     *  control and throttle valves set their link's opening every step, beforeFluidStep). */
    @Override
    public List<Link> fluidLinks() {
        List<Link> out = valveLinks();
        if (junctionLinks == null) {
            junctionLinks = new ArrayList<>();
            for (VoxelPart p : parts)
                if (p instanceof JunctionPart j && j.fluid != null)
                    for (PipePart arm : armsAt(j.x, j.y, j.z)) if (arm.fluid != null) junctionLinks.add(armLink(arm, j));
        }
        out.addAll(junctionLinks);
        return out;
    }

    private List<Link> junctionLinks;

    /**
     * An arm's link to its junction: from the arm's end, at that end's height on its piece, with the end's own
     * conductance (as at a port: half the piece's length to it); a horizontal end is an opening as tall as the bore
     * (as a pipe's end on a side face), a vertical end a point at its end; the junction's side is an opening as tall as
     * its body. So, as between two pipes' ends, a vertical arm gets the counter-flow pair (liquid down out of a sealed
     * riser while the junction's gas goes up into it: LabAgent's review) and a level arm one edge across the two
     * openings.
     */
    static Link armLink(PipePart arm, JunctionPart j) {
        int[] pts = arm.points;
        boolean first = pts[0] == j.x && pts[1] == j.y && pts[2] == j.z;
        int k = first ? 3 : pts.length - 6;
        boolean vertical = pts[k] == j.x && pts[k + 2] == j.z;
        double draw = arm.heightVoxels() == arm.bore() ? 0.5 : (j.y - arm.minY()) / (double) arm.heightVoxels();
        double span = vertical ? 0 : Math.min(1, arm.bore() / (double) arm.heightVoxels());
        double k0 = 2 * PipePart.K_PER_BLOCK[arm.tier] * 16.0 / Math.max(1, arm.length());
        return new Link(arm.fluid, draw, j.fluid, 0.5, k0).spans(span, 1);
    }

    private List<Link> valveLinks() {
        if (valveLinks != null) return new ArrayList<>(valveLinks.keySet());
        valveLinks = new java.util.LinkedHashMap<>();
        for (VoxelPart p : parts) {
            if (!(p instanceof ValvePart v)) continue;
            List<PipePart> ends = new ArrayList<>();
            for (VoxelPart q : parts)
                if (q instanceof PipePart pipe && pipe.fluid != null)
                    for (int[] e : pipe.innerEnds()) if (e[0] == v.x && e[1] == v.y && e[2] == v.z) ends.add(pipe);
            if (ends.size() != 2) continue;
            PipePart a = ends.get(0), b = ends.get(1);
            // (a pressure valve passes one way, in its arrow's direction: a is the pipe before it)
            if (v.mode == ValvePart.PRESSURE && side(a, v) == v.flow) {
                PipePart t = a;
                a = b;
                b = t;
            }
            double k = PipePart.K_PER_BLOCK[Math.min(a.tier, b.tier)] * (v.mode == ValvePart.PRESSURE ? 1 : 4);   // (a quarter block of pipe; a
            // pressure valve's seat is narrower, a block's worth, so it can hold its set point without overshooting)
            // (a valve meters by the orifice law, its seat the smaller pipe's bore area: VOLUME_PER_BLOCK is m^3 a metre)
            Link l = new Link(a.fluid, 0.5, b.fluid, 0.5, k).area(PipePart.VOLUME_PER_BLOCK[Math.min(a.tier, b.tier)]);
            if (v.mode == ValvePart.PRESSURE) l.kind(mods.eln.sim.fluid.FluidEdge.Kind.REGULATOR);
            l.open = v.opening();
            valveLinks.put(l, v);
        }
        return new ArrayList<>(valveLinks.keySet());
    }

    private java.util.Map<Link, ValvePart> valveLinks;

    /** Which side of a valve, along its axis, a pipe ending at it lies on (+1 or -1). */
    static int side(PipePart pipe, ValvePart v) {
        int[] at = {v.x, v.y, v.z};
        for (int i = 0; i + 2 < pipe.points.length; i += 3) {
            int d = pipe.points[i + v.axis] - at[v.axis];
            if (d != 0) return d > 0 ? 1 : -1;
        }
        return 1;
    }

    /** The adapter on that face (for the fluid capability), or null. */
    public AdapterPart adapterOn(int face) {
        for (VoxelPart p : parts) if (p instanceof AdapterPart a && a.face == face) return a;
        return null;
    }

    /** Fit a valve in the pipe running straight through the block's centre (it is cut there into two). */
    public String addValve() {
        return addValve(8, 8, 8);
    }

    /** The pipe a safety valve sits on (the pipe passing through its point). */
    PipePart pipeUnder(SafetyValvePart v) {
        for (VoxelPart p : parts) if (p instanceof PipePart pipe && pipe.fluid != null && pipe.passes(v.x, v.y, v.z)) return pipe;
        return null;
    }

    /** Fit a safety valve on the pipe under the click, at the lattice point nearest the click, its outlet out of the
     *  pipe's most open side; set to the pipe's working pressure. */
    public String addSafetyValveNear(double hx, double hy, double hz) {
        return addSafetyValveNear(hx, hy, hz, false);
    }

    /** ... or a purge valve (gas only), set just over the air's pressure. */
    public String addSafetyValveNear(double hx, double hy, double hz, boolean purge) {
        int idx = partAt(hx, hy, hz);
        if (idx >= 0 && (parts.get(idx) instanceof SafetyValvePart || parts.get(idx) instanceof ValvePart)) return "there's a valve there already";
        for (VoxelPart q : parts) if (q instanceof SafetyValvePart) return "one safety valve per block (this one has one)";
        if (idx < 0 || !(parts.get(idx) instanceof PipePart pipe)) return "fit it on a pipe";
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int x = 4; x <= 12; x += 4)
            for (int y = 4; y <= 12; y += 4)
                for (int z = 4; z <= 12; z += 4) {
                    if (!pipe.passes(x, y, z)) continue;
                    double d = (x - hx * 16) * (x - hx * 16) + (y - hy * 16) * (y - hy * 16) + (z - hz * 16) * (z - hz * 16);
                    if (d < bd) {
                        bd = d;
                        best = new int[]{x, y, z};
                    }
                }
        if (best == null) return "the pipe has no room for it here";
        int axis = -1;
        for (int a = 0; a < 3; a++) {
            int[] lo = best.clone(), hi = best.clone();
            lo[a] -= 2;
            hi[a] += 2;
            if (pipe.passes(lo[0], lo[1], lo[2]) && pipe.passes(hi[0], hi[1], hi[2])) axis = a;
        }
        if (axis < 0) return "the pipe turns there";
        // outlet: up if the pipe isn't vertical and there's room, else the most open side
        int outlet = -1;
        for (int d : axis == 1 ? new int[]{2, 3, 4, 5} : new int[]{1, 2, 3, 4, 5, 0}) {
            var v = new SafetyValvePart(best[0], best[1], best[2], d, pipe.bore(), pipe.workingPa(mods.eln.sim.fluid.Water.T_AMBIENT));
            if (v.boxes().get(0)[3] - v.boxes().get(0)[0] < 4 || v.boxes().get(0)[4] - v.boxes().get(0)[1] < 4
                || v.boxes().get(0)[5] - v.boxes().get(0)[2] < 4) continue;   // (cut off by the block's edge)
            if (fits(v)) {
                outlet = d;
                break;
            }
        }
        if (outlet < 0) return "no room around the pipe there";
        var v = new SafetyValvePart(best[0], best[1], best[2], outlet, pipe.bore(),
            purge ? SafetyValvePart.PURGE_PA : pipe.workingPa(mods.eln.sim.fluid.Water.T_AMBIENT), purge);
        parts.add(v);
        markUsed();
        changed();
        return null;
    }

    /** Fit a valve where the pipe under the click runs straight: the lattice point on it nearest the click. */
    public String addValveNear(double hx, double hy, double hz) {
        return addValveNear(hx, hy, hz, ValvePart.ON_OFF);
    }

    /** A valve of a kind (ValvePart.ON_OFF, CONTROL, THROTTLE) at the lattice point on the pipe nearest the click. */
    public String addValveNear(double hx, double hy, double hz, int mode) {
        int idx = partAt(hx, hy, hz);
        if (idx >= 0 && parts.get(idx) instanceof ValvePart) return "there is a valve here already";
        if (idx < 0 || !(parts.get(idx) instanceof PipePart pipe)) return "fit it on a pipe";
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int x = 4; x <= 12; x += 4)
            for (int y = 4; y <= 12; y += 4)
                for (int z = 4; z <= 12; z += 4) {
                    if (!pipe.passes(x, y, z)) continue;
                    double d = (x - hx * 16) * (x - hx * 16) + (y - hy * 16) * (y - hy * 16) + (z - hz * 16) * (z - hz * 16);
                    if (d < bd) {
                        bd = d;
                        best = new int[]{x, y, z};
                    }
                }
        return best == null ? "the pipe has no room for a valve here" : addValve(best[0], best[1], best[2], mode);
    }

    public String addValve(int x, int y, int z) {
        return addValve(x, y, z, ValvePart.ON_OFF);
    }

    public String addValve(int x, int y, int z, int mode) {
        for (VoxelPart p : parts) if (p instanceof ValvePart) return "there is a valve here already";
        for (int i = 0; i < parts.size(); i++) {
            if (!(parts.get(i) instanceof PipePart pipe) || !pipe.passes(x, y, z)) continue;
            int axis = -1;
            for (int a = 0; a < 3; a++) {
                int[] lo = {x, y, z}, hi = {x, y, z};
                lo[a] -= 2;
                hi[a] += 2;
                if (pipe.passes(lo[0], lo[1], lo[2]) && pipe.passes(hi[0], hi[1], hi[2])) axis = a;
            }
            if (axis < 0) return "the pipe turns here: fit a valve on a straight piece";
            BitSet others = new BitSet(4096), want = new BitSet(4096);
            for (int j = 0; j < parts.size(); j++) if (j != i) for (int[] b : parts.get(j).boxes()) fill(others, b);
            for (int[] b : new ValvePart(x, y, z, axis, pipe.bore(), mode).boxes()) fill(want, b);
            if (want.intersects(others)) return "something is in the way";
            PipePart[] halves = pipe.splitAt(x, y, z);
            if (halves == null) return "the pipe ends here";
            mods.eln.sim.fluid.FluidNode saved = pipe.fluid;
            parts.remove(i);
            for (PipePart hp : halves) {
                double share = hp.length() / (double) Math.max(1, pipe.length());
                hp.burst = pipe.burst;
                hp.makeNode(coordonate.y, mods.eln.sim.Ambient.kelvin(coordonate), saved, share);
                parts.add(hp);
            }
            parts.add(new ValvePart(x, y, z, axis, pipe.bore(), mode));
            markUsed();
            changed();
            return null;
        }
        return "the pipe doesn't run straight there";
    }

    /** A pipe ending at or passing through a point of this block (a valve sits between two that end there), or null. */
    PipePart pipeNear(int x, int y, int z) {
        PipePart at = pipeAt(x, y, z);
        if (at != null) return at;
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null)
                for (int i = 0; i + 2 < pipe.points.length; i += 3)
                    if (pipe.points[i] == x && pipe.points[i + 1] == y && pipe.points[i + 2] == z) return pipe;
        return null;
    }

    /** The pipe passing through a point of this block, or null. */
    public PipePart pipeAt(int x, int y, int z) {
        for (VoxelPart p : parts) if (p instanceof PipePart pipe && pipe.fluid != null && pipe.passes(x, y, z)) return pipe;
        return null;
    }

    /**
     * Fit a pipe sensor (Baughn 2026-10-27) on the pipe under the click, as a safety valve: at the lattice point nearest
     * the click on a straight stretch, its dial out of the pipe's most open side (up first). One per block: its output
     * joins every signal cable in the block, so two would drive the same wire.
     */
    public String addSensorNear(double hx, double hy, double hz, int sensing) {
        for (VoxelPart q : parts)
            if (q instanceof PipeSensorPart) return "one sensor per block (its signal goes on every signal cable in the block)";
        int idx = partAt(hx, hy, hz);
        if (idx < 0 || !(parts.get(idx) instanceof PipePart pipe)) return "fit it on a pipe";
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int x = 4; x <= 12; x += 4)
            for (int y = 4; y <= 12; y += 4)
                for (int z = 4; z <= 12; z += 4) {
                    if (!pipe.passes(x, y, z)) continue;
                    boolean taken = false;
                    for (VoxelPart q : parts)
                        if (q instanceof ValvePart v && v.x == x && v.y == y && v.z == z || q instanceof SafetyValvePart v2 && v2.x == x && v2.y == y && v2.z == z
                            || q instanceof PipeSensorPart s && s.x == x && s.y == y && s.z == z) taken = true;
                    if (taken) continue;
                    double d = (x - hx * 16) * (x - hx * 16) + (y - hy * 16) * (y - hy * 16) + (z - hz * 16) * (z - hz * 16);
                    if (d < bd) {
                        bd = d;
                        best = new int[]{x, y, z};
                    }
                }
        if (best == null) return "the pipe has no room for it here";
        int axis = -1;
        for (int a = 0; a < 3; a++) {
            int[] lo = best.clone(), hi = best.clone();
            lo[a] -= 2;
            hi[a] += 2;
            if (pipe.passes(lo[0], lo[1], lo[2]) && pipe.passes(hi[0], hi[1], hi[2])) axis = a;
        }
        if (axis < 0) return "the pipe turns there";
        double scale = PipeSensorPart.defaultScale(sensing);
        for (int d : axis == 1 ? new int[]{2, 3, 4, 5} : new int[]{1, 2, 3, 4, 5, 0}) {
            var s = new PipeSensorPart(best[0], best[1], best[2], d, pipe.bore(), sensing, scale);
            int[] b = s.boxes().get(0);
            if (b[3] - b[0] < 3 || b[4] - b[1] < 3 || b[5] - b[2] < 3) continue;   // (cut off by the block's edge)
            if (fits(s)) {
                parts.add(s);
                markUsed();
                changed();
                return null;
            }
        }
        return "no room around the pipe there";
    }

    /**
     * What the crosshair (or a click) is on, at block-relative hit (x, y, z): a hosted device's slot (a device's own face
     * first, then within reach of a face), else a part's index; -1 for none. One rule for clicks, meters and Jade
     * (Baughn 2026-10-28: each worked it out alone, and the meters read the block's first cable or pipe).
     */
    public record Aimed(int element, int part) {
    }

    public Aimed aimed(double x, double y, double z) {
        // a pipe sensor device wins over the end of the pipe laid to its port, which covers its body from most angles
        // (a lab test: Jade showed "Iron Pipe")
        for (int i = 6; i < SLOTS; i++)
            if (sideElementList[i] instanceof mods.eln.sixnode.pipetransmitter.PipeTransmitterElement t)
                for (int[] b : t.aimBoxes(elementBoxes(i).get(0))) if (VoxelPart.contains(b, x, y, z, 0.05)) return new Aimed(i, -1);
        int el = elementAt(x, y, z, 0), pi = partAt(x, y, z);
        if (el < 0 && pi < 0) el = elementAt(x, y, z, 2);
        return new Aimed(el, pi);
    }

    /** The pipe a part stands for: itself, or the pipe a valve, safety valve or sensor sits on; null if none. */
    public PipePart pipeOf(int part) {
        VoxelPart p = part >= 0 && part < parts.size() ? parts.get(part) : null;
        if (p instanceof PipePart pp && pp.fluid != null) return pp;
        if (p instanceof ValvePart v) return pipeNear(v.x, v.y, v.z);
        if (p instanceof SafetyValvePart sv) return pipeAt(sv.x, sv.y, sv.z);
        if (p instanceof PipeSensorPart ps) return pipeAt(ps.x, ps.y, ps.z);
        return null;
    }

    /** The cable conductor a part belongs to, or null. */
    public CableNet.Conductor conductorOf(int part) {
        VoxelPart p = part >= 0 && part < parts.size() ? parts.get(part) : null;
        if (!(p instanceof CablePart)) return null;
        for (CableNet.Conductor k : cables.conductors) if (k.valid() && k.parts.contains(p)) return k;
        return null;
    }

    /** A right-click on a valve (empty hand) turns it. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        // a six-node element hosted here: as in a six-node block (its own use, GUI, repair, meters)
        Aimed a = aimed(vx, vy, vz);
        int el = a.element(), pi = a.part();
        if (el >= 0) return onSlotActivated(player, el, vx, vy, vz);
        if (pi >= 0 && parts.get(pi) instanceof CablePart c && c.spec != null && recolour(player, pi, c)) return true;
        if (player.getMainHandItem().isEmpty()) {
            int i = pi;
            if (i >= 0 && parts.get(i) instanceof SafetyValvePart sv) {
                // (Baughn 2026-10-27: a screen with a slider, not stepping through presets)
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) openPartScreen(sp, i, SCREEN_SAFETY, sv.setPa / 1e5);
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof PipeSensorPart s) {
                s.scale = s.nextScale();
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("%s: full scale (5 V) %.0f %s", s.name(), s.scale, s.unit())));
                setNeedPublish(true);
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.ON_OFF) {
                v.open = !v.open;
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(v.open ? "Valve open" : "Valve shut"));
                changed();
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.CONTROL) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("Control valve %.0f%% open: a signal cable laid into its block sets it",
                    v.opening() * 100)));
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.PRESSURE && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                // its set point, mode and arrow (ThrottleValveScreen: the value carries mode and arrow, see pressureScreenValue)
                openPartScreen(sp, i, SCREEN_PRESSURE, pressureScreenValue(v));
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.THROTTLE && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                openPartScreen(sp, i, SCREEN_THROTTLE, v.setting);
                return true;
            }
        }
        // meters: the part clicked (its cable or pipe), not the block's first
        return pi >= 0 && meterUse(player, () -> multiMeterString(pi), () -> thermoMeterString(pi));
    }

    /** A GUI packet's slot byte for a part of this block (ValvePart.THROTTLE's slider), not an element. */
    public static final byte PART_PACKET = -2;

    /** The part screens (client ThrottleValveScreen): a throttle valve's opening, a safety valve's set pressure. */
    public static final byte SCREEN_THROTTLE = 0, SCREEN_SAFETY = 1, SCREEN_PRESSURE = 2;
    /** The pressure valve's screen packets (PART_PACKET floats): a set point (bar, >= 0), or these to toggle its mode or
     *  turn its arrow round. */
    public static final float TOGGLE_PRESSURE_MODE = -1, FLIP_PRESSURE_ARROW = -2;

    /** A pressure valve's state for its screen in one float: set point (bar) + 1000 if reducing + 2000 if its arrow
     *  points to -axis. */
    static double pressureScreenValue(ValvePart v) {
        return v.setPa / 1e5 + (v.pmode == ValvePart.REDUCING ? 1000 : 0) + (v.flow < 0 ? 2000 : 0);
    }

    /** Open a part's slider screen on the player's client (ClientPacketHandler, Eln.packetValveScreen): the part's
     *  index, the kind of screen and its value (0..1 open, or bar). */
    void openPartScreen(net.minecraft.server.level.ServerPlayer player, int index, byte screen, double value) {
        try {
            var bos = new java.io.ByteArrayOutputStream();
            var out = new java.io.DataOutputStream(bos);
            out.writeByte(Eln.packetValveScreen);
            out.writeInt(coordonate.x);
            out.writeInt(coordonate.y);
            out.writeInt(coordonate.z);
            out.writeByte(index);
            out.writeFloat((float) value);
            out.writeByte(screen);
            mods.eln.network.ElnNet.sendToPlayer(player, bos);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /** From a client: a throttle valve's new setting (PART_PACKET, part index, float 0..1) or a safety valve's set
     *  pressure (bar); else an element's packet. */
    @Override
    public void networkUnserialize(java.io.DataInputStream stream, net.minecraft.server.level.ServerPlayer player) {
        try {
            stream.mark(64);
            if (stream.readByte() == PART_PACKET) {
                int i = stream.readByte();
                float f = stream.readFloat();
                if (i >= 0 && i < parts.size() && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.THROTTLE && Float.isFinite(f)) {
                    v.setting = Math.max(0, Math.min(1, f));
                    setNeedPublish(true);
                }
                if (i >= 0 && i < parts.size() && parts.get(i) instanceof ValvePart v && v.mode == ValvePart.PRESSURE && Float.isFinite(f)) {
                    if (f == TOGGLE_PRESSURE_MODE) v.pmode = v.pmode == ValvePart.RELIEF ? ValvePart.REDUCING : ValvePart.RELIEF;
                    else if (f == FLIP_PRESSURE_ARROW) {
                        v.flow = -v.flow;
                        changed();   // (its link turns round)
                    } else if (f >= 0) v.setPa = Math.max(ValvePart.MIN_BAR, Math.min(ValvePart.MAX_BAR, f)) * 1e5;
                    setNeedPublish(true);
                }
                if (i >= 0 && i < parts.size() && parts.get(i) instanceof SafetyValvePart sv && Float.isFinite(f)) {
                    sv.setPa = Math.max(SafetyValvePart.MIN_BAR, Math.min(SafetyValvePart.MAX_BAR, f)) * 1e5;
                    setNeedPublish(true);
                }
                return;
            }
            stream.reset();
        } catch (java.io.IOException e) {
            return;
        }
        super.networkUnserialize(stream, player);
    }

    /** The element (its slot: 0..5 centred on that face) whose model box, grown by grow voxels, holds the block-local point (0..1); -1, also
     *  where a part is. */
    public int elementAt(double x, double y, double z, int grow) {
        for (VoxelPart p : parts)   // (a part's own box wins: a model box can reach past the plate, over a cable)
            for (int[] b : p.boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return -1;
        for (int f = 0; f < SLOTS; f++) {
            if (sideElementList[f] == null) continue;
            for (int[] b : elementBoxes(f)) {
                int[] g = {b[0] - grow, b[1] - grow, b[2] - grow, b[3] + grow, b[4] + grow, b[5] + grow};
                if (VoxelPart.contains(g, x, y, z, 0.05)) return f;
            }
        }
        return -1;
    }

    /** placeError for a device of this stack at that place and front (a probe element, not added). */
    public String placeErrorAt(ItemStack stack, int slot, Direction face, int ox, int oy, int oz, LRDU front) {
        var d = Eln.sixNodeItem.getDescriptor(stack);
        if (!d.allowedOnSide(face)) return "It can't go on that side";   // (the power sockets: walls only)
        var probe = standIn(stack, slot, face, ox, oy, oz, front);
        if (probe == null) return "Can't place that";
        try {
            return placeError(probe);
        } finally {
            probe.unload();   // (devices join their registries in initialize, which a probe never gets; unload is the backstop)
        }
    }

    /**
     * The front a wrench click turns the device off centre in e's slot to: the next one clockwise; but if pins of it are
     * joined to cables (in line, or on a cable's end), the next one clockwise at which pins of the same kinds are at
     * all those places (an energy meter in line: straight round, reversing it; AdventurAgent: a quarter turn cut the
     * run). Null, and why in err[0], if there is none or it would not fit.
     */
    public LRDU wrenchTurn(mods.eln.node.six.SixNodeElement e, String[] err) {
        List<int[]> held = new ArrayList<>();   // (tip x, y, z, mask of each joined pin)
        for (Pin p : pins(e)) if (e.slotMask.get(p.lrdu())) held.add(new int[]{p.tip()[0], p.tip()[1], p.tip()[2], p.mask()});
        LRDU was = e.front, f = was;
        String why = null;
        for (int k = 0; k < 3; k++) {
            f = f.getNextClockwise();
            boolean keeps = true;
            if (!held.isEmpty()) {
                e.front = f;
                try {
                    List<Pin> now = pins(e);
                    for (int[] h : held) {
                        boolean found = false;
                        for (Pin p : now)
                            if (p.tip()[0] == h[0] && p.tip()[1] == h[1] && p.tip()[2] == h[2] && compareConnectionMask(p.mask(), h[3])) found = true;
                        keeps &= found;
                    }
                } finally {
                    e.front = was;
                }
            }
            if (!keeps) continue;
            why = turnError(e, f);
            if (why == null) return f;
            if (held.isEmpty()) break;   // (not joined: the quarter turn or nothing, as before)
        }
        err[0] = why != null ? why : "It can't turn here without taking its pins off the cables";
        return null;
    }

    /** Why the device off centre in e's slot can't be turned to front (its body and pins move: a lopsided model), or null. */
    public String turnError(mods.eln.node.six.SixNodeElement e, LRDU front) {
        if (e.slot < 6 || sideElementList[e.slot] != e) return null;
        LRDU was = e.front;
        sideElementList[e.slot] = null;
        markUsed();
        try {
            e.front = front;
            return placeError(e);
        } finally {
            e.front = was;
            sideElementList[e.slot] = e;
            markUsed();
        }
    }

    /** An element of this stack's kind at that place and front, not added (to read its pins); never initialised, so it
     *  joins no registry (VoxelDeviceCases.ProbesLeaveNoTraceCase); the caller still unloads it. Null if it can't be made. */
    mods.eln.node.six.SixNodeElement standIn(ItemStack stack, int slot, Direction face, int ox, int oy, int oz, LRDU front) {
        var d = Eln.sixNodeItem.getDescriptor(stack);
        mods.eln.node.six.SixNodeElement probe;
        try {
            probe = (mods.eln.node.six.SixNodeElement) d.ElementClass.getConstructor(mods.eln.node.six.SixNode.class, Direction.class,
                mods.eln.node.six.SixNodeDescriptor.class).newInstance(this, face, d);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
        probe.slot = slot;
        probe.ox = ox;
        probe.oy = oy;
        probe.oz = oz;
        probe.front = front;
        return probe;
    }

    /** The used voxels again after DevicePlacer changed the parts for a check. */
    void markUsedForPlacer() {
        markUsed();
    }

    /** A free slot past the faces, or -1. */
    public int freeSlot() {
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] == null && !isSideKept(i)) return i;
        return -1;
    }

    /**
     * Why a device can't go off centre at e's place (its slot, face, offset and front set; not yet in the node), or null.
     * Its box must be inside the block and clear of everything; and a voxel of air all round it (Baughn), except where one
     * of its pins touches a cable or meets another device's pin (connected, so legible).
     */
    public String placeError(mods.eln.node.six.SixNodeElement e) {
        var d = e.sixNodeElementDescriptor;
        int f = e.side.getInt();
        List<Pin> mine = pins(e);
        int[] body = rawBodyBox(d, e.side, e.front, e.ox, e.oy, e.oz);
        List<int[]> boxes = new ArrayList<>();
        boxes.add(body);
        for (Pin p : mine) boxes.add(p.box);
        BitSet want = new BitSet(4096);
        for (int[] b : boxes) {
            for (int k = 0; k < 6; k++) if (b[k] < 0 || b[k] > 16) return "It doesn't fit on the face there";
            fill(want, b);
        }
        if (want.intersects(used)) return "No room for it there";
        // a voxel of air round its body and pins; a pin may touch a pin (tip to tip) or a cable of its kind
        for (int i = 0; i < SLOTS; i++) {
            var o = sideElementList[i];
            if (o == null) continue;
            List<Pin> theirs = i >= 6 ? pins(o) : List.of();
            if (near(body, elementBoxes(i))) return tooClose(o);
            for (Pin p : mine) {
                if (near(p.box, List.of(elementBoxes(i).get(0)))) return tooClose(o);   // (their body)
                for (Pin q : theirs) if (near(p.box, List.of(q.box)) && !meet(p, q)) return tooClose(o);
            }
            for (Pin q : theirs) if (near(body, List.of(q.box))) return tooClose(o);
        }
        for (VoxelPart part : parts) {
            if (near(body, part.boxes())) return part instanceof CablePart ? "Too close to the cable: leave a voxel of air, or touch it with a pin"
                : "Too close to that: leave a voxel of air";
            for (Pin p : mine) {
                if (!near(p.box, part.boxes())) continue;
                boolean touched = false;
                if (part instanceof CablePart c) for (int[] b : c.boxes()) touched |= overlaps(b, p.contact);
                if (!touched) return part instanceof CablePart ? "Too close to the cable: leave a voxel of air, or touch it with a pin"
                    : "Too close to that: leave a voxel of air";
                if (!compareConnectionMask(p.mask, ((CablePart) part).tierInfo().mask()))
                    return "That cable doesn't fit its pin";   // (it would look connected and not be)
            }
        }
        return null;
    }

    /** Is any of the boxes within a voxel of b (b grown by one overlaps it)? */
    static boolean near(int[] b, List<int[]> boxes) {
        int[] g = {b[0] - 1, b[1] - 1, b[2] - 1, b[3] + 1, b[4] + 1, b[5] + 1};
        for (int[] x : boxes) if (overlaps(g, x)) return true;
        return false;
    }

    private static String tooClose(mods.eln.node.six.SixNodeElement o) {
        return "Too close to the " + o.sixNodeElementDescriptor.name + ": leave a voxel of air, or put their pins together";
    }

    /** Could an element go on face f (EA Direction int): free face, and its plate clear of the parts? */
    public boolean canPlaceElement(int f) {
        if (sideElementList[f] != null || isSideKept(f)) return false;
        BitSet want = new BitSet(4096);
        fill(want, plate(f));
        return !want.intersects(used);
    }

    @Override
    public boolean createSubBlockAt(ItemStack itemStack, int slot, Direction direction, int ox, int oy, int oz, Player player) {
        boolean ok = super.createSubBlockAt(itemStack, slot, direction, ox, oy, oz, player);
        markUsed();
        return ok;
    }

    /**
     * A device that may go anywhere on a face (SixNodeDescriptor.placeAnywhere: chips) placed on face `face` where the
     * block-local point (hx, hy, hz) is, snapped to voxels and kept on the face. Null if placed, else why not.
     */
    public String placeAnywhere(ItemStack stack, Direction face, double hx, double hy, double hz, Player player) {
        var d = Eln.sixNodeItem.getDescriptor(stack);
        if (d == null || !d.placeAnywhere() || d.hasGhostGroup()) return "That can't go off centre";
        int slot = freeSlot();
        if (slot < 0) return "No more devices fit in this block";
        if (!d.allowedOnSide(face)) return "It can't go on that side";
        LRDU front = d.getFrontFromPlace(face, player);
        int[] o = snap(d, face, front, hx, hy, hz);
        // in the middle of a free face: the face's own slot, as before (ports at the face's edges, which meet the
        // next block, a machine beside it, by EA's edge rule: a trimmer beside a furnace)
        if (o[0] == 0 && o[1] == 0 && o[2] == 0 && sideElementList[face.getInt()] == null && canPlaceElement(face.getInt()))
            return createSubBlock(stack, face, player) ? null : "Can't place that";
        // where it goes, in line on a cable, on a cable's end or beside one (DevicePlacer)
        var plan = DevicePlacer.plan(this, stack, slot, face, new double[]{hx, hy, hz}, player);
        if (plan.err() != null) return plan.err();
        var cut = plan.cut();
        CablePart old = cut == null ? null : (CablePart) parts.get(cut.index());
        if (cut != null) DevicePlacer.apply(this, cut);
        placingFront = plan.front();
        boolean ok;
        try {
            ok = createSubBlockAt(stack, slot, face, plan.ox(), plan.oy(), plan.oz(), player);
        } finally {
            placingFront = null;
        }
        if (!ok) {
            if (cut != null) DevicePlacer.unapply(this, cut, old);
            return "Can't place that";
        }
        if (cut != null && cut.inline()) d.placedInLine(getElement(slot));   // (an energy meter starts closed)
        if (cut != null) {
            changed();   // (the cable's conductors and links rebuilt round the device)
            // the cut-out cable back to the player: cable is neither made nor lost
            ItemStack back = CableKitItem.kit(old.tier, old.spec, cut.returned());
            if (!back.isEmpty() && player != null && !player.isCreative() && !player.getInventory().add(back)) dropItem(back);
        }
        return null;
    }

    /**
     * A device of this stack off centre exactly at (ox, oy, oz) on face, turned to front, in a slot past the faces even
     * at the face's centre (PipeSensorItem: its port must stay where it was put). Null if placed, else why not.
     */
    String placeOffCentre(ItemStack stack, Direction face, int ox, int oy, int oz, LRDU front, Player player) {
        int slot = freeSlot();
        if (slot < 0) return "No more devices fit in this block";
        String err = placeErrorAt(stack, slot, face, ox, oy, oz, front);
        if (err != null) return err;
        placingFront = front;
        try {
            return createSubBlockAt(stack, slot, face, ox, oy, oz, player) ? null : "Can't place that";
        } finally {
            placingFront = null;
        }
    }

    /** The front of the device being placed by placeAnywhere (its plan), else null: the player's facing. */
    private LRDU placingFront;

    @Override
    protected LRDU frontFor(mods.eln.node.six.SixNodeDescriptor d, Direction side, Player player) {
        return placingFront != null ? placingFront : super.frontFor(d, side, player);
    }

    /**
     * The face a device placed by a click into this block goes on: the face behind the clicked face, or, if the click is
     * on a device here, that device's face (AdventurAgent: clicking a chip's side aimed at the wall beside it).
     */
    public Direction placeFace(net.minecraft.core.Direction clicked, double hx, double hy, double hz) {
        int el = elementAt(hx, hy, hz, 0);
        if (el >= 0) return sideElementList[el].side;
        // on a cable: the face it lies on (AdventurAgent: a click on the end of a floor cable aimed at the open side
        // and did nothing); not a cable crossing the air, whose face is no surface (a device there would hang on nothing)
        int part = partAt(hx, hy, hz);
        if (part >= 0 && parts.get(part) instanceof CablePart c && !c.isKnuckle()
            && (coordonate.world() == null || CablePlacer.supported(coordonate.world(), coordonate.pos(), c)))
            return Direction.fromIntMinecraftSide(c.face);
        return Direction.fromIntMinecraftSide(clicked.get3DDataValue()).getInverse();
    }

    /** Where a device placed anywhere goes for a click at the block-local point: its centre's offset (world voxels), snapped
     *  to even voxels (pins on the routers' 2-voxel lattice: a cable can be laid to them) and kept with its body and pins
     *  (turned to front) on the face. */
    public static int[] snap(mods.eln.node.six.SixNodeDescriptor d, Direction face, LRDU front, double hx, double hy, double hz) {
        int n = CablePart.normalAxis(face.toEnumFacing().get3DDataValue());
        int[] ext = d.bodyExtents(face, front);
        int[] lo = new int[3], hi = new int[3];
        for (LRDU l : LRDU.values()) {
            int dv = face.applyLRDU(l).toEnumFacing().get3DDataValue(), a = CablePart.normalAxis(dv);
            if (dv % 2 == 1) hi[a] = ext[l.toInt()] + 1;
            else lo[a] = ext[l.toInt()] + 1;
        }
        double[] h = {hx, hy, hz};
        int[] o = new int[3];
        for (int a = 0; a < 3; a++)
            if (a != n) o[a] = Math.max(lo[a], Math.min(16 - hi[a], 2 * (int) Math.round(h[a] * 8))) - 8;
        return o;
    }

    /** Breaking by a player: the block's own break handles the last thing going. */
    boolean breakingByPlayer;

    @Override
    protected void afterSubBlockDeleted() {
        markUsed();
        setNeedPublish(true);
        // an element that went on its own (self-destruction) with nothing else left: the block goes too
        if (!breakingByPlayer && isEmptyNode() && !keepsCarving() && !isDestructing() && coordonate.world() != null) removeEmptyBlock();
    }

    /**
     * Join a new pipe to an existing one at a point on its centre line (a junction: Baughn, routes may join a run mid-way):
     * the existing pipe is cut there into two, and the new one may overlap them only around the junction.
     */
    public String addJoined(PipePart part, int x, int y, int z) {
        String err = joinError(part, x, y, z);
        if (err != null) return err;
        int target = -1;
        for (int i = 0; i < parts.size(); i++) if (parts.get(i) instanceof PipePart p && p.passes(x, y, z)) target = i;
        PipePart old = (PipePart) parts.get(target);
        PipePart[] halves = old.splitAt(x, y, z);
        mods.eln.sim.fluid.FluidNode saved = old.fluid;
        double T0 = mods.eln.sim.Ambient.kelvin(coordonate);
        // a junction at the point (Baughn 2026-11-11: a tee fitting with its own node), filled with its share of the pipe
        // it is cut into (by volume; the pieces only hold their run's contents after writeBackOf)
        JunctionPart j = junctionAt(x, y, z);
        boolean fresh = j == null;
        if (fresh) {
            j = new JunctionPart(x, y, z, Math.min(old.tier, part.tier));
            j.makeNode(coordonate.y, T0, null);
        }
        if (saved != null) mods.eln.fluid.FluidWorld.writeBackOf(saved);
        double sj = fresh && saved != null ? j.fluid.volume / (j.fluid.volume + saved.volume) : 0;
        if (fresh && saved != null) {
            j.fluid.setShareOf(saved, sj);
            j.fluid.update();
        }
        if (halves != null) {
            parts.remove(target);
            for (PipePart hp : halves) {
                double share = (1 - sj) * hp.length() / (double) Math.max(1, old.length());
                hp.makeNode(coordonate.y, T0, saved, share);
                parts.add(hp);
            }
        } else if (sj > 0) {
            saved.setShareOf(saved, 1 - sj);   // (joined at an existing end: that pipe gives the junction its share)
            saved.update();
        }
        if (part.fluid == null) part.makeNode(coordonate.y, T0, null);
        parts.add(part);
        if (fresh) parts.add(j);
        markUsed();
        changed();
        return null;
    }

    /** Why part can't join the pipe at (x, y, z), or null: addJoined's checks, also run before any of a joining route
     *  is laid (so a refused join lays nothing). */
    public String joinError(PipePart part, int x, int y, int z) {
        if (!part.valid()) return "not a straight-pieced pipe inside the block";
        boolean any = false;
        for (VoxelPart p : parts) any |= p instanceof PipePart pp && pp.passes(x, y, z);
        if (!any) return "no pipe there to join";
        if (fittingAt(x, y, z)) return "a valve or sensor sits there";
        // (a tee takes a third arm, a cross a fourth; no more)
        if (junctionAt(x, y, z) != null && armsAt(x, y, z).size() >= 4) return "a cross takes no more pipes";
        int h = PipePart.JOIN_CLEAR;
        BitSet want = new BitSet(4096);
        for (int[] b : part.boxes()) fill(want, b);
        BitSet around = new BitSet(4096);
        fill(around, new int[]{Math.max(0, x - h), Math.max(0, y - h), Math.max(0, z - h), Math.min(16, x + h), Math.min(16, y + h), Math.min(16, z + h)});
        want.andNot(around);
        if (want.intersects(used)) return "something is in the way";
        return null;
    }

    /** Is a valve, safety valve or pipe sensor at that point of a pipe? (A third pipe there would end at the valve and
     *  uncouple all three: its link needs exactly two pipe ends.) */
    boolean fittingAt(int x, int y, int z) {
        for (VoxelPart p : parts)
            if (p instanceof ValvePart v && v.x == x && v.y == y && v.z == z || p instanceof SafetyValvePart s && s.x == x && s.y == y && s.z == z
                || p instanceof PipeSensorPart s2 && s2.x == x && s2.y == y && s2.z == z) return true;
        return false;
    }

    /** A burst pipe vents its run. */
    @Override
    public List<Vent> fluidVents() {
        if (vents == null) {
            vents = new java.util.LinkedHashMap<>();
            for (VoxelPart p : parts)
                if (p instanceof PipePart pipe && pipe.burst && pipe.fluid != null) {
                    Vent v = new Vent(pipe.fluid, 0.5, PipePart.BURST_VENT_K);
                    v.open = 1;
                    vents.put(v, p);
                } else if (p instanceof SafetyValvePart sv && pipeUnder(sv) != null) {
                    PipePart under = pipeUnder(sv);
                    double draw = sv.outlet == 1 ? 1 : sv.outlet == 0 ? 0 : 0.5;   // (a valve on top lets out what's at the top)
                    Vent vent = new Vent(under.fluid, draw, SafetyValvePart.VENT_K);
                    vent.gasOnly = sv.purge;
                    vents.put(vent, p);
                }
        }
        return new ArrayList<>(vents.keySet());
    }

    /** The vents (burst pipes, safety valves) and their parts, kept while the parts don't change (the world sets each
     *  vent's edge, which the jets read). */
    private java.util.Map<Vent, VoxelPart> vents;

    /** The pipe stub behind an adapter (the pipe ending at the adapter's face, at the face's centre). */
    PipePart stubOf(AdapterPart a) {
        int[] s = AdapterPart.stub(a.face);
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null && pipe.passes(s[3], s[4], s[5])) return pipe;
        return null;
    }

    /** Adapters: what other mods' pipes put in or took out since the last step goes into or out of the run. */
    public void syncAdapters() {
        for (VoxelPart p : parts) {
            if (!(p instanceof AdapterPart a)) continue;
            PipePart stub = stubOf(a);
            if (stub != null) a.tanks.sync(mods.eln.fluid.FluidWorld.nodeFor(stub.fluid), mods.eln.sim.Ambient.kelvin(coordonate));   // (other mods' fluids: at the ambient)
        }
    }

    private void showAdapters() {
        for (VoxelPart p : parts) {
            if (!(p instanceof AdapterPart a)) continue;
            PipePart stub = stubOf(a);
            a.tanks.show(stub == null ? null : mods.eln.fluid.FluidWorld.nodeFor(stub.fluid));
        }
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncAdapters();
        // pipe walls
        double ambient = mods.eln.sim.Ambient.kelvin(coordonate);
        for (VoxelPart p : parts) if (p instanceof PipePart pipe) pipe.wallBefore(ambient);
        for (VoxelPart p : parts) if (p instanceof JunctionPart j) j.wallBefore(ambient);
        // valves: as far open as they are (a control valve by its signal, a throttle valve by its setting)
        if (valveLinks != null)
            for (var e : valveLinks.entrySet()) {
                ValvePart v = e.getValue();
                double o = v.opening();
                e.getKey().open = o;
                if (v.mode == ValvePart.PRESSURE) {
                    // (a regulator in the fluid core's solve, Network.Kind.REGULATOR: it answers within the step; an
                    // offset set from the step's starting pressures let a pipe just filled from a tank through by bars)
                    e.getKey().open = 1;
                    e.getKey().boost = 0;
                    e.getKey().setPa = v.setPoint();
                    e.getKey().relief = v.pmode == ValvePart.RELIEF;
                    // (its lift, for Jade and the drawing, from the pressures now)
                    v.boost(mods.eln.fluid.FluidWorld.nodeFor(e.getKey().a).P(), mods.eln.fluid.FluidWorld.nodeFor(e.getKey().b).P());
                    o = v.opening();
                }
                if (v.mode != ValvePart.ON_OFF && Math.abs(o - v.shown) > 0.02) {
                    v.shown = o;
                    setNeedPublish(true);   // (the drawing turns with it)
                }
            }
        // pipe sensors read the run under them (Baughn 2026-10-27)
        for (VoxelPart p : parts)
            if (p instanceof PipeSensorPart s && s.outProcess != null) {
                PipePart under = pipeAt(s.x, s.y, s.z);
                if (under == null) continue;
                s.read(mods.eln.fluid.FluidWorld.nodeFor(under.fluid));
                if (Math.abs(s.share() - s.shown) > 0.05) {
                    s.shown = s.share();
                    setNeedPublish(true);   // (the dial's colour)
                }
            }
        // safety valves open by the pressure under them
        if (vents != null)
            for (var e : vents.entrySet())
                if (e.getValue() instanceof SafetyValvePart sv) e.getKey().open = sv.follow(mods.eln.fluid.FluidWorld.nodeFor(e.getKey().node).P(), dt);
    }

    /** Over its rating a pipe bursts (the weakest of a run goes first: each piece checks its own rating against the
     *  run's pressure); over its working pressure it strains and hisses. */
    @Override
    public void afterFluidStep(double dt) {
        boolean changed = false;
        var level = coordonate.world();
        double ambient = mods.eln.sim.Ambient.kelvin(coordonate);
        for (VoxelPart p : parts) if (p instanceof PipePart pipe) pipe.wallAfter(ambient, dt);
        for (VoxelPart p : parts) if (p instanceof JunctionPart j) j.wallAfter(ambient, dt);
        for (VoxelPart p : parts) {
            if (!(p instanceof PipePart pipe) || pipe.burst || pipe.fluid == null) continue;
            double P = pipe.fluid.P(), T = pipe.fluid.T();
            // (a junction is no stronger than its arms: over their rating, an arm at it bursts; LabAgent's review)
            for (int[] e : pipe.innerEnds()) {
                JunctionPart j = junctionAt(e[0], e[1], e[2]);
                if (j != null && j.fluid != null && j.fluid.P() > P) {
                    P = j.fluid.P();
                    T = j.fluid.T();
                }
            }
            if (P > pipe.burstPa(T) && mods.eln.fluid.FluidWorld.claimBurst(pipe.fluid, level.getGameTime())) {
                pipe.burst = true;
                changed = true;
                if (level instanceof net.minecraft.server.level.ServerLevel sl0) {
                    int[] c = pipe.crackPoint();
                    pipe.burstDir = SteamJet.chooseDir(sl0, coordonate.pos(), c[3]);
                }
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    var c = net.minecraft.world.phys.Vec3.atCenterOf(coordonate.pos());
                    sl.playSound(null, coordonate.pos(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.6f);
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, c.x, c.y, c.z, 30, 0.3, 0.3, 0.3, 0.1);
                }
                mods.eln.misc.Utils.println("Pipe burst at " + coordonate + String.format(": %.1f bar, %.0f C", P / 1e5, T - 273.15));
            } else if (P > pipe.workingPa(T) && level instanceof net.minecraft.server.level.ServerLevel sl && sl.getGameTime() % 10 == 0) {
                var c = net.minecraft.world.phys.Vec3.atCenterOf(coordonate.pos());
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, c.x, c.y, c.z, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (changed) changed();
        showAdapters();
        // jets out of burst pipes
        if (vents != null && level instanceof net.minecraft.server.level.ServerLevel sl)
            for (var e : vents.entrySet())
                if (e.getValue() instanceof PipePart pipe && pipe.burstDir >= 0 && e.getKey().edge != null) {
                    int[] c = pipe.crackPoint();
                    var dir = net.minecraft.core.Direction.from3DDataValue(pipe.burstDir);
                    var origin = new net.minecraft.world.phys.Vec3(coordonate.x + c[0] / 16.0, coordonate.y + c[1] / 16.0, coordonate.z + c[2] / 16.0)
                        .add(dir.getStepX() * pipe.drawn() / 32.0, dir.getStepY() * pipe.drawn() / 32.0, dir.getStepZ() * pipe.drawn() / 32.0);
                    SteamJet.tick(sl, origin, dir, e.getKey().edge, SteamJet.MAX_LENGTH);
                } else if (e.getValue() instanceof SafetyValvePart sv && e.getKey().edge != null && e.getKey().open > 0) {
                    var dir = net.minecraft.core.Direction.from3DDataValue(sv.outlet);
                    int[] b = sv.boxes().get(0);
                    var origin = new net.minecraft.world.phys.Vec3(coordonate.x + (b[0] + b[3]) / 32.0, coordonate.y + (b[1] + b[4]) / 32.0,
                        coordonate.z + (b[2] + b[5]) / 32.0).add(dir.getStepX() * 0.125, dir.getStepY() * 0.125, dir.getStepZ() * 0.125);
                    double q = SteamJet.tick(sl, origin, dir, e.getKey().edge, 1.0);
                    // the whistle: steam only, pitch and loudness with the flow
                    if (q > 200 && SteamJet.gasShare(e.getKey().edge) > 0.5 && sl.getGameTime() % 4 == 0)
                        sl.playSound(null, coordonate.pos(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(), net.minecraft.sounds.SoundSource.BLOCKS,
                            (float) Math.min(2, 0.4 + q / 10000), (float) Math.min(2, 1.4 + q / 40000));
                }
    }

    /**
     * Cables: the conductors go into the simulation, then each port is linked to what's across its face: another voxel
     * node's cable ending at the same point, or a six-node or transparent device by EA's edge rule (a cable lying on
     * face A, ending on face F, takes the (F, edge F-A) connection a six-node cable on A would). No outer-corner
     * wrapping: a cable round an outer corner is a knuckle in the corner block.
     */
    @Override
    public void connectJob() {
        markUsed();   // (an off-centre device's pins follow its front, which a wrench turns)
        connectElements();   // (six-node elements hosted here: their loads and links inside the block)
        cables.build();
        cables.addToSimulation();
        connectElementsToCables();
        connectPins();
        pinsChanged();
        connectValves();
        connectSensors();
        cablesBuilt = !cables.conductors.isEmpty();
        for (Direction dir : Direction.values()) {
            mods.eln.node.NodeBase other = getNeighbor(dir);
            if (other == null || !other.isAdded()) continue;
            if (other instanceof VoxelNode v) {
                connectVoxel(dir, v);
                connectPinsAcross(dir, v);
            }
            else {
                // (legacy() gives no cable on an edge with several pins: connectPeerPins links those)
                for (LRDU lrdu : LRDU.values()) tryConnectTwoNode(this, dir, lrdu, other, dir.getInverse(), lrdu.inverseIfLR());
                // a bus port on the face toward a bus cable here (a machine at another mod's block): a bus link
                if (other instanceof mods.eln.bus.BusPeer peer && peer.busPortOn(dir.getInverse()) instanceof mods.eln.bus.BusPort bp)
                    for (LRDU lrdu : LRDU.values()) mods.eln.bus.BusNet.link(bp, busPortAt(dir, lrdu));
                connectPeerPins(dir, other);
            }
        }
        // only blocks with pipes in the fluid world (it steps every device every tick)
        if (hasFluid()) FluidWorld.add(this);
        else FluidWorld.remove(this);
    }

    private boolean hasFluid() {
        for (VoxelPart p : parts) if (!(p instanceof CablePart)) return true;
        return false;
    }

    /**
     * A connection point of this block (block-local voxels): a cable port, or an edge of a six-node element hosted here
     * (the midpoint of an edge of its face). out: the face it opens on (3D value); elSide/lrdu: the element's (or -1).
     */
    record End(int x, int y, int z, int out, int mask, ElectricalLoad load, ThermalLoad thermal, int elSide, LRDU lrdu,
               mods.eln.bus.BusPort bus) {
        boolean at(int[] p) {
            return x == p[0] && y == p[1] && z == p[2];
        }
    }

    List<End> ends() {
        List<End> out = new ArrayList<>();
        for (CableNet.Conductor k : cables.live())
            for (CablePart c : k.parts)
                for (CablePart.Port p : c.cablePorts()) out.add(new End(p.x(), p.y(), p.z(), p.face(), k.mask(), k.load, k.thermal, -1, null, k.port));
        for (int f = 0; f < 6; f++) {
            var e = sideElementList[f];
            if (e == null) continue;
            Direction fs = Direction.fromInt(f);
            int af = fs.toEnumFacing().get3DDataValue();
            var edgePins = e.edgePins();
            for (LRDU l : LRDU.values()) {
                int dv = fs.applyLRDU(l).toEnumFacing().get3DDataValue();
                int[] c = {8, 8, 8};
                c[CablePart.normalAxis(af)] = CablePart.plane(af);
                c[CablePart.normalAxis(dv)] = dv % 2 == 0 ? 0 : 16;
                boolean burnt = e.isBurnt();
                boolean pinned = false;
                // several terminals on this edge: each its own point along it (SixNodeElement.edgePins)
                for (var p : edgePins) {
                    if (p.lrdu() != l) continue;
                    pinned = true;
                    int along = fs.applyLRDU(l.left()).toEnumFacing().get3DDataValue();
                    int[] q = c.clone();
                    q[CablePart.normalAxis(along)] = 8 + (along % 2 == 0 ? -p.offset() : p.offset());
                    out.add(new End(q[0], q[1], q[2], dv, p.mask(), burnt ? null : p.load(), null, f, l,
                        p.mask() == maskRs485 ? e.busPort(l) : null));
                }
                if (!pinned)
                    out.add(new End(c[0], c[1], c[2], dv, e.getConnectionMask(l), burnt ? null : e.getElectricalLoad(l), burnt ? null : e.getThermalLoad(l), f, l,
                        e.busPort(l)));
            }
        }
        return out;
    }

    /** Every bus port in this block: its bus conductors' and its devices' (a disconnect detaches them all). */
    public List<mods.eln.bus.BusPort> busPorts() {
        List<mods.eln.bus.BusPort> out = cables.busPorts();
        for (var e : sideElementList)
            if (e != null) for (LRDU l : LRDU.values()) {
                var p = e.busPort(l);
                if (p != null && !out.contains(p)) out.add(p);
            }
        return out;
    }

    /** Links between hosted elements and cables in this block (made in connectJob). */
    private final List<mods.eln.sim.ElectricalConnection> elementLinksE = new ArrayList<>();
    private final List<mods.eln.sim.ThermalConnection> elementLinksT = new ArrayList<>();

    /**
     * A pin of a device placed off centre (a slot past the faces): its tip on the face plane (block voxels), the box of
     * voxels just outside it that a touching cable takes (1 deep, 2 wide, 1 high), the direction it points (3D value),
     * and the element's mask and loads toward that edge.
     */
    record Pin(int slot, LRDU lrdu, int[] tip, int[] box, int[] contact, int out, int mask, ElectricalLoad load, ThermalLoad thermal,
               mods.eln.bus.BusPort bus) {}

    /** The pins of the element in a slot past the faces (one per edge with a connection). */
    static List<Pin> pins(mods.eln.node.six.SixNodeElement e) {
        List<Pin> out = new ArrayList<>();
        for (LRDU l : LRDU.values()) {
            int mask = e.getConnectionMask(l);
            if (mask == 0) continue;
            int[][] g = pinGeometry(e.sixNodeElementDescriptor, e.side, e.front, e.ox, e.oy, e.oz, l);
            int dv = e.side.applyLRDU(l).toEnumFacing().get3DDataValue();
            boolean burnt = e.isBurnt();
            out.add(new Pin(e.slot, l, g[0], g[1], g[2], dv, mask, burnt ? null : e.getElectricalLoad(l), burnt ? null : e.getThermalLoad(l),
                e.busPort(l)));
        }
        return out;
    }

    static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }

    /** Do two pins meet tip to tip, pointing at each other? */
    static boolean meet(Pin p, Pin q) {
        return java.util.Arrays.equals(p.tip, q.tip) && q.out == (p.out ^ 1);
    }

    private void link(ElectricalLoad a, ElectricalLoad b, ThermalLoad ta, ThermalLoad tb) {
        if (a != null && b != null) {
            var e = new mods.eln.sim.ElectricalConnection(a, b);
            Eln.simulator.addElectricalComponent(e);
            elementLinksE.add(e);
        }
        if (ta != null && tb != null) {
            var t = new mods.eln.sim.ThermalConnection(ta, tb);
            Eln.simulator.addThermalConnection(t);
            elementLinksT.add(t);
        }
    }

    /** Devices placed off centre: a pin touching a cable (the cable takes a voxel just past its tip) or another device's
     *  pin tip to tip is connected to it (Baughn: chips whose pins touch are connected). */
    private void connectPins() {
        List<Pin> all = new ArrayList<>();
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] != null) all.addAll(pins(sideElementList[i]));
        for (Pin p : all) {
            for (CableNet.Conductor k : cables.live()) {
                if (!compareConnectionMask(p.mask, k.mask())) continue;
                boolean touch = false;
                for (CablePart c : k.parts) for (int[] b : c.boxes()) touch |= overlaps(b, p.contact);
                if (!touch) continue;
                sideElementList[p.slot].slotMask.set(p.lrdu, true);
                link(p.load, k.load, p.thermal, k.thermal);
                mods.eln.bus.BusNet.link(p.bus, k.port);
            }
            for (Pin q : all) {
                if (q.slot <= p.slot || !meet(p, q) || !compareConnectionMask(p.mask, q.mask)) continue;
                sideElementList[p.slot].slotMask.set(p.lrdu, true);
                sideElementList[q.slot].slotMask.set(q.lrdu, true);
                link(p.load, q.load, p.thermal, q.thermal);
                mods.eln.bus.BusNet.link(p.bus, q.bus);
            }
        }
    }

    /** Each off-centre device's pins as last published (slot * 4 + edge bits), to see them change. */
    private long publishedPins = -1;

    /**
     * Pins that come and go after placement (a Hub's with the cables in its slots, a Temperature Probe's back pin with
     * its slot): the clients get them (AdventurAgent: a Hub given cables showed no pins), and a pin that now lands on
     * something is reported in the log. Its links follow its kind in connectPins either way.
     */
    private void pinsChanged() {
        long sig = 0;
        for (int i = 6; i < SLOTS; i++) {
            var e = sideElementList[i];
            if (e == null) continue;
            int m = 0;
            for (LRDU l : LRDU.values()) if (e.getConnectionMask(l) != 0) m |= 1 << l.toInt();
            sig = sig * 31 + i * 16L + m;
        }
        if (sig == publishedPins) return;
        boolean first = publishedPins == -1;
        publishedPins = sig;
        setNeedPublish(true);
        if (first) return;
        for (int i = 6; i < SLOTS; i++) {
            var e = sideElementList[i];
            if (e == null) continue;
            String err = turnError(e, e.front);
            if (err != null) Eln.LOG.info("Electrical Age: {} at {} has pins in the way now: {}", e.sixNodeElementDescriptor.name, coordonate, err);
        }
    }

    /**
     * Control valves (Baughn 2026-10-26): the signal input in the simulation, joined to the signal cables laid in this
     * block (cables never run in mid-air, so not to the actuator itself: AdventurAgent; a cable kit clicked on the valve
     * lays to the floor under it, CablePlacer).
     */
    private void connectValves() {
        for (VoxelPart p : parts) {
            if (!(p instanceof ValvePart v) || v.signal == null) continue;
            Eln.simulator.addElectricalLoad(v.signal);
            for (CableNet.Conductor k : cables.live())
                if (k.load != null && k.d != null && k.d.signalWire) link(v.signal, k.load, null, null);
        }
    }

    /** Pipe sensors: their signal outputs in the simulation, joined to the signal cables laid in this block (as valves). */
    private void connectSensors() {
        for (VoxelPart p : parts) {
            if (!(p instanceof PipeSensorPart s) || s.out == null) continue;
            Eln.simulator.addElectricalLoad(s.out);
            Eln.simulator.addElectricalComponent(s.outProcess);
            for (CableNet.Conductor k : cables.live())
                if (k.load != null && k.d != null && k.d.signalWire) link(s.out, k.load, null, null);
        }
    }

    /** Hosted elements and cable ends meeting at a point in this block (on the element's face, or an inner corner). */
    private void connectElementsToCables() {
        elementLinksE.clear();
        elementLinksT.clear();
        List<End> all = ends();
        for (End a : all) {
            if (a.elSide < 0) continue;
            for (End b : all) {
                if (b.elSide >= 0 || !a.at(new int[]{b.x, b.y, b.z}) || !compareConnectionMask(a.mask, b.mask)) continue;
                lrduElementMask.set(Direction.fromInt(a.elSide), a.lrdu, true);
                mods.eln.bus.BusNet.link(a.bus, b.bus);
                if (a.load == null || b.load == null) continue;
                var e = new mods.eln.sim.ElectricalConnection(a.load, b.load);
                Eln.simulator.addElectricalComponent(e);
                elementLinksE.add(e);
                if (a.thermal != null && b.thermal != null) {
                    var t = new mods.eln.sim.ThermalConnection(a.thermal, b.thermal);
                    Eln.simulator.addThermalConnection(t);
                    elementLinksT.add(t);
                }
            }
        }
    }

    /** A box moved by one block toward dir (block voxels: into the neighbour's coordinates, seen from there, it's -16). */
    static int[] shifted(int[] b, Direction dir, int sign) {
        int f = dir.toEnumFacing().get3DDataValue(), a = CablePart.normalAxis(f), d = (f % 2 == 1 ? -16 : 16) * sign;
        int[] r = b.clone();
        if (r.length == 6) {
            r[a] += d;
            r[a + 3] += d;
        } else r[a] += d;
        return r;
    }

    /**
     * Pins on the face toward a neighbouring voxel block (a device off centre at the block's edge): a pin touching a
     * cable there (its contact voxel in that block), or meeting a device's pin there tip to tip, either way round.
     */
    private void connectPinsAcross(Direction dir, VoxelNode v) {
        List<Pin> mine = new ArrayList<>(), theirs = new ArrayList<>();
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] != null) mine.addAll(pins(sideElementList[i]));
        for (int i = 6; i < SLOTS; i++) if (v.sideElementList[i] != null) theirs.addAll(pins(v.sideElementList[i]));
        if (mine.isEmpty() && theirs.isEmpty()) return;
        // my pins against their cables, theirs against mine (contact boxes in the other block's coordinates)
        for (Pin p : mine) pinToCables(dir, this, p, v, shifted(p.contact, dir, 1));
        for (Pin q : theirs) pinToCables(dir.getInverse(), v, q, this, shifted(q.contact, dir, -1));
        for (Pin p : mine) {
            int[] tip = shifted(p.tip, dir, 1);
            for (Pin q : theirs) {
                if (!java.util.Arrays.equals(tip, q.tip) || q.out != (p.out ^ 1) || !compareConnectionMask(p.mask, q.mask)) continue;
                sideElementList[p.slot].slotMask.set(p.lrdu, true);
                v.sideElementList[q.slot].slotMask.set(q.lrdu, true);
                if (p.bus != null && q.bus != null) mods.eln.bus.BusNet.link(p.bus, q.bus);
                else across(dir, v, p.load, q.load, p.thermal, q.thermal);
            }
        }
    }

    private void pinToCables(Direction dir, VoxelNode a, Pin p, VoxelNode b, int[] contact) {
        for (CableNet.Conductor k : b.cables.live()) {
            if (!compareConnectionMask(p.mask, k.mask())) continue;
            boolean touch = false;
            for (CablePart c : k.parts) for (int[] bx : c.boxes()) touch |= overlaps(bx, contact);
            if (!touch) continue;
            a.sideElementList[p.slot].slotMask.set(p.lrdu, true);
            // a bus pin on a bus cable: a bus link, not a NodeConnection (that would merge fault networks)
            if (p.bus != null && k.port != null) mods.eln.bus.BusNet.link(p.bus, k.port);
            else if (a == this) across(dir, b, p.load, k.load, p.thermal, k.thermal);
            else across(dir.getInverse(), a, k.load, p.load, k.thermal, p.thermal);
        }
    }

    /** A link to the neighbour v toward dir, kept by both nodes (dropped when either disconnects). */
    private void across(Direction dir, VoxelNode v, ElectricalLoad a, ElectricalLoad b, ThermalLoad ta, ThermalLoad tb) {
        mods.eln.sim.ElectricalConnection e = null;
        mods.eln.sim.ThermalConnection t = null;
        if (a != null && b != null) {
            e = new mods.eln.sim.ElectricalConnection(a, b);
            Eln.simulator.addElectricalComponent(e);
        }
        if (ta != null && tb != null) {
            t = new mods.eln.sim.ThermalConnection(ta, tb);
            Eln.simulator.addThermalConnection(t);
        }
        var c = new mods.eln.node.NodeConnection(this, dir, LRDU.Up, v, dir.getInverse(), LRDU.Up, e, t);
        nodeConnectionList.add(c);
        v.nodeConnectionList.add(c);
        v.setNeedPublish(true);
    }

    /**
     * Across a face to another voxel block: every end meeting one of its ends at the same point; then cables whose bodies
     * touch across the face though their ends don't meet (connectTouching).
     */
    private void connectVoxel(Direction dir, VoxelNode v) {
        int f = dir.toEnumFacing().get3DDataValue();
        List<End> theirs = v.ends();
        java.util.Set<java.util.List<Object>> joined = new java.util.HashSet<>();
        for (End a : ends()) {
            if (a.out != f) continue;
            CablePart.Port m = new CablePart.Port(a.out, a.x, a.y, a.z).mirrored();
            for (End b : theirs) {
                if (b.out != m.face() || b.x != m.x() || b.y != m.y() || b.z != m.z() || !compareConnectionMask(a.mask, b.mask)) continue;
                if (a.elSide >= 0) lrduElementMask.set(Direction.fromInt(a.elSide), a.lrdu, true);
                if (b.elSide >= 0) v.lrduElementMask.set(Direction.fromInt(b.elSide), b.lrdu, true);
                if (a.bus != null && b.bus != null) {   // (bus links stay out of NodeConnections: fault networks)
                    mods.eln.bus.BusNet.link(a.bus, b.bus);
                    v.setNeedPublish(true);
                    continue;
                }
                mods.eln.sim.ElectricalConnection e = null;
                mods.eln.sim.ThermalConnection t = null;
                if (a.load != null && b.load != null) joined.add(java.util.List.of(a.load, b.load));
                if (a.thermal != null && b.thermal != null) joined.add(java.util.List.of(a.thermal, b.thermal));
                if (a.bus != null && b.bus != null) joined.add(java.util.List.of(a.bus, b.bus));
                if (a.load != null && b.load != null) {
                    e = new mods.eln.sim.ElectricalConnection(a.load, b.load);
                    Eln.simulator.addElectricalComponent(e);
                }
                if (a.thermal != null && b.thermal != null) {
                    t = new mods.eln.sim.ThermalConnection(a.thermal, b.thermal);
                    Eln.simulator.addThermalConnection(t);
                }
                var c = new mods.eln.node.NodeConnection(this, dir, LRDU.Up, v, dir.getInverse(), LRDU.Up, e, t);
                nodeConnectionList.add(c);
                v.nodeConnectionList.add(c);
                v.setNeedPublish(true);
            }
        }
        connectTouching(dir, f, v, joined);
    }

    /**
     * Cables in two voxel blocks whose bodies touch across the face between them, with an area of contact, though no
     * end meets an end there: an inner corner falling on the blocks' boundary (Baughn 2026-11-08: a cable along a
     * ceiling, up a shaft chiselled through the block above, drawn connected but carrying nothing; the ceiling piece
     * ends at its centre line, the shaft's piece lies on its wall's plane, a voxel apart). As one block's touching
     * conductors are joined (CableNet), these are.
     */
    private void connectTouching(Direction dir, int f, VoxelNode v, java.util.Set<java.util.List<Object>> joined) {
        int[] d = CableRouter.DV[f];
        int axis = d[0] != 0 ? 0 : d[1] != 0 ? 1 : 2, plane = d[axis] > 0 ? 16 : 0;
        for (CableNet.Conductor ka : cables.live())
            for (CableNet.Conductor kb : v.cables.live()) {
                if (ka.isBurntNow() || kb.isBurntNow() || !compareConnectionMask(ka.mask(), kb.mask())) continue;
                if (ka.load != null && kb.load != null && joined.contains(java.util.List.of(ka.load, kb.load))) continue;
                if (ka.thermal != null && kb.thermal != null && joined.contains(java.util.List.of(ka.thermal, kb.thermal))) continue;
                if (ka.port != null && kb.port != null && joined.contains(java.util.List.of(ka.port, kb.port))) continue;
                if (!touchAcross(ka, kb, axis, plane)) continue;
                if (ka.port != null && kb.port != null) {
                    mods.eln.bus.BusNet.link(ka.port, kb.port);
                    v.setNeedPublish(true);
                    continue;
                }
                mods.eln.sim.ElectricalConnection e = null;
                mods.eln.sim.ThermalConnection t = null;
                if (ka.load != null && kb.load != null) {
                    e = new mods.eln.sim.ElectricalConnection(ka.load, kb.load);
                    Eln.simulator.addElectricalComponent(e);
                }
                if (ka.thermal != null && kb.thermal != null) {
                    t = new mods.eln.sim.ThermalConnection(ka.thermal, kb.thermal);
                    Eln.simulator.addThermalConnection(t);
                }
                if (e == null && t == null) continue;
                var c = new mods.eln.node.NodeConnection(this, dir, LRDU.Up, v, dir.getInverse(), LRDU.Up, e, t);
                nodeConnectionList.add(c);
                v.nodeConnectionList.add(c);
                v.setNeedPublish(true);
            }
    }

    /** Do a box of ka's touching the boundary plane (axis, plane) and one of kb's (on the other side) overlap there? */
    static boolean touchAcross(CableNet.Conductor ka, CableNet.Conductor kb, int axis, int plane) {
        int other = 16 - plane;
        for (CablePart p : ka.parts)
            for (int[] a : p.boxes()) {
                if ((plane == 16 ? a[axis + 3] : a[axis]) != plane) continue;
                for (CablePart q : kb.parts)
                    for (int[] b : q.boxes()) {
                        if ((other == 16 ? b[axis + 3] : b[axis]) != other) continue;
                        boolean over = true;
                        for (int k = 0; k < 3 && over; k++)
                            if (k != axis) over = Math.min(a[k + 3], b[k + 3]) > Math.max(a[k], b[k]);
                        if (over) return true;
                    }
            }
        return false;
    }

    /** (Element stubs are set by the connections themselves; a neighbour dropping its links reconnects at once.) */
    @Override
    public void externalDisconnect(Direction side, LRDU lrdu) {
    }

    /** A legacy neighbour linked to (side, edge): an element's stub, if it's an element's. */
    @Override
    public void newConnectionAt(Direction side, LRDU lrdu) {
        if (sideElementList[side.applyLRDU(lrdu).getInt()] != null) super.newConnectionAt(side, lrdu);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        for (VoxelPart p : parts) if (p instanceof ValvePart v && v.signal != null) Eln.simulator.removeElectricalLoad(v.signal);
        for (VoxelPart p : parts)
            if (p instanceof PipeSensorPart s && s.out != null) {
                Eln.simulator.removeElectricalComponent(s.outProcess);
                Eln.simulator.removeElectricalLoad(s.out);
            }
        for (var e : elementLinksE) Eln.simulator.removeElectricalComponent(e);
        for (var t : elementLinksT) Eln.simulator.removeThermalConnection(t);
        elementLinksE.clear();
        elementLinksT.clear();
        cables.removeFromSimulation();
        mods.eln.bus.BusNet.detach(busPorts());
        FluidWorld.remove(this);
    }

    /** The cable conductor taking EA's (side, edge) connection, or null: see connectJob. Not for voxel neighbours
     *  (exact points) nor through air (no outer-corner wraps). */
    private CableNet.Conductor legacy(Direction side, LRDU lrdu) {
        mods.eln.node.NodeBase other = getNeighbor(side);
        if (other == null || other instanceof VoxelNode) return null;
        int f = side.toEnumFacing().get3DDataValue(), a = side.applyLRDU(lrdu).toEnumFacing().get3DDataValue();
        if (other instanceof mods.eln.node.FacePins fp)   // an edge with several pins: only by pin (connectPeerPins)
            for (var pin : fp.facePins(side.getInverse()))
                if (side.getInverse().applyLRDU(pin.lrdu()).toEnumFacing().get3DDataValue() == a) return null;
        for (CableNet.Conductor k : cables.live())
            for (CablePart c : k.parts) {
                List<CablePart.Port> ps = c.cablePorts();
                for (CablePart.Port p : ps) {
                    if (p.face() != f) continue;
                    // a cable lying on a; a knuckle whose other side opens on a (the cable round the corner lies on it)
                    if (c.isKnuckle() ? ps.stream().anyMatch(q -> q.face() == a) : k.face == a) return k;
                }
            }
        return null;
    }

    /**
     * Links the pins of the neighbour toward dir (FacePins) to the cables here that end at them: a cable lying on the
     * pin's edge (the floor or ceiling here), with an end on the face toward the neighbour at the pin's point along the
     * edge (8 voxels plus its leftward, toward the left of someone here facing the neighbour). A bus pin takes a bus
     * link, the others an electrical one (kept by both nodes, dropped when either disconnects; never twice).
     */
    public void connectPeerPins(Direction dir, mods.eln.node.NodeBase other) {
        if (!(other instanceof mods.eln.node.FacePins fp) || isDestructing()) return;
        var fwd = dir.toEnumFacing();
        if (fwd.getAxis().isVertical()) return;   // (positions are on side faces)
        int lx = fwd.getStepZ(), lz = -fwd.getStepX();   // (left = up x forward)
        int f = fwd.get3DDataValue();
        for (var pin : fp.facePins(dir.getInverse())) {
            int a = dir.getInverse().applyLRDU(pin.lrdu()).toEnumFacing().get3DDataValue();
            int along = 8 + pin.leftward() * (lx != 0 ? lx : lz);
            for (CableNet.Conductor k : cables.live()) {
                if (k.face != a) continue;
                boolean ends = false;
                for (CablePart c : k.parts)
                    for (CablePart.Port p : c.cablePorts())
                        ends |= !c.isKnuckle() && p.face() == f && (lx != 0 ? p.x() : p.z()) == along;
                if (!ends) continue;
                if (pin.bus() != null) {
                    if (k.port != null) mods.eln.bus.BusNet.link(pin.bus(), k.port);
                    continue;
                }
                if (k.load == null || pin.load() == null || !compareConnectionMask(pin.mask(), k.mask())) continue;
                boolean already = false;
                for (var c : nodeConnectionList) already |= c.electrical() != null && c.electrical().joins(k.load, pin.load());
                if (already) continue;
                var e = new mods.eln.sim.ElectricalConnection(k.load, pin.load());
                Eln.simulator.addElectricalComponent(e);
                var c = new mods.eln.node.NodeConnection(this, dir, LRDU.Up, other, dir.getInverse(), pin.lrdu(), e, null);
                nodeConnectionList.add(c);
                other.nodeConnectionList.add(c);
                other.setNeedPublish(true);
            }
        }
    }

    /** The bus port of the cable taking EA's (side, edge) connection toward a non-voxel neighbour (see legacy), or null. */
    public mods.eln.bus.BusPort busPortAt(Direction side, LRDU lrdu) {
        CableNet.Conductor k = legacy(side, lrdu);
        return k == null ? null : k.port;
    }

    /** The working cable conductor with a piece taking voxel (x, y, z): its electrical load (index 0) and thermal load
     *  (index 1), either null; null if none (tests). */
    public Object[] cableAt(int x, int y, int z) {
        int[] v = {x, y, z, x + 1, y + 1, z + 1};
        for (CableNet.Conductor k : cables.live())
            for (CablePart c : k.parts)
                for (int[] b : c.boxes()) if (overlaps(b, v)) return new Object[]{k.load, k.thermal};
        return null;
    }

    /** The pins of the device off centre in a slot: {LRDU int, tip x, y, z} each (tests). */
    public List<int[]> pinTips(int slot) {
        var e = sideElementList[slot];
        List<int[]> out = new ArrayList<>();
        if (slot < 6 || e == null) return out;
        for (Pin p : pins(e)) out.add(new int[]{p.lrdu().toInt(), p.tip()[0], p.tip()[1], p.tip()[2]});
        return out;
    }

    /** Each working cable conductor's voltage, current and temperature (C above ambient; NaN without one): tests. */
    public List<double[]> cableStates() {
        List<double[]> out = new ArrayList<>();
        for (CableNet.Conductor k : cables.live())
            out.add(new double[]{k.load == null ? Double.NaN : k.load.getU(), k.load == null ? Double.NaN : k.load.getI(), k.thermal == null ? Double.NaN : k.thermal.Tc});
        return out;
    }

    /** The load of the working cable conductor lying on face (3D value), or null. */
    public ElectricalLoad cableLoad(int face) {
        for (CableNet.Conductor k : cables.live()) if (k.face == face) return k.load;
        return null;
    }

    /** The thermal load of the working cable conductor lying on face (3D value), or null. */
    public ThermalLoad cableThermal(int face) {
        for (CableNet.Conductor k : cables.live()) if (k.face == face) return k.thermal;
        return null;
    }

    /** Has any cable here burnt out? */
    public boolean cableBurnt() {
        for (VoxelPart p : parts) if (p instanceof CablePart c && c.burnt != null) return true;
        return false;
    }

    /** The cables' watchdogs (FaultNetwork). */
    public List<mods.eln.sim.IProcess> slowProcesses() {
        return cables.slowProcesses();
    }

    // ---- node

    @Override
    public void initializeFromThat(Direction front, LivingEntity entityLiving, ItemStack itemStack) {
        connect();
    }

    @Override
    public void initializeFromNBT() {
        connect();
        showAdapters();
        invalidateCapabilities();
    }

    @Override
    public void onBreakBlock() {
        for (VoxelPart p : parts) if (!partItem(p).isEmpty()) dropItem(partItem(p));
        parts.clear();
        super.onBreakBlock();
        invalidateCapabilities();
    }

    @Override
    public void unload() {
        super.unload();
        invalidateCapabilities();
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrdu) {
        int m = super.getSideConnectionMask(side, lrdu);   // (an element on that face)
        if (m != 0) return m;
        CableNet.Conductor k = legacy(side, lrdu);
        return k == null ? 0 : k.mask();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        if (sideElementList[side.applyLRDU(lrdu).getInt()] != null) return super.getThermalLoad(side, lrdu);
        CableNet.Conductor k = legacy(side, lrdu);
        return k == null ? null : k.thermal;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (sideElementList[side.applyLRDU(lrdu).getInt()] != null) return super.getElectricalLoad(side, lrdu);
        CableNet.Conductor k = legacy(side, lrdu);
        return k == null ? null : k.load;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        // (other mods' pipes may have used an adapter since the last step: into the run, and the run into its pieces)
        if (isAdded()) {
            cables.save();
            syncAdapters();
            for (VoxelPart p : parts) if (p instanceof AdapterPart a && stubOf(a) != null) mods.eln.fluid.FluidWorld.writeBackOf(stubOf(a).fluid);
            showAdapters();
        }
        super.writeToNBT(nbt);
        // (and the node's own data at the top level too, as before voxel nodes hosted elements: a world saved by this
        // version still loads in the previous one, without the hosted elements)
        CompoundTag node = nbt.getCompoundOrEmpty("node");
        for (String k : node.keySet()) if (!nbt.contains(k)) nbt.put(k, node.get(k).copy());
        if (legacy != null) nbt.put("legacy", legacy);
        if (host != null) nbt.put("host", net.minecraft.nbt.NbtUtils.writeBlockState(host));
        if (carved != null && !carved.isEmpty()) nbt.putLongArray("carved", carved.toLongArray());
        nbt.putInt("parts", parts.size());
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag t = new CompoundTag();
            t.putString("kind", parts.get(i).kind());
            parts.get(i).write(t);
            nbt.put("p" + i, t);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        // Voxel nodes saved before they hosted elements have the node's own data at the top level, not under "node".
        boolean six = nbt.contains("node");
        coordonate.readFromNBT(six ? nbt.getCompoundOrEmpty("node") : nbt, "c");
        parts.clear();
        legacy = nbt.getCompound("legacy").orElse(null);
        host = nbt.getCompound("host").map(t -> net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK, t)).orElse(null);
        carved = nbt.getLongArray("carved").map(BitSet::valueOf).orElse(null);
        int n = nbt.getIntOr("parts", 0);
        for (int i = 0; i < n; i++) {
            VoxelPart p = VoxelPart.read(nbt.getCompoundOrEmpty("p" + i), coordonate.y);
            if (p != null) parts.add(p);
        }
        if (six) super.readFromNBT(nbt);
        else {
            CompoundTag wrap = new CompoundTag();
            wrap.put("node", nbt);
            super.readFromNBT(wrap);
        }
        markUsed();
    }

    /** Client data: the part count, then each part's kind and length-prefixed bytes (an unknown kind is skipped), then a
     *  glow step byte per part (Incandescence); then SixNode's elements. */
    @Override
    protected void publishBeforeElements(DataOutputStream stream) throws IOException {
        {
            stream.writeInt(host == null ? 0 : net.minecraft.world.level.block.Block.getId(host));
            byte[] cv = carved == null ? new byte[0] : carved.toByteArray();
            stream.writeShort(cv.length);
            stream.write(cv);
            stream.writeShort(parts.size());
            for (VoxelPart p : parts) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                p.serialize(new DataOutputStream(b));
                stream.writeUTF(p.kind());
                stream.writeShort(b.size());
                stream.write(b.toByteArray());
            }
            // the thermal cables' glow, one step byte per part (Incandescence; not in the parts' bytes: no re-mesh)
            stream.write(cables.glowSteps(parts));
        }
    }

    /** Each part's glow step as published (0: dark), for tests. */
    public byte[] glowSteps() {
        return cables.glowSteps(parts);
    }

    /** The block's light: its elements' and its hot thermal cables' (Incandescence). */
    @Override
    public void recalculateLightValue() {
        int light = cables.glowLight();
        for (mods.eln.node.six.SixNodeElement e : sideElementList) if (e != null && !e.isBurnt()) light = Math.max(light, e.getLightValue());
        setLightValue(light);
    }

    /** For the probe (Jade): a headline, and with details the run's contents, ratings and fittings. */
    public java.util.LinkedHashMap<String, String> getWaila(boolean details) {
        return getWaila(details, -1);
    }

    /**
     * The lines for the part under the crosshair (its index in parts, or -1): that pipe, the pipe a fitting sits on, the
     * cables if it is a cable (Baughn 2026-10-28: with several pipes in a block, Jade showed one, not the one looked at);
     * with none, a burst pipe, else the first pipe, else the cables.
     */
    public java.util.LinkedHashMap<String, String> getWaila(boolean details, int hit) {
        var out = new java.util.LinkedHashMap<String, String>();
        VoxelPart aimed = hit >= 0 && hit < parts.size() ? parts.get(hit) : null;
        PipePart pipe = pipeOf(hit);
        if (pipe == null && !(aimed instanceof CablePart))
            for (VoxelPart p : parts) if (p instanceof PipePart pp && pp.fluid != null && (pipe == null || pp.burst)) pipe = pp;
        if (pipe == null) {
            // cables: each conductor's tier, voltage and current (temperature with details)
            int n = 0;
            // (looking at a cable: only its own conductor, Baughn 2026-10-28; else all of the block's)
            CableNet.Conductor only = conductorOf(hit);
            for (CableNet.Conductor k : cables.conductors) {
                if (!k.valid() || (only != null ? k != only : k.face < 0)) continue;
                String key = n++ == 0 ? "Cable" : "Cable " + n;
                if (k.isBurntNow()) {
                    out.put(key, k.name() + ", BURNT");
                    continue;
                }
                if (k.port != null) {   // the field bus: its channels in use, by number and name (Baughn: colourblind)
                    var b = k.port.bus();
                    if (b == null) {
                        out.put("Bus", "no devices");   // (Jade's title already says Field Bus Cable, Baughn)
                        continue;
                    }
                    int used = 0;
                    for (int c = 0; c < mods.eln.bus.BusChannel.COUNT; c++) if (b.inUse(c)) used++;
                    out.put("Bus", used + (used == 1 ? " channel in use, " : " channels in use, ") + b.portCount() + " ports");
                    for (int c = 0; c < mods.eln.bus.BusChannel.COUNT; c++) {
                        if (!b.inUse(c)) continue;
                        int w = b.writers(c);
                        out.put("  " + mods.eln.bus.BusChannel.label(c), mods.eln.misc.Utils.plotVolt("", b.value(c) * mods.eln.Eln.SVU)
                            + (w > 1 ? " from " + w + " writers" : w == 0 ? " unwritten" : ""));
                    }
                    var addresses = b.addresses();   // devices by address, duplicates flagged
                    if (!addresses.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        addresses.forEach((a, many) -> sb.append(sb.length() == 0 ? "" : ", ").append(a).append(many > 1 ? " (" + many + "x, DUPLICATE)" : ""));
                        out.put("Devices", sb.toString());
                    }
                    if (b.delivered() > 0 || b.droppedTotal() > 0)   // packets (Baughn: an RS485 bus)
                        out.put("Packets", b.delivered() + " this tick" + (b.droppedTotal() > 0
                            ? ", " + b.droppedTotal() + " dropped (over " + mods.eln.bus.Bus.CAP + " a tick)" : ""));
                    continue;
                }
                if (k.load == null) {   // (the thermal cable)
                    out.put(key, k.name() + ": " + mods.eln.misc.Utils.plotCelsius("", k.thermal.Tc) + " " + mods.eln.misc.Utils.plotPower("", k.thermal.getPower()));
                    continue;
                }
                out.put(key, k.name() + ": " + mods.eln.misc.Utils.plotVolt("", k.load.getU()) + " " + mods.eln.misc.Utils.plotAmpere("", k.load.getI()));
                if (details && k.thermal != null) out.put(key + " temperature", mods.eln.misc.Utils.plotCelsius("", k.thermal.Tc));
            }
            return out;
        }
        var n = mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid);
        out.put("Pipe", PipePart.TIER_NAME[pipe.tier] + (pipe.burst ? ", BURST" : ""));
        // (Baughn: say what the pressure is of; steam in grams)
        out.put("Pressure", String.format("%.2f bar of %s, %.0f °C", n.P() / 1e5, EaUnits.contents(n), n.T() - 273.15));
        out.put("Contents", EaUnits.composition(n));
        for (VoxelPart p : parts)
            if (p instanceof ValvePart v) out.put(switch (v.mode) {
                case ValvePart.CONTROL -> "Control valve";
                case ValvePart.THROTTLE -> "Throttle valve";
                case ValvePart.PRESSURE -> "Pressure valve";
                default -> "Valve";
            }, v.mode == ValvePart.ON_OFF ? (v.open ? "open" : "shut")
                : v.mode == ValvePart.PRESSURE ? String.format("%s %.1f bar%s, %.0f%% open", v.pmode == ValvePart.RELIEF ? "holds before it at" : "holds after it at",
                    v.setPoint() / 1e5, v.signalled() ? " by signal" : "", v.opening() * 100)
                : String.format("%.0f%% open", v.opening() * 100));
        for (VoxelPart p : parts)
            if (p instanceof SafetyValvePart sv) {
                String name = sv.purge ? "Purge valve" : "Safety valve";
                out.put(out.containsKey(name) ? name + " " + (out.size()) : name,
                    String.format(sv.purge ? "%.1f bar%s" : "%.0f bar%s", sv.setPa / 1e5, sv.opening(n.P()) <= 0 ? ""
                        : sv.purge && n.level() >= 0.999 ? ", shut: liquid at it" : sv.purge ? ", venting gas" : ", blowing"));
            }
        for (VoxelPart p : parts)
            if (p instanceof PipeSensorPart s) out.put(s.name(), String.format(s.sensing == PipeSensorPart.PRESSURE ? "%.2f %s (absolute) of %.0f: %.1f V" : "%.0f %s of %.0f: %.1f V",
                s.reading, s.unit(), s.scale, s.share() * mods.eln.Eln.SVU));
        if (!details) return out;
        out.put("Liquid", String.format("%.0f mB", n.liquidVolume() * mods.eln.sim.fluid.Water.RHO_L / EaUnits.KG_PER_MB_WATER));
        out.put("Run volume", String.format("%.2f L", n.volume * 1000));
        out.put("Rating", String.format("works to %.0f bar, bursts at %.0f", pipe.workingPa(n.T()) / 1e5, pipe.burstPa(n.T()) / 1e5));
        for (VoxelPart p : parts) if (p instanceof AdapterPart a) out.put("Adapter", net.minecraft.core.Direction.from3DDataValue(a.face).getName());
        return out;
    }

    /** The multimeter on a part: its cable's voltage and current, or its pipe run's pressure and contents (the run's
     *  lump: the piece's own node is only written back after each step). */
    public String multiMeterString(int part) {
        CableNet.Conductor k = conductorOf(part);
        if (k != null && k.load != null)
            return k.d.signalWire ? mods.eln.misc.Utils.plotSignal(k.load.getU(), k.load.getI()) : mods.eln.misc.Utils.plotUIP(k.load.getU(), k.load.getI());
        PipePart pipe = pipeOf(part);
        if (pipe == null) return "";
        var n = mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid);
        return String.format("%.2f bar of %s  %s%s  ", n.P() / 1e5, EaUnits.contents(n), EaUnits.composition(n), pipe.burst ? "  BURST" : "");
    }

    /** The thermometer on a part: its cable's or its pipe run's temperature. */
    public String thermoMeterString(int part) {
        CableNet.Conductor k = conductorOf(part);
        if (k != null && k.thermal != null) return mods.eln.misc.Utils.plotCelsius("T", k.thermal.Tc);
        PipePart pipe = pipeOf(part);
        return pipe == null ? "" : String.format("%.0f °C  ", mods.eln.fluid.FluidWorld.nodeFor(pipe.fluid).T() - 273.15);
    }

    /** By side (a face's hosted device; else the block's first cable, then its first pipe: callers without a hit). */
    @Override
    public String multiMeterString(Direction side) {
        if (sideElementList[side.getInt()] != null) return super.multiMeterString(side);
        return multiMeterString(firstPart());
    }

    @Override
    public String thermoMeterString(Direction side) {
        if (sideElementList[side.getInt()] != null) return super.thermoMeterString(side);
        return thermoMeterString(firstPart());
    }

    /** The first live cable's piece, else the first pipe (the readings without a hit). */
    private int firstPart() {
        for (CableNet.Conductor k : cables.live())
            if (!k.parts.isEmpty()) return parts.indexOf(k.parts.get(0));
        for (int i = 0; i < parts.size(); i++) if (parts.get(i) instanceof PipePart pp && pp.fluid != null) return i;
        return -1;
    }

    public boolean playerAskToBreakPart(Player player, int index) {
        return removePart(index, player == null || !player.isCreative());
    }
}
