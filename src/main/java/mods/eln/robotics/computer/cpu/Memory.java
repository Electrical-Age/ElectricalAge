package mods.eln.robotics.computer.cpu;

/**
 * The computer's address space, little-endian (Baughn: 8 slots of 1 MB, boot vector at 0x400, NULL unmapped).
 * Slot k covers k MB .. k+1 MB and holds a RAM chip, a ROM chip or nothing; the first 1 KB, empty slots, the part
 * of a slot past its chip's last page, and everything outside the slots and the I/O window fault.
 * <p>
 * Memory is 4 KB pages. RAM pages are allocated on their first write (until then they read as zeros), so an unused
 * chip costs no heap. snapshotRam() hands out the RAM pages as they are and makes them copy-on-write: the next write
 * to each copies it first, so a save can read the snapshot on another thread while the CPU runs on.
 * Misaligned accesses work (as a pair of aligned ones) outside the I/O window, which takes aligned ones only.
 */
public final class Memory {
    public static final int SLOTS = 8, SLOT_BYTES = 1 << 20, PAGE_SHIFT = 12, PAGE_BYTES = 1 << PAGE_SHIFT;
    public static final int NULL_GUARD = 0x400, BOOT_VECTOR = 0x400;
    public static final int IO_BASE = 0x1000_0000, IO_SIZE = 0x0001_0000;
    static final int PAGES = SLOTS * SLOT_BYTES >> PAGE_SHIFT, WORDS = PAGE_BYTES >> 2;
    private static final int[] ZERO = new int[WORDS];
    private static final byte NONE = 0, RAM = 1, ROM = 2;

    /** Readable pages (null: fault, or page 0, which takes the slow path for the NULL guard). */
    private final int[][] pages = new int[PAGES][];
    /** Pages writable in place (null: fault, or not yet allocated, or shared with a snapshot). */
    private final int[][] writable = new int[PAGES][];
    private final byte[] kind = new byte[PAGES];
    private final Object[] chips = new Object[SLOTS];
    /** Page 0's data (pages[0] stays null so that every access to it checks the NULL guard). */
    private int[] page0;
    Mmio io = Mmio.NONE;

    public void setIo(Mmio io) {
        this.io = io == null ? Mmio.NONE : io;
    }

    // --- chips ---

    /** Put a RAM chip of this many bytes (a multiple of 4 KB, at most 1 MB) in a slot; it starts as zeros. */
    public void setRam(int slot, int bytes) {
        if (bytes <= 0 || bytes > SLOT_BYTES || (bytes & (PAGE_BYTES - 1)) != 0) throw new IllegalArgumentException("RAM size");
        clear(slot);
        int first = slot * (SLOT_BYTES >> PAGE_SHIFT);
        for (int p = first; p < first + (bytes >> PAGE_SHIFT); p++) {
            kind[p] = RAM;
            if (p == 0) page0 = ZERO;
            else pages[p] = ZERO;
        }
        chips[slot] = bytes;
    }

    public void setRom(int slot, Rom rom) {
        clear(slot);
        int first = slot * (SLOT_BYTES >> PAGE_SHIFT);
        for (int i = 0; i < rom.pages.length; i++) {
            kind[first + i] = ROM;
            if (first + i == 0) page0 = rom.pages[0];
            else pages[first + i] = rom.pages[i];
        }
        chips[slot] = rom;
    }

    public void clear(int slot) {
        int first = slot * (SLOT_BYTES >> PAGE_SHIFT);
        for (int p = first; p < first + (SLOT_BYTES >> PAGE_SHIFT); p++) {
            pages[p] = writable[p] = null;
            kind[p] = NONE;
        }
        if (slot == 0) page0 = null;
        chips[slot] = null;
    }

    /** What a slot holds: an Integer (RAM bytes), a Rom, or null. */
    public Object chip(int slot) {
        return chips[slot];
    }

    // --- the slow paths: page 0, allocation, copy-on-write, faults ---

    /** Page p's data for reading, or null: nothing there. */
    private int[] slowRead(int a, int p) {
        return p == 0 && Integer.compareUnsigned(a, NULL_GUARD) >= 0 ? page0 : null;
    }

    /** Page p made writable in place (allocated, or copied from a snapshot's), or null: not RAM. */
    private int[] makeWritable(int a, int p) {
        if (kind[p] != RAM || (p == 0 && Integer.compareUnsigned(a, NULL_GUARD) < 0)) return null;
        int[] old = p == 0 ? page0 : pages[p];
        int[] page = old == ZERO ? new int[WORDS] : old.clone();
        writable[p] = page;
        if (p == 0) page0 = page;
        else pages[p] = page;
        return page;
    }

    // --- word access ---

    /** The aligned word holding address a, for a load (or a fetch, with cause FETCH_FAULT). */
    int word(int a, int cause) {
        int p = a >>> PAGE_SHIFT;
        if (p < PAGES) {
            int[] page = pages[p];
            if (page == null && (page = slowRead(a, p)) == null) throw new Trap(cause, a);
            return page[(a >>> 2) & (WORDS - 1)];
        }
        throw new Trap(cause, a);
    }

    private void storeWord(int a, int mask, int bits) {
        int p = a >>> PAGE_SHIFT;
        int[] page = p < PAGES ? writable[p] : null;
        if (page == null && (p >= PAGES || (page = makeWritable(a, p)) == null)) throw new Trap(Trap.STORE_FAULT, a);
        int i = (a >>> 2) & (WORDS - 1);
        page[i] = page[i] & ~mask | bits & mask;
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
        storeWord(a, 0xFF << ((a & 3) * 8), v << ((a & 3) * 8));
    }

    /** Fault before the first half of a split store is written, so a faulting store changes nothing. */
    private void checkStore(int a) {
        int p = a >>> PAGE_SHIFT;
        if (p >= PAGES || kind[p] != RAM || (p == 0 && Integer.compareUnsigned(a, NULL_GUARD) < 0))
            throw new Trap(Trap.STORE_FAULT, a);
    }

    // --- whole images ---

    /** Copy bytes into RAM at address a (loading programs, tests). */
    public void loadRam(int a, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) store8(a + i, bytes[i]);
    }

    public byte[] read(int a, int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) load8(a + i);
        return b;
    }

    /**
     * The RAM pages as they are now, indexed by page number (null: not RAM, or never written), for saving. They
     * stay as they are: the CPU's next write to any of them copies it first. Call it between slices.
     */
    public int[][] snapshotRam() {
        int[][] snap = new int[PAGES][];
        for (int p = 0; p < PAGES; p++) {
            if (kind[p] != RAM) continue;
            int[] data = p == 0 ? page0 : pages[p];
            if (data == null || data == ZERO) continue;
            snap[p] = data;           // every page that holds anything, written since the last snapshot or not
            writable[p] = null;       // still readable; the next write copies
        }
        return snap;
    }

    /** Put saved RAM pages back (null pages read as zeros); the slots must hold the same RAM chips. */
    public void restoreRam(int[][] saved) {
        for (int p = 0; p < PAGES; p++) {
            if (kind[p] != RAM) continue;
            int[] page = saved[p] == null ? null : saved[p].clone();
            writable[p] = page;
            if (p == 0) page0 = page == null ? ZERO : page;
            else pages[p] = page == null ? ZERO : page;
        }
    }
}
