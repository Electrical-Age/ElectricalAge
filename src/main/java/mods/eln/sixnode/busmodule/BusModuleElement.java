package mods.eln.sixnode.busmodule;

import mods.eln.Eln;
import mods.eln.bus.Bus;
import mods.eln.bus.BusChannel;
import mods.eln.bus.BusPort;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static mods.eln.i18n.I18N.tr;

/**
 * The Bus I/O Module: the bridge between a field bus (mods.eln.bus) and EA's signal wires. Its front edge is the bus
 * terminal; its left, back and right edges hold three signal terminals each, 9 in all (Baughn). Each terminal is
 * tuned to one of the bus's 16 colour channels (BusChannel), or unused, from its screen or with an EA brush, and has a
 * direction: in, the wire's level goes onto the bus every tick; out, the bus drives the wire like a signal source. In a voxel block the three terminals of an edge are pins 4 voxels apart (edgePins);
 * in a plain six-node block an edge has one connection, the middle terminal's, and there is no bus cable to join.
 */
public class BusModuleElement extends SixNodeElement {
    public static final int TERMINALS = 9;
    /** Where an edge's three terminals are, voxels from its middle toward edge.left(): numbered on round the module
     *  from the bus edge's left corner to its right one. */
    public static final int[] PIN_OFFSETS = {-4, 0, 4};

    /** A terminal's wire: an input while the driver is high impedance, else an output. */
    final NbtElectricalGateOutput[] gates = new NbtElectricalGateOutput[TERMINALS];
    final NbtElectricalGateOutputProcess[] drivers = new NbtElectricalGateOutputProcess[TERMINALS];
    /** Each terminal's channel, or BusChannel.UNUSED. */
    final int[] channels = new int[TERMINALS];
    final boolean[] out = new boolean[TERMINALS];
    /** Its bus pin, the same for the element's life (VoxelNode links it to the bus cable it touches). */
    public final BusPort port = new BusPort();
    boolean activity;

    /** Screen packets: a terminal's channel (terminal byte, channel byte; anything not a channel: unused), its direction. */
    public static final byte setChannelId = 1, setOutId = 2;

    public BusModuleElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        for (int i = 0; i < TERMINALS; i++) {
            gates[i] = new NbtElectricalGateOutput("t" + i);
            drivers[i] = new NbtElectricalGateOutputProcess("t" + i + "drive", gates[i]);
            electricalLoadList.add(gates[i]);
            electricalComponentList.add(drivers[i]);
            channels[i] = BusChannel.UNUSED;
            drivers[i].setHighImpedance(true);
        }
        // once a simulator tick, after the circuit: the electrical processes run several times a tick, and every
        // write counts as a writer on the bus
        slowProcessList.add(new BusProcess());
    }

    /** The edge the bus pin is on. */
    public LRDU busEdge() {
        return front;
    }

    /** The edge terminal i is on: the first three on front.left(), then the back, then front.right(). */
    public LRDU edgeOf(int i) {
        return switch (i / 3) {
            case 0 -> front.left();
            case 1 -> front.inverse();
            default -> front.right();
        };
    }

    /** The terminal group (0..2) on an edge, or -1 (the bus edge). */
    int groupOf(LRDU l) {
        if (l == front.left()) return 0;
        if (l == front.inverse()) return 1;
        if (l == front.right()) return 2;
        return -1;
    }

    boolean inVoxelBlock() {
        return sixNode instanceof mods.eln.node.voxel.VoxelNode;
    }

    @Override
    public java.util.List<EdgePin> edgePins() {
        java.util.List<EdgePin> pins = new java.util.ArrayList<>();
        for (int i = 0; i < TERMINALS; i++)
            pins.add(new EdgePin(edgeOf(i), PIN_OFFSETS[i % 3], NodeBase.maskElectricalGate, gates[i]));
        return pins;
    }

    /** In a plain block (no edge pins there): each signal edge's middle terminal. */
    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        int g = groupOf(lrdu);
        return g < 0 ? null : gates[g * 3 + 1];
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == busEdge()) return inVoxelBlock() ? NodeBase.maskRs485 : 0;
        return groupOf(lrdu) >= 0 ? NodeBase.maskElectricalGate : 0;
    }

    @Override
    public BusPort busPort(LRDU lrdu) {
        return lrdu == busEdge() ? port : null;
    }

    /** Terminal i's channel, or BusChannel.UNUSED. */
    public int channel(int i) {
        return channels[i];
    }

    public boolean isOut(int i) {
        return out[i];
    }

    /** The voltage on terminal i's wire. */
    public double volts(int i) {
        return gates[i].getU();
    }

    /** The channels its terminals use, bit c for channel c. */
    int used() {
        int m = 0;
        for (int c : channels) if (BusChannel.valid(c)) m |= 1 << c;
        return m;
    }

    /** Terminal i's channel: 0..15, anything else unused. */
    public void setChannel(int i, int c) {
        if (i < 0 || i >= TERMINALS) return;
        channels[i] = BusChannel.orUnused(c);
        port.claim(used());
        needPublish();
    }

    /**
     * The terminal nearest a click on the module (block-local 0..1, the face's normal ignored): each terminal's point is
     * at its pin's root, 5 voxels from the module's centre toward its edge and PIN_OFFSETS along it toward
     * edge.left(), as VoxelNode places edge pins. In a plain block only the middle terminals (2, 5, 8) count, as only
     * they connect there.
     */
    public int terminalAt(float vx, float vy, float vz) {
        double[] centre = {(8 + ox) / 16.0, (8 + oy) / 16.0, (8 + oz) / 16.0}, hit = {vx, vy, vz};
        net.minecraft.core.Direction normal = side.toEnumFacing();
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < TERMINALS; i++) {
            int o = PIN_OFFSETS[i % 3];
            if (!inVoxelBlock() && o != 0) continue;
            var e = side.applyLRDU(edgeOf(i)).toEnumFacing();
            var a = side.applyLRDU(edgeOf(i).left()).toEnumFacing();
            int[] es = {e.getStepX(), e.getStepY(), e.getStepZ()}, as = {a.getStepX(), a.getStepY(), a.getStepZ()};
            double d = 0;
            for (int k = 0; k < 3; k++) {
                if (k == normal.getAxis().ordinal()) continue;
                double p = centre[k] + es[k] * 5 / 16.0 + as[k] * o / 16.0;
                d += (p - hit[k]) * (p - hit[k]);
            }
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /**
     * An EA brush tunes the terminal nearest the click to its colour, using paint only if that changes it, and says the
     * terminal's channel by number and name; anything else in hand: as other devices.
     */
    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (!(mods.eln.item.BrushDescriptor.getDescriptor(stack) instanceof mods.eln.item.BrushDescriptor brush))
            return super.onBlockActivated(player, side, vx, vy, vz);
        int i = terminalAt(vx, vy, vz);
        if (i < 0) return true;
        int c = BusChannel.fromBrush(brush.getColor(stack));
        if (c != channels[i] && brush.use(stack, player)) setChannel(i, c);
        Utils.sendMessage(player, tr("Terminal %1$: %2$", i + 1, channels[i] == BusChannel.UNUSED ? tr("unused") : BusChannel.label(channels[i])));
        return true;
    }

    /** Terminal i's direction: out drives its wire from the bus, in puts its wire on the bus. */
    public void setOut(int i, boolean o) {
        if (i < 0 || i >= TERMINALS) return;
        out[i] = o;
        drivers[i].setHighImpedance(!o);
        needPublish();
    }

    /** IN terminals write their wire's level; OUT terminals take theirs from the bus (as it was at this tick's start). */
    class BusProcess implements IProcess {
        @Override
        public void process(double time) {
            boolean any = false;
            for (int i = 0; i < TERMINALS; i++) {
                int c = channels[i];
                if (c == BusChannel.UNUSED) continue;
                if (out[i]) drivers[i].setOutputNormalized(port.read(c));
                else port.write(c, gates[i].getU() / Eln.SVU);
                any |= gates[i].getU() > 0.5;
            }
            if (any != activity) {
                activity = any;
                needPublish();
            }
        }
    }


    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < TERMINALS; i++) {
            // (absent, or no channel: unused)
            channels[i] = BusChannel.orUnused(nbt.getByteOr("t" + i + "bus", (byte) BusChannel.UNUSED));
            out[i] = nbt.getBooleanOr("t" + i + "out", false);
            drivers[i].setHighImpedance(!out[i]);
        }
        port.claim(used());
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        for (int i = 0; i < TERMINALS; i++) {
            if (channels[i] != BusChannel.UNUSED) nbt.putByte("t" + i + "bus", (byte) channels[i]);
            if (out[i]) nbt.putBoolean("t" + i + "out", true);
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            for (int i = 0; i < TERMINALS; i++) {
                stream.writeByte(channels[i]);
                stream.writeBoolean(out[i]);
            }
            stream.writeBoolean(port.bus() != null);
            stream.writeBoolean(activity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setChannelId -> {
                    int i = stream.readByte();
                    setChannel(i, stream.readByte());
                }
                case setOutId -> {
                    int i = stream.readByte();
                    setOut(i, stream.readBoolean());
                }
                default -> {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();
        Bus b = port.bus();
        info.put(I18N.tr("Bus"), b == null ? I18N.tr("no bus") : I18N.tr("on a bus"));
        // terminals by their number, 1 to 9; channels by number and name, 0 to 15
        for (int i = 0; i < TERMINALS; i++) {
            if (channels[i] == BusChannel.UNUSED) continue;
            info.put(I18N.tr("Terminal %1$", i + 1), BusChannel.label(channels[i]) + ", " + (out[i] ? I18N.tr("out") : I18N.tr("in")) + " "
                + Utils.plotVolt("", gates[i].getU()));
        }
        return info;
    }

    @Override
    public String multiMeterString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < TERMINALS; i++)
            if (channels[i] != BusChannel.UNUSED)
                s.append(s.isEmpty() ? "" : "  ").append(i + 1).append(": ").append(BusChannel.label(channels[i])).append(' ').append(Utils.plotVolt("", gates[i].getU()));
        return s.toString();
    }

    @Override
    public String thermoMeterString() {
        return "";
    }
}
