package mods.eln.robotics.computer.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The computer's address space: slots, the NULL guard, ROM, lazy RAM pages, copy-on-write snapshots, the I/O window. */
class MemoryTest {
    static final int MB = Memory.SLOT_BYTES;

    static int cause(Runnable r) {
        try {
            r.run();
        } catch (Trap t) {
            return t.cause;
        }
        return -1;
    }

    @Test
    void nullGuardFaultsEvenOverRam() {
        Memory m = new Memory();
        m.setRam(0, MB);
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(0)));
        assertEquals(Trap.STORE_FAULT, cause(() -> m.store32(0x3FC, 1)));
        assertEquals(Trap.STORE_FAULT, cause(() -> m.store32(0x3FE, 1)), "a store straddling the guard writes nothing");
        assertEquals(0, m.load32(0x400));
        m.store32(0x400, 0x12345678);
        assertEquals(0x12345678, m.load32(0x400));
        assertEquals(0x1234, m.load16(0x402));
    }

    @Test
    void romReadsAndRefusesWrites() {
        byte[] img = new byte[0x800];
        img[0x400] = 0x13;   // a nop's low byte at the boot vector
        Rom rom = new Rom(img);
        Memory m = new Memory();
        m.setRom(0, rom);
        assertEquals(0x13, m.load8(0x400));
        assertEquals(Trap.STORE_FAULT, cause(() -> m.store8(0x400, 0)));
        assertEquals(0, m.load8(0x800), "the rest of its last page reads as zeros");
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load8(0x1000)), "past the chip's last page");
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load8(0x100)), "the NULL guard");
    }

    @Test
    void emptySlotsAndOutsideFault() {
        Memory m = new Memory();
        m.setRam(1, 64 << 10);
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(2 * MB)));
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(MB + (64 << 10))));
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(0x8000_0000)));
        assertEquals(Trap.STORE_FAULT, cause(() -> m.store32(0x8000_0000, 1)));
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(Memory.IO_BASE)), "no devices attached");
    }

    @Test
    void misalignedAcrossPagesAndSlots() {
        Memory m = new Memory();
        m.setRam(1, MB);
        m.setRam(2, MB);
        int a = 2 * MB - 2;
        m.store32(a, 0xA1B2C3D4);
        assertEquals(0xA1B2C3D4, m.load32(a));
        assertEquals(0xC3D4, m.load16(a));
        assertEquals(0xA1B2, m.load16(a + 2));
    }

    @Test
    void snapshotIsCopyOnWrite() {
        Memory m = new Memory();
        m.setRam(0, MB);
        m.setRam(1, MB);
        m.store32(0x400, 1);
        m.store32(MB, 2);
        int[][] snap = m.snapshotRam();
        assertNull(snap[2], "never written");
        m.store32(0x400, 10);
        m.store32(MB, 20);
        assertEquals(10, m.load32(0x400));
        assertEquals(20, m.load32(MB));
        assertEquals(1, snap[0][0x400 >> 2], "the snapshot kept page 0 as it was");
        assertEquals(2, snap[MB >> Memory.PAGE_SHIFT][0]);

        Memory n = new Memory();
        n.setRam(0, MB);
        n.setRam(1, MB);
        n.restoreRam(snap);
        assertEquals(1, n.load32(0x400));
        assertEquals(2, n.load32(MB));
        assertEquals(0, n.load32(MB + 8192));
    }

    @Test
    void ioWindow() {
        Memory m = new Memory();
        int[] last = new int[3];
        m.setIo(new Mmio() {
            @Override
            public int read(int offset, int size) {
                return offset + size;
            }

            @Override
            public void write(int offset, int size, int value) {
                last[0] = offset;
                last[1] = size;
                last[2] = value;
            }
        });
        assertEquals(0x104 + 4, m.load32(Memory.IO_BASE + 0x104));
        m.store8(Memory.IO_BASE + 7, 0x1FF);
        assertArrayEquals(new int[]{7, 1, 0xFF}, last);
        assertEquals(Trap.LOAD_FAULT, cause(() -> m.load32(Memory.IO_BASE + 2)), "misaligned I/O");
    }

    @Test
    void aSecondSnapshotHasThePagesNotWrittenSince() {
        Memory m = new Memory();
        m.setRam(1, MB);
        m.store32(MB, 1);
        m.store32(MB + 8192, 2);
        m.snapshotRam();
        m.store32(MB + 8192, 3);   // only this page is written after the first snapshot
        int[][] snap = m.snapshotRam();
        Memory n = new Memory();
        n.setRam(1, MB);
        n.restoreRam(snap);
        assertEquals(1, n.load32(MB), "a page untouched since the first snapshot is in the second");
        assertEquals(3, n.load32(MB + 8192));
    }
}
