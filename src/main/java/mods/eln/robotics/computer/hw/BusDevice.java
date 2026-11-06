package mods.eln.robotics.computer.hw;

import mods.eln.bus.BusChannel;
import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;

/**
 * The computer's RS485 port as a device. To send: fill TX_TARGET, TX_TYPE, TX_INDEX, TX_VALUE (float bits) and write
 * SEND = 1 (the source is the port's address) or 2 (the source is TX_SOURCE: the raw API, which Baughn wants able to
 * fake a sender). At most OUT_CAP packets a tick go out; more are dropped and counted. To receive: RX_COUNT packets
 * wait; writing RX_POP moves the oldest into the RX registers. The 16 colour channels' levels, as of the tick's start,
 * are float bits at LEVELS. A data packet's bytes are TX_LEN bytes at TX_DATA (four to a word, the lowest byte first),
 * and come in as RX_LEN bytes at RX_DATA. The host hands packets in and takes them out between slices.
 */
public final class BusDevice implements Device {
    static final int TX_TARGET = 0x00, TX_SOURCE = 0x04, TX_TYPE = 0x08, TX_INDEX = 0x0C, TX_VALUE = 0x10, SEND = 0x14;
    static final int RX_COUNT = 0x20, RX_POP = 0x24, RX_TARGET = 0x28, RX_SOURCE = 0x2C, RX_TYPE = 0x30, RX_INDEX = 0x34,
            RX_VALUE = 0x38;
    static final int ADDRESS = 0x40, IRQ = 0x44, STATUS = 0x48, DROPPED = 0x4C, TX_LEN = 0x50, RX_LEN = 0x54, LEVELS = 0x80;
    static final int TX_DATA = 0x100, RX_DATA = 0x140;
    public static final int OUT_CAP = 16, IN_CAP = 64;

    private int txTarget, txSource, txType, txIndex, txValue, txLen;
    private final byte[] txData = new byte[Packet.MAX_DATA];
    private Packet rx;
    private final java.util.ArrayDeque<Packet> in = new java.util.ArrayDeque<>();
    private final java.util.List<Packet> out = new java.util.ArrayList<>();
    final float[] levels = new float[BusChannel.COUNT];
    int address, irq;
    boolean onBus;
    long dropped;

    @Override
    public int read(int off) {
        if (off >= LEVELS && off < LEVELS + 4 * BusChannel.COUNT) return Float.floatToRawIntBits(levels[(off - LEVELS) >> 2]);
        if (off >= TX_DATA && off < TX_DATA + Packet.MAX_DATA) return word(txData, txData.length, off - TX_DATA);
        if (off >= RX_DATA && off < RX_DATA + Packet.MAX_DATA) {
            byte[] d = rx == null ? new byte[0] : rx.data();
            return word(d, d.length, off - RX_DATA);
        }
        return switch (off) {
            case TX_TARGET -> txTarget;
            case TX_SOURCE -> txSource;
            case TX_TYPE -> txType;
            case TX_INDEX -> txIndex;
            case TX_VALUE -> txValue;
            case RX_COUNT -> in.size();
            case RX_TARGET -> rx == null ? 0 : rx.target();
            case RX_SOURCE -> rx == null ? 0 : rx.source();
            case RX_TYPE -> rx == null ? 0 : rx.type().id;
            case RX_INDEX -> rx == null ? 0 : rx.index();
            case RX_VALUE -> rx == null ? 0 : Float.floatToRawIntBits(rx.value());
            case ADDRESS -> address;
            case IRQ -> irq;
            case STATUS -> (in.isEmpty() ? 0 : 1) | (out.size() < OUT_CAP ? 2 : 0) | (onBus ? 4 : 0);
            case DROPPED -> (int) dropped;
            case TX_LEN -> txLen;
            case RX_LEN -> rx == null ? 0 : rx.dataLength();
            default -> 0;
        };
    }

    /** Four bytes of b from at (aligned down to a word), the lowest first; past n they are 0. */
    private static int word(byte[] b, int n, int at) {
        at &= ~3;
        int w = 0;
        for (int i = 3; i >= 0; i--) w = w << 8 | (at + i < n ? b[at + i] & 0xFF : 0);
        return w;
    }

    @Override
    public void write(int off, int v) {
        if (off >= TX_DATA && off < TX_DATA + Packet.MAX_DATA) {
            int at = (off - TX_DATA) & ~3;
            for (int i = 0; i < 4; i++) txData[at + i] = (byte) (v >>> 8 * i);
            return;
        }
        switch (off) {
            case TX_TARGET -> txTarget = v & 0xFF;
            case TX_SOURCE -> txSource = v & 0xFF;
            case TX_TYPE -> txType = v & 0xFF;
            case TX_INDEX -> txIndex = v & 0xFF;
            case TX_VALUE -> txValue = v;
            case TX_LEN -> txLen = Math.max(0, Math.min(Packet.MAX_DATA, v));
            case SEND -> send(v);
            case RX_POP -> rx = in.poll();
            case IRQ -> irq = v & 1;
            default -> {
            }
        }
    }

    private void send(int how) {
        PacketType t = PacketType.byId(txType);
        if (t == null || (how != 1 && how != 2)) return;   // unknown types never reach the bus
        if (out.size() >= OUT_CAP) {
            dropped++;
            return;
        }
        out.add(new Packet(txTarget, how == 1 ? address : txSource, t, txIndex, Float.intBitsToFloat(txValue),
            t.hasData ? java.util.Arrays.copyOf(txData, txLen) : null));
    }

    @Override
    public boolean interrupt() {
        return (irq & 1) != 0 && !in.isEmpty();
    }

    /** Host: packets delivered to the port (the oldest are dropped past IN_CAP). */
    public void give(java.util.Collection<Packet> packets) {
        for (Packet p : packets) {
            if (in.size() >= IN_CAP) {
                in.poll();
                dropped++;
            }
            in.add(p);
        }
    }

    /** Host: the packets sent since the last take, in order. */
    public java.util.List<Packet> take() {
        java.util.List<Packet> l = new java.util.ArrayList<>(out);
        out.clear();
        return l;
    }

    /** Host: the port's address, whether it is on a bus, and the channel levels as of this tick. */
    public void setState(int address, boolean onBus, double[] channelLevels) {
        this.address = address;
        this.onBus = onBus;
        for (int c = 0; c < levels.length; c++) levels[c] = channelLevels == null ? 0 : (float) channelLevels[c];
    }
}
