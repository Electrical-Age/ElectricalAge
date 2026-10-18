package mods.eln.node.voxel;

import mods.eln.fluid.FluidWorld;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
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
public class VoxelNode extends Node implements FluidDevice {
    public static final String UUID = "v";

    public final List<VoxelPart> parts = new ArrayList<>();
    private final BitSet used = new BitSet(4096);

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

    /** Add a part (a pipe laid here): null if done, else why not. */
    public String addPart(VoxelPart p) {
        if (p instanceof PipePart pipe && !pipe.valid()) return "not a straight-pieced pipe inside the block";
        if (p instanceof AdapterPart a) {   // (its flange sits on its own stub's end)
            BitSet others = new BitSet(4096), want = new BitSet(4096);
            PipePart stub = stubOf(a);
            for (VoxelPart q : parts) if (q != stub) for (int[] b : q.boxes()) fill(others, b);
            for (int[] b : a.boxes()) fill(want, b);
            if (stub == null) return "no pipe ends there";
            if (want.intersects(others)) return "something is in the way";
        } else if (!fits(p)) return "something is in the way";
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
        if (drop && p instanceof ValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()));
        if (drop && p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
        changed();
        return parts.isEmpty();
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
        if (isAdded()) FluidWorld.changed(this);
        setNeedPublish(true);
    }

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
        if (player.getMainHandItem().isEmpty()) {
            int i = partAt(vx, vy, vz);
            if (i >= 0 && parts.get(i) instanceof ValvePart v) {
                v.open = !v.open;
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(v.open ? "Valve open" : "Valve shut"));
                changed();
                return true;
            }
        }
        return super.onBlockActivated(player, side, vx, vy, vz);
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
        List<Vent> out = new ArrayList<>();
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.burst && pipe.fluid != null) {
                Vent v = new Vent(pipe.fluid, 0.5, PipePart.BURST_VENT_K);
                v.open = 1;
                out.add(v);
            }
        return out;
    }

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
            if (P > pipe.burstPa(T) && mods.eln.fluid.FluidWorld.claimBurst(pipe.fluid)) {
                pipe.burst = true;
                changed = true;
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
    }

    @Override
    public void connectJob() {
        super.connectJob();
        FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
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
            if (p instanceof ValvePart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()));
            if (p instanceof AdapterPart) dropItem(new ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get()));
        }
        parts.clear();
        super.onBreakBlock();
    }

    @Override
    public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
        return 0;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
        return null;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        // (other mods' pipes may have used an adapter since the last step: into the run, and the run into its pieces)
        if (isAdded()) {
            syncAdapters();
            for (VoxelPart p : parts) if (p instanceof AdapterPart a && stubOf(a) != null) mods.eln.fluid.FluidWorld.writeBackOf(stubOf(a).fluid);
            showAdapters();
        }
        super.writeToNBT(nbt);
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
        super.readFromNBT(nbt);
        parts.clear();
        int n = nbt.getIntOr("parts", 0);
        for (int i = 0; i < n; i++) {
            VoxelPart p = VoxelPart.read(nbt.getCompoundOrEmpty("p" + i), coordonate.y);
            if (p != null) parts.add(p);
        }
        markUsed();
    }

    /** Client data: the part count, then each part's kind and length-prefixed bytes (an unknown kind is skipped). */
    @Override
    public void publishSerialize(DataOutputStream stream) {
        super.publishSerialize(stream);
        try {
            stream.writeShort(parts.size());
            for (VoxelPart p : parts) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                p.serialize(new DataOutputStream(b));
                stream.writeUTF(p.kind());
                stream.writeShort(b.size());
                stream.write(b.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** The multimeter on a pipe: its run's pressure and what's in it (a gauge until there's a gauge part). */
    @Override
    public String multiMeterString(Direction side) {
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
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.fluid != null) return String.format("%.0f °C  ", pipe.fluid.T() - 273.15);
        return "";
    }

    public boolean playerAskToBreakPart(Player player, int index) {
        return removePart(index, player == null || !player.isCreative());
    }
}
