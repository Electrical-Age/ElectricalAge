package mods.eln.robotics.computer.cpu;

/** The devices behind the I/O window (Memory.IO_BASE..). Offsets are from IO_BASE; size is 1, 2 or 4 bytes. */
public interface Mmio {
    /** The value read, or throw Trap(LOAD_FAULT) for nothing there. */
    int read(int offset, int size);

    /** Throw Trap(STORE_FAULT) for nothing there. */
    void write(int offset, int size, int value);

    Mmio NONE = new Mmio() {
        @Override
        public int read(int offset, int size) {
            throw new Trap(Trap.LOAD_FAULT, Memory.IO_BASE + offset);
        }

        @Override
        public void write(int offset, int size, int value) {
            throw new Trap(Trap.STORE_FAULT, Memory.IO_BASE + offset);
        }
    };
}
