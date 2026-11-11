package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
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
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PipeMixerDescriptor: three small nodes (lumped into the pipes on them), inlet A, inlet B and the outlet, and two
 * check-valve links A -> outlet and B -> outlet opened s and 1 - s by its signal each fluid step.
 */
public class PipeMixerElement extends TransparentNodeElement implements FluidDevice {
    public static final double NODE_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    /** The height of its line (voxels): low, as the inline pump's. */
    public static final int LINE_Y = 4;
    public final NbtElectricalGateInput signal = new NbtElectricalGateInput("signal");
    public FluidNode a, b, out;
    private double[] loadedA, loadedB, loadedOut;
    private Link fromA, fromB;
    private List<FluidPort> ports = List.of();
    /** GameTests: the signal (0..1) instead of the gate's, when set. */
    public double testSignal = -1;
    /** A's share of the opening now (0..1), and what came in by each inlet (kg/s, averaged a second). */
    public double share = 0.5, kgA, kgB;
    double meterA, meterB, meterTime;

    public PipeMixerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        electricalLoadList.add(signal);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    /** A's opening (0..1): the signal, or half with none. */
    double shareNow() {
        if (testSignal >= 0) return Math.min(1, testSignal);
        return signal.getConnectedComponents().isEmpty() ? 0.5 : Math.max(0, Math.min(1, signal.getNormalized()));
    }

    @Override
    public void beforeFluidStep(double dt) {
        if (fromA == null) return;
        share = shareNow();
        fromA.open = share;
        fromB.open = 1 - share;
    }

    @Override
    public void afterFluidStep(double dt) {
        if (fromA == null) return;
        meterA += (fromA.edge == null ? 0 : Math.max(0, fromA.edge.flow)) * dt;
        meterB += (fromB.edge == null ? 0 : Math.max(0, fromB.edge.flow)) * dt;
        meterTime += dt;
        if (meterTime >= 1) {
            kgA = meterA / meterTime;
            kgB = meterB / meterTime;
            meterA = meterB = meterTime = 0;
            needPublish();
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

    @Override
    public List<Link> fluidLinks() {
        return fromA == null ? List.of() : List.of(fromA, fromB);
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        int fa = front.left().toEnumFacing().get3DDataValue(), fb = front.right().toEnumFacing().get3DDataValue(),
            fo = front.back().toEnumFacing().get3DDataValue();
        ports = List.of(new FluidPort(a, at, fa, 0.5, PORT_K, TankElement.sideSpot(fa, LINE_Y)),
            new FluidPort(b, at, fb, 0.5, PORT_K, TankElement.sideSpot(fb, LINE_Y)),
            new FluidPort(out, at, fo, 0.5, PORT_K, TankElement.sideSpot(fo, LINE_Y)));
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

    @Override
    public void initialize() {
        a = node(loadedA);
        b = node(loadedB);
        out = node(loadedOut);
        // (each a check valve, its conductance a port's: steered, not metered)
        fromA = new Link(a, 0.5, out, 0.5, PORT_K).kind(FluidEdge.Kind.CHECK);
        fromB = new Link(b, 0.5, out, 0.5, PORT_K).kind(FluidEdge.Kind.CHECK);
        fromA.open = fromB.open = 0.5;
        makePorts();
        connect();
    }

    private FluidNode node(double[] saved) {
        FluidNode n = new FluidNode(NODE_VOLUME, node.coordonate.y, 0.25, false);
        n.lumpable = true;
        n.resistance = 1 / PORT_K;
        if (saved != null) {
            mods.eln.fluid.FluidSaves.restore(n, saved);
            n.update();
        } else n.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        return n;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? signal : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front ? NodeBase.maskElectricalInputGate : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return mods.eln.misc.Utils.plotVolt("U:", signal.getU());
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
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (a != null) mods.eln.fluid.FluidSaves.put(nbt, "a", a);
        if (b != null) mods.eln.fluid.FluidSaves.put(nbt, "b", b);
        if (out != null) mods.eln.fluid.FluidSaves.put(nbt, "out", out);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        loadedA = read(nbt, "a");
        loadedB = read(nbt, "b");
        loadedOut = read(nbt, "out");
    }

    private static double[] read(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "W")) return null;
        return mods.eln.fluid.FluidSaves.array(nbt, key);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Opening"), String.format("A %.0f%%, B %.0f%%", share * 100, (1 - share) * 100));
        info.put(I18N.tr("In"), String.format("A %.1f g/s, B %.1f g/s", kgA * 1000, kgB * 1000));
        if (out != null) {
            var n = FluidWorld.nodeFor(out);
            info.put(I18N.tr("Out"), String.format("%.2f bar: %s", n.P() / 1e5, EaUnits.composition(n)));
        }
        return info;
    }
}
