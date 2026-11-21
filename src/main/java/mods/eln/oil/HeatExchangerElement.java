package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HeatExchangerDescriptor: two sides, each a chain of slices along the exchanger's length (SLICES, 2 in a lone block:
 * slices()), the slices at each position coupled by a heat conductance (UA / slices) inside the fluid core's step (notes 23-hx.md; Baughn 2026-11-10:
 * devices are components of the implicit step). Counterflow, parallel flow or a pool boiling off follow from how the
 * streams really run; each slice's link gets cellBoost() over its share of UA, so 8 slices (2 in a lone block) come within
 * a few % of ideal counterflow and parallel flow at any capacity ratio (without it, 8 % under on balanced streams). Blocks stack into one
 * exchanger (HeatExchangerGroup): `ownA`/`ownB` are this block's shares (what it saves), `a`/`b` the sides in use (its
 * own, or the stack's) as whole volumes, kept summed from the slices; the stack's lowest corner owns the slices.
 */
public class HeatExchangerElement extends TransparentNodeElement implements FluidDevice, mods.eln.fluid.FluidPlacement.Turnable {
    final HeatExchangerDescriptor desc;
    public FluidNode a, b, ownA, ownB;
    /** The stack it is part of, or null alone. */
    public HeatExchangerGroup group;
    private int stackCheck;
    private double[] loadedA, loadedB;
    private List<FluidPort> ports = List.of();
    /** Heat A to B now (W), the inlet temperatures (K, last step's), and totals (J) since loaded. */
    public double q, tAin, tBin, tAout, tBout, totalJ;
    double meterQ, meterTime, qShown;

    public HeatExchangerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (HeatExchangerDescriptor) d;
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        // (a lone block whose neighbours stack with it re-forms the stack: as a tank box, TankElement)
        slowProcessList.add(t -> {
            if (a != null && group == null && ++stackCheck % 20 == 0 && !neighbours().isEmpty() && HeatExchangerGroup.wouldStack(this)) {
                List<HeatExchangerElement> seeds = neighbours();
                seeds.add(this);
                HeatExchangerGroup.regroup(seeds, null);
                if (group != null) mods.eln.Eln.LOG.info("Electrical Age: heat exchanger stack at {} re-formed late", node.coordonate);
            }
        });
    }

    /** What flows into n (kg/s) and the flow-weighted temperature it comes from (K), from the edges' last flows. */
    public static double[] inflow(FluidNode n) {
        var me = FluidWorld.nodeFor(n);
        double m = 0, mT = 0;
        for (var e : FluidWorld.edgesOf(n)) {
            double in = e.b == me ? e.flow : e.a == me ? -e.flow : 0;
            if (in <= 0) continue;
            FluidNode from = e.b == me ? e.a : e.b;
            m += in;
            mT += in * from.T();
        }
        return new double[]{m, m > 0 ? mT / m : n.T()};
    }

    /** Slices a side. */
    public static final int SLICES = 8;
    /**
     * The least a slice holds (m^3: an iron pipe block's). At 1 B a side a lone block's eight slices were 8.8 mL; liquid
     * full, their dP/dm (2.4e14 Pa/kg) put the fluid core's Newton at its precision floor and it crawled at 257 sub-steps.
     */
    static final double MIN_SLICE = 3e-5;

    /** How many slices a side of volume v (m^3) is cut into: SLICES, fewer for a small exchanger (a lone block: 2). */
    static int slices(double v) {
        return Math.max(2, Math.min(SLICES, (int) Math.floor(v / MIN_SLICE)));
    }
    /** The slices (the head's: a lone block or a stack's corner), the sides they were cut from, and their links. */
    FluidNode[] sliceA, sliceB;
    private FluidNode slicedA, slicedB;
    private List<FluidDevice.Link> links = List.of();

    /** Cut this exchanger's sides a and b into slices (again if the sides changed: a stack formed or split). */
    void slice() {
        if (sliceA != null && slicedA == a && slicedB == b) return;
        sliceA = split(a);
        sliceB = split(b);
        slicedA = a;
        slicedB = b;
        // (a joint's conductance: a tenth of the ports', JOINT_SHARE; wider, the drop fell under the pipe streams' 10 Pa
        // band and their return stream mixed the slices back towards one: 81 % against 88 %)
        double k = endK() * JOINT_SHARE;
        List<FluidDevice.Link> out = new java.util.ArrayList<>();
        int n = sliceA.length;
        for (int i = 0; i + 1 < n; i++)
            for (FluidNode[] side : new FluidNode[][]{sliceA, sliceB}) {
                // (floor to floor and ceiling to ceiling: liquid runs along under the gas, none trapped at mid-height)
                for (double at : new double[]{0, 1}) {
                    FluidDevice.Link j = new FluidDevice.Link(side[i], at, side[i + 1], at, k / 2);
                    // (a narrow band: at rest a joint's two streams mix its slices by g band ln 2, and a slow water stream
                    // through an economizer mixed back to one node at the core's 10 Pa, 1.9 kW of its 4.4)
                    j.band = 0.5;
                    out.add(j);
                }
            }
        for (int i = 0; i < n; i++) {
            FluidDevice.Link c = new FluidDevice.Link(sliceA[i], 0.5, sliceB[i], 0.5, 0);
            c.conductance = ua() / n;
            out.add(c);
        }
        links = out;
    }

    /**
     * A slice joint's conductance over its ports' (endK). At 1 the fluid core crawled whenever water circulated through a
     * small exchanger (a feed check valve opening on a running plant: ~130 iterations a step for 10 steps, then 40 a step);
     * joints that stiff turned a Pa of a Newton iterate's error into grams of flow between 35 mL slices. At 0.1 it takes ~70,
     * then ~23. Softer joints also mix the slices less at rest (g band ln 2 a joint): counterflow 64 -> 67 % of its 67 %
     * ideal. The cost is a stream's drop: a 2 x 2 x 4 stack's joints drop ~4 kPa for 44 g/s of 2.4 bar steam (0.35 at 1),
     * a gas turbine's steam raiser backs its exhaust up 5 kPa more; water streams drop a few Pa (Baughn 2026-11-16: a
     * higher drop is fine).
     */
    static final double JOINT_SHARE = 0.1;

    /** The pressure difference between a side's two end slices (Pa, first minus last; 0 if not sliced): its joints' drop. */
    public double jointDrop(boolean sideA) {
        HeatExchangerElement h = head();
        FluidNode[] side = sideA ? h.sliceA : h.sliceB;
        return side == null ? 0 : side[0].P() - side[side.length - 1].P();
    }

    /** A slice's volume when its joints and ports were made a large pipe's end (m^3: a lone block's at 8 L a side). */
    static final double K_VOLUME = 1e-3;

    /**
     * Its ports' conductance (m^3; its joints take JOINT_SHARE of it): a large pipe's end, times its slices' volume over K_VOLUME (at most 1).
     * At 1 B a side a lone block's slice is 8.8 mL; with a large pipe's end on it the fluid core crawled at 257 sub-steps
     * a step on nine empty blocks at rest (conductance x dt over contents 114 x what it had). Scaled, a stream drops more
     * across it: a quarter slice is for small streams, a 2 x 2 stack for the gas ones.
     */
    double endK() {
        HeatExchangerElement h = head();
        double v = h.a == null ? desc.sideVolume : h.a.volume, slice = v / slices(v);
        return mods.eln.node.voxel.PipePart.LARGE_END_K * Math.min(1, slice / K_VOLUME);
    }

    private static FluidNode[] split(FluidNode whole) {
        int count = slices(whole.volume);
        FluidNode[] out = new FluidNode[count];
        for (int i = 0; i < count; i++) {
            FluidNode n = new FluidNode(whole.volume / count, whole.z, whole.height, false);
            n.setShareOf(whole, 1.0 / count);
            n.update();
            out[i] = n;
        }
        return out;
    }

    /**
     * Sum the slices back into the whole sides a and b (for readings, saving and regrouping). A side written to since
     * the last sum (a test filling it, a save restored into it) is spread back into its slices instead: the whole
     * side is then what holds.
     */
    void gather() {
        if (sliceA == null || slicedA != a || slicedB != b) return;
        gatherA = sum(a, sliceA, gatherA);
        gatherB = sum(b, sliceB, gatherB);
    }

    private double[] gatherA, gatherB;

    private static double[] sum(FluidNode whole, FluidNode[] slices, double[] last) {
        if (last != null && (whole.mWater != last[0] || whole.mAir != last[1] || whole.energy != last[2])) {
            for (FluidNode n : slices) {
                n.setShareOf(whole, n.volume / whole.volume);
                n.update();
            }
        } else {
            whole.setShareOf(slices[0], 1);
            for (int i = 1; i < slices.length; i++) whole.addShareOf(slices[i], 1);
            whole.update();
        }
        return new double[]{whole.mWater, whole.mAir, whole.energy};
    }

    /** What flows into a side of this exchanger from outside (kg/s), and the flow-weighted temperature it comes from (K). */
    public double[] sideInflow(boolean sideA) {
        HeatExchangerElement h = head();
        FluidNode[] side = sideA ? h.sliceA : h.sliceB;
        if (side == null) return inflow(sideA ? a : b);
        double[] x = inflowFromOutside(side[0], side), y = inflowFromOutside(side[side.length - 1], side);
        double m = x[0] + y[0];
        return new double[]{m, m > 0 ? (x[0] * x[1] + y[0] * y[1]) / m : side[0].T()};
    }

    /**
     * Set a side's temperatures along its length, its masses kept: tHot at the end facing hotEnd, tCold at the other,
     * linear between (a running exchanger's profile, for tests that start it running).
     */
    public void profile(boolean sideA, Direction hotEnd, double tHot, double tCold) {
        HeatExchangerElement h = head();
        h.slice();
        h.gather();
        FluidNode[] side = sideA ? h.sliceA : h.sliceB;
        boolean hotAtFront = hotEnd == front;
        for (int i = 0; i < side.length; i++) {
            double x = (double) i / (side.length - 1), t = hotAtFront ? tCold + (tHot - tCold) * x : tHot + (tCold - tHot) * x;
            side[i].energy = side[i].energyAt(t);
            side[i].update();
        }
        if (sideA) h.gatherA = null;
        else h.gatherB = null;
        h.gather();
    }

    /** Whether a pipe or device is coupled to one of its ports (an edge from an end slice to a node outside it). */
    public boolean portCoupled() {
        HeatExchangerElement h = head();
        if (h.sliceA == null) return false;
        java.util.Set<FluidNode> own = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        own.addAll(java.util.Arrays.asList(h.sliceA));
        own.addAll(java.util.Arrays.asList(h.sliceB));
        for (FluidNode n : new FluidNode[]{h.sliceA[0], h.sliceA[h.sliceA.length - 1], h.sliceB[0], h.sliceB[h.sliceB.length - 1]})
            for (var e : FluidWorld.edgesOf(n)) if (!own.contains(e.a) || !own.contains(e.b)) return true;
        return false;
    }

    /** What flows into slice n from outside its side (kg/s) and the flow-weighted temperature it comes from (K). */
    static double[] inflowFromOutside(FluidNode n, FluidNode[] side) {
        double m = 0, mT = 0;
        for (var e : FluidWorld.edgesOf(n)) {
            if (e.conductance > 0) continue;
            FluidNode from = e.b == n ? e.a : e.a == n ? e.b : null;
            if (from == null || java.util.Arrays.asList(side).contains(from)) continue;
            double in = e.b == n ? e.flow : -e.flow;
            if (in <= 0) continue;
            m += in;
            mT += in * from.T();
        }
        return new double[]{m, m > 0 ? mT / m : n.T()};
    }

    @Override
    public List<FluidDevice.Link> fluidLinks() {
        if (a == null || group != null && group.master() != this) return List.of();
        slice();
        return links;
    }

    /** What enters a side (kg/s) and at what temperature, at whichever end slice the flow comes in; its other end slice's temperature. */
    static double[] ends(FluidNode[] side) {
        double[] first = inflowFromOutside(side[0], side), last = inflowFromOutside(side[side.length - 1], side);
        return first[0] >= last[0] ? new double[]{first[1], side[side.length - 1].T()} : new double[]{last[1], side[0].T()};
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (a == null || group != null && group.master() != this || sliceA == null) return;
        gather();   // (a side written to since the last step: into its slices)
        double k = ua() / sliceA.length * cellBoost(ua() / sliceA.length, invCA, invCB);
        for (FluidDevice.Link l : links) if (l.conductance > 0) l.conductance = k;
    }

    /**
     * Each side's 1 / (capacity rate) (K/W, 0: a side boiling or condensing, or not known yet), smoothed over steps: the
     * heat it took over the change in its temperature across the exchanger, last steps.
     */
    double invCA, invCB;

    /**
     * How much more conductance a slice's link gets than its share of UA (1..3), so a slice, both sides well mixed, passes
     * what that length of a real exchanger would at these streams: a slice of conductance U between capacity rates C1 >=
     * C2 passes 1 / (1 / U + 1 / C1 + 1 / C2) of the inlet difference, a length of exchanger of NTU n = U / C2 to second
     * order n (1 - n (1 + Cr) / 2), the same for counterflow and parallel flow; so U' = U / (1 - n (1 + Cr) / 2). Without
     * it 8 slices gave 59 % on balanced streams at NTU 2.1, where counterflow gives 67 % (2026-11-16, todo #495).
     */
    static double cellBoost(double u, double invCa, double invCb) {
        double iMax = Math.max(invCa, invCb), iMin = Math.min(invCa, invCb);
        if (!(iMax > 0)) return 1;
        double n = u * iMax, cr = iMin / iMax, d = 1 - n * (1 + cr) / 2;
        return d <= 1.0 / 3 ? 3 : 1 / d;
    }

    /** One side's 1 / (capacity rate) from a step (K/W): heat over its temperature change, NaN if it says nothing. */
    private static double invC(double heat, double tIn, double tOut) {
        if (Math.abs(heat) < 1) return Double.NaN;
        double dT = Math.abs(tIn - tOut);
        return dT < 0.5 ? 0 : dT / Math.abs(heat);
    }

    @Override
    public void afterFluidStep(double dt) {
        if (a == null || group != null && group.master() != this || sliceA == null) return;
        double heat = 0;
        for (FluidDevice.Link l : links) if (l.conductance > 0 && l.edge != null) heat += l.edge.fEnthalpy;
        double[] ea = ends(sliceA), eb = ends(sliceB);
        tAin = ea[0];
        tAout = ea[1];
        tBin = eb[0];
        tBout = eb[1];
        // (smoothed: the boost moves the heat that measures it)
        double ia = invC(heat, tAin, tAout), ib = invC(heat, tBin, tBout);
        if (!Double.isNaN(ia) && !Double.isNaN(ib)) {
            invCA += 0.1 * (ia - invCA);
            invCB += 0.1 * (ib - invCB);
        }
        gather();
        q = heat;
        totalJ += heat * dt;
        meterQ += heat * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            qShown = meterQ / meterTime;
            meterQ = meterTime = 0;
        }
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    /** Its UA (W/K): a block's, times the blocks in its stack. */
    public double ua() {
        return desc.ua * (group == null ? 1 : group.members.size());
    }

    /** The exchanger it is part of (its stack's corner, or itself): it holds the readings. */
    HeatExchangerElement head() {
        return group == null ? this : group.master();
    }

    @Override
    public Direction[] fronts() {
        // (one of a stack, or about to be: it stays as placed; turning would re-form the stack under it)
        if (group != null || !neighbours().isEmpty()) return new Direction[]{front};
        return mods.eln.fluid.FluidPlacement.horizontal();
    }

    /**
     * Ports on its two end faces (along its front's axis), both sides on each: A at the face's centre, B up high on it
     * (TankElement.sideSpot), each drawing at its height in the side (a stack's top row draws B's steam). Counterflow:
     * A in at the back end and out at the front, B in at the front and out at the back.
     */
    @Override
    public void remakePorts() {
        long at = node.coordonate.pos().asLong();
        double k = endK();
        List<FluidPort> out = new java.util.ArrayList<>();
        HeatExchangerElement h = head();
        h.slice();
        int j = group == null ? 0 : node.coordonate.y - group.y0, hh = group == null ? 1 : group.sy;
        for (Direction d : new Direction[]{front.back(), front}) {
            if (group != null) {
                HeatExchangerElement o = HeatExchangerGroup.neighbour(this, d, null);
                if (o != null && group.members.contains(o)) continue;   // (not an end)
            }
            int f = d.toEnumFacing().get3DDataValue(), end = d == front ? h.sliceA.length - 1 : 0;   // (the slices run back to front)
            out.add(new FluidPort(h.sliceA[end], at, f, (j * 16 + 8) / (16.0 * hh), k));
            out.add(new FluidPort(h.sliceB[end], at, f, (j * 16 + TankElement.HIGH) / (16.0 * hh), k, TankElement.sideSpot(f, TankElement.HIGH)));
        }
        ports = out;
    }

    /** The exchangers of its kind on its six sides (for regrouping). */
    List<HeatExchangerElement> neighbours() {
        List<HeatExchangerElement> out = new java.util.ArrayList<>();
        for (Direction d : HeatExchangerGroup.DIRS) {
            HeatExchangerElement o = HeatExchangerGroup.neighbour(this, d, null);
            if (o != null) out.add(o);
        }
        return out;
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (a != null) {
            remakePorts();
            FluidWorld.add(this);
            List<HeatExchangerElement> seeds = neighbours();
            seeds.add(this);
            HeatExchangerGroup.regroup(seeds, null);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
        // (only a block that goes away splits the stack; one out of the simulation stays in it: as TankElement)
        if (group != null && node.isDestructing()) {
            HeatExchangerGroup g = group;
            g.writeBack();
            List<HeatExchangerElement> rest = new java.util.ArrayList<>(g.members);
            rest.remove(this);
            group = null;
            a = ownA;
            b = ownB;
            HeatExchangerGroup.regroup(rest, this);
        }
    }

    /**
     * A side as saved. Saved for a bigger side (8 L before 2026-11-15, no volume key): kept at the state it had (pressure,
     * temperature, mix) at the new size, the rest vented, counted in `vented` (kg: water, all) and shown. Vented, not
     * spilled into the pipes: in a closed plant the old exchanger parked up to 113 B a block out of the loop, and a boiler
     * holds 8 B and a hotwell floods; let out into the plant it would drown it.
     */
    private FluidNode node(double[] saved, double[] vented) {
        FluidNode n = new FluidNode(desc.sideVolume, node.coordonate.y, 1.0, false);
        if (saved != null && savedVolume > desc.sideVolume * (1 + 1e-9)) {
            FluidNode old = new FluidNode(savedVolume, node.coordonate.y, 1.0, false);
            mods.eln.fluid.FluidSaves.restore(old, saved);
            old.update();
            n.setShareOf(old, desc.sideVolume / savedVolume);
            n.update();
            vented[0] += old.mWater - n.mWater;
            vented[1] += old.mass() - n.mass();
            if (vented[1] > 1e-6) mods.eln.Eln.LOG.info("Electrical Age: heat exchanger at {} resized to {} L a side: vented {} kg ({} kg water) at {} bar, {} C",
                node.coordonate, String.format("%.4f", desc.sideVolume * 1000), String.format("%.3f", old.mass() - n.mass()),
                String.format("%.3f", old.mWater - n.mWater), String.format("%.2f", old.P() / 1e5), String.format("%.0f", old.T() - Water.T0));
        } else if (saved != null) {
            mods.eln.fluid.FluidSaves.restore(n, saved);
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        return n;
    }

    /** The side volume its contents were saved for (m^3; its own if saved since the resize or new). */
    private double savedVolume;
    /** What the resize vented from this block's sides (kg): {water, all}, A then B; saved, for Jade. */
    public final double[] ventedA = new double[2], ventedB = new double[2];

    @Override
    public void initialize() {
        a = ownA = node(loadedA, ventedA);
        b = ownB = node(loadedB, ventedB);
        remakePorts();
        connect();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return a == null ? "" : "A " + mods.eln.api.Meters.celsius(a.T() - Water.T0) + ", B " + mods.eln.api.Meters.celsius(b.T() - Water.T0);
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            // the stack: whether it draws it (its lowest corner), and its size (alone: 1 1 1)
            stream.writeBoolean(group == null || group.master() == this);
            stream.writeByte(group == null ? 1 : group.sx);
            stream.writeByte(group == null ? 1 : group.sy);
            stream.writeByte(group == null ? 1 : group.sz);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (group != null) group.writeBack();   // (its share of the stack)
        else gather();   // (its slices summed into its own sides)
        if (ownA != null) {
            // (to float precision, as tanks: a stack re-formed on load from the shares differs in the last bits)
            for (Object[] s : new Object[][]{{"sideA", ownA}, {"sideB", ownB}}) {
                FluidNode n = (FluidNode) s[1];
                nbt.putDouble(s[0] + "W", (float) n.mWater);
                nbt.putDouble(s[0] + "A", (float) n.mAir);
                nbt.putDouble(s[0] + "U", (float) n.energy);
                mods.eln.fluid.FluidSaves.putExtra(nbt, s[0] + "S", n);
                nbt.putInt(s[0] + "Fv", mods.eln.fluid.FluidSaves.FORMAT);
            }
            nbt.putDouble("sideV", ownA.volume);
        }
        for (Object[] s : new Object[][]{{"ventA", ventedA}, {"ventB", ventedB}}) {
            double[] v = (double[]) s[1];
            if (v[1] > 0) {
                nbt.putDouble(s[0] + "W", v[0]);
                nbt.putDouble(s[0] + "M", v[1]);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedA = CondenserVessel.get(nbt, "sideA");
        loadedB = CondenserVessel.get(nbt, "sideB");
        // (no volume key: saved before the resize, 8 L a side)
        savedVolume = nbt.getDoubleOr("sideV", HeatExchangerDescriptor.OLD_SIDE_VOLUME);
        ventedA[0] = nbt.getDoubleOr("ventAW", 0);
        ventedA[1] = nbt.getDoubleOr("ventAM", 0);
        ventedB[0] = nbt.getDoubleOr("ventBW", 0);
        ventedB[1] = nbt.getDoubleOr("ventBM", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();   // (in order: Jade's headline is the first lines)
        if (a == null) return info;
        HeatExchangerElement h = head();
        // (not stepped yet, as just loaded: no inlet reading, so its sides' own temperatures)
        boolean read = h.tAin > 0;
        info.put(I18N.tr("Stream A"), String.format("%s -> %s, %.2f bar of %s", mods.eln.api.Meters.celsius((read ? h.tAin : a.T()) - Water.T0), mods.eln.api.Meters.celsius((read ? h.tAout : a.T()) - Water.T0), a.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(a)));
        info.put(I18N.tr("Stream B"), String.format("%s -> %s, %.2f bar of %s", mods.eln.api.Meters.celsius((read ? h.tBin : b.T()) - Water.T0), mods.eln.api.Meters.celsius((read ? h.tBout : b.T()) - Water.T0), b.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(b)));
        info.put(I18N.tr("A holds"), mods.eln.sim.fluid.EaUnits.composition(a));
        info.put(I18N.tr("B holds"), mods.eln.sim.fluid.EaUnits.composition(b));
        info.put(I18N.tr("Heat A to B"), Utils.plotPower("", h.qShown));
        double vw = 0, vm = 0;
        for (HeatExchangerElement m : group == null ? List.of(this) : group.members) {
            vw += m.ventedA[0] + m.ventedB[0];
            vm += m.ventedA[1] + m.ventedB[1];
        }
        if (vm > 1e-6) info.put(I18N.tr("Resized"), String.format(I18N.tr("1 B a side a block now; it vented %s over that, both sides"),
            vw > 0.01 * vm ? mods.eln.sim.fluid.EaUnits.volume(vw / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER) + " water" : String.format("%.1f g of gas", vm * 1000)));
        info.put(I18N.tr("Exchanger"), group == null ? String.format(I18N.tr("one block, a quarter slice: %.1f W/K"), ua())
            : String.format(I18N.tr("2 x 2 x %d blocks: %.0f W/K"), group.length(), ua()));
        return info;
    }
}
