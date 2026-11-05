package mods.eln.robotics.computer.hw;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.robotics.computer.cpu.Memory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The bus port and the floppy drives through their registers, as a program sees them. */
class DevicesTest {
    @Test
    void busSendsWithItsAddressOrAFakeOne() {
        BusDevice b = new BusDevice();
        b.setState(200, true, null);
        b.write(BusDevice.TX_TARGET, 30);
        b.write(BusDevice.TX_TYPE, PacketType.WRITE.id);
        b.write(BusDevice.TX_INDEX, 0);
        b.write(BusDevice.TX_VALUE, Float.floatToRawIntBits(5f));
        b.write(BusDevice.SEND, 1);
        b.write(BusDevice.TX_SOURCE, 42);
        b.write(BusDevice.SEND, 2);
        b.write(BusDevice.TX_TYPE, 99);   // unknown type: never sent
        b.write(BusDevice.SEND, 1);
        List<Packet> out = b.take();
        assertEquals(2, out.size());
        assertEquals(new Packet(30, 200, PacketType.WRITE, 0, 5f), out.get(0));
        assertEquals(42, out.get(1).source());
    }

    @Test
    void busCapsItsSendsPerTick() {
        BusDevice b = new BusDevice();
        b.write(BusDevice.TX_TYPE, PacketType.PRESS.id);
        for (int i = 0; i < 20; i++) b.write(BusDevice.SEND, 1);
        assertEquals(BusDevice.OUT_CAP, b.take().size());
        assertEquals(4, b.read(BusDevice.DROPPED));
    }

    @Test
    void busReceivesInOrderAndInterrupts() {
        BusDevice b = new BusDevice();
        b.write(BusDevice.IRQ, 1);
        assertFalse(b.interrupt());
        b.give(List.of(new Packet(0, 23, PacketType.PRESS, 1, 0), new Packet(200, 30, PacketType.REPLY, 1, 2.5f)));
        assertTrue(b.interrupt());
        assertEquals(2, b.read(BusDevice.RX_COUNT));
        b.write(BusDevice.RX_POP, 1);
        assertEquals(23, b.read(BusDevice.RX_SOURCE));
        assertEquals(PacketType.PRESS.id, b.read(BusDevice.RX_TYPE));
        b.write(BusDevice.RX_POP, 1);
        assertEquals(2.5f, Float.intBitsToFloat(b.read(BusDevice.RX_VALUE)));
        assertFalse(b.interrupt());
    }

    static Machine withRam() {
        Machine m = new Machine();
        m.mem.setRam(1, Machine.RAM_CHIP);
        m.mem.setRom(2, new mods.eln.robotics.computer.cpu.Rom(new byte[4096]));
        return m;
    }

    static void cmd(FloppyController f, int drive, int lba, int addr, int count, int c) {
        f.write(FloppyController.DRIVE, drive);
        f.write(FloppyController.LBA, lba);
        f.write(FloppyController.ADDR, addr);
        f.write(FloppyController.COUNT, count);
        f.write(FloppyController.CMD, c);
    }

    @Test
    void floppyReadsTakeTicks() {
        Machine m = withRam();
        byte[] img = new byte[Floppy.BYTES];
        for (int i = 0; i < img.length; i++) img[i] = (byte) (i / Floppy.SECTOR);
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), img, false));
        m.floppy.write(FloppyController.IRQ, 1);
        cmd(m.floppy, 0, 10, Memory.SLOT_BYTES, 100, 1);
        assertEquals(1, m.floppy.read(FloppyController.STATUS) & 1, "busy");
        m.tick();
        assertFalse(m.floppy.interrupt(), "64 sectors a tick: not done after one");
        m.tick();
        assertTrue(m.floppy.interrupt());
        assertEquals(10, m.mem.load8(Memory.SLOT_BYTES));
        assertEquals(109, m.mem.load8(Memory.SLOT_BYTES + 99 * Floppy.SECTOR));
    }

    @Test
    void floppyWritesMarkSectorsAndRespectTheTab() {
        Machine m = withRam();
        Floppy f = new Floppy(java.util.UUID.randomUUID(), null, true);
        m.floppy.insert(1, f);
        m.mem.store8(Memory.SLOT_BYTES, 0x5A);
        cmd(m.floppy, 1, 3, Memory.SLOT_BYTES, 1, 2);
        assertEquals(FloppyController.ERR_PROTECTED, m.floppy.read(FloppyController.ERROR));
        f.writeProtected = false;
        cmd(m.floppy, 1, 3, Memory.SLOT_BYTES, 1, 2);
        m.tick();
        assertTrue(f.dirty());
        assertEquals(0x5A, f.snapshot()[3 * Floppy.SECTOR]);
        assertFalse(f.dirty());
    }

    @Test
    void floppyDmaOnlyIntoRam() {
        Machine m = withRam();
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), null, false));
        cmd(m.floppy, 0, 0, 2 * Memory.SLOT_BYTES, 1, 1);   // the ROM in slot 2
        m.tick();
        assertEquals(FloppyController.ERR_MEMORY, m.floppy.read(FloppyController.ERROR));
        cmd(m.floppy, 0, 0, 0, 1, 1);                       // the NULL guard
        m.tick();
        assertEquals(FloppyController.ERR_MEMORY, m.floppy.read(FloppyController.ERROR));
        cmd(m.floppy, 0, 2879, Memory.SLOT_BYTES, 2, 1);    // off the end of the disk
        assertEquals(FloppyController.ERR_RANGE, m.floppy.read(FloppyController.ERROR));
    }
}
