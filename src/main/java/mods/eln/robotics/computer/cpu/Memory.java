package mods.eln.robotics.computer.cpu;

/**
 * The computer's address space, little-endian: ROM at 0 (read-only), the I/O window at IO_BASE, RAM at RAM_BASE.
 * Anything else faults. RAM remembers which 4 KB pages were written since the last clearDirty(), so saving can skip
 * the rest. Misaligned accesses work (as a pair of aligned ones) in RAM and ROM; the I/O window takes aligned ones only.
 */
public final class Memory {
    public static final int ROM_BASE = 0, IO_BASE = 0x1000_0000, IO_SIZE = 0x1000_0000, RAM_BASE = 0x8000_0000;
    public static final int PAGE_SHIFT = 12;

    final int[] rom, ram;
    final int romBytes, ramBytes;
    private final long[] dirty;
    Mmio io = Mmio.NONE;

    /** rom: the ROM image (may be null), ramBytes: a multiple of 4 KB. */
    public Memory(byte[] romImage, int ramBytes) {
        if (ramBytes <= 0 || (ramBytes & ((1 << PAGE_SHIFT) - 1)) != 0) throw new IllegalArgumentException("RAM size");
        int rb = romImage == null ? 0 : (romImage.length + 3) & ~3;
        rom = new int[rb >> 2];
        if (romImage != null) for (int i = 0; i < romImage.length; i++) rom[i >> 2] |= (romImage[i] & 0xFF) << ((i & 3) * 8);
        romBytes = rb;
        this.ramBytes = ramBytes;
        ram = new int[ramBytes >> 2];
        dirty = new long[((ramBytes >> PAGE_SHIFT) + 63) >> 6];
    }

    public void setIo(Mmio io) {
        this.io = io == null ? Mmio.NONE : io;
    }

    public int ramBytes() {
        return ramBytes;
    }

    // --- word access, the one place that maps addresses ---

    /** The aligned word holding address a, for a load (or a fetch, with cause FETCH_FAULT). */
    int word(int a, int cause) {
        int off = a - RAM_BASE;
        if (Integer.compareUnsigned(off, ramBytes) < 0) return ram[off >>> 2];
        if (Integer.compareUnsigned(a, romBytes) < 0) return rom[a >>> 2];
        throw new Trap(cause, a);
    }

    private void storeWord(int a, int mask, int bits) {
        int off = a - RAM_BASE;
        if (Integer.compareUnsigned(off, ramBytes) >= 0) throw new Trap(Trap.STORE_FAULT, a);
        int i = off >>> 2;
        ram[i] = ram[i] & ~mask | bits & mask;
        int page = off >>> PAGE_SHIFT;
        dirty[page >> 6] |= 1L << page;
    }

    static boolean io(int a) {
        return Integer.compareUnsigned(a - IO_BASE, IO_SIZE) < 0;
    }

    // --- loads ---

    public int load32(int a) {
        if (io(a)) {
            if ((a & 3) != 0) throw new Trap(Trap.LOAD_FAULT, a);
            return io.read(a - IO_BASE, 4);
        }
        int s = (a & 3) * 8;
        if (s == 0) return word(a, Trap.LOAD_FAULT);
        return word(a, Trap.LOAD_FAULT) >>> s | word(a + 4, Trap.LOAD_FAULT) << (32 - s);
    }

    /** A 16-bit load, zero-extended. */
    public int load16(int a) {
        if (io(a)) {
            if ((a & 1) != 0) throw new Trap(Trap.LOAD_FAULT, a);
            return io.read(a - IO_BASE, 2) & 0xFFFF;
        }
        int s = (a & 3) * 8;
        if (s <= 16) return word(a, Trap.LOAD_FAULT) >>> s & 0xFFFF;
        return (word(a, Trap.LOAD_FAULT) >>> 24 | word(a + 4, Trap.LOAD_FAULT) << 8) & 0xFFFF;
    }

    /** An 8-bit load, zero-extended. */
    public int load8(int a) {
        if (io(a)) return io.read(a - IO_BASE, 1) & 0xFF;
        return word(a, Trap.LOAD_FAULT) >>> ((a & 3) * 8) & 0xFF;
    }

    // --- stores ---

    public void store32(int a, int v) {
        if (io(a)) {
            if ((a & 3) != 0) throw new Trap(Trap.STORE_FAULT, a);
            io.write(a - IO_BASE, 4, v);
            return;
        }
        int s = (a & 3) * 8;
        if (s == 0) {
            storeWord(a, -1, v);
            return;
        }
        checkStore(a + 4);
        storeWord(a, -1 << s, v << s);
        storeWord(a + 4, -1 >>> (32 - s), v >>> (32 - s));
    }

    public void store16(int a, int v) {
        if (io(a)) {
            if ((a & 1) != 0) throw new Trap(Trap.STORE_FAULT, a);
            io.write(a - IO_BASE, 2, v & 0xFFFF);
            return;
        }
        int s = (a & 3) * 8;
        if (s <= 16) {
            storeWord(a, 0xFFFF << s, v << s);
            return;
        }
        checkStore(a + 4);
        storeWord(a, 0xFF00_0000, v << 24);
        storeWord(a + 4, 0xFF, v >>> 8);
    }

    public void store8(int a, int v) {
        if (io(a)) {
            io.write(a - IO_BASE, 1, v & 0xFF);
            return;
        }
        int s = (a & 3) * 8;
        storeWord(a, 0xFF << s, v << s);
    }

    /** Fault before the first half of a split store is written, so a faulting store changes nothing. */
    private void checkStore(int a) {
        if (Integer.compareUnsigned(a - RAM_BASE, ramBytes) >= 0) throw new Trap(Trap.STORE_FAULT, a);
    }

    // --- whole-image access (loading, saving, tests) ---

    /** Copy bytes into RAM at address a (RAM only), marking the pages dirty. */
    public void loadRam(int a, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) store8(a + i, bytes[i]);
    }

    public byte[] readRam(int a, int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) load8(a + i);
        return b;
    }

    /** Was this RAM page (index from RAM_BASE) written since clearDirty()? */
    public boolean dirty(int page) {
        return (dirty[page >> 6] >>> page & 1) != 0;
    }

    public void clearDirty() {
        java.util.Arrays.fill(dirty, 0);
    }

    /** The RAM words themselves (saving and loading whole images). */
    public int[] ramWords() {
        return ram;
    }
}
