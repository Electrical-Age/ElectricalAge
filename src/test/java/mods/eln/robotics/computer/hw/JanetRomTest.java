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
        until(m, out, "repl:1:> ", 40);
        assertTrue(out.indexOf("Janet 1.41.2 on Electrical Age") >= 0, out.toString());
        m.terminal.give("(print \"hi\") (+ 1 2)\n".getBytes(StandardCharsets.UTF_8));
        String s = until(m, out, "repl:2:> ", 40);
        assertTrue(s.contains("hi\n"), s);
        assertTrue(s.contains("3"), s);
    }

    @Test
    void sleepsWhileWaitingForInput() throws IOException {
        Machine m = machine(2);
        until(m, new StringBuilder(), "repl:1:> ", 40);
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
        until(m, out, "repl:1:> ", 40);
        // no FFI, so poke the CPU instead: wake it with a keystroke and jump to address 0 (the NULL guard)
        m.terminal.give(new byte[]{'\n'});
        m.cpu.pc = 0;
        until(m, out, "Trap: cause 00000001 at 00000000", 2);
        assertTrue(m.halted());
    }
}
