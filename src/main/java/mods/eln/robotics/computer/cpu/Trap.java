package mods.eln.robotics.computer.cpu;

/** A synchronous exception inside an instruction: its cause and mtval. Thrown rarely, so it carries no stack trace. */
public final class Trap extends RuntimeException {
    public static final int MISALIGNED_FETCH = 0, FETCH_FAULT = 1, ILLEGAL = 2, BREAKPOINT = 3, MISALIGNED_LOAD = 4,
            LOAD_FAULT = 5, MISALIGNED_STORE = 6, STORE_FAULT = 7, ECALL_U = 8, ECALL_M = 11;

    public final int cause, tval;

    public Trap(int cause, int tval) {
        super(null, null, false, false);
        this.cause = cause;
        this.tval = tval;
    }
}
