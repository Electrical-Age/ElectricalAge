package mods.eln.robotics.computer.hw;

/**
 * A byte channel between the program and the screen: the terminal, or the editor's channel. The program writes TX
 * and reads RX (-1: nothing waiting); STATUS bit 0 is input waiting, bit 1 room to send; IRQ enables the interrupt
 * for either. The host side takes output and gives input between slices.
 */
public final class Console implements Device {
    static final int TX = 0x00, RX = 0x04, STATUS = 0x08, IRQ = 0x0C;
    /**
     * Block transfer (Wire 2, Baughn 2026-11-24: "register pair"): the host copies a whole buffer from or into the
     * program's RAM at once, as 1991's DMA channels did, instead of a store or load a byte. TX: set address and length,
     * write TX_GO; TX_LEN then reads how many went (less when the channel filled: send the rest later; none is dropped).
     * RX: set address and room, write RX_GO; RX_LEN reads how many bytes were written there.
     */
    static final int TX_ADDR = 0x10, TX_LEN = 0x14, TX_GO = 0x18, RX_ADDR = 0x20, RX_CAP = 0x24, RX_GO = 0x28, RX_LEN = 0x2C;
    /** The RAM block transfers use (set by the machine). */
    mods.eln.robotics.computer.cpu.Memory mem;
    private int txAddr, txLen, rxAddr, rxCap, rxLen;
    private final java.util.ArrayDeque<Byte> in = new java.util.ArrayDeque<>();
    private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    private final int outCap, inCap;
    /**
     * A byte was sent with no room for it: the rest of that message is dropped up to its newline, and the host is told
     * (CANCEL after what it had) to drop the start it already has, rather than fuse the two halves into another,
     * well-formed but wrong message.
     */
    private boolean dropping, cancelPending;
    public static final byte CANCEL = 0x18;
    int irq;

    public Console(int outCap, int inCap) {
        this.outCap = outCap;
        this.inCap = inCap;
    }

    @Override
    public int read(int off) {
        return switch (off) {
            case RX -> in.isEmpty() ? -1 : in.poll() & 0xFF;
            case STATUS -> status();
            case IRQ -> irq;
            case TX_ADDR -> txAddr;
            case TX_LEN -> txLen;
            case RX_ADDR -> rxAddr;
            case RX_CAP -> rxCap;
            case RX_LEN -> rxLen;
            default -> 0;
        };
    }

    /** TX_GO: as many bytes of [txAddr, txAddr + txLen) as there is room for, in order; txLen becomes the count. */
    private void sendBlock() {
        int n = 0;
        try {
            while (n < txLen && (dropping || out.size() < outCap)) {   // (dropping: the cut message's rest, through its newline)
                write(TX, mem.load8(txAddr + n) & 0xFF);
                n++;
            }
        } catch (RuntimeException e) {   // (an address past the RAM: what went, went)
        }
        txLen = n;
    }

    /** RX_GO: waiting input into [rxAddr, rxAddr + rxCap); rxLen becomes the count. */
    private void receiveBlock() {
        int n = 0;
        try {
            while (n < rxCap && !in.isEmpty()) {
                mem.store8(rxAddr + n, in.peek());
                in.poll();
                n++;
            }
        } catch (RuntimeException e) {   // (an address that isn't RAM: the byte stays waiting)
        }
        rxLen = n;
    }

    @Override
    public void write(int off, int v) {
        switch (off) {
            case TX -> {
                if (dropping) {
                    if ((v & 0xFF) == '\n') dropping = false;
                } else if (out.size() < outCap) out.write(v);
                else {
                    dropping = (v & 0xFF) != '\n';
                    cancelPending = true;
                }
            }
            case IRQ -> irq = v & 3;
            case TX_ADDR -> txAddr = v;
            case TX_LEN -> txLen = Math.max(0, v);
            case TX_GO -> {
                if (mem != null) sendBlock();
            }
            case RX_ADDR -> rxAddr = v;
            case RX_CAP -> rxCap = Math.max(0, v);
            case RX_GO -> {
                if (mem != null) receiveBlock();
            }
            default -> {
            }
        }
    }

    int status() {
        return (in.isEmpty() ? 0 : 1) | (out.size() < outCap ? 2 : 0);
    }

    @Override
    public boolean interrupt() {
        return (irq & status()) != 0;
    }

    /** Host: queue input (bytes past the input buffer's room are dropped); returns how many were taken. */
    public int give(byte[] b) {
        int n = 0;
        for (byte x : b) {
            if (in.size() >= inCap) break;
            in.add(x);
            n++;
        }
        return n;
    }

    /** A reset: what was waiting either way belongs to the program that was running, and the interrupt is off. */
    void clear() {
        in.clear();
        out.reset();
        irq = 0;
        dropping = cancelPending = false;
    }

    /** Host: the output since the last take. */
    public byte[] take() {
        if (cancelPending) {
            out.write(CANCEL);
            cancelPending = false;
        }
        byte[] b = out.toByteArray();
        out.reset();
        return b;
    }
}
