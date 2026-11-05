package mods.eln.robotics.computer.hw;

import mods.eln.robotics.computer.cpu.Memory;
import mods.eln.robotics.computer.cpu.Trap;

/**
 * Two floppy drives, A (0) and B (1). A transfer: set DRIVE, LBA, ADDR (RAM), COUNT (sectors), then write CMD = 1
 * (read the disk into RAM) or 2 (write RAM to the disk). It moves SECTORS_PER_TICK sectors at each tick and then
 * sets done, which raises the interrupt if enabled. A range that leaves the disk or touches anything but RAM fails
 * with an error code and moves nothing more.
 */
public final class FloppyController implements Device {
    static final int DRIVE = 0x00, LBA = 0x04, ADDR = 0x08, COUNT = 0x0C, CMD = 0x10, STATUS = 0x14, IRQ = 0x18,
            ERROR = 0x1C, CHANGED = 0x20;
    public static final int SECTORS_PER_TICK = 64;
    public static final int ERR_NONE = 0, ERR_NO_DISK = 1, ERR_RANGE = 2, ERR_MEMORY = 3, ERR_PROTECTED = 4,
            ERR_BUSY = 5, ERR_COMMAND = 6;

    private final Memory mem;
    final Floppy[] drives = new Floppy[2];
    private int drive, lba, addr, count, error, irq, changed;
    private int cmd;              // the transfer in progress: 0 none, 1 read, 2 write
    private int done;             // sectors moved of it
    private boolean finished;

    FloppyController(Memory mem) {
        this.mem = mem;
    }

    @Override
    public int read(int off) {
        return switch (off) {
            case DRIVE -> drive;
            case LBA -> lba;
            case ADDR -> addr;
            case COUNT -> count;
            case STATUS -> (cmd != 0 ? 1 : 0) | (finished ? 2 : 0) | (error != 0 ? 4 : 0)
                    | (drives[0] != null ? 8 : 0) | (drives[1] != null ? 16 : 0)
                    | (drives[0] != null && drives[0].writeProtected ? 32 : 0)
                    | (drives[1] != null && drives[1].writeProtected ? 64 : 0);
            case IRQ -> irq;
            case ERROR -> error;
            case CHANGED -> changed;
            default -> 0;
        };
    }

    @Override
    public void write(int off, int v) {
        switch (off) {
            case DRIVE -> drive = v & 1;
            case LBA -> lba = v;
            case ADDR -> addr = v;
            case COUNT -> count = v;
            case CMD -> start(v);
            case IRQ -> irq = v & 1;
            case CHANGED -> changed &= ~v;
            case STATUS -> finished = false;   // any write acknowledges done
            default -> {
            }
        }
    }

    private void start(int c) {
        finished = false;
        if (cmd != 0) {
            fail(ERR_BUSY);
            return;
        }
        error = ERR_NONE;
        Floppy f = drives[drive];
        if (c != 1 && c != 2) fail(ERR_COMMAND);
        else if (f == null) fail(ERR_NO_DISK);
        else if (count < 0 || lba < 0 || (long) lba + count > Floppy.SECTORS) fail(ERR_RANGE);
        else if (c == 2 && f.writeProtected) fail(ERR_PROTECTED);
        else {
            cmd = c;
            done = 0;
        }
    }

    private void fail(int e) {
        error = e;
        cmd = 0;
        finished = true;
    }

    /** One tick: move up to SECTORS_PER_TICK sectors of the transfer in progress. */
    void tick() {
        if (cmd == 0) return;
        Floppy f = drives[drive];
        if (f == null) {
            fail(ERR_NO_DISK);
            return;
        }
        int n = Math.min(SECTORS_PER_TICK, count - done);
        try {
            for (int s = 0; s < n; s++) {
                int sector = lba + done, base = addr + done * Floppy.SECTOR, off = sector * Floppy.SECTOR;
                if (cmd == 1) for (int i = 0; i < Floppy.SECTOR; i++) mem.store8(base + i, f.data[off + i]);
                else {
                    for (int i = 0; i < Floppy.SECTOR; i++) f.data[off + i] = (byte) mem.load8(base + i);
                    f.dirty.set(sector);
                }
                done++;
            }
        } catch (Trap t) {
            fail(ERR_MEMORY);
            return;
        }
        if (done >= count) {
            cmd = 0;
            finished = true;
        }
    }

    @Override
    public boolean interrupt() {
        return (irq & 1) != 0 && finished;
    }

    /** Host: put a disk in a drive (null: eject); a transfer on it stops with an error. */
    public void insert(int d, Floppy f) {
        if (drives[d] == f) return;
        if (cmd != 0 && drive == d) fail(ERR_NO_DISK);
        drives[d] = f;
        changed |= 1 << d;
    }

    public Floppy disk(int d) {
        return drives[d];
    }
}
