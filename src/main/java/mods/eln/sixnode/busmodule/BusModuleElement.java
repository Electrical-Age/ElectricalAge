package mods.eln.sixnode.busmodule;

import mods.eln.Eln;
import mods.eln.bus.Bus;
import mods.eln.bus.BusNet;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static mods.eln.i18n.I18N.tr;

/**
 * The Bus I/O Module: the bridge between a field bus (mods.eln.bus) and EA's signal wires. Its front edge is the bus
 * terminal; its left, back and right edges hold three signal terminals each, 9 in all (Baughn). Each terminal has a
 * channel name (empty: unused) and a direction: in, the wire's level goes onto the bus every tick; out, the bus drives
 * the wire like a signal source. In a voxel block the three terminals of an edge are pins 4 voxels apart (edgePins);
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
    final String[] names = new String[TERMINALS];
    final boolean[] out = new boolean[TERMINALS];
    /** Its bus pin, the same for the element's life (VoxelNode links it to the bus cable it touches). */
    public final BusPort port = new BusPort();
    /** Why the last name set was refused, or "": shown in its screen. */
    String message = "";
    boolean activity;

    public static final byte setNameId = 1, setOutId = 2;

    public BusModuleElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        for (int i = 0; i < TERMINALS; i++) {
            gates[i] = new NbtElectricalGateOutput("t" + i);
            drivers[i] = new NbtElectricalGateOutputProcess("t" + i + "drive", gates[i]);
            electricalLoadList.add(gates[i]);
            electricalComponentList.add(drivers[i]);
            names[i] = "";
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

    /** Why the last name set was refused, or "". */
    public String message() {
        return message;
    }

    public String name(int i) {
        return names[i];
    }

    public boolean isOut(int i) {
        return out[i];
    }

    /** The voltage on terminal i's wire. */
    public double volts(int i) {
        return gates[i].getU();
    }

    Set<String> used(String[] n) {
        Set<String> s = new LinkedHashSet<>();
        for (String x : n) if (!x.isEmpty()) s.add(x);
        return s;
    }

    /**
     * Terminal i's channel name, from the player: trimmed and lower-cased; empty frees it. Null if taken, else why it
     * was refused (a name the bus can't have, or a 65th channel on its bus), which the screen shows.
     */
    public String setName(int i, String raw) {
        if (i < 0 || i >= TERMINALS) return tr("No such terminal");
        String n = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        String why = null;
        if (!n.isEmpty() && !Bus.validName(n)) why = tr("Names are 1 to 16 of a-z, 0-9 and _");
        else {
            String[] next = names.clone();
            next[i] = n;
            Set<String> want = used(next);
            if (!BusNet.room(port, want)) why = tr("Bus full: it has %1$ channels already", Bus.MAX_CHANNELS);
            else {
                names[i] = n;
                port.claim(want);
            }
        }
        message = why == null ? "" : why;
        needPublish();
        return why;
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
                String n = names[i];
                if (n.isEmpty()) continue;
                if (out[i]) drivers[i].setOutputNormalized(port.read(n));
                else port.write(n, gates[i].getU() / Eln.SVU);
                any |= gates[i].getU() > 0.5;
            }
            if (any != activity) {
                activity = any;
                needPublish();
            }
        }
    }

    /** 0: on no bus, 1: on one, 2: on one past its 64 channels (a merge of two buses; nothing is dropped). */
    int busState() {
        Bus b = port.bus();
        if (b == null) return 0;
        return b.names().size() > Bus.MAX_CHANNELS ? 2 : 1;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < TERMINALS; i++) {
            String n = nbt.getStringOr("t" + i + "name", "");
            names[i] = Bus.validName(n) ? n : "";
            out[i] = nbt.getBooleanOr("t" + i + "out", false);
            drivers[i].setHighImpedance(!out[i]);
        }
        port.claim(used(names));
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        for (int i = 0; i < TERMINALS; i++) {
            if (!names[i].isEmpty()) nbt.putString("t" + i + "name", names[i]);
            if (out[i]) nbt.putBoolean("t" + i + "out", true);
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            for (int i = 0; i < TERMINALS; i++) {
                stream.writeUTF(names[i]);
                stream.writeBoolean(out[i]);
            }
            stream.writeByte(busState());
            stream.writeBoolean(activity);
            stream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setNameId -> {
                    int i = stream.readByte();
                    setName(i, stream.readUTF());
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
        if (b == null) info.put(I18N.tr("Bus"), I18N.tr("no bus"));
        else info.put(I18N.tr("Bus"), (b.names().size() == 1 ? I18N.tr("1 channel") : I18N.tr("%1$ channels", b.names().size()))
            + (b.names().size() > Bus.MAX_CHANNELS ? ", " + I18N.tr("bus full") : ""));
        for (int i = 0; i < TERMINALS; i++) {
            if (names[i].isEmpty()) continue;
            info.put((i + 1) + " " + names[i], (out[i] ? I18N.tr("out") : I18N.tr("in")) + " " + Utils.plotVolt("", gates[i].getU()));
        }
        return info;
    }

    @Override
    public String multiMeterString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < TERMINALS; i++)
            if (!names[i].isEmpty()) s.append(i + 1).append(' ').append(names[i]).append(' ').append(Utils.plotVolt("", gates[i].getU()));
        return s.toString();
    }

    @Override
    public String thermoMeterString() {
        return "";
    }
}
