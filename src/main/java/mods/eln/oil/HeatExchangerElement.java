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
 * HeatExchangerDescriptor: a node per side. Each fluid step, from what flowed into each side last step (its mass flow and
 * the temperature it came from) the counterflow effectiveness gives the heat; with one side still, the plain UA x dT
 * between the two. At most (90 % of) the way to even in a step. Each side's heat capacity rate is its flow times its
 * specific heat: liquid water 4186, vapour 1900, air 1005 J/kg K, and a side holding water and steam together (boiling
 * or condensing) a very large one: its temperature stays at saturation. Blocks stack into one exchanger
 * (HeatExchangerGroup): `ownA`/`ownB` are this block's shares, `a`/`b` the sides in use (its own, or the stack's); the
 * stack's lowest corner steps it.
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

    static final double BOILING = 1e7;

    /** A side's specific heat (J/kg K) for its heat capacity rate: very large while it holds water and steam together. */
    static double cp(FluidNode n) {
        double mw = n.mWater, ml = n.mLiquid(), mv = n.mVapour(), ma = n.mAir;
        // (boiling or condensing: water and steam, the steam nearly all the pressure, so at saturation; water held at 16
        // bar with a trace of vapour at its own vapour pressure is not: taken so, cold feed soaked up any heat at 20 C,
        // and feed at 178 C, 9.5 bar of its 16, held an economizer to 1.6 kW of 5)
        if (ml > 1e-6 && mv > 1e-6 && n.pVapour() > 0.9 * n.P() && Math.abs(n.T() - Water.tsat(Math.max(1000, n.pVapour()))) < 2) return BOILING;
        double m = mw + ma;
        if (m <= 0) return 1005;
        return (ml * Water.CP_L + mv * Water.CP_V + ma * 1005) / m;
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (a == null || group != null && group.master() != this) return;   // (a stack is stepped once, by its corner)
        var ia = inflow(a);
        var ib = inflow(b);
        double cpA = cp(a), cpB = cp(b);
        // (a side boiling or condensing meets the other at its saturation temperature, whatever its feed came in at: water
        // fed at 20 C into a drum boiling at 170 mixes up to 170 there)
        tAin = ia[1];
        tBin = ib[1];
        double inA = cpA >= BOILING ? a.T() : tAin, inB = cpB >= BOILING ? b.T() : tBin;
        double ua = ua(), heat;
        boolean flowing;
        // (a side boiling or condensing takes any heat at its saturation temperature, fed or not: a pool boiling off)
        double cA = cpA >= BOILING ? BOILING : ia[0] * cpA, cB = cpB >= BOILING ? BOILING : ib[0] * cpB;
        if (flowing = cA > 1e-6 && cB > 1e-6) {
            // counterflow effectiveness-NTU, from the two inlets
            double cMin = Math.min(cA, cB), cMax = Math.max(cA, cB), cr = cMin / cMax, ntu = ua / cMin, eps;
            if (cr > 0.999) eps = ntu / (1 + ntu);
            else {
                double x = Math.exp(-ntu * (1 - cr));
                eps = (1 - x) / (1 - cr * x);
            }
            heat = eps * cMin * (inA - inB);
        } else heat = ua * (a.T() - b.T());   // (a side still: the two volumes)
        // at most (90 % of) the way to even in a step: the heat that brings both sides to one temperature, from each side's
        // heat to reach the other's (1 / (1/fromA + 1/intoB): half the smaller for two like sides, nearly all of a thin
        // gas's heat against boiling water; capped at half the smaller, 4 L of exhaust gave 8 kW of the 13 it could. It
        // is taken before this step's inflow arrives, so its holding, not its through-flow, is the most it can give.)
        // Both flowing, each side goes at most to the other's inlet (counterflow: the gas leaving meets the feed coming
        // in, colder than the warm water the economizer holds, which held it to 1.6 kW of 5)
        double ta = flowing ? inA : a.T(), tb = flowing ? inB : b.T();
        if (heat > 0) {
            double fromA = (a.energy - mods.eln.sim.fluid.Flash.energyAt(a.mWater, a.mAir, a.volume, tb)) / dt;
            double intoB = (mods.eln.sim.fluid.Flash.energyAt(b.mWater, b.mAir, b.volume, ta) - b.energy) / dt;
            heat = fromA <= 0 || intoB <= 0 ? 0 : Math.min(heat, 0.9 / (1 / fromA + 1 / intoB));
        } else {
            double fromB = (b.energy - mods.eln.sim.fluid.Flash.energyAt(b.mWater, b.mAir, b.volume, ta)) / dt;
            double intoA = (mods.eln.sim.fluid.Flash.energyAt(a.mWater, a.mAir, a.volume, tb) - a.energy) / dt;
            heat = fromB <= 0 || intoA <= 0 ? 0 : Math.max(heat, -0.9 / (1 / fromB + 1 / intoA));
        }
        // (what leaves each side: a gas, its inlet less the heat over its flow, since a few litres of gas swing within a
        // step; water, what it holds: 64 L warms over hours, and that is what leaves)
        tAout = cA > 1e-6 && cpA < BOILING && a.mLiquid() < 1e-3 ? tAin - heat / cA : a.T();
        tBout = cB > 1e-6 && cpB < BOILING && b.mLiquid() < 1e-3 ? tBin + heat / cB : b.T();
        a.heat -= heat;
        b.heat += heat;
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
        int j = group == null ? 0 : node.coordonate.y - group.y0, h = group == null ? 1 : group.sy;
        for (Direction d : new Direction[]{front.back(), front}) {
            if (group != null) {
                HeatExchangerElement o = HeatExchangerGroup.neighbour(this, d, null);
                if (o != null && group.members.contains(o)) continue;   // (not an end)
            }
            int f = d.toEnumFacing().get3DDataValue();
            out.add(new FluidPort(a, at, f, (j * 16 + 8) / (16.0 * h), k));
            out.add(new FluidPort(b, at, f, (j * 16 + TankElement.HIGH) / (16.0 * h), k, TankElement.sideSpot(f, TankElement.HIGH)));
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
            n.mWater = saved[0];
            n.mAir = saved[1];
            n.energy = saved[2];
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
        if (ownA != null) {
            // (to float precision, as tanks: a stack re-formed on load from the shares differs in the last bits)
            for (Object[] s : new Object[][]{{"sideA", ownA}, {"sideB", ownB}}) {
                FluidNode n = (FluidNode) s[1];
                nbt.putDouble(s[0] + "W", (float) n.mWater);
                nbt.putDouble(s[0] + "A", (float) n.mAir);
                nbt.putDouble(s[0] + "U", (float) n.energy);
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
        Map<String, String> info = new HashMap<>();
        if (a == null) return info;
        HeatExchangerElement h = head();
        info.put(I18N.tr("Stream A"), String.format("%.0f -> %.0f °C, %.2f bar of %s", h.tAin - Water.T0, h.tAout - Water.T0, a.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(a)));
        info.put(I18N.tr("Stream B"), String.format("%.0f -> %.0f °C, %.2f bar of %s", h.tBin - Water.T0, h.tBout - Water.T0, b.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(b)));
        info.put(I18N.tr("Heat A to B"), Utils.plotPower("", h.qShown));
        info.put(I18N.tr("Exchanger"), group == null ? String.format(I18N.tr("one block, a quarter slice: %.1f W/K"), ua())
            : String.format(I18N.tr("2 x 2 x %d blocks: %.0f W/K"), group.length(), ua()));
        return info;
    }
}
