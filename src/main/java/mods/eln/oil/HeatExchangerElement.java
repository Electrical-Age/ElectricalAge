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
 * HeatExchangerDescriptor: two sides, each a chain of SLICES slices along the exchanger's length, the slices at each
 * position coupled by a heat conductance (UA / SLICES) inside the fluid core's step (notes 23-hx.md; Baughn 2026-11-10:
 * devices are components of the implicit step). Counterflow, parallel flow or a pool boiling off follow from how the
 * streams really run; 8 slices come within 4 % of ideal counterflow at NTU 2.5 (HeatChainTest). Blocks stack into one
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
        // (a joint's conductance: a large pipe's end, so a gas stream drops ~100 Pa a joint; wider, the drop fell under the
        // pipe streams' 10 Pa band and their return stream mixed the slices back towards one: 81 % against 88 %)
        double k = mods.eln.node.voxel.PipePart.LARGE_END_K;
        List<FluidDevice.Link> out = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < SLICES; i++)
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
        for (int i = 0; i < SLICES; i++) {
            FluidDevice.Link c = new FluidDevice.Link(sliceA[i], 0.5, sliceB[i], 0.5, 0);
            c.conductance = ua() / SLICES;
            out.add(c);
        }
        links = out;
    }

    private static FluidNode[] split(FluidNode whole) {
        FluidNode[] out = new FluidNode[SLICES];
        for (int i = 0; i < SLICES; i++) {
            FluidNode n = new FluidNode(whole.volume / SLICES, whole.z, whole.height, false);
            n.setShareOf(whole, 1.0 / SLICES);
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
     * linear between (a running exchanger's profile, for tests that start it running: one temperature through 64 L of
     * water takes hours of flow to wash out).
     */
    public void profile(boolean sideA, Direction hotEnd, double tHot, double tCold) {
        HeatExchangerElement h = head();
        h.slice();
        h.gather();
        FluidNode[] side = sideA ? h.sliceA : h.sliceB;
        boolean hotAtFront = hotEnd == front;
        for (int i = 0; i < SLICES; i++) {
            double x = (double) i / (SLICES - 1), t = hotAtFront ? tCold + (tHot - tCold) * x : tHot + (tCold - tHot) * x;
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
        for (FluidNode n : new FluidNode[]{h.sliceA[0], h.sliceA[SLICES - 1], h.sliceB[0], h.sliceB[SLICES - 1]})
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

    /** Placed facing the player, its ports made from that front; the wrench turns it (FluidPlacement). */
    @Override
    public void initializeFromThat(Direction front, net.minecraft.world.entity.LivingEntity entityLiving, net.minecraft.nbt.CompoundTag itemStackNbt) {
        super.initializeFromThat(front, entityLiving, itemStackNbt);
        remakePorts();
    }

    @Override
    public Direction[] fronts() {
        // (one of a stack, or about to be: it stays as placed; turning would re-form the stack under it)
        if (group != null || !neighbours().isEmpty()) return new Direction[]{front};
        return mods.eln.fluid.FluidPlacement.horizontal();
    }

    @Override
    public boolean wrenchTurn(net.minecraft.world.entity.player.Player player) {
        return mods.eln.fluid.FluidPlacement.turn(this);
    }

    @Override
    public void remakePorts() {
        makePorts();
    }

    /**
     * Ports on its two end faces (along its front's axis), both sides on each: A at the face's centre, B up high on it
     * (TankElement.sideSpot), each drawing at its height in the side (a stack's top row draws B's steam). Counterflow:
     * A in at the back end and out at the front, B in at the front and out at the back.
     */
    void makePorts() {
        long at = node.coordonate.pos().asLong();
        double k = mods.eln.node.voxel.PipePart.LARGE_END_K;
        List<FluidPort> out = new java.util.ArrayList<>();
        HeatExchangerElement h = head();
        h.slice();
        int j = group == null ? 0 : node.coordonate.y - group.y0, hh = group == null ? 1 : group.sy;
        for (Direction d : new Direction[]{front.back(), front}) {
            if (group != null) {
                HeatExchangerElement o = HeatExchangerGroup.neighbour(this, d, null);
                if (o != null && group.members.contains(o)) continue;   // (not an end)
            }
            int f = d.toEnumFacing().get3DDataValue(), end = d == front ? SLICES - 1 : 0;   // (the slices run back to front)
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
            makePorts();
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

    private FluidNode node(double[] saved) {
        FluidNode n = new FluidNode(desc.sideVolume, node.coordonate.y, 1.0, false);
        if (saved != null) {
            mods.eln.fluid.FluidSaves.restore(n, saved);
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        return n;
    }

    @Override
    public void initialize() {
        a = ownA = node(loadedA);
        b = ownB = node(loadedB);
        makePorts();
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
        return a == null ? "" : String.format("A %.0f °C, B %.0f °C", a.T() - Water.T0, b.T() - Water.T0);
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
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedA = CondenserVessel.get(nbt, "sideA");
        loadedB = CondenserVessel.get(nbt, "sideB");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();   // (in order: Jade's headline is the first lines)
        if (a == null) return info;
        HeatExchangerElement h = head();
        info.put(I18N.tr("Stream A"), String.format("%.0f -> %.0f °C, %.2f bar of %s", h.tAin - Water.T0, h.tAout - Water.T0, a.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(a)));
        info.put(I18N.tr("Stream B"), String.format("%.0f -> %.0f °C, %.2f bar of %s", h.tBin - Water.T0, h.tBout - Water.T0, b.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(b)));
        info.put(I18N.tr("A holds"), mods.eln.sim.fluid.EaUnits.composition(a));
        info.put(I18N.tr("B holds"), mods.eln.sim.fluid.EaUnits.composition(b));
        info.put(I18N.tr("Heat A to B"), Utils.plotPower("", h.qShown));
        info.put(I18N.tr("Exchanger"), group == null ? String.format(I18N.tr("one block, a quarter slice: %.1f W/K"), ua())
            : String.format(I18N.tr("2 x 2 x %d blocks: %.0f W/K"), group.length(), ua()));
        return info;
    }
}
