package mods.eln.robotics.computer.hw;

import mods.eln.robotics.computer.cpu.Cpu;
import mods.eln.robotics.computer.cpu.Memory;
import mods.eln.robotics.computer.cpu.Mmio;
import mods.eln.robotics.computer.cpu.Rom;
import mods.eln.robotics.computer.cpu.Trap;

/**
 * A computer without the world: CPU, memory, chips and devices, pure Java. The host calls tick() once per game tick
 * the computer is loaded and powered (advance() for several at once), then run() for its slice. Device pages from
 * Memory.IO_BASE, 4 KB each: 0 info, 1 timer, 2 terminal, 3 bus, 4 floppy drives, 5 power, 7 editor channel.
 */
public final class Machine implements Mmio {
    public static final int INFO = 0, TIMER = 1, TERMINAL = 2, BUS = 3, FLOPPY = 4, POWER = 5, EDITOR = 7;
    public static final int MAGIC = 0x3143_4145;   // "EAC1"
    public static final int RAM_CHIP = Memory.SLOT_BYTES;

    public final Memory mem = new Memory();
    public final Cpu cpu = new Cpu(mem);
    public final Console terminal = new Console(4096, 1024), editor = new Console(64 << 10, 64 << 10);
    public final BusDevice bus = new BusDevice();
    public final FloppyController floppy = new FloppyController(mem);
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
        devices[BUS] = bus;
        devices[FLOPPY] = floppy;
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
                else if (v == 2) reset(RESET_PROGRAM);
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

    /** One game tick passed for this computer: time moves on, disk transfers move, and the lines follow. */
    public void tick() {
        advance(1);
    }

    /** n ticks passed (a late computer catches up at its next exchange). */
    public void advance(int n) {
        for (int i = 0; i < n; i++) {
            ticks++;
            floppy.tick();
        }
        cpu.time = ticks;
        lines();
    }

    /** Would run() do anything: not halted, and not asleep on WFI with nothing to wake it? */
    public boolean runnable() {
        lines();
        return !halted && (!cpu.waiting() || cpu.wakeable());
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

    // --- saving ---

    static final int SAVE_MAGIC = 0x4541_4D31, SAVE_VERSION = 1;   // "EAM1"

    /**
     * Everything needed to resume, between slices: chips (ROMs by hash), the hart, the devices' lasting state, and
     * the RAM pages, taken copy-on-write so writing them out can happen on another thread. Console buffers, packets
     * in flight and a disk transfer in progress are not kept (a transfer restarts as failed).
     */
    public Saved save() {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream o = new java.io.DataOutputStream(b)) {
            o.writeInt(SAVE_MAGIC);
            o.writeInt(SAVE_VERSION);
            for (int s = 0; s < Memory.SLOTS; s++) {
                Object c = mem.chip(s);
                o.writeByte(c instanceof Integer ? 1 : c instanceof Rom ? 2 : 0);
                o.writeInt(size(s));
                o.writeUTF(c instanceof Rom r ? r.hash : "");
            }
            cpu.save(o);
            o.writeLong(ticks);
            o.writeLong(timerCmp);
            o.writeBoolean(halted);
            o.writeInt(resetCause);
            o.writeInt(terminal.irq);
            o.writeInt(editor.irq);
            o.writeInt(bus.irq);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return new Saved(b.toByteArray(), mem.snapshotRam());
    }

    /** A machine's saved state: the header and hart, and the RAM pages (null: never written). */
    public record Saved(byte[] state, int[][] pages) {
        /** The whole thing as one compressed stream (run it off the server thread). */
        public void write(java.io.OutputStream out) throws java.io.IOException {
            java.util.zip.DeflaterOutputStream d = new java.util.zip.DeflaterOutputStream(out);
            java.io.DataOutputStream o = new java.io.DataOutputStream(d);
            o.writeInt(state.length);
            o.write(state);
            for (int p = 0; p < pages.length; p++) {
                if (pages[p] == null) continue;
                o.writeInt(p);
                for (int w : pages[p]) o.writeInt(w);
            }
            o.writeInt(-1);
            o.flush();
            d.finish();
        }

        public static Saved read(java.io.InputStream in) throws java.io.IOException {
            java.io.DataInputStream i = new java.io.DataInputStream(new java.util.zip.InflaterInputStream(in));
            byte[] state = new byte[i.readInt()];
            i.readFully(state);
            int[][] pages = new int[Memory.SLOTS * Memory.SLOT_BYTES >> Memory.PAGE_SHIFT][];
            for (int p; (p = i.readInt()) >= 0; ) {
                if (p >= pages.length) throw new java.io.IOException("bad page " + p);
                int[] page = new int[Memory.PAGE_BYTES >> 2];
                for (int w = 0; w < page.length; w++) page[w] = i.readInt();
                pages[p] = page;
            }
            return new Saved(state, pages);
        }
    }

    /**
     * Resume from a save. The chips must already be in their slots; if they differ from the saved ones (a ROM changed
     * with a mod update, a chip swapped), the RAM is not restored and the computer cold-boots instead. Returns
     * whether it resumed.
     */
    public boolean load(Saved saved) {
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(saved.state()))) {
            if (in.readInt() != SAVE_MAGIC || in.readInt() != SAVE_VERSION) return coldBoot();
            for (int s = 0; s < Memory.SLOTS; s++) {
                Object c = mem.chip(s);
                int kind = in.readByte(), size = in.readInt();
                String hash = in.readUTF();
                int k = c instanceof Integer ? 1 : c instanceof Rom ? 2 : 0;
                if (kind != k || size != size(s) || !hash.equals(c instanceof Rom r ? r.hash : "")) return coldBoot();
            }
            cpu.load(in);
            ticks = in.readLong();
            timerCmp = in.readLong();
            halted = in.readBoolean();
            resetCause = in.readInt();
            terminal.irq = in.readInt();
            editor.irq = in.readInt();
            bus.irq = in.readInt();
        } catch (java.io.IOException e) {
            return coldBoot();
        }
        mem.restoreRam(saved.pages());
        cpu.time = ticks;
        lines();
        return true;
    }

    private boolean coldBoot() {
        reset(RESET_CHANGED);
        return false;
    }

    /** Reset causes, as the info page reports them. */
    public static final int RESET_POWER = 0, RESET_PROGRAM = 2, RESET_CHANGED = 3;
}
