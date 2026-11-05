package mods.eln.robotics.computer.cpu;

/**
 * The C extension (RV32C with Zcf and Zcd): every 16-bit instruction expanded once into the 32-bit instruction it
 * stands for, so the CPU decodes only one format. 0 marks a reserved or illegal encoding.
 */
final class Rvc {
    private Rvc() {
    }

    private static final int[] TABLE = new int[1 << 16];

    static {
        for (int h = 0; h < 1 << 16; h++) if ((h & 3) != 3) TABLE[h] = expand(h);
    }

    /** The 32-bit form of 16-bit instruction h, or 0: illegal. */
    static int expand16(int h) {
        return TABLE[h & 0xFFFF];
    }

    private static int bit(int h, int i) {
        return h >>> i & 1;
    }

    private static int bits(int h, int hi, int lo) {
        return h >>> lo & ((1 << (hi - lo + 1)) - 1);
    }

    private static int sext(int v, int width) {
        return v << (32 - width) >> (32 - width);
    }

    // 32-bit encoders
    static int i(int op, int rd, int f3, int rs1, int imm) {
        return imm << 20 | rs1 << 15 | f3 << 12 | rd << 7 | op;
    }

    static int r(int op, int rd, int f3, int rs1, int rs2, int f7) {
        return f7 << 25 | rs2 << 20 | rs1 << 15 | f3 << 12 | rd << 7 | op;
    }

    static int s(int op, int f3, int rs1, int rs2, int imm) {
        return (imm >> 5 & 0x7F) << 25 | rs2 << 20 | rs1 << 15 | f3 << 12 | (imm & 0x1F) << 7 | op;
    }

    static int b(int f3, int rs1, int rs2, int imm) {
        return (imm >> 12 & 1) << 31 | (imm >> 5 & 0x3F) << 25 | rs2 << 20 | rs1 << 15 | f3 << 12
                | (imm >> 1 & 0xF) << 8 | (imm >> 11 & 1) << 7 | 0x63;
    }

    static int j(int rd, int imm) {
        return (imm >> 20 & 1) << 31 | (imm >> 1 & 0x3FF) << 21 | (imm >> 11 & 1) << 20 | (imm >> 12 & 0xFF) << 12
                | rd << 7 | 0x6F;
    }

    private static final int LOAD = 0x03, LOAD_FP = 0x07, OP_IMM = 0x13, STORE = 0x23, STORE_FP = 0x27, OP = 0x33,
            LUI = 0x37, JALR = 0x67;

    static int expand(int h) {
        int f3 = bits(h, 15, 13);
        int rdp = 8 + bits(h, 4, 2), rs1p = 8 + bits(h, 9, 7);
        int rd = bits(h, 11, 7), rs2 = bits(h, 6, 2);
        switch (h & 3) {
            case 0 -> {
                // offsets of C.LW/C.FLW/C.SW/C.FSW and of C.FLD/C.FSD
                int w = bits(h, 12, 10) << 3 | bit(h, 6) << 2 | bit(h, 5) << 6;
                int d = bits(h, 12, 10) << 3 | bits(h, 6, 5) << 6;
                return switch (f3) {
                    case 0 -> {
                        int imm = bits(h, 12, 11) << 4 | bits(h, 10, 7) << 6 | bit(h, 6) << 2 | bit(h, 5) << 3;
                        yield imm == 0 ? 0 : i(OP_IMM, rdp, 0, 2, imm);   // C.ADDI4SPN
                    }
                    case 1 -> i(LOAD_FP, rdp, 3, rs1p, d);                // C.FLD
                    case 2 -> i(LOAD, rdp, 2, rs1p, w);                   // C.LW
                    case 3 -> i(LOAD_FP, rdp, 2, rs1p, w);                // C.FLW
                    case 5 -> s(STORE_FP, 3, rs1p, rdp, d);               // C.FSD
                    case 6 -> s(STORE, 2, rs1p, rdp, w);                  // C.SW
                    case 7 -> s(STORE_FP, 2, rs1p, rdp, w);               // C.FSW
                    default -> 0;
                };
            }
            case 1 -> {
                int imm6 = sext(bit(h, 12) << 5 | bits(h, 6, 2), 6);
                int jimm = sext(bit(h, 12) << 11 | bit(h, 11) << 4 | bits(h, 10, 9) << 8 | bit(h, 8) << 10
                        | bit(h, 7) << 6 | bit(h, 6) << 7 | bits(h, 5, 3) << 1 | bit(h, 2) << 5, 12);
                int bimm = sext(bit(h, 12) << 8 | bits(h, 11, 10) << 3 | bits(h, 6, 5) << 6 | bits(h, 4, 3) << 1
                        | bit(h, 2) << 5, 9);
                return switch (f3) {
                    case 0 -> i(OP_IMM, rd, 0, rd, imm6 & 0xFFF);          // C.ADDI, C.NOP
                    case 1 -> j(1, jimm);                                   // C.JAL
                    case 2 -> i(OP_IMM, rd, 0, 0, imm6 & 0xFFF);           // C.LI
                    case 3 -> {
                        if (rd == 2) {                                      // C.ADDI16SP
                            int imm = sext(bit(h, 12) << 9 | bit(h, 6) << 4 | bit(h, 5) << 6 | bits(h, 4, 3) << 7
                                    | bit(h, 2) << 5, 10);
                            yield imm == 0 ? 0 : i(OP_IMM, 2, 0, 2, imm & 0xFFF);
                        }
                        yield imm6 == 0 ? 0 : (imm6 & 0xFFFFF) << 12 | rd << 7 | LUI;   // C.LUI
                    }
                    case 4 -> switch (bits(h, 11, 10)) {
                        case 0 -> bit(h, 12) != 0 ? 0 : i(OP_IMM, rs1p, 5, rs1p, bits(h, 6, 2));            // C.SRLI
                        case 1 -> bit(h, 12) != 0 ? 0 : i(OP_IMM, rs1p, 5, rs1p, 0x400 | bits(h, 6, 2));    // C.SRAI
                        case 2 -> i(OP_IMM, rs1p, 7, rs1p, imm6 & 0xFFF);                                   // C.ANDI
                        default -> bit(h, 12) != 0 ? 0 : switch (bits(h, 6, 5)) {
                            case 0 -> r(OP, rs1p, 0, rs1p, rdp, 0x20);    // C.SUB
                            case 1 -> r(OP, rs1p, 4, rs1p, rdp, 0);       // C.XOR
                            case 2 -> r(OP, rs1p, 6, rs1p, rdp, 0);       // C.OR
                            default -> r(OP, rs1p, 7, rs1p, rdp, 0);      // C.AND
                        };
                    };
                    case 5 -> j(0, jimm);                                   // C.J
                    case 6 -> b(0, rs1p, 0, bimm);                          // C.BEQZ
                    default -> b(1, rs1p, 0, bimm);                         // C.BNEZ
                };
            }
            case 2 -> {
                int lwsp = bit(h, 12) << 5 | bits(h, 6, 4) << 2 | bits(h, 3, 2) << 6;
                int ldsp = bit(h, 12) << 5 | bits(h, 6, 5) << 3 | bits(h, 4, 2) << 6;
                int swsp = bits(h, 12, 9) << 2 | bits(h, 8, 7) << 6;
                int sdsp = bits(h, 12, 10) << 3 | bits(h, 9, 7) << 6;
                return switch (f3) {
                    case 0 -> bit(h, 12) != 0 ? 0 : i(OP_IMM, rd, 1, rd, bits(h, 6, 2));     // C.SLLI
                    case 1 -> i(LOAD_FP, rd, 3, 2, ldsp);                                    // C.FLDSP
                    case 2 -> rd == 0 ? 0 : i(LOAD, rd, 2, 2, lwsp);                         // C.LWSP
                    case 3 -> i(LOAD_FP, rd, 2, 2, lwsp);                                    // C.FLWSP
                    case 4 -> {
                        if (bit(h, 12) == 0) {
                            if (rs2 == 0) yield rd == 0 ? 0 : i(JALR, 0, 0, rd, 0);          // C.JR
                            yield r(OP, rd, 0, 0, rs2, 0);                                   // C.MV
                        }
                        if (rs2 == 0) yield rd == 0 ? 0x0010_0073 : i(JALR, 1, 0, rd, 0);    // C.EBREAK, C.JALR
                        yield r(OP, rd, 0, rd, rs2, 0);                                      // C.ADD
                    }
                    case 5 -> s(STORE_FP, 3, 2, rs2, sdsp);                                  // C.FSDSP
                    case 6 -> s(STORE, 2, 2, rs2, swsp);                                     // C.SWSP
                    default -> s(STORE_FP, 2, 2, rs2, swsp);                                 // C.FSWSP
                };
            }
            default -> {
                return 0;
            }
        }
    }
}
