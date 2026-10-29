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
 * between the two. At most half the way to even in a step (a side of thin gas has little heat capacity). Each side's
 * heat capacity rate is its flow times its specific heat: liquid water 4186, vapour 1900, air 1005 J/kg K, and a side
 * holding water and steam together (boiling or condensing) a very large one: its temperature stays at saturation.
 */
public class HeatExchangerElement extends TransparentNodeElement implements FluidDevice {
    final HeatExchangerDescriptor desc;
    public FluidNode a, b;
    private double[] loadedA, loadedB;
    private List<FluidPort> ports = List.of();
    /** Heat A to B now (W), the inlet temperatures (K, last step's), and totals (J) since loaded. */
    public double q, tAin, tBin, tAout, tBout, totalJ;
    double meterQ, meterTime, qShown;

    public HeatExchangerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (HeatExchangerDescriptor) d;
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    /** What flows into n (kg/s) and the flow-weighted temperature it comes from (K), from the edges' last flows. */
    static double[] inflow(FluidNode n) {
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
        if (ml > 1e-6 && mv > 1e-6 && Math.abs(n.T() - Water.tsat(Math.max(1000, n.pVapour()))) < 2) return BOILING;   // (boiling or condensing)
        double m = mw + ma;
        if (m <= 0) return 1005;
        return (ml * Water.CP_L + mv * Water.CP_V + ma * 1005) / m;
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (a == null) return;
        var ia = inflow(a);
        var ib = inflow(b);
        double cpA = cp(a), cpB = cp(b);
        // (a side boiling or condensing meets the other at its saturation temperature, whatever its feed came in at: water
        // fed at 20 C into a drum boiling at 170 mixes up to 170 there)
        tAin = ia[1];
        tBin = ib[1];
        double inA = cpA >= BOILING ? a.T() : tAin, inB = cpB >= BOILING ? b.T() : tBin;
        double ua = desc.ua, heat;
        double cA = ia[0] * cpA, cB = ib[0] * cpB;
        if (cA > 1e-6 && cB > 1e-6) {
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
        double ta = a.T(), tb = b.T();
        if (heat > 0) {
            double fromA = (a.energy - mods.eln.sim.fluid.Flash.energyAt(a.mWater, a.mAir, a.volume, tb)) / dt;
            double intoB = (mods.eln.sim.fluid.Flash.energyAt(b.mWater, b.mAir, b.volume, ta) - b.energy) / dt;
            heat = fromA <= 0 || intoB <= 0 ? 0 : Math.min(heat, 0.9 / (1 / fromA + 1 / intoB));
        } else {
            double fromB = (b.energy - mods.eln.sim.fluid.Flash.energyAt(b.mWater, b.mAir, b.volume, ta)) / dt;
            double intoA = (mods.eln.sim.fluid.Flash.energyAt(a.mWater, a.mAir, a.volume, tb) - a.energy) / dt;
            heat = fromB <= 0 || intoA <= 0 ? 0 : Math.max(heat, -0.9 / (1 / fromB + 1 / intoA));
        }
        // (what leaves each side: its inlet less the heat over its flow; the node's own temperature swings within a step)
        tAout = cA > 1e-6 && cpA < BOILING ? tAin - heat / cA : a.T();
        tBout = cB > 1e-6 && cpB < BOILING ? tBin + heat / cB : b.T();
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

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        double k = mods.eln.node.voxel.PipePart.LARGE_END_K;
        ports = List.of(new FluidPort(a, at, front.left().toEnumFacing().get3DDataValue(), 0.5, k),    // A in: left
            new FluidPort(a, at, front.right().toEnumFacing().get3DDataValue(), 0.5, k),                  // A out: right
            new FluidPort(b, at, front.back().toEnumFacing().get3DDataValue(), 0.0, k),                   // B in: back (to its bottom)
            new FluidPort(b, at, front.toEnumFacing().get3DDataValue(), 1.0, k));                         // B out: front (from its top)
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (a != null) {
            makePorts();
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
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
        a = node(loadedA);
        b = node(loadedB);
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
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (a != null) {
            CondenserVessel.put(nbt, "sideA", a);
            CondenserVessel.put(nbt, "sideB", b);
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
        info.put(I18N.tr("Stream A"), String.format("%.0f -> %.0f °C, %.2f bar of %s", tAin - Water.T0, tAout - Water.T0, a.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(a)));
        info.put(I18N.tr("Stream B"), String.format("%.0f -> %.0f °C, %.2f bar of %s", tBin - Water.T0, tBout - Water.T0, b.P() / 1e5,
            mods.eln.sim.fluid.EaUnits.contents(b)));
        info.put(I18N.tr("Heat A to B"), Utils.plotPower("", qShown));
        return info;
    }
}
