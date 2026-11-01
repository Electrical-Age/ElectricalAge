package mods.eln.sixnode.electricaldatalogger;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The data loggers: a channel per input wire. In a voxel block a logger also has a field bus pin (busEdge), and each
 * channel may record a bus channel, set by name in its screen, instead of its wire.
 */
public class ElectricalDataLoggerElement extends SixNodeElement {

    NbtElectricalGateInput inputGate;
    /** The inputs, channel by channel (inputGate first; the three-channel logger has two more). */
    final NbtElectricalGateInput[] inputs;
    ElectricalDataLoggerProcess slowProcess = new ElectricalDataLoggerProcess(this);
    public ElectricalDataLoggerDescriptor descriptor;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    /** The history (board #45): a channel per input; shown per channel scale and unit. */
    public final Logger logger;
    public final LogStore store;
    public final LogChannel[] channels;

    static final byte publishId = 1, dataId = 2;

    public static final byte resetId = 1;
    public static final byte setSamplingPeriodeId = 2, setMaxValue = 3;
    public static final byte setUnitId = 4;
    /** A client that can see the graph asks for it (LogWatchers), every second while it can. */
    public static final byte newClientId = 5;
    public static final byte printId = 6;
    public static final byte tooglePauseId = 7;
    public static final byte setMinValue = 8;
    /** A channel's bus channel name (channel byte, name), from the screen. (Logger's packets take 1 to 9.) */
    public static final byte setBusId = 10;

    /** Its bus pin, the same for the element's life (VoxelNode links it to the bus cable it touches). */
    public final mods.eln.bus.BusPort port = new mods.eln.bus.BusPort();
    /** Per channel: the bus channel it records, or "": its wire. */
    final String[] busNames;
    /** Why the last bus name set was refused, or "": shown in its screen. */
    String busMessage = "";

    public static final byte toClientLogsClear = 1;
    public static final byte toClientLogsAdd = 2;

    boolean printToDo;
    boolean pause = false;

    public ElectricalDataLoggerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;

        int n = Math.max(1, this.descriptor.channels);
        inputs = new NbtElectricalGateInput[n];
        LogChannel[] ch = new LogChannel[n];
        for (int c = 0; c < n; c++) {
            inputs[c] = new NbtElectricalGateInput(c == 0 ? "inputGate" : "inputGate" + c);
            electricalLoadList.add(inputs[c]);
            ch[c] = new LogChannel();
            if (n > 1) ch[c].name = String.valueOf((char) ('A' + c));
        }
        inputGate = inputs[0];
        busNames = new String[n];
        java.util.Arrays.fill(busNames, "");
        logger = new Logger(this, -1, ch);
        store = logger.store;
        channels = logger.channels;
        electricalProcessList.add(slowProcess);
    }

    public static boolean canBePlacedOnSide(Direction side, SixNodeDescriptor descriptor) {
        var d = (ElectricalDataLoggerDescriptor) descriptor;
        if (d.onFloor && side == Direction.YN || d.ceiling) return true;
        return false;
    }

    public SixNodeElementInventory getInventory() {
        return inventory;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByteOr("front", (byte) 0);
        front = LRDU.fromInt((value >> 0) & 0x3);

        pause = nbt.getBooleanOr("pause", false);
        pinTurn = Math.floorMod(nbt.getByteOr("pinTurn", (byte) 0), 4);
        if (!logger.readFromNBT(nbt) && nbt.contains("logssamplingPeriod")) {
            migrate(nbt);
        }
        for (int c = 0; c < busNames.length; c++) {
            String b = nbt.getStringOr("bus" + c, "");
            busNames[c] = mods.eln.bus.Bus.validName(b) ? b : "";
        }
        port.claim(busUsed(busNames));
    }

    /**
     * A logger saved before 2026-10-30 kept a byte per sample (newest first) at its period and one scale: its samples
     * are replayed into the store as recorded, and the scale becomes channel 0's.
     */
    void migrate(CompoundTag nbt) {
        byte[] log = nbt.getByteArray("logslog").orElse(new byte[0]);
        float period = nbt.getFloatOr("logssamplingPeriod", 0.5f);
        channels[0].min = nbt.getFloatOr("logsminValue", 0f);
        channels[0].max = nbt.getFloatOr("logsmaxValue", 100f);
        channels[0].unit = LogChannel.oldUnit(nbt.getByteOr("logsunitType", DataLogs.percentType));
        float[][] samples = new float[log.length][];
        for (int i = 0; i < log.length; i++) samples[i] = new float[]{DataLogs.oldSample(log[log.length - 1 - i])};
        store.clear();
        store.setPeriod(period > 0 ? period : 0.5);
        store.replay(samples, store.period());
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("front", (byte) (front.toInt() << 0));
        nbt.putBoolean("pause", pause);
        if (pinTurn != 0) nbt.putByte("pinTurn", (byte) pinTurn);
        logger.writeToNBT(nbt);
        for (int c = 0; c < busNames.length; c++) if (!busNames[c].isEmpty()) nbt.putString("bus" + c, busNames[c]);
    }

    /** The graph as shown: the store's view at its period, with the channels' scales. */
    public DataLogs view() {
        return logger.view();
    }

    /**
     * Where the inputs are: the first on the back edge. The three-channel logger's others: in a voxel block, beside the
     * first on the same edge (edgePins, Baughn 2026-10-30: three pins on its lower side); in a plain block, on its left
     * and right edges (a six-node edge has one connection).
     */
    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (pinEdge() == lrdu) return inputGate;
        if (inputs.length > 1 && !inVoxelBlock()) {
            if (lrdu == turn(front.left())) return inputs[1];
            if (lrdu == turn(front.right()) && inputs.length > 2) return inputs[2];
        }
        return null;
    }

    /**
     * The inputs' edge, in quarter turns from the back (front.inverse()). The tilted terminal turns it with shift and the
     * wrench key (C) on a right-click, as C alone turns the logger (Baughn 2026-10-31); its terminal strip follows.
     */
    int pinTurn;

    LRDU turn(LRDU l) {
        for (int i = 0; i < pinTurn; i++) l = l.getNextClockwise();
        return l;
    }

    /** The edge the first input (and, in a voxel block, all three) is on. */
    public LRDU pinEdge() {
        return turn(front.inverse());
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (descriptor.ceiling && entityPlayer.isShiftKeyDown() && Utils.isPlayerUsingWrench(entityPlayer)) return turnInputs();
        return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
    }

    /**
     * The tilted terminal's inputs to the next edge, skipping the one the screen faces (Baughn 2026-10-31): reconnected,
     * saved, published (the clients draw the strip there). False for a logger that can't.
     */
    public boolean turnInputs() {
        if (!descriptor.ceiling) return false;
        do pinTurn = (pinTurn + 1) % 4; while (pinEdge() == front);
        sixNode.reconnect();
        sixNode.setNeedPublish(true);
        needPublish();
        return true;
    }

    boolean inVoxelBlock() {
        return sixNode instanceof mods.eln.node.voxel.VoxelNode;
    }

    /** The places of the three inputs along the back edge (voxels from its middle, toward front.inverse().left()). */
    public static final int[] PIN_OFFSETS = {4, 0, -4};

    @Override
    public java.util.List<EdgePin> edgePins() {
        if (inputs.length == 1) return java.util.List.of();
        java.util.List<EdgePin> out = new java.util.ArrayList<>();
        for (int c = 0; c < inputs.length; c++)
            out.add(new EdgePin(pinEdge(), PIN_OFFSETS[c % PIN_OFFSETS.length], NodeBase.maskElectricalInputGate, inputs[c]));
        return out;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (inVoxelBlock() && lrdu == busEdge()) return NodeBase.maskRs485;
        return getElectricalLoad(lrdu) != null ? NodeBase.maskElectricalInputGate : 0;
    }

    /** The bus pin's edge, alone in its middle: the first edge clockwise from the inputs' that isn't the front (the
     *  edge the screen faces). Only in a voxel block, whose free edges these are. */
    public LRDU busEdge() {
        return busEdge(pinEdge(), front);
    }

    public static LRDU busEdge(LRDU pinEdge, LRDU front) {
        LRDU l = pinEdge;
        for (int i = 0; i < 3; i++) {
            l = l.getNextClockwise();
            if (l != front) return l;
        }
        return l;
    }

    @Override
    public mods.eln.bus.BusPort busPort(LRDU lrdu) {
        return inVoxelBlock() && lrdu == busEdge() ? port : null;
    }

    /** Channel c's bus channel, or "": it records its wire. */
    public String busName(int c) {
        return c >= 0 && c < busNames.length ? busNames[c] : "";
    }

    /** Why the last bus name set was refused, or "". */
    public String busMessage() {
        return busMessage;
    }

    static java.util.Set<String> busUsed(String[] names) {
        java.util.Set<String> s = new java.util.LinkedHashSet<>();
        for (String x : names) if (!x.isEmpty()) s.add(x);
        return s;
    }

    /**
     * Channel c records this bus channel (trimmed, lower-cased; empty: its wire again). Null if taken, else why it was
     * refused (a name the bus can't have, or a 65th channel on its bus), which the screen shows.
     */
    public String setBus(int c, String raw) {
        if (c < 0 || c >= busNames.length) return I18N.tr("No such channel");
        String n = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        String why = null;
        if (!n.isEmpty() && !mods.eln.bus.Bus.validName(n)) why = I18N.tr("Not a channel name");
        else {
            String[] next = busNames.clone();
            next[c] = n;
            var want = busUsed(next);
            if (!mods.eln.bus.BusNet.room(port, want)) why = I18N.tr("Bus full");
            else {
                busNames[c] = n;
                port.claim(want);
            }
        }
        busMessage = why == null ? "" : why;
        needPublish();
        return why;
    }

    /** Channel c's level now, 0..1: its bus channel's, or its wire's. */
    double level(int c) {
        return busNames[c].isEmpty() ? inputs[c].getNormalized() : port.read(busNames[c]);
    }

    @Override
    public String multiMeterString() {
        if (inputs.length == 1) return inputGate.plot("In: ");
        StringBuilder b = new StringBuilder();
        for (int c = 0; c < inputs.length; c++) b.append(inputs[c].plot((char) ('A' + c) + ": "));
        return b.toString();
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        if (inputs.length == 1) info.put(I18N.tr("Input"), Utils.plotVolt("", inputGate.getU()));
        else {
            // (A red, B blue, C green)
            String[] col = {"\u00A7c", "\u00A79", "\u00A7a"};
            StringBuilder b = new StringBuilder();
            for (int c = 0; c < inputs.length; c++) b.append(Utils.plotVolt(col[c % 3], inputs[c].getU()));
            info.put(I18N.tr("Inputs"), b.toString());
        }
        for (int c = 0; c < busNames.length; c++)
            if (!busNames[c].isEmpty())
                info.put(I18N.tr("Bus") + (busNames.length > 1 ? " " + (char) ('A' + c) : ""), busNames[c] + " " + Utils.plotVolt("", port.read(busNames[c]) * mods.eln.Eln.SVU));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(pause);
            logger.publish(stream);
            for (String b : busNames) stream.writeUTF(b);
            stream.writeUTF(busMessage);
            stream.writeByte(pinTurn);   // (last: tests read it there)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
    }

    public void computeElectricalLoad() {
    }

    @Override
    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        super.networkUnserialize(stream);
        try {
            byte header = stream.readByte();
            if (logger.handle(header, stream, player)) return;
            switch (header) {
                case printId:
                    printToDo = true;
                    break;
                case tooglePauseId:
                    pause = !pause;
                    needPublish();
                    break;
                case setBusId: {
                    int c = stream.readByte();
                    setBus(c, stream.readUTF());
                    break;
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

    /** The whole graph, to one player (who just started watching) or to every watcher (null). */
    public void sendView(ServerPlayer player) {
        logger.sendView(player);
    }
}
