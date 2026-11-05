package mods.eln.robotics.computer.hw;

import mods.eln.robotics.computer.cpu.Memory;
import mods.eln.robotics.computer.cpu.Rom;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** The Janet ROM (rom/, built into resources) booting in a Machine and answering on its terminal. */
class JanetRomTest {
    static Rom janet() throws IOException {
        try (InputStream in = JanetRomTest.class.getResourceAsStream("/eln/computer/rom/janet.rom")) {
            assertNotNull(in, "janet.rom missing");
            return new Rom(in.readAllBytes());
        }
    }

    static Machine machine(int ramChips) throws IOException {
        Machine m = new Machine();
        m.mem.setRom(0, janet());
        for (int s = 1; s <= ramChips; s++) m.mem.setRam(s, Machine.RAM_CHIP);
        m.reset(0);
        return m;
    }

    /** Run ticks of 1M instructions until the terminal shows want (or fail after maxTicks); returns all output. */
    static String until(Machine m, StringBuilder seen, String want, int maxTicks) {
        for (int t = 0; t < maxTicks; t++) {
            m.tick();
            m.run(1_000_000);
            seen.append(new String(m.terminal.take(), StandardCharsets.UTF_8));
            if (seen.indexOf(want) >= 0) return seen.toString();
            if (m.halted()) break;
        }
        fail("no \"" + want + "\" on the terminal; got: " + seen);
        return null;
    }

    @Test
    void bootsToTheReplAndEvaluates() throws IOException {
        Machine m = machine(3);
        StringBuilder out = new StringBuilder();
        until(m, out, "\n> ", 60);
        assertTrue(out.indexOf("Janet 1.41.2 on Electrical Age") >= 0, out.toString());
        m.terminal.give("(print \"hi\") (+ 1 2)\n".getBytes(StandardCharsets.UTF_8));
        String s = until(m, out, "\n3\n> ", 40);
        assertTrue(s.contains("> (print \"hi\") (+ 1 2)\nhi\nnil\n3\n> "), "echoed, then each form's output and value: " + s);
    }

    @Test
    void sleepsWhileWaitingForInput() throws IOException {
        Machine m = machine(2);
        until(m, new StringBuilder(), "\n> ", 60);
        m.tick();
        m.run(1_000_000);
        m.tick();
        assertEquals(0, m.run(1_000_000), "parked on WFI with no input");
        assertTrue(m.cpu.waiting());
    }

    @Test
    void tooLittleRamSaysSoAndHalts() throws IOException {
        Machine m = machine(1);
        until(m, new StringBuilder(), "at least 2 MB of RAM", 2);
        assertTrue(m.halted());
    }

    @Test
    void aWildPointerTrapsAndHalts() throws IOException {
        Machine m = machine(2);
        StringBuilder out = new StringBuilder();
        until(m, out, "\n> ", 60);
        // no FFI, so poke the CPU instead: wake it with a keystroke and jump to address 0 (the NULL guard)
        m.terminal.give(new byte[]{'\n'});
        m.cpu.pc = 0;
        until(m, out, "Trap: cause 00000001 at 00000000", 2);
        assertTrue(m.halted());
    }

    @Test
    void floppyBootRunsSectorZero() throws IOException {
        Rom boot;
        try (InputStream in = JanetRomTest.class.getResourceAsStream("/eln/computer/rom/boot.rom")) {
            boot = new Rom(in.readAllBytes());
        }
        Machine m = new Machine();
        m.mem.setRom(0, boot);
        m.mem.setRam(1, Machine.RAM_CHIP);
        m.reset(0);
        StringBuilder out = new StringBuilder();
        until(m, out, "no disk in drive A", 2);
        // a boot sector: print "OK" on the terminal and spin
        int[] code = {0x100022b7, 0x04f00313, 0x0062a023, 0x04b00313, 0x0062a023, 0x00a00313, 0x0062a023, 0x0000006f};
        byte[] img = new byte[Floppy.BYTES];
        for (int i = 0; i < code.length; i++)
            for (int b = 0; b < 4; b++) img[4 * i + b] = (byte) (code[i] >>> (8 * b));
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), img, false));
        m.reset(0);
        until(m, new StringBuilder(), "OK\n", 2);
    }
}
