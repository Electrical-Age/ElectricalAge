package mods.eln.robotics.computer.cpu;

/**
 * The F and D extensions for one Cpu. Java's arithmetic rounds to nearest-even; the other rounding modes and the
 * inexact flag come from the exact error of each operation (TwoSum for add, a fused residual for mul, div and sqrt),
 * which says on which side of the true result the rounded one lies. NaN results are canonical, as RISC-V wants.
 */
final class Fpu {
    static final int NX = 1, UF = 2, OF = 4, DZ = 8, NV = 16;
    static final int CANON_S = 0x7FC0_0000;
    static final long CANON_D = 0x7FF8_0000_0000_0000L, BOX = 0xFFFF_FFFF_0000_0000L;

    private final Cpu c;
    /** The exact result minus the rounded one, from the last operation (0: exact). */
    private double err;

    Fpu(Cpu c) {
        this.c = c;
    }

    // --- registers ---

    /** A single's bits; a register not properly NaN-boxed reads as the canonical NaN. */
    private int sBits(int r) {
        long v = c.f[r];
        return (v & BOX) == BOX ? (int) v : CANON_S;
    }

    private float s(int r) {
        return Float.intBitsToFloat(sBits(r));
    }

    private double d(int r) {
        return Double.longBitsToDouble(c.f[r]);
    }

    private void setS(int r, int bits) {
        c.f[r] = BOX | bits & 0xFFFF_FFFFL;
        c.mstatus |= Cpu.MSTATUS_FS;
    }

    private void putS(int r, float v) {
        setS(r, v != v ? CANON_S : Float.floatToRawIntBits(v));
    }

    private void putD(int r, double v) {
        c.f[r] = v != v ? CANON_D : Double.doubleToRawLongBits(v);
        c.mstatus |= Cpu.MSTATUS_FS;
    }

    private void flag(int fl) {
        c.fflags |= fl;
        if (fl != 0) c.mstatus |= Cpu.MSTATUS_FS;
    }

    static boolean snanS(int b) {
        return (b & 0x7F80_0000) == 0x7F80_0000 && (b & 0x007F_FFFF) != 0 && (b & 0x0040_0000) == 0;
    }

    static boolean snanD(long b) {
        return (b & 0x7FF0_0000_0000_0000L) == 0x7FF0_0000_0000_0000L && (b & 0x000F_FFFF_FFFF_FFFFL) != 0
                && (b & 0x0008_0000_0000_0000L) == 0;
    }

    /** The rounding mode an instruction uses (its rm field, or frm for 7); illegal if reserved. */
    private int rm(int inst) {
        int rm = inst >>> 12 & 7;
        if (rm == 7) rm = c.frm;
        if (rm > 4) throw Cpu.illegal(inst);
        return rm;
    }

    // --- rounding from the error ---

    /** r rounded-to-nearest moved to what mode rm gives, knowing err = exact - r. */
    private static double roundD(double r, double err, int rm) {
        if (err == 0 || err != err || r != r) return r;
        return switch (rm) {
            case 1 -> err < 0 && r > 0 ? Math.nextDown(r) : err > 0 && r < 0 ? Math.nextUp(r) : r;
            case 2 -> err < 0 ? Math.nextDown(r) : r;
            case 3 -> err > 0 ? Math.nextUp(r) : r;
            case 4 -> !Double.isInfinite(r) && Math.abs(err) == Math.ulp(r) / 2 && Math.signum(err) == Math.signum(r)
                    ? (r > 0 ? Math.nextUp(r) : Math.nextDown(r)) : r;
            default -> r;
        };
    }

    private static float roundF(float r, double err, int rm) {
        if (err == 0 || err != err || r != r) return r;
        return switch (rm) {
            case 1 -> err < 0 && r > 0 ? Math.nextDown(r) : err > 0 && r < 0 ? Math.nextUp(r) : r;
            case 2 -> err < 0 ? Math.nextDown(r) : r;
            case 3 -> err > 0 ? Math.nextUp(r) : r;
            case 4 -> !Float.isInfinite(r) && Math.abs(err) == Math.ulp(r) / 2.0 && Math.signum(err) == Math.signum(r)
                    ? (r > 0 ? Math.nextUp(r) : Math.nextDown(r)) : r;
            default -> r;
        };
    }

    /** Flags for a finished result: NX from the error, OF for an infinity from finite operands, UF for tiny. */
    private void resultFlagsD(double r, boolean finiteIn, double rounded) {
        boolean overflow = finiteIn && Double.isInfinite(r);
        boolean inexact = err != 0 || overflow;
        if (overflow) flag(OF);
        if (inexact) {
            flag(NX);
            if (Math.abs(rounded) < Double.MIN_NORMAL || Math.abs(r) < Double.MIN_NORMAL) flag(UF);
        }
    }

    private void resultFlagsF(float r, boolean finiteIn, float rounded) {
        boolean overflow = finiteIn && Float.isInfinite(r);
        boolean inexact = err != 0 || overflow;
        if (overflow) flag(OF);
        if (inexact) {
            flag(NX);
            if (Math.abs(rounded) < Float.MIN_NORMAL || Math.abs(r) < Float.MIN_NORMAL) flag(UF);
        }
    }

    // --- the operations, rounded to nearest, setting err ---

    private double addD(double a, double b) {
        double s = a + b;
        if (Double.isInfinite(s)) err = Double.isInfinite(a) || Double.isInfinite(b) ? 0 : -s;
        else {
            double bb = s - a;
            err = (a - (s - bb)) + (b - bb);
        }
        return s;
    }

    private float addF(float a, float b) {
        float s = a + b;
        if (Float.isInfinite(s)) err = Float.isInfinite(a) || Float.isInfinite(b) ? 0 : -s;
        else {
            float bb = s - a;
            err = (a - (s - bb)) + (b - bb);
        }
        return s;
    }

    private double mulD(double a, double b) {
        double p = a * b;
        err = Double.isInfinite(p) ? (Double.isInfinite(a) || Double.isInfinite(b) ? 0 : -p) : Math.fma(a, b, -p);
        if (err == 0 && p == 0 && a != 0 && b != 0) err = Math.signum(a) * Math.signum(b) * Double.MIN_VALUE;
        return p;
    }

    private float mulF(float a, float b) {
        float p = a * b;
        double exact = (double) a * b;   // exact: 24 + 24 bits
        err = Float.isInfinite(p) ? (Float.isInfinite(a) || Float.isInfinite(b) ? 0 : -p) : exact - p;
        return p;
    }

    private double divD(double a, double b) {
        double q = a / b;
        if (Double.isInfinite(q) || q != q || Double.isInfinite(a) || b == 0) err = Double.isInfinite(q) && !Double.isInfinite(a) && b != 0 ? -q : 0;
        else {
            double rem = Math.fma(-q, b, a);   // a - q*b, exact
            err = rem == 0 ? 0 : rem / b;
            if (err == 0 && rem != 0) err = Math.signum(rem) * Math.signum(b) * Double.MIN_VALUE;
        }
        return q;
    }

    private float divF(float a, float b) {
        float q = a / b;
        if (Float.isInfinite(q) || q != q || Float.isInfinite(a) || b == 0) err = Float.isInfinite(q) && !Float.isInfinite(a) && b != 0 ? -q : 0;
        else {
            double rem = Math.fma(-(double) q, b, a);   // exact in double
            err = rem == 0 ? 0 : rem / b;
            if (err == 0 && rem != 0) err = Math.signum(rem) * Math.signum(b) * Double.MIN_VALUE;
        }
        return q;
    }

    private double sqrtD(double a) {
        double r = Math.sqrt(a);
        if (r != r || Double.isInfinite(r) || r == 0) err = 0;
        else {
            double rem = Math.fma(-r, r, a);
            err = rem / (2 * r);
            if (err == 0 && rem != 0) err = Math.signum(rem) * Double.MIN_VALUE;
        }
        return r;
    }

    private float sqrtF(float a) {
        float r = (float) Math.sqrt(a);
        if (r != r || Float.isInfinite(r) || r == 0) err = 0;
        else {
            double rem = (double) a - (double) r * r;   // exact: r*r has 48 bits
            err = rem / (2.0 * r);
            if (err == 0 && rem != 0) err = Math.signum(rem) * Double.MIN_VALUE;
        }
        return r;
    }

    private double fmaD(double a, double b, double cc) {
        double r = Math.fma(a, b, cc);
        if (Double.isInfinite(r) || r != r) {
            err = Double.isInfinite(r) && !Double.isInfinite(a) && !Double.isInfinite(b) && !Double.isInfinite(cc) ? -r : 0;
            return r;
        }
        double p = a * b;
        if (Double.isInfinite(p)) {
            err = Math.signum(-r) * Double.MIN_VALUE;   // the product overflowed but the sum didn't: rare
            return r;
        }
        double pe = Math.fma(a, b, -p);
        double s = p + cc, bb = s - p, e1 = (p - (s - bb)) + (cc - bb);
        err = (s - r) + e1 + pe;
        return r;
    }

    private float fmaF(float a, float b, float cc) {
        float r = Math.fma(a, b, cc);
        if (Float.isInfinite(r) || r != r) {
            err = Float.isInfinite(r) && !Float.isInfinite(a) && !Float.isInfinite(b) && !Float.isInfinite(cc) ? -r : 0;
            return r;
        }
        double p = (double) a * b;   // exact
        double s = p + cc, bb = s - p, e1 = (p - (s - bb)) + (cc - bb);
        err = (s - r) + e1;
        return r;
    }

    // --- decode ---

    void exec(int inst) {
        c.fpOn(inst);
        int op = inst & 0x7F, rd = inst >>> 7 & 31, f3 = inst >>> 12 & 7, rs1 = inst >>> 15 & 31,
                rs2 = inst >>> 20 & 31;
        switch (op) {
            case 0x07 -> {
                int a = c.x[rs1] + (inst >> 20);
                if (f3 == 2) setS(rd, c.mem.load32(a));
                else if (f3 == 3) {
                    long lo = c.mem.load32(a) & 0xFFFF_FFFFL, hi = c.mem.load32(a + 4);
                    c.f[rd] = hi << 32 | lo;
                    c.mstatus |= Cpu.MSTATUS_FS;
                } else throw Cpu.illegal(inst);
            }
            case 0x27 -> {
                int a = c.x[rs1] + ((inst >> 25) << 5 | rd);
                if (f3 == 2) c.mem.store32(a, (int) c.f[rs2]);
                else if (f3 == 3) {
                    long v = c.f[rs2];
                    c.mem.load32(a + 4);   // fault before writing half
                    c.mem.store32(a, (int) v);
                    c.mem.store32(a + 4, (int) (v >>> 32));
                } else throw Cpu.illegal(inst);
            }
            case 0x43, 0x47, 0x4B, 0x4F -> fused(inst, op, rd, rs1, rs2);
            default -> opFp(inst, rd, f3, rs1, rs2);
        }
    }

    private void fused(int inst, int op, int rd, int rs1, int rs2) {
        int rs3 = inst >>> 27, fmt = inst >>> 25 & 3, rm = rm(inst);
        boolean negProd = op == 0x4B || op == 0x4F, negAdd = op == 0x47 || op == 0x4F;
        if (fmt == 0) {
            int ab = sBits(rs1), bb = sBits(rs2), cb = sBits(rs3);
            float a = Float.intBitsToFloat(ab), b = Float.intBitsToFloat(bb), cc = Float.intBitsToFloat(cb);
            if (snanS(ab) || snanS(bb) || snanS(cb) || invalidProduct(a, b)) flag(NV);
            float r = fmaF(negProd ? -a : a, b, negAdd ? -cc : cc);
            if (r != r && !(a != a || b != b || cc != cc)) flag(NV);
            float out = roundF(r, err, rm);
            if (r == r) resultFlagsF(r, finite(a) && finite(b) && finite(cc), out);
            putS(rd, out);
        } else if (fmt == 1) {
            long ab = c.f[rs1], bb = c.f[rs2], cb = c.f[rs3];
            double a = d(rs1), b = d(rs2), cc = d(rs3);
            if (snanD(ab) || snanD(bb) || snanD(cb) || invalidProduct(a, b)) flag(NV);
            double r = fmaD(negProd ? -a : a, b, negAdd ? -cc : cc);
            if (r != r && !(a != a || b != b || cc != cc)) flag(NV);
            double out = roundD(r, err, rm);
            if (r == r) resultFlagsD(r, finite(a) && finite(b) && finite(cc), out);
            putD(rd, out);
        } else throw Cpu.illegal(inst);
    }

    private static boolean invalidProduct(double a, double b) {
        return (a == 0 && Double.isInfinite(b)) || (Double.isInfinite(a) && b == 0);
    }

    private static boolean finite(double v) {
        return !Double.isInfinite(v) && v == v;
    }

    private void opFp(int inst, int rd, int f3, int rs1, int rs2) {
        int f7 = inst >>> 25;
        boolean dbl = (f7 & 1) != 0;
        switch (f7 & ~1) {
            case 0x00, 0x04, 0x08, 0x0C -> arith(inst, f7 & ~1, dbl, rd, rs1, rs2);
            case 0x2C -> {
                if (rs2 != 0) throw Cpu.illegal(inst);
                int rm = rm(inst);
                if (dbl) {
                    double a = d(rs1);
                    if (snanD(c.f[rs1]) || a < 0) flag(NV);
                    double r = sqrtD(a), out = roundD(r, err, rm);
                    if (r == r && err != 0) flag(NX);
                    putD(rd, out);
                } else {
                    int ab = sBits(rs1);
                    float a = Float.intBitsToFloat(ab);
                    if (snanS(ab) || a < 0) flag(NV);
                    float r = sqrtF(a), out = roundF(r, err, rm);
                    if (r == r && err != 0) flag(NX);
                    putS(rd, out);
                }
            }
            case 0x10 -> {                                                              // FSGNJ, FSGNJN, FSGNJX
                if (f3 > 2) throw Cpu.illegal(inst);
                if (dbl) {
                    long a = c.f[rs1], b = c.f[rs2], sign = Long.MIN_VALUE;
                    long sb = f3 == 0 ? b & sign : f3 == 1 ? ~b & sign : (a ^ b) & sign;
                    c.f[rd] = a & ~sign | sb;
                    c.mstatus |= Cpu.MSTATUS_FS;
                } else {
                    int a = sBits(rs1), b = sBits(rs2), sign = Integer.MIN_VALUE;
                    int sb = f3 == 0 ? b & sign : f3 == 1 ? ~b & sign : (a ^ b) & sign;
                    setS(rd, a & ~sign | sb);
                }
            }
            case 0x14 -> {                                                              // FMIN, FMAX
                if (f3 > 1) throw Cpu.illegal(inst);
                boolean max = f3 == 1;
                if (dbl) {
                    long ab = c.f[rs1], bb = c.f[rs2];
                    double a = d(rs1), b = d(rs2);
                    if (snanD(ab) || snanD(bb)) flag(NV);
                    double r = a != a ? b : b != b ? a : max ? Math.max(a, b) : Math.min(a, b);
                    putD(rd, r);
                } else {
                    int ab = sBits(rs1), bb = sBits(rs2);
                    float a = Float.intBitsToFloat(ab), b = Float.intBitsToFloat(bb);
                    if (snanS(ab) || snanS(bb)) flag(NV);
                    float r = a != a ? b : b != b ? a : max ? Math.max(a, b) : Math.min(a, b);
                    putS(rd, r);
                }
            }
            case 0x20 -> {                                                              // FCVT.S.D, FCVT.D.S
                if (f7 == 0x20 && rs2 == 1) {
                    int rm = rm(inst);
                    long db = c.f[rs1];
                    double v = d(rs1);
                    if (snanD(db)) flag(NV);
                    if (v != v) {
                        setS(rd, CANON_S);
                        return;
                    }
                    float r = (float) v;
                    err = Float.isInfinite(r) && !Double.isInfinite(v) ? -r : Float.isInfinite(r) ? 0 : v - r;
                    float out = roundF(r, err, rm);
                    resultFlagsF(r, !Double.isInfinite(v), out);
                    putS(rd, out);
                } else if (f7 == 0x21 && rs2 == 0) {
                    rm(inst);
                    int sb = sBits(rs1);
                    if (snanS(sb)) flag(NV);
                    putD(rd, Float.intBitsToFloat(sb));
                } else throw Cpu.illegal(inst);
            }
            case 0x50 -> {                                                              // FLE, FLT, FEQ
                boolean anyNan, anySnan;
                double a, b;
                if (dbl) {
                    a = d(rs1);
                    b = d(rs2);
                    anySnan = snanD(c.f[rs1]) || snanD(c.f[rs2]);
                } else {
                    int ab = sBits(rs1), bb = sBits(rs2);
                    a = Float.intBitsToFloat(ab);
                    b = Float.intBitsToFloat(bb);
                    anySnan = snanS(ab) || snanS(bb);
                }
                anyNan = a != a || b != b;
                int r = switch (f3) {
                    case 0 -> {
                        if (anyNan) flag(NV);
                        yield a <= b ? 1 : 0;
                    }
                    case 1 -> {
                        if (anyNan) flag(NV);
                        yield a < b ? 1 : 0;
                    }
                    case 2 -> {
                        if (anySnan) flag(NV);
                        yield a == b ? 1 : 0;
                    }
                    default -> throw Cpu.illegal(inst);
                };
                c.x[rd] = r;
            }
            case 0x60 -> {                                                              // FCVT.W[U].S/D
                if (rs2 > 1) throw Cpu.illegal(inst);
                int rm = rm(inst);
                double v;
                if (dbl) {
                    v = d(rs1);
                } else {
                    v = s(rs1);
                }
                c.x[rd] = toInt(v, rm, rs2 == 1);
            }
            case 0x68 -> {                                                              // FCVT.S/D.W[U]
                if (rs2 > 1) throw Cpu.illegal(inst);
                int rm = rm(inst);
                int xv = c.x[rs1];
                double v = rs2 == 1 ? xv & 0xFFFF_FFFFL : xv;
                if (dbl) putD(rd, v);
                else {
                    float r = (float) v;
                    err = v - r;
                    float out = roundF(r, err, rm);
                    if (err != 0) flag(NX);
                    putS(rd, out);
                }
            }
            case 0x70 -> {                                                              // FMV.X.W, FCLASS
                if (rs2 != 0) throw Cpu.illegal(inst);
                if (f3 == 0 && !dbl) c.x[rd] = (int) c.f[rs1];
                else if (f3 == 1) c.x[rd] = dbl ? classD(c.f[rs1]) : classS(sBits(rs1));
                else throw Cpu.illegal(inst);
            }
            case 0x78 -> {                                                              // FMV.W.X
                if (rs2 != 0 || f3 != 0 || dbl) throw Cpu.illegal(inst);
                setS(rd, c.x[rs1]);
            }
            default -> throw Cpu.illegal(inst);
        }
    }

    private void arith(int inst, int kind, boolean dbl, int rd, int rs1, int rs2) {
        int rm = rm(inst);
        if (dbl) {
            long ab = c.f[rs1], bb = c.f[rs2];
            double a = d(rs1), b = d(rs2);
            if (snanD(ab) || snanD(bb)) flag(NV);
            double r = switch (kind) {
                case 0x00 -> addD(a, b);
                case 0x04 -> addD(a, -b);
                case 0x08 -> mulD(a, b);
                default -> {
                    if (b == 0 && a == a && a != 0 && !Double.isInfinite(a)) flag(DZ);
                    yield divD(a, b);
                }
            };
            if (r != r && a == a && b == b) flag(NV);
            double out = roundD(r, err, rm);
            if (r == r) resultFlagsD(r, finite(a) && finite(b) && !(kind == 0x0C && b == 0), out);
            putD(rd, out);
        } else {
            int ab = sBits(rs1), bb = sBits(rs2);
            float a = Float.intBitsToFloat(ab), b = Float.intBitsToFloat(bb);
            if (snanS(ab) || snanS(bb)) flag(NV);
            float r = switch (kind) {
                case 0x00 -> addF(a, b);
                case 0x04 -> addF(a, -b);
                case 0x08 -> mulF(a, b);
                default -> {
                    if (b == 0 && a == a && a != 0 && !Float.isInfinite(a)) flag(DZ);
                    yield divF(a, b);
                }
            };
            if (r != r && a == a && b == b) flag(NV);
            float out = roundF(r, err, rm);
            if (r == r) resultFlagsF(r, finite(a) && finite(b) && !(kind == 0x0C && b == 0), out);
            putS(rd, out);
        }
    }

    /** v rounded to an integer by mode rm, saturated to int or unsigned int, with NV and NX as RISC-V says. */
    private int toInt(double v, int rm, boolean unsigned) {
        if (v != v) {
            flag(NV);
            return unsigned ? -1 : Integer.MAX_VALUE;
        }
        double r = rint(v, rm);
        if (unsigned) {
            if (r > 4294967295.0) {
                flag(NV);
                return -1;
            }
            if (r < 0) {
                flag(NV);
                return 0;
            }
            if (r != v) flag(NX);
            return (int) (long) r;
        }
        if (r > Integer.MAX_VALUE) {
            flag(NV);
            return Integer.MAX_VALUE;
        }
        if (r < Integer.MIN_VALUE) {
            flag(NV);
            return Integer.MIN_VALUE;
        }
        if (r != v) flag(NX);
        return (int) r;
    }

    private static double rint(double v, int rm) {
        if (Double.isInfinite(v) || Math.abs(v) >= 0x1p52) return v;
        return switch (rm) {
            case 1 -> v < 0 ? Math.ceil(v) : Math.floor(v);
            case 2 -> Math.floor(v);
            case 3 -> Math.ceil(v);
            case 4 -> Math.copySign(Math.floor(Math.abs(v) + 0.5), v);
            default -> Math.rint(v);
        };
    }

    static int classS(int b) {
        boolean neg = b < 0;
        int exp = b >>> 23 & 0xFF, man = b & 0x7F_FFFF;
        if (exp == 0xFF) return man == 0 ? (neg ? 1 : 1 << 7) : (man & 0x40_0000) != 0 ? 1 << 9 : 1 << 8;
        if (exp == 0) return man == 0 ? (neg ? 1 << 3 : 1 << 4) : (neg ? 1 << 2 : 1 << 5);
        return neg ? 1 << 1 : 1 << 6;
    }

    static int classD(long b) {
        boolean neg = b < 0;
        long exp = b >>> 52 & 0x7FF, man = b & 0xF_FFFF_FFFF_FFFFL;
        if (exp == 0x7FF) return man == 0 ? (neg ? 1 : 1 << 7) : (man & 0x8_0000_0000_0000L) != 0 ? 1 << 9 : 1 << 8;
        if (exp == 0) return man == 0 ? (neg ? 1 << 3 : 1 << 4) : (neg ? 1 << 2 : 1 << 5);
        return neg ? 1 << 1 : 1 << 6;
    }
}
