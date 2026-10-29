package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InlinePumpDescriptor: an inlet and an outlet node (small, lumped into the pipes on them) and a one-way link between
 * them with the pump's head as its boost, as the condenser's condensate pump (CondenserElement). Each fluid step sets
 * its speed (signal x supply), head and opening; the electrical load draws the power for what it moved.
 */
public class InlinePumpElement extends TransparentNodeElement implements FluidDevice {
    final InlinePumpDescriptor desc;
    public static final double NODE_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    /** The height of its line (voxels): low, as the tanks' low ports. */
    public static final int LINE_Y = 4;
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor resistor = new Resistor(load, null);
    public final NbtElectricalGateInput speed = new NbtElectricalGateInput("speed");
    public FluidNode inlet, outlet;
    private double[] loadedIn, loadedOut;
    private Link pump, purge;
    private List<FluidPort> ports = List.of();
    /** Speed now (0..1), cavitating, the water moved (kg/s, averaged) and the power wanted (W). */
    public double share;
    public boolean cavitating, gasBound;
    public double kgPerS, wantW;
    double meterKg, meterTime;
    public double totalKg;

    public InlinePumpElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (InlinePumpDescriptor) d;
        electricalLoadList.add(load);
        electricalLoadList.add(speed);
        electricalComponentList.add(resistor);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        resistor.setR(1e9);
    }

    /** Its speed now (0..1): the signal (or full, with none) as far as the supply allows. */
    double speedNow() {
        double signal = speed.getConnectedComponents().isEmpty() ? 1 : speed.getNormalized();
        return Math.max(0, Math.min(1, signal)) * Math.max(0, Math.min(1, load.getU() / desc.nominalU));
    }

    /** Is its inlet gas-bound (mostly air or steam: an unprimed line)? */
    static boolean gasBound(FluidNode in) {
        return in.mLiquid() / Water.RHO_L / in.volume < InlinePumpDescriptor.MIN_LIQUID;
    }

    /** Is its inlet water too close to boiling (under NPSH_PA over its vapour pressure)? */
    static boolean cavitates(FluidNode in) {
        return in.P() - Water.psat(in.T()) < InlinePumpDescriptor.NPSH_PA;
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (pump == null) return;
        share = speedNow();
        boolean was = cavitating;
        var in = FluidWorld.nodeFor(inlet);
        // gas-bound (an unprimed line): it passes the gas on with next to no head, so water follows it in (priming);
        // water too close to boiling: it cavitates, a vapour lock: it moves next to nothing
        boolean gas = share > 0.05 && gasBound(in);
        gasBound = gas;
        cavitating = share > 0.05 && (gas || cavitates(in));
        pump.open = cavitating ? 0.05 * share : share;
        pump.boost = cavitating ? 0.02 * InlinePumpDescriptor.HEAD_PA * share * share : InlinePumpDescriptor.HEAD_PA * share * share;
        // (gas-bound: its casing vent passes the gas at the top on, so the water behind it comes in: it primes itself)
        purge.open = gas ? share : 0;
        purge.boost = 0.02 * InlinePumpDescriptor.HEAD_PA * share * share;
        if (was != cavitating) needPublish();
    }

    @Override
    public void afterFluidStep(double dt) {
        if (pump == null) return;
        double kg = pump.edge == null ? 0 : Math.max(0, pump.edge.fLiquid);   // (kg/s this step)
        totalKg += kg * dt;
        meterKg += kg * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            kgPerS = meterKg / meterTime;
            meterKg = meterTime = 0;
            needPublish();
        }
        // the power for what it moved (hydraulic / efficiency), plus its idling, at its speed
        double hydraulic = kg / Water.RHO_L * pump.boost;
        wantW = share <= 0.01 ? 0 : Math.min(1.2 * InlinePumpDescriptor.NOMINAL_P, hydraulic / InlinePumpDescriptor.EFFICIENCY + InlinePumpDescriptor.IDLE_W * share);
        double u = load.getU();
        resistor.setR(wantW > 1 && u > 1 ? u * u / wantW : 1e9);
        if (cavitating && node.coordonate.world() instanceof net.minecraft.server.level.ServerLevel sl && sl.getGameTime() % 8 == 0)
            sl.playSound(null, node.coordonate.pos(), net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.BLOCKS,
                0.4f, 1.6f);   // (the rattle of cavitation)
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    @Override
    public List<Link> fluidLinks() {
        return pump == null ? List.of() : List.of(pump, purge);
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        // (low on its flanks, LINE_Y voxels up: a pump wants its suction flooded, so it sits under what it draws from)
        int in = front.left().toEnumFacing().get3DDataValue(), out = front.right().toEnumFacing().get3DDataValue();
        ports = List.of(new FluidPort(inlet, at, in, 0.5, PORT_K, mods.eln.oil.TankElement.sideSpot(in, LINE_Y)),   // in: its left flank
            new FluidPort(outlet, at, out, 0.5, PORT_K, mods.eln.oil.TankElement.sideSpot(out, LINE_Y)));           // out: its right
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet != null) {
            makePorts();
            FluidWorld.add(this);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        var c = node.coordonate;
        inlet = node(loadedIn);
        outlet = node(loadedOut);
        // g = rho k / mu: its rated flow with no head against it, none at the shutoff
        pump = new Link(inlet, 0, outlet, 0, InlinePumpDescriptor.RATED_KG_PER_S / InlinePumpDescriptor.HEAD_PA * Water.MU_L / Water.RHO_L)
            .kind(FluidEdge.Kind.CHECK);
        pump.open = 0;
        purge = new Link(inlet, 1.0, outlet, 0.5, pump.k).kind(FluidEdge.Kind.CHECK);
        purge.open = 0;
        makePorts();
        connect();
    }

    private FluidNode node(double[] saved) {
        FluidNode n = new FluidNode(NODE_VOLUME, node.coordonate.y, 0.25, false);
        n.lumpable = true;
        n.resistance = 1 / PORT_K;
        if (saved != null) {
            n.mWater = saved[0];
            n.mAir = saved[1];
            n.energy = saved[2];
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        return n;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        return side == front ? load : side == front.back() ? speed : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return side == front ? NodeBase.maskElectricalPower : side == front.back() ? NodeBase.maskElectricalInputGate : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) share);
            stream.writeBoolean(cavitating);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        save(nbt, "inlet", inlet);
        save(nbt, "outlet", outlet);
    }

    private static void save(CompoundTag nbt, String key, FluidNode n) {
        if (n == null) return;
        nbt.putDouble(key + "W", n.mWater);
        nbt.putDouble(key + "A", n.mAir);
        nbt.putDouble(key + "U", n.energy);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedIn = read(nbt, "inlet");
        loadedOut = read(nbt, "outlet");
    }

    private static double[] read(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "W")) return null;
        return new double[]{nbt.getDoubleOr(key + "W", 0), nbt.getDoubleOr(key + "A", 0), nbt.getDoubleOr(key + "U", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Speed"), String.format("%.0f%%%s", share * 100, gasBound ? I18N.tr(", priming: gas in its line (it passes it on)")
            : cavitating ? I18N.tr(", CAVITATING: its water is too close to boiling (cool it, or raise the pressure at its inlet)") : ""));
        info.put(I18N.tr("Head"), String.format("%.1f bar", pump == null ? 0 : pump.boost / 1e5));
        info.put(I18N.tr("Pumping"), String.format("%.0f mB/s", kgPerS / EaUnits.KG_PER_MB_WATER));
        info.put(I18N.tr("Power"), Utils.plotPower("", resistor.getP()));
        if (inlet != null) {
            var n = FluidWorld.nodeFor(inlet);
            info.put(I18N.tr("Inlet"), String.format("%.2f bar of %s, %.0f °C", n.P() / 1e5, EaUnits.contents(n), n.T() - Water.T0));
        }
        return info;
    }
}
