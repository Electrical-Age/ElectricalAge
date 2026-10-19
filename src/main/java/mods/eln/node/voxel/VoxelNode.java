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

    private void markUsed() {
        used.clear();
        for (VoxelPart p : parts) for (int[] b : p.boxes()) fill(used, b);
        for (int f = 0; f < 6; f++) if (sideElementList[f] != null || isSideKept(f)) fill(used, plate(f));
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
        if (p instanceof PipePart pipe && pipe.fluid == null) pipe.makeNode(coordonate.y, null);
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
        if (drop && p instanceof ValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()));
        if (drop && p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
        if (drop && p instanceof SafetyValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_SAFETY_VALVE.get()));
        changed();
        return isEmptyNode();
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
        if (isAdded()) {
            if (hasFluid()) FluidWorld.add(this);
            FluidWorld.changed(this);
            // cables: rebuild the conductors and their links (reconnect keeps their state through the pieces)
            boolean cable = false;
            for (VoxelPart p : parts) if (p instanceof CablePart) cable = true;
            if (cable || cablesBuilt) reconnect();
        }
        setNeedPublish(true);
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

    /** Each valve links the two pipes that end at it, open or shut. */
    @Override
    public List<Link> fluidLinks() {
        List<Link> out = new ArrayList<>();
        for (VoxelPart p : parts) {
            if (!(p instanceof ValvePart v)) continue;
            List<PipePart> ends = new ArrayList<>();
            for (VoxelPart q : parts)
                if (q instanceof PipePart pipe && pipe.fluid != null)
                    for (int[] e : pipe.innerEnds()) if (e[0] == v.x && e[1] == v.y && e[2] == v.z) ends.add(pipe);
            if (ends.size() != 2) continue;
            PipePart a = ends.get(0), b = ends.get(1);
            double k = PipePart.K_PER_BLOCK[Math.min(a.tier, b.tier)] * 4;   // (a quarter block of pipe)
            Link l = new Link(a.fluid, 0.5, b.fluid, 0.5, k);
            l.open = v.open ? 1 : 0;
            out.add(l);
        }
        return out;
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
        int idx = partAt(hx, hy, hz);
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
        return best == null ? "the pipe has no room for a valve here" : addValve(best[0], best[1], best[2]);
    }

    public String addValve(int x, int y, int z) {
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
            for (int[] b : new ValvePart(x, y, z, axis, pipe.bore()).boxes()) fill(want, b);
            if (want.intersects(others)) return "something is in the way";
            PipePart[] halves = pipe.splitAt(x, y, z);
            if (halves == null) return "the pipe ends here";
            double[] saved = pipe.fluid == null ? null : new double[]{pipe.fluid.mWater, pipe.fluid.mAir, pipe.fluid.energy};
            parts.remove(i);
            for (PipePart hp : halves) {
                double share = hp.length() / (double) Math.max(1, pipe.length());
                hp.burst = pipe.burst;
                hp.makeNode(coordonate.y, saved == null ? null : new double[]{saved[0] * share, saved[1] * share, saved[2] * share});
                parts.add(hp);
            }
            parts.add(new ValvePart(x, y, z, axis, pipe.bore()));
            markUsed();
            changed();
            return null;
        }
        return "the pipe doesn't run straight there";
    }

    /** A right-click on a valve (empty hand) turns it. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        // a six-node element hosted here: as in a six-node block (its own use, GUI, repair, meters)
        int el = elementAt(vx, vy, vz, 0);
        int pi = partAt(vx, vy, vz);
        if (el < 0 && pi < 0) el = elementAt(vx, vy, vz, 2);
        if (el >= 0) return super.onBlockActivated(player, Direction.fromInt(el), vx, vy, vz);
        if (player.getMainHandItem().isEmpty()) {
            int i = pi;
            if (i >= 0 && parts.get(i) instanceof SafetyValvePart sv) {
                double next = SafetyValvePart.PRESETS[0] * 1e5;
                for (double b : SafetyValvePart.PRESETS) if (b * 1e5 > sv.setPa + 1) { next = b * 1e5; break; }
                sv.setPa = next;
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format("Safety valve set to %.0f bar", next / 1e5)));
                setNeedPublish(true);
                return true;
            }
            if (i >= 0 && parts.get(i) instanceof ValvePart v) {
                v.open = !v.open;
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(v.open ? "Valve open" : "Valve shut"));
                changed();
                return true;
            }
        }
        return pi >= 0 && meterUse(player, side);
    }

    /** The element (EA Direction int) whose plate, grown by grow voxels, holds the block-local point (0..1); -1. */
    public int elementAt(double x, double y, double z, int grow) {
        for (int f = 0; f < 6; f++) {
            if (sideElementList[f] == null) continue;
            int[] b = plate(f);
            int[] g = {b[0] - grow, b[1] - grow, b[2] - grow, b[3] + grow, b[4] + grow, b[5] + grow};
            if (VoxelPart.contains(g, x, y, z, 0.05)) return f;
        }
        return -1;
    }

    /** Could an element go on face f (EA Direction int): free face, and its plate clear of the parts? */
    public boolean canPlaceElement(int f) {
        if (sideElementList[f] != null || isSideKept(f)) return false;
        BitSet want = new BitSet(4096);
        fill(want, plate(f));
        return !want.intersects(used);
    }

    @Override
    public boolean createSubBlock(ItemStack itemStack, Direction direction, Player player) {
        boolean ok = super.createSubBlock(itemStack, direction, player);
        markUsed();
        return ok;
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
                hp.makeNode(coordonate.y, saved == null ? null : new double[]{saved[0] * share, saved[1] * share, saved[2] * share});
                parts.add(hp);
            }
        }
        if (part.fluid == null) part.makeNode(coordonate.y, null);
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
            if (d[0] > 0) EaUnits.addWater(run, d[0] * EaUnits.KG_PER_MB_WATER, mods.eln.sim.fluid.Water.T_AMBIENT, false);
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
        // safety valves open by the pressure under them
        if (vents != null)
            for (var e : vents.entrySet())
                if (e.getValue() instanceof SafetyValvePart sv) e.getKey().open = sv.opening(mods.eln.fluid.FluidWorld.nodeFor(e.getKey().node).P());
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
                        .add(dir.getStepX() * pipe.bore() / 32.0, dir.getStepY() * pipe.bore() / 32.0, dir.getStepZ() * pipe.bore() / 32.0);
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
        connectElements();   // (six-node elements hosted here: their loads and links inside the block)
        cables.build();
        cables.addToSimulation();
        connectElementsToCables();
        cablesBuilt = !cables.conductors.isEmpty();
        for (Direction dir : Direction.values()) {
            mods.eln.node.NodeBase other = getNeighbor(dir);
            if (other == null || !other.isAdded()) continue;
            if (other instanceof VoxelNode v) connectVoxel(dir, v);
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
    }

    @Override
    public void onBreakBlock() {
        for (VoxelPart p : parts) {
            if (p instanceof PipePart pipe && !pipe.burst) dropItem(PipeKitItem.kit(pipe.tier, pipe.length()));
            if (p instanceof CablePart c && c.burnt == null) dropItem(CableKitItem.kit(c.tier, c.dropLength()));
            if (p instanceof ValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()));
            if (p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
            if (p instanceof SafetyValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_SAFETY_VALVE.get()));
        }
        parts.clear();
        super.onBreakBlock();
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

    /** Client data: the part count, then each part's kind and length-prefixed bytes (an unknown kind is skipped); then
     *  SixNode's elements. */
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
        }
    }

    /** For the probe (Jade): a headline, and with details the run's contents, ratings and fittings. */
    public java.util.LinkedHashMap<String, String> getWaila(boolean details) {
        var out = new java.util.LinkedHashMap<String, String>();
        PipePart pipe = null;
        for (VoxelPart p : parts) if (p instanceof PipePart pp && pp.fluid != null && (pipe == null || pp.burst)) pipe = pp;
        if (pipe == null) {
            // cables: each conductor's tier, voltage and current (temperature with details)
            int n = 0;
            for (CableNet.Conductor k : cables.conductors) {
                if (!k.valid() || k.face < 0) continue;
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
        out.put("Pressure", String.format("%.2f bar, %.0f °C", n.P() / 1e5, n.T() - 273.15));
        for (VoxelPart p : parts) if (p instanceof ValvePart v) out.put("Valve", v.open ? "open" : "shut");
        for (VoxelPart p : parts)
            if (p instanceof SafetyValvePart sv) out.put(out.containsKey("Safety valve") ? "Safety valve " + (out.size()) : "Safety valve",
                String.format("%.0f bar%s", sv.setPa / 1e5, sv.opening(n.P()) > 0 ? ", blowing" : ""));
        if (!details) return out;
        out.put("Water", String.format("%.0f mB", n.mLiquid() / EaUnits.KG_PER_MB_WATER));
        out.put("Steam", String.format("%.0f mB", n.mVapour() / EaUnits.KG_PER_MB_STEAM));
        out.put("Run volume", String.format("%.2f L", n.volume * 1000));
        out.put("Rating", String.format("works to %.0f bar, bursts at %.0f", pipe.workingPa(n.T()) / 1e5, pipe.burstPa(n.T()) / 1e5));
        for (VoxelPart p : parts) if (p instanceof AdapterPart a) out.put("Adapter", net.minecraft.core.Direction.from3DDataValue(a.face).getName());
        return out;
    }

    /** The multimeter on a pipe: its run's pressure and what's in it (a gauge until there's a gauge part). */
    @Override
    public String multiMeterString(Direction side) {
        if (sideElementList[side.getInt()] != null) return super.multiMeterString(side);
        for (CableNet.Conductor k : cables.live())
            if (k.load != null) return k.d.signalWire ? mods.eln.misc.Utils.plotSignal(k.load.getU(), k.load.getI()) : mods.eln.misc.Utils.plotUIP(k.load.getU(), k.load.getI());
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null) {
                var n = pipe.fluid;
                double lq = n.mLiquid() / EaUnits.KG_PER_MB_WATER, st = n.mVapour() / EaUnits.KG_PER_MB_STEAM;
                return String.format("%.2f bar  water %.0f mB  steam %.0f mB%s  ", n.P() / 1e5, lq, st, pipe.burst ? "  BURST" : "");
            }
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        if (sideElementList[side.getInt()] != null) return super.thermoMeterString(side);
        for (CableNet.Conductor k : cables.live())
            if (k.thermal != null) return mods.eln.misc.Utils.plotCelsius("T", k.thermal.Tc);
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null) return String.format("%.0f °C  ", pipe.fluid.T() - 273.15);
        return "";
    }

    public boolean playerAskToBreakPart(Player player, int index) {
        return removePart(index, player == null || !player.isCreative());
    }
}
