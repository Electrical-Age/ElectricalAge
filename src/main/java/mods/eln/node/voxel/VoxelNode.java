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
        out.add(bodyBox(e.sixNodeElementDescriptor, e.side.getInt(), e.ox, e.oy, e.oz));
        for (Pin p : pins(e)) out.add(p.box());
        return out;
    }

    /** The body of a device off centre (its model without the pins: SixNodeDescriptor.bodyHalf), clipped to the block. */
    public static int[] bodyBox(mods.eln.node.six.SixNodeDescriptor d, int f, int ox, int oy, int oz) {
        int[] r = rawBodyBox(d, f, ox, oy, oz);
        for (int k = 0; k < 6; k++) r[k] = Math.max(0, Math.min(16, r[k]));
        return r;
    }

    static int[] rawBodyBox(mods.eln.node.six.SixNodeDescriptor d, int f, int ox, int oy, int oz) {
        int face = Direction.fromInt(f).toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(face), bh = d.bodyHalf();
        int[] c = {8 + ox, 8 + oy, 8 + oz};
        int[] r = new int[6];
        for (int a = 0; a < 3; a++) {
            r[a] = c[a] - bh;
            r[a + 3] = c[a] + bh;
        }
        int h = d.footprint()[1];
        r[n] = CablePart.plane(face) == 0 ? 0 : 16 - h;
        r[n + 3] = CablePart.plane(face) == 0 ? h : 16;
        return r;
    }

    /** The pin boxes of a device off centre on face f: one per edge `has` (client: the published pin mask). */
    public static List<int[]> pinBoxes(mods.eln.node.six.SixNodeDescriptor d, Direction side, int ox, int oy, int oz, java.util.function.Predicate<LRDU> has) {
        List<int[]> out = new ArrayList<>();
        for (LRDU l : LRDU.values()) if (has.test(l)) out.add(pinGeometry(d, side, ox, oy, oz, l)[1]);
        return out;
    }

    /** {tip (x, y, z), box, contact} of a pin toward edge l of a device off centre (block voxels). */
    static int[][] pinGeometry(mods.eln.node.six.SixNodeDescriptor d, Direction side, int ox, int oy, int oz, LRDU l) {
        int af = side.toEnumFacing().get3DDataValue(), n = CablePart.normalAxis(af), bh = d.bodyHalf();
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
        markUsed();
        if (drop && p instanceof PipePart pipe && !pipe.burst) dropItem(PipeKitItem.kit(pipe.tier, pipe.length()));
        if (drop && p instanceof CablePart c && c.burnt == null) dropItem(CableKitItem.kit(c.tier, c.dropLength()));
        if (drop && p instanceof ValvePart v) dropItem(valveItem(v));
        if (drop && p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
        if (drop && p instanceof SafetyValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_SAFETY_VALVE.get()));
        if (drop && p instanceof PipeSensorPart s) dropItem(sensorItem(s));
        changed();
        return isEmptyNode();
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

    /** Nothing left: no parts, no elements (the block goes). */
    public boolean isEmptyNode() {
        return parts.isEmpty() && !getIfSideRemain();
    }

    /** The part whose boxes hold the block-local point (0..1), or -1. */
    public int partAt(double x, double y, double z) {
        for (int i = parts.size() - 1; i >= 0; i--)
            for (int[] b : parts.get(i).boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return i;
        for (int i = parts.size() - 1; i >= 0; i--)   // (the outline is 2 voxels fatter than the parts)
            for (int[] b : parts.get(i).boxes()) if (VoxelPart.contains(b, x, y, z, 2.05)) return i;
        return -1;
    }

    private void changed() {
        vents = null;
        valveLinks = null;
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
            if (v.mode == ValvePart.PRESSURE) l.kind(mods.eln.sim.fluid.FluidEdge.Kind.CHECK);
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
        var v = new SafetyValvePart(best[0], best[1], best[2], outlet, pipe.bore(), pipe.workingPa(mods.eln.sim.fluid.Water.T_AMBIENT));
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
            double[] saved = pipe.fluid == null ? null : new double[]{pipe.fluid.mWater, pipe.fluid.mAir, pipe.fluid.energy};
            parts.remove(i);
            for (PipePart hp : halves) {
                double share = hp.length() / (double) Math.max(1, pipe.length());
                hp.burst = pipe.burst;
                hp.makeNode(coordonate.y, mods.eln.sim.Ambient.kelvin(coordonate), saved == null ? null : new double[]{saved[0] * share, saved[1] * share, saved[2] * share});
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
    PipePart pipeAt(int x, int y, int z) {
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
        if (player.getMainHandItem().isEmpty()) {
            int i = pi;
            if (i >= 0 && parts.get(i) instanceof SafetyValvePart sv) {
                // (Baughn 2026-10-27: a screen with a slider, not stepping through presets)
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) openPartScreen(sp, i, SCREEN_SAFETY, sv.setPa / 1e5);
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof PipeSensorPart s) {
                s.scale = s.nextScale();
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("%s: full scale (50 V) %.0f %s", s.name(), s.scale, s.unit())));
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
        mods.eln.node.six.SixNodeElement probe;
        try {
            probe = (mods.eln.node.six.SixNodeElement) d.ElementClass.getConstructor(mods.eln.node.six.SixNode.class, Direction.class,
                mods.eln.node.six.SixNodeDescriptor.class).newInstance(this, face, d);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return "Can't place that";
        }
        probe.slot = slot;
        probe.ox = ox;
        probe.oy = oy;
        probe.oz = oz;
        probe.front = front;
        return placeError(probe);
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
        int[] body = rawBodyBox(d, f, e.ox, e.oy, e.oz);
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
                var cd = ((CablePart) part).tierInfo().descriptor();
                if (!compareConnectionMask(p.mask, cd != null ? cd.getNodeMask() : maskThermalWire))
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
        int[] o = snap(d, face, hx, hy, hz);
        // in the middle of a free face: the face's own slot, as before (ports at the face's edges, which meet the
        // next block, a machine beside it, by EA's edge rule: a trimmer beside a furnace)
        if (o[0] == 0 && o[1] == 0 && o[2] == 0 && sideElementList[face.getInt()] == null && canPlaceElement(face.getInt()))
            return createSubBlock(stack, face, player) ? null : "Can't place that";
        String err = placeErrorAt(stack, slot, face, o[0], o[1], o[2], d.getFrontFromPlace(face, player));
        if (err != null) return err;
        return createSubBlockAt(stack, slot, face, o[0], o[1], o[2], player) ? null : "Can't place that";
    }

    /**
     * The face a device placed by a click into this block goes on: the face behind the clicked face, or, if the click is
     * on a device here, that device's face (AdventurAgent: clicking a chip's side aimed at the wall beside it).
     */
    public Direction placeFace(net.minecraft.core.Direction clicked, double hx, double hy, double hz) {
        int el = elementAt(hx, hy, hz, 0);
        if (el >= 0) return sideElementList[el].side;
        return Direction.fromIntMinecraftSide(clicked.get3DDataValue()).getInverse();
    }

    /** Where a device placed anywhere goes for a click at the block-local point: its centre's offset (world voxels), snapped
     *  to even voxels (pins on the routers' 2-voxel lattice: a cable can be laid to them) and kept with its body and pins
     *  on the face. */
    public static int[] snap(mods.eln.node.six.SixNodeDescriptor d, Direction face, double hx, double hy, double hz) {
        int half = d.bodyHalf() + 1, n = CablePart.normalAxis(face.toEnumFacing().get3DDataValue());
        double[] h = {hx, hy, hz};
        int[] o = new int[3];
        for (int a = 0; a < 3; a++)
            if (a != n) o[a] = Math.max(half, Math.min(16 - half, 2 * (int) Math.round(h[a] * 8))) - 8;
        return o;
    }

    /** Breaking by a player: the block's own break handles the last thing going. */
    boolean breakingByPlayer;

    @Override
    protected void afterSubBlockDeleted() {
        markUsed();
        setNeedPublish(true);
        // an element that went on its own (self-destruction) with nothing else left: the block goes too
        if (!breakingByPlayer && isEmptyNode() && !isDestructing() && coordonate.world() != null) removeEmptyBlock();
    }

    /**
     * Join a new pipe to an existing one at a point on its centre line (a junction: Baughn, routes may join a run mid-way):
     * the existing pipe is cut there into two, and the new one may overlap them only around the junction.
     */
    public String addJoined(PipePart part, int x, int y, int z) {
        if (!part.valid()) return "not a straight-pieced pipe inside the block";
        int target = -1;
        for (int i = 0; i < parts.size(); i++) if (parts.get(i) instanceof PipePart p && p.passes(x, y, z)) target = i;
        if (target < 0) return "no pipe there to join";
        PipePart old = (PipePart) parts.get(target);
        int h = Math.max(old.bore(), part.bore()) / 2 + 1;
        BitSet want = new BitSet(4096);
        for (int[] b : part.boxes()) fill(want, b);
        BitSet around = new BitSet(4096);
        fill(around, new int[]{Math.max(0, x - h), Math.max(0, y - h), Math.max(0, z - h), Math.min(16, x + h), Math.min(16, y + h), Math.min(16, z + h)});
        want.andNot(around);
        if (want.intersects(used)) return "something is in the way";
        PipePart[] halves = old.splitAt(x, y, z);
        double[] saved = old.fluid == null ? null : new double[]{old.fluid.mWater, old.fluid.mAir, old.fluid.energy};
        if (halves != null) {
            parts.remove(target);
            for (PipePart hp : halves) {
                double share = hp.length() / (double) Math.max(1, old.length());
                hp.makeNode(coordonate.y, mods.eln.sim.Ambient.kelvin(coordonate), saved == null ? null : new double[]{saved[0] * share, saved[1] * share, saved[2] * share});
                parts.add(hp);
            }
        }
        if (part.fluid == null) part.makeNode(coordonate.y, mods.eln.sim.Ambient.kelvin(coordonate), null);
        parts.add(part);
        markUsed();
        changed();
        return null;
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
                    vents.put(new Vent(under.fluid, draw, SafetyValvePart.VENT_K), p);
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
            if (stub == null) continue;
            var run = mods.eln.fluid.FluidWorld.nodeFor(stub.fluid);
            int[] d = a.tanks.take();
            if (d[0] > 0) EaUnits.addWater(run, d[0] * EaUnits.KG_PER_MB_WATER, mods.eln.sim.Ambient.kelvin(coordonate), false);   // (other mods' water: at the ambient)
            if (d[1] > 0) drainLiquid(run, d[1] * EaUnits.KG_PER_MB_WATER);
            if (d[2] > 0) EaUnits.addWater(run, d[2] * EaUnits.KG_PER_MB_STEAM, mods.eln.sim.fluid.Water.TB, true);
            if (d[3] > 0) EaUnits.drainVapour(run, d[3] * EaUnits.KG_PER_MB_STEAM);
        }
    }

    static void drainLiquid(mods.eln.sim.fluid.FluidNode n, double m) {
        m = Math.min(m, n.mLiquid());
        if (m <= 0) return;
        n.mWater -= m;
        n.energy -= m * mods.eln.sim.fluid.Water.hLiquid(n.T());
        n.update();
    }

    private void showAdapters() {
        for (VoxelPart p : parts) {
            if (!(p instanceof AdapterPart a)) continue;
            PipePart stub = stubOf(a);
            if (stub == null) continue;
            var run = mods.eln.fluid.FluidWorld.nodeFor(stub.fluid);
            double freeL = Math.max(0, 0.9 * run.volume - run.mLiquid() / mods.eln.sim.fluid.Water.RHO_L);
            double gasRoom = Math.max(0, 9e5 - run.P()) * Math.max(1e-6, freeL) / (mods.eln.sim.fluid.Water.R_V * Math.max(300, run.T()));
            a.tanks.show((int) Math.floor(run.mLiquid() / EaUnits.KG_PER_MB_WATER), (int) Math.floor(EaUnits.steamAvailable(run) / EaUnits.KG_PER_MB_STEAM),
                (int) (freeL * mods.eln.sim.fluid.Water.RHO_L / EaUnits.KG_PER_MB_WATER), (int) (gasRoom / EaUnits.KG_PER_MB_STEAM));
        }
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncAdapters();
        // valves: as far open as they are (a control valve by its signal, a throttle valve by its setting)
        if (valveLinks != null)
            for (var e : valveLinks.entrySet()) {
                ValvePart v = e.getValue();
                double o = v.opening();
                e.getKey().open = o;
                if (v.mode == ValvePart.PRESSURE) {
                    e.getKey().open = 1;
                    // (before it: the most the pipe there can rise to this step, its neighbours' pressure: a reducing valve
                    // on the pressure there at the step's start let a pipe just filled from a tank through by bars)
                    var up = mods.eln.fluid.FluidWorld.nodeFor(e.getKey().a);
                    double before = up.P();
                    for (var edge : mods.eln.fluid.FluidWorld.edgesOf(up))
                        if (edge != e.getKey().edge) before = Math.max(before, (edge.a == up ? edge.b : edge.a).P());
                    e.getKey().boost = v.boost(before, mods.eln.fluid.FluidWorld.nodeFor(e.getKey().b).P());
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
        for (VoxelPart p : parts) {
            if (!(p instanceof PipePart pipe) || pipe.burst || pipe.fluid == null) continue;
            double P = pipe.fluid.P(), T = pipe.fluid.T();
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
            else for (LRDU lrdu : LRDU.values()) tryConnectTwoNode(this, dir, lrdu, other, dir.getInverse(), lrdu.inverseIfLR());
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
    record End(int x, int y, int z, int out, int mask, ElectricalLoad load, ThermalLoad thermal, int elSide, LRDU lrdu) {
        boolean at(int[] p) {
            return x == p[0] && y == p[1] && z == p[2];
        }
    }

    List<End> ends() {
        List<End> out = new ArrayList<>();
        for (CableNet.Conductor k : cables.live())
            for (CablePart c : k.parts)
                for (CablePart.Port p : c.cablePorts()) out.add(new End(p.x(), p.y(), p.z(), p.face(), k.mask(), k.load, k.thermal, -1, null));
        for (int f = 0; f < 6; f++) {
            var e = sideElementList[f];
            if (e == null) continue;
            Direction fs = Direction.fromInt(f);
            int af = fs.toEnumFacing().get3DDataValue();
            for (LRDU l : LRDU.values()) {
                int dv = fs.applyLRDU(l).toEnumFacing().get3DDataValue();
                int[] c = {8, 8, 8};
                c[CablePart.normalAxis(af)] = CablePart.plane(af);
                c[CablePart.normalAxis(dv)] = dv % 2 == 0 ? 0 : 16;
                boolean burnt = e.isBurnt();
                out.add(new End(c[0], c[1], c[2], dv, e.getConnectionMask(l), burnt ? null : e.getElectricalLoad(l), burnt ? null : e.getThermalLoad(l), f, l));
            }
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
    record Pin(int slot, LRDU lrdu, int[] tip, int[] box, int[] contact, int out, int mask, ElectricalLoad load, ThermalLoad thermal) {}

    /** The pins of the element in a slot past the faces (one per edge with a connection). */
    static List<Pin> pins(mods.eln.node.six.SixNodeElement e) {
        List<Pin> out = new ArrayList<>();
        for (LRDU l : LRDU.values()) {
            int mask = e.getConnectionMask(l);
            if (mask == 0) continue;
            int[][] g = pinGeometry(e.sixNodeElementDescriptor, e.side, e.ox, e.oy, e.oz, l);
            int dv = e.side.applyLRDU(l).toEnumFacing().get3DDataValue();
            boolean burnt = e.isBurnt();
            out.add(new Pin(e.slot, l, g[0], g[1], g[2], dv, mask, burnt ? null : e.getElectricalLoad(l), burnt ? null : e.getThermalLoad(l)));
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
            }
            for (Pin q : all) {
                if (q.slot <= p.slot || !meet(p, q) || !compareConnectionMask(p.mask, q.mask)) continue;
                sideElementList[p.slot].slotMask.set(p.lrdu, true);
                sideElementList[q.slot].slotMask.set(q.lrdu, true);
                link(p.load, q.load, p.thermal, q.thermal);
            }
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
                across(dir, v, p.load, q.load, p.thermal, q.thermal);
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
            if (a == this) across(dir, b, p.load, k.load, p.thermal, k.thermal);
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

    /** Across a face to another voxel block: every end meeting one of its ends at the same point. */
    private void connectVoxel(Direction dir, VoxelNode v) {
        int f = dir.toEnumFacing().get3DDataValue();
        List<End> theirs = v.ends();
        for (End a : ends()) {
            if (a.out != f) continue;
            CablePart.Port m = new CablePart.Port(a.out, a.x, a.y, a.z).mirrored();
            for (End b : theirs) {
                if (b.out != m.face() || b.x != m.x() || b.y != m.y() || b.z != m.z() || !compareConnectionMask(a.mask, b.mask)) continue;
                if (a.elSide >= 0) lrduElementMask.set(Direction.fromInt(a.elSide), a.lrdu, true);
                if (b.elSide >= 0) v.lrduElementMask.set(Direction.fromInt(b.elSide), b.lrdu, true);
                mods.eln.sim.ElectricalConnection e = null;
                mods.eln.sim.ThermalConnection t = null;
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
        FluidWorld.remove(this);
    }

    /** The cable conductor taking EA's (side, edge) connection, or null: see connectJob. Not for voxel neighbours
     *  (exact points) nor through air (no outer-corner wraps). */
    private CableNet.Conductor legacy(Direction side, LRDU lrdu) {
        mods.eln.node.NodeBase other = getNeighbor(side);
        if (other == null || other instanceof VoxelNode) return null;
        int f = side.toEnumFacing().get3DDataValue(), a = side.applyLRDU(lrdu).toEnumFacing().get3DDataValue();
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
        for (VoxelPart p : parts) {
            if (p instanceof PipePart pipe && !pipe.burst) dropItem(PipeKitItem.kit(pipe.tier, pipe.length()));
            if (p instanceof CablePart c && c.burnt == null) dropItem(CableKitItem.kit(c.tier, c.dropLength()));
            if (p instanceof ValvePart v) dropItem(valveItem(v));
            if (p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
            if (p instanceof SafetyValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_SAFETY_VALVE.get()));
            if (p instanceof PipeSensorPart s) dropItem(sensorItem(s));
        }
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
        for (mods.eln.node.six.SixNodeElement e : sideElementList) if (e != null) light = Math.max(light, e.getLightValue());
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
        if (n.mVapour() > 1e-7) out.put("Steam", EaUnits.steamGrams(n.mVapour() / EaUnits.KG_PER_MB_STEAM));
        for (VoxelPart p : parts)
            if (p instanceof ValvePart v) out.put(switch (v.mode) {
                case ValvePart.CONTROL -> "Control valve";
                case ValvePart.THROTTLE -> "Throttle valve";
                case ValvePart.PRESSURE -> "Pressure valve";
                default -> "Valve";
            }, v.mode == ValvePart.ON_OFF ? (v.open ? "open" : "shut")
                : v.mode == ValvePart.PRESSURE ? String.format("%s %.1f bar%s, %.0f%% open", v.pmode == ValvePart.RELIEF ? "holds before it at" : "holds after it at",
                    v.setPoint() / 1e5, v.signalled() ? " (signal)" : "", v.opening() * 100)
                : String.format("%.0f%% open", v.opening() * 100));
        for (VoxelPart p : parts)
            if (p instanceof SafetyValvePart sv) out.put(out.containsKey("Safety valve") ? "Safety valve " + (out.size()) : "Safety valve",
                String.format("%.0f bar%s", sv.setPa / 1e5, sv.opening(n.P()) > 0 ? ", blowing" : ""));
        for (VoxelPart p : parts)
            if (p instanceof PipeSensorPart s) out.put(s.name(), String.format(s.sensing == PipeSensorPart.PRESSURE ? "%.2f %s (absolute) of %.0f: %.1f V" : "%.0f %s of %.0f: %.1f V",
                s.reading, s.unit(), s.scale, s.share() * mods.eln.Eln.SVU));
        if (!details) return out;
        out.put("Water", String.format("%.0f mB", n.mLiquid() / EaUnits.KG_PER_MB_WATER));
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
        double lq = n.mLiquid() / EaUnits.KG_PER_MB_WATER, st = n.mVapour() / EaUnits.KG_PER_MB_STEAM;
        return String.format("%.2f bar of %s  water %.0f mB  steam %s%s  ", n.P() / 1e5, EaUnits.contents(n), lq, EaUnits.steamGrams(st), pipe.burst ? "  BURST" : "");
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
