package mods.eln.robotics.computer.cpu;

/**
 * One RV32IMAFDC hart with Zicsr and Zifencei, machine mode only, no MMU: an interpreter. run(budget) executes up to
 * budget instructions and stops early when the hart sleeps on WFI; it wakes when an enabled interrupt is pending
 * (setPending), whether or not interrupts are globally on, as the spec allows.
 * <p>
 * 16-bit instructions are expanded to their 32-bit forms (Rvc), so there is one decoder. Floating point is in Fpu.
 */
public final class Cpu {
    public final int[] x = new int[32];
    /** FP registers, raw bits; singles are NaN-boxed in the upper half. */
    public final long[] f = new long[32];
    public int pc;
    public final Memory mem;
    private final Fpu fpu = new Fpu(this);

    // machine CSRs
    int mstatus, mie, mip, mtvec, mepc, mcause, mtval, mscratch;
    int fflags, frm;
    long cycle, instret;
    /** The time CSR (the host's clock, in its own units). */
    public long time;
    boolean waiting;
    /** LR/SC reservation: an address, or -1. */
    int reservation = -1;

    public static final int MSTATUS_MIE = 1 << 3, MSTATUS_MPIE = 1 << 7, MSTATUS_MPP = 3 << 11, MSTATUS_FS = 3 << 13;
    public static final int IRQ_SOFT = 3, IRQ_TIMER = 7, IRQ_EXTERNAL = 11;
    static final int MISA = 1 << 30 | 1 /* A */ | 1 << 2 /* C */ | 1 << 3 /* D */ | 1 << 5 /* F */ | 1 << 8 /* I */
            | 1 << 12 /* M */;

    public Cpu(Memory mem) {
        this.mem = mem;
        reset(Memory.ROM_BASE);
    }

    /** Registers cleared, pc at the reset vector, FPU off, interrupts off. Memory is left alone. */
    public void reset(int resetPc) {
        java.util.Arrays.fill(x, 0);
        java.util.Arrays.fill(f, 0);
        pc = resetPc;
        mstatus = MSTATUS_MPP;
        mie = mip = mepc = mcause = mtval = mscratch = 0;
        mtvec = resetPc;
        fflags = frm = 0;
        waiting = false;
        reservation = -1;
    }

    /** Sleeping on WFI. */
    public boolean waiting() {
        return waiting;
    }

    public long instret() {
        return instret;
    }

    /** Raise or clear an interrupt line (IRQ_SOFT, IRQ_TIMER, IRQ_EXTERNAL). */
    public void setPending(int irq, boolean on) {
        if (on) mip |= 1 << irq;
        else mip &= ~(1 << irq);
    }

    /** Execute up to budget instructions; returns how many ran (fewer when it went to sleep on WFI). */
    public int run(int budget) {
        int n = 0;
        while (n < budget) {
            if ((mip & mie) != 0) {
                waiting = false;
                if ((mstatus & MSTATUS_MIE) != 0) interrupt();
            }
            if (waiting) break;
            try {
                step();
            } catch (Trap t) {
                trap(t.cause, t.tval, false);
            }
            n++;
            instret++;
        }
        cycle += n;
        return n;
    }

    private void interrupt() {
        int p = mip & mie;
        int irq = (p & 1 << IRQ_EXTERNAL) != 0 ? IRQ_EXTERNAL : (p & 1 << IRQ_SOFT) != 0 ? IRQ_SOFT : IRQ_TIMER;
        trap(irq, 0, true);
    }

    /** Enter the trap handler at mtvec (vectored for interrupts if mtvec's mode is 1). */
    void trap(int cause, int tval, boolean interrupt) {
        mepc = pc;
        mcause = interrupt ? (1 << 31 | cause) : cause;
        mtval = tval;
        int mieBit = (mstatus & MSTATUS_MIE) != 0 ? MSTATUS_MPIE : 0;
        mstatus = mstatus & ~(MSTATUS_MIE | MSTATUS_MPIE) | mieBit;
        int base = mtvec & ~3;
        pc = interrupt && (mtvec & 3) == 1 ? base + 4 * cause : base;
        reservation = -1;
    }

    /** The length of the instruction fetch() returned: 2 or 4. */
    private int ilen;

    /** The instruction at pc in its 32-bit form. */
    private int fetch() {
        int p = pc;
        if ((p & 1) != 0) throw new Trap(Trap.MISALIGNED_FETCH, p);
        int w = mem.word(p, Trap.FETCH_FAULT);
        int lo = (p & 2) == 0 ? w & 0xFFFF : w >>> 16;
        if ((lo & 3) != 3) {
            ilen = 2;
            int e = Rvc.expand16(lo);
            if (e == 0) throw new Trap(Trap.ILLEGAL, lo);
            return e;
        }
        ilen = 4;
        if ((p & 2) == 0) return w;
        return lo | mem.word(p + 4, Trap.FETCH_FAULT) << 16;
    }

    void step() {
        int inst = fetch();
        int next = pc + ilen;
        int rd = inst >>> 7 & 31, f3 = inst >>> 12 & 7, rs1 = inst >>> 15 & 31, rs2 = inst >>> 20 & 31;
        int[] x = this.x;
        switch (inst & 0x7F) {
            case 0x37 -> x[rd] = inst & 0xFFFFF000;                                     // LUI
            case 0x17 -> x[rd] = pc + (inst & 0xFFFFF000);                              // AUIPC
            case 0x6F -> {                                                              // JAL
                int imm = (inst >> 31) << 20 | (inst >>> 21 & 0x3FF) << 1 | (inst >>> 20 & 1) << 11
                        | (inst >>> 12 & 0xFF) << 12;
                x[rd] = next;
                next = pc + imm;
            }
            case 0x67 -> {                                                              // JALR
                if (f3 != 0) throw illegal(inst);
                int t = (x[rs1] + (inst >> 20)) & ~1;
                x[rd] = next;
                next = t;
            }
            case 0x63 -> {                                                              // branches
                int a = x[rs1], b = x[rs2];
                boolean take = switch (f3) {
                    case 0 -> a == b;
                    case 1 -> a != b;
                    case 4 -> a < b;
                    case 5 -> a >= b;
                    case 6 -> Integer.compareUnsigned(a, b) < 0;
                    case 7 -> Integer.compareUnsigned(a, b) >= 0;
                    default -> throw illegal(inst);
                };
                if (take) next = pc + ((inst >> 31) << 12 | (inst >>> 25 & 0x3F) << 5 | (inst >>> 8 & 0xF) << 1
                        | (inst >>> 7 & 1) << 11);
            }
            case 0x03 -> {                                                              // loads
                int a = x[rs1] + (inst >> 20);
                int v = switch (f3) {
                    case 0 -> (byte) mem.load8(a);
                    case 1 -> (short) mem.load16(a);
                    case 2 -> mem.load32(a);
                    case 4 -> mem.load8(a);
                    case 5 -> mem.load16(a);
                    default -> throw illegal(inst);
                };
                x[rd] = v;
            }
            case 0x23 -> {                                                              // stores
                int a = x[rs1] + ((inst >> 25) << 5 | rd);
                switch (f3) {
                    case 0 -> mem.store8(a, x[rs2]);
                    case 1 -> mem.store16(a, x[rs2]);
                    case 2 -> mem.store32(a, x[rs2]);
                    default -> throw illegal(inst);
                }
                if (reservation != -1 && (a ^ reservation) >>> 3 == 0) reservation = -1;
            }
            case 0x13 -> {                                                              // OP-IMM
                int a = x[rs1], imm = inst >> 20;
                x[rd] = switch (f3) {
                    case 0 -> a + imm;
                    case 1 -> {
                        if ((inst >>> 25) != 0) throw illegal(inst);
                        yield a << rs2;
                    }
                    case 2 -> a < imm ? 1 : 0;
                    case 3 -> Integer.compareUnsigned(a, imm) < 0 ? 1 : 0;
                    case 4 -> a ^ imm;
                    case 5 -> switch (inst >>> 25) {
                        case 0 -> a >>> rs2;
                        case 0x20 -> a >> rs2;
                        default -> throw illegal(inst);
                    };
                    case 6 -> a | imm;
                    default -> a & imm;
                };
            }
            case 0x33 -> x[rd] = op(inst, f3, x[rs1], x[rs2]);                          // OP, M
            case 0x0F -> {                                                              // FENCE, FENCE.I
                if (f3 > 1) throw illegal(inst);
            }
            case 0x73 -> next = system(inst, rd, f3, rs1, next);
            case 0x2F -> amo(inst, rd, f3, rs1, rs2);
            case 0x07, 0x27, 0x43, 0x47, 0x4B, 0x4F, 0x53 -> fpu.exec(inst);
            default -> throw illegal(inst);
        }
        x[0] = 0;
        pc = next;
    }

    static Trap illegal(int inst) {
        return new Trap(Trap.ILLEGAL, inst);
    }

    private static int op(int inst, int f3, int a, int b) {
        int f7 = inst >>> 25;
        if (f7 == 1) return switch (f3) {
            case 0 -> a * b;
            case 1 -> (int) (((long) a * b) >> 32);
            case 2 -> (int) (((long) a * (b & 0xFFFFFFFFL)) >> 32);
            case 3 -> (int) (((a & 0xFFFFFFFFL) * (b & 0xFFFFFFFFL)) >>> 32);
            case 4 -> b == 0 ? -1 : (a == Integer.MIN_VALUE && b == -1) ? a : a / b;
            case 5 -> b == 0 ? -1 : Integer.divideUnsigned(a, b);
            case 6 -> b == 0 ? a : (a == Integer.MIN_VALUE && b == -1) ? 0 : a % b;
            default -> b == 0 ? a : Integer.remainderUnsigned(a, b);
        };
        if (f7 == 0) return switch (f3) {
            case 0 -> a + b;
            case 1 -> a << b;
            case 2 -> a < b ? 1 : 0;
            case 3 -> Integer.compareUnsigned(a, b) < 0 ? 1 : 0;
            case 4 -> a ^ b;
            case 5 -> a >>> b;
            case 6 -> a | b;
            default -> a & b;
        };
        if (f7 == 0x20) {
            if (f3 == 0) return a - b;
            if (f3 == 5) return a >> b;
        }
        throw illegal(inst);
    }

    private int system(int inst, int rd, int f3, int rs1, int next) {
        if (f3 == 0) {
            if (rd != 0 || rs1 != 0) throw illegal(inst);
            switch (inst >>> 20) {
                case 0x000 -> throw new Trap(Trap.ECALL_M, 0);
                case 0x001 -> throw new Trap(Trap.BREAKPOINT, pc);
                case 0x302 -> {                                                         // MRET
                    int mpie = (mstatus & MSTATUS_MPIE) != 0 ? MSTATUS_MIE : 0;
                    mstatus = mstatus & ~MSTATUS_MIE | mpie | MSTATUS_MPIE;
                    reservation = -1;
                    return mepc;
                }
                case 0x105 -> {                                                         // WFI
                    if ((mip & mie) == 0) waiting = true;
                    return next;
                }
                default -> throw illegal(inst);
            }
        }
        if (f3 == 4) throw illegal(inst);
        int csr = inst >>> 20;
        int src = f3 >= 5 ? rs1 : x[rs1];
        boolean write = f3 == 1 || f3 == 5 || rs1 != 0;
        if (write && (csr >>> 10) == 3) throw illegal(inst);                          // read-only CSR
        int old = readCsr(csr, inst);
        if (write) {
            int v = switch (f3 & 3) {
                case 1 -> src;
                case 2 -> old | src;
                default -> old & ~src;
            };
            writeCsr(csr, v, inst);
        }
        x[rd] = old;
        return next;
    }

    private int readCsr(int csr, int inst) {
        return switch (csr) {
            case 0x001 -> {
                fpOn(inst);
                yield fflags;
            }
            case 0x002 -> {
                fpOn(inst);
                yield frm;
            }
            case 0x003 -> {
                fpOn(inst);
                yield frm << 5 | fflags;
            }
            case 0x300 -> mstatus | ((mstatus & MSTATUS_FS) == MSTATUS_FS ? 1 << 31 : 0);
            case 0x301 -> MISA;
            case 0x304 -> mie;
            case 0x305 -> mtvec;
            case 0x310, 0x320, 0xF11, 0xF12, 0xF13, 0xF14, 0xF15 -> 0;
            case 0x340 -> mscratch;
            case 0x341 -> mepc;
            case 0x342 -> mcause;
            case 0x343 -> mtval;
            case 0x344 -> mip;
            case 0xB00, 0xC00 -> (int) cycle;
            case 0xB80, 0xC80 -> (int) (cycle >>> 32);
            case 0xB02, 0xC02 -> (int) instret;
            case 0xB82, 0xC82 -> (int) (instret >>> 32);
            case 0xC01 -> (int) time;
            case 0xC81 -> (int) (time >>> 32);
            default -> throw illegal(inst);
        };
    }

    private void writeCsr(int csr, int v, int inst) {
        switch (csr) {
            case 0x001 -> {
                fflags = v & 0x1F;
                mstatus |= MSTATUS_FS;
            }
            case 0x002 -> {
                frm = v & 7;
                mstatus |= MSTATUS_FS;
            }
            case 0x003 -> {
                fflags = v & 0x1F;
                frm = v >>> 5 & 7;
                mstatus |= MSTATUS_FS;
            }
            case 0x300 -> mstatus = v & (MSTATUS_MIE | MSTATUS_MPIE | MSTATUS_FS) | MSTATUS_MPP;
            case 0x304 -> mie = v & (1 << IRQ_SOFT | 1 << IRQ_TIMER | 1 << IRQ_EXTERNAL);
            case 0x305 -> mtvec = v & ~2;                                                // modes 0 and 1
            case 0x340 -> mscratch = v;
            case 0x341 -> mepc = v & ~1;
            case 0x342 -> mcause = v;
            case 0x343 -> mtval = v;
            case 0x301, 0x310, 0x320, 0x344 -> {
                // misa fixed; mstatush, mcountinhibit read 0; mip's lines belong to the devices
            }
            case 0xB00 -> cycle = cycle & ~0xFFFFFFFFL | v & 0xFFFFFFFFL;
            case 0xB80 -> cycle = cycle & 0xFFFFFFFFL | (long) v << 32;
            case 0xB02 -> instret = (instret & ~0xFFFFFFFFL | v & 0xFFFFFFFFL) - 1;     // this instruction counts after
            case 0xB82 -> instret = (instret & 0xFFFFFFFFL | (long) v << 32) - 1;
            default -> throw illegal(inst);
        }
    }

    /** FP instructions and CSRs are illegal while mstatus.FS is off. */
    void fpOn(int inst) {
        if ((mstatus & MSTATUS_FS) == 0) throw illegal(inst);
    }

    private void amo(int inst, int rd, int f3, int rs1, int rs2) {
        if (f3 != 2) throw illegal(inst);
        int a = x[rs1];
        int op = inst >>> 27;
        if ((a & 3) != 0) throw new Trap(op == 2 ? Trap.MISALIGNED_LOAD : Trap.MISALIGNED_STORE, a);
        switch (op) {
            case 2 -> {                                                                 // LR.W
                if (rs2 != 0) throw illegal(inst);
                x[rd] = mem.load32(a);
                reservation = a;
                return;
            }
            case 3 -> {                                                                 // SC.W
                if (reservation == a) {
                    mem.store32(a, x[rs2]);
                    x[rd] = 0;
                } else x[rd] = 1;
                reservation = -1;
                return;
            }
            default -> {
            }
        }
        int old;
        try {
            old = mem.load32(a);
        } catch (Trap t) {
            throw new Trap(Trap.STORE_FAULT, t.tval);                                   // AMOs report store faults
        }
        int b = x[rs2];
        int v = switch (op) {
            case 0x01 -> b;                                                             // AMOSWAP
            case 0x00 -> old + b;                                                       // AMOADD
            case 0x04 -> old ^ b;
            case 0x0C -> old & b;
            case 0x08 -> old | b;
            case 0x10 -> Math.min(old, b);
            case 0x14 -> Math.max(old, b);
            case 0x18 -> Integer.compareUnsigned(old, b) <= 0 ? old : b;
            case 0x1C -> Integer.compareUnsigned(old, b) >= 0 ? old : b;
            default -> throw illegal(inst);
        };
        mem.store32(a, v);
        if (reservation == a) reservation = -1;
        x[rd] = old;
    }
}
