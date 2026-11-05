package mods.eln.robotics.computer.hw;

/**
 * A byte channel between the program and the screen: the terminal, or the editor's channel. The program writes TX
 * and reads RX (-1: nothing waiting); STATUS bit 0 is input waiting, bit 1 room to send; IRQ enables the interrupt
 * for either. The host side takes output and gives input between slices.
 */
public final class Console implements Device {
    static final int TX = 0x00, RX = 0x04, STATUS = 0x08, IRQ = 0x0C;
    private final java.util.ArrayDeque<Byte> in = new java.util.ArrayDeque<>();
    private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    private final int outCap, inCap;
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
            default -> 0;
        };
    }

    @Override
    public void write(int off, int v) {
        switch (off) {
            case TX -> {
                if (out.size() < outCap) out.write(v);
            }
            case IRQ -> irq = v & 3;
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

    /** Host: the output since the last take. */
    public byte[] take() {
        byte[] b = out.toByteArray();
        out.reset();
        return b;
    }
}
