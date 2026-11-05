package mods.eln.robotics.computer.hw;

import mods.eln.robotics.computer.cpu.Cpu;
import mods.eln.robotics.computer.cpu.Memory;
import mods.eln.robotics.computer.cpu.Mmio;
import mods.eln.robotics.computer.cpu.Rom;
import mods.eln.robotics.computer.cpu.Trap;

/**
 * A computer without the world: CPU, memory, chips and devices, pure Java. The host calls tick() once per game tick
 * the computer is loaded and powered, then run() for its slice. Device pages from Memory.IO_BASE, 4 KB each:
 * 0 info, 1 timer, 2 terminal, 5 power, 7 editor channel.
 */
public final class Machine implements Mmio {
    public static final int INFO = 0, TIMER = 1, TERMINAL = 2, POWER = 5, EDITOR = 7;
    public static final int MAGIC = 0x3143_4145;   // "EAC1"
    public static final int RAM_CHIP = Memory.SLOT_BYTES;

    public final Memory mem = new Memory();
    public final Cpu cpu = new Cpu(mem);
    public final Console terminal = new Console(4096, 1024), editor = new Console(64 << 10, 64 << 10);
    private final Device[] devices = new Device[16];
    long ticks, timerCmp = -1;
    boolean halted;
    int resetCause;

    public Machine() {
        mem.setIo(this);
        devices[INFO] = new Device() {
            @Override
            public int read(int off) {
                if (off == 0) return MAGIC;
                if (off >= 0x10 && off < 0x30) return kind((off - 0x10) >> 2);
                if (off >= 0x30 && off < 0x50) return size((off - 0x30) >> 2);
                return switch (off) {
                    case 0x50 -> (int) ticks;
                    case 0x54 -> (int) (ticks >>> 32);
                    case 0x58 -> resetCause;
                    default -> 0;
                };
            }

            @Override
            public void write(int off, int v) {
            }
        };
        devices[TIMER] = new Device() {
            @Override
            public int read(int off) {
                return off == 0 ? (int) timerCmp : off == 4 ? (int) (timerCmp >>> 32) : 0;
            }

            @Override
            public void write(int off, int v) {
                if (off == 0) timerCmp = timerCmp & ~0xFFFF_FFFFL | v & 0xFFFF_FFFFL;
                else if (off == 4) timerCmp = timerCmp & 0xFFFF_FFFFL | (long) v << 32;
            }
        };
        devices[TERMINAL] = terminal;
        devices[EDITOR] = editor;
        devices[POWER] = new Device() {
            @Override
            public int read(int off) {
                return 0;
            }

            @Override
            public void write(int off, int v) {
                if (off != 0) return;
                if (v == 1) halted = true;
                else if (v == 2) reset(2);
            }
        };
    }

    private int kind(int slot) {
        Object c = mem.chip(slot);
        return c instanceof Integer ? 1 : c instanceof Rom ? 2 : 0;
    }

    private int size(int slot) {
        Object c = mem.chip(slot);
        return c instanceof Integer i ? i : c instanceof Rom r ? r.bytes : 0;
    }

    /** Start from the boot vector: registers cleared, RAM kept (a reset doesn't clear chips). */
    public void reset(int cause) {
        cpu.reset(Memory.BOOT_VECTOR);
        halted = false;
        resetCause = cause;
        timerCmp = -1;
    }

    public boolean halted() {
        return halted;
    }

    public long ticks() {
        return ticks;
    }

    /** One game tick passed for this computer: time moves on, and the timer line follows it. */
    public void tick() {
        ticks++;
        cpu.time = ticks;
        lines();
    }

    /** Run up to budget instructions; returns how many ran. */
    public int run(int budget) {
        if (halted) return 0;
        lines();
        int n = 0;
        while (n < budget && !halted) {
            int r = cpu.run(budget - n);
            n += r;
            if (cpu.waiting()) break;
        }
        return n;
    }

    /** The interrupt lines from the devices' state. */
    void lines() {
        boolean ext = false;
        for (Device d : devices) if (d != null && d.interrupt()) ext = true;
        cpu.setPending(Cpu.IRQ_EXTERNAL, ext);
        cpu.setPending(Cpu.IRQ_TIMER, Long.compareUnsigned(ticks, timerCmp) >= 0);
    }

    @Override
    public int read(int offset, int size) {
        Device d = devices[offset >>> 12 & 15];
        if (d == null || size != 4 || offset >= 16 << 12) throw new Trap(Trap.LOAD_FAULT, Memory.IO_BASE + offset);
        int v = d.read(offset & 0xFFF);
        lines();
        return v;
    }

    @Override
    public void write(int offset, int size, int value) {
        Device d = devices[offset >>> 12 & 15];
        if (d == null || size != 4 || offset >= 16 << 12) throw new Trap(Trap.STORE_FAULT, Memory.IO_BASE + offset);
        d.write(offset & 0xFFF, value);
        lines();
    }
}
