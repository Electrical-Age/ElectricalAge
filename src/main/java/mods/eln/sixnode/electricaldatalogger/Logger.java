package mods.eln.sixnode.electricaldatalogger;

import mods.eln.node.six.SixNodeElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A device's logger, server side (board #45): its history ({@link LogStore}), each channel's scale and unit, the graph's
 * mode, and who is watching. The data logger is one; the signal processor carries one with A, B, C and its output.
 * Packets from the screen go to {@link #handle}; the graph goes to watchers only ({@link LogWatchers}).
 */
public final class Logger {
    /** How the channels are drawn: each as a line on its own axis, stacked areas, or each in its own lane. */
    public static final byte LINES = 0, STACKED = 1, LANES = 2, MODES = 3;
    /** Packets from a screen (the data logger's own ids). */
    public static final byte RESET = 1, PERIOD = 2, MAX = 3, UNIT = 4, WATCH = 5, MIN = 8, MODE = 9;
    /** Packets to clients. */
    public static final byte TO_CLIENT_ALL = 1, TO_CLIENT_POINT = 2;

    final SixNodeElement owner;
    /** Prefixed to packets to clients (a device whose own packets use the first byte), or -1. */
    final int clientHeader;
    public final LogStore store;
    public final LogChannel[] channels;
    public byte mode = LINES;
    final LogWatchers watchers = new LogWatchers();

    public Logger(SixNodeElement owner, int clientHeader, LogChannel... channels) {
        this.owner = owner;
        this.clientHeader = clientHeader;
        this.channels = channels;
        store = new LogStore(channels.length);
        store.setPeriod(0.5);
    }

    /** Records the channels' values held for dt; sends each point as it is due. */
    public void record(float[] values, double dt) {
        if (store.record(values, dt)) sendPoint(store.emit());
    }

    /** The graph as shown: the store's view at its period, with the channels' scales and the mode. */
    public DataLogs view() {
        DataLogs d = new DataLogs(channels.length);
        d.samplingPeriod = store.periodSeconds();
        d.mode = mode;
        for (int c = 0; c < channels.length; c++) d.channels[c] = new LogChannel(channels[c].min, channels[c].max, channels[c].unit);
        d.set(store.view(DataLogs.POINTS));
        return d;
    }

    public void writeToNBT(CompoundTag nbt) {
        CompoundTag s = new CompoundTag();
        store.writeToNBT(s);
        nbt.put("store", s);
        for (int c = 0; c < channels.length; c++) channels[c].writeToNBT(nbt, "ch" + c);
        nbt.putByte("graphMode", mode);
    }

    /** False when the tag has no logger (an older save). */
    public boolean readFromNBT(CompoundTag nbt) {
        if (!nbt.contains("store")) return false;
        store.readFromNBT(nbt.getCompoundOrEmpty("store"));
        for (int c = 0; c < channels.length; c++) channels[c].readFromNBT(nbt, "ch" + c);
        mode = (byte) Math.floorMod(nbt.getByteOr("graphMode", LINES), MODES);
        return true;
    }

    /** The published part: period, mode, channels. */
    public void publish(DataOutputStream o) throws IOException {
        o.writeFloat(store.periodSeconds());
        o.writeByte(mode);
        o.writeByte(channels.length);
        for (LogChannel c : channels) c.write(o);
    }

    /** A screen's packet (its header read); false if not a logger packet. */
    public boolean handle(byte header, DataInputStream in, ServerPlayer player) throws IOException {
        switch (header) {
            case PERIOD -> {
                // the history stays: only the view changes
                float p = in.readFloat();
                if (Float.isFinite(p)) store.setPeriod(Math.max(LogStore.TICK, p));
                owner.needPublish();
                sendView(null);
            }
            case MAX, MIN, UNIT -> {
                // channel, value: a scale only changes how the history is shown
                int c = in.readByte();
                if (c < 0 || c >= channels.length) return true;
                if (header == UNIT) channels[c].unit = LogChannel.sanitize(in.readUTF());
                else {
                    float v = in.readFloat();
                    if (!Float.isFinite(v)) return true;
                    if (header == MAX) channels[c].max = v;
                    else channels[c].min = v;
                }
                owner.needPublish();
            }
            case MODE -> {
                mode = (byte) Math.floorMod(in.readByte(), MODES);
                owner.needPublish();
            }
            case RESET -> {
                store.clear();
                sendView(null);
            }
            case WATCH -> {
                if (player != null && watchers.watch(player, player.level().getGameTime())) sendView(player);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    DataOutputStream packet(ByteArrayOutputStream bos) throws IOException {
        DataOutputStream packet = new DataOutputStream(bos);
        owner.preparePacketForClient(packet);
        if (clientHeader >= 0) packet.writeByte(clientHeader);
        return packet;
    }

    /** The whole graph, to one player (who just started watching) or to every watcher (null). */
    public void sendView(ServerPlayer player) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        try {
            DataOutputStream packet = packet(bos);
            packet.writeByte(TO_CLIENT_ALL);
            packet.writeByte(channels.length);
            view().writeAll(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (player != null) owner.sendPacketToClient(bos, player);
        else toWatchers(bos);
    }

    /** A point just emitted, to the watchers: a few bytes a channel at the sampling rate. */
    void sendPoint(float[][] point) {
        if (watchers.isEmpty()) return;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        try {
            DataOutputStream packet = packet(bos);
            packet.writeByte(TO_CLIENT_POINT);
            packet.writeByte(channels.length);
            DataLogs.writePoint(packet, point, channels.length);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        toWatchers(bos);
    }

    void toWatchers(ByteArrayOutputStream bos) {
        if (watchers.isEmpty() || !(owner.sixNode.coordonate.world() instanceof ServerLevel level)) return;
        for (ServerPlayer p : watchers.players(level)) owner.sendPacketToClient(bos, p);
    }

    /** Client side: a packet from {@link #sendView} or {@link #sendPoint} (after any device header) into the graph. */
    public static DataLogs receive(DataInputStream in, DataLogs log) throws IOException {
        byte header = in.readByte();
        int n = in.readByte();
        if (n != log.channelCount()) log = log.resized(n);
        if (header == TO_CLIENT_ALL) log.readAll(in);
        else if (header == TO_CLIENT_POINT) log.add(DataLogs.readPoint(in, n));
        return log;
    }

    /** Client side: the published part into the graph. */
    public static DataLogs readPublish(DataInputStream in, DataLogs log) throws IOException {
        float period = in.readFloat();
        byte mode = in.readByte();
        int n = in.readByte();
        if (n != log.channelCount()) log = log.resized(n);
        log.samplingPeriod = period;
        log.mode = mode;
        log.version++;
        for (LogChannel c : log.channels) c.read(in);
        return log;
    }
}
