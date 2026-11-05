package mods.eln.robotics.computer.hw;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** A computer driven as the world drives it: ticks on this thread, slices on the pool, saves between slices. */
class RunnerTest {
    /** Tick (as the server would, 1 ms apart so slices finish) until the terminal shows want. */
    static String until(Runner r, StringBuilder seen, String want) {
        for (int t = 0; t < 2000; t++) {
            r.tick(true);
            seen.append(new String(r.takeTerminal(), StandardCharsets.UTF_8));
            if (seen.indexOf(want) >= 0) return seen.toString();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        fail("no \"" + want + "\"; got: " + seen);
        return null;
    }

    @Test
    void bootsOnThePoolAndAnswers() throws IOException {
        Runner r = new Runner(JanetRomTest.machine(2));
        StringBuilder out = new StringBuilder();
        until(r, out, "\n> ");
        r.type("(* 6 7)\n".getBytes(StandardCharsets.UTF_8));
        until(r, out, "\n42\n> ");
        r.awaitIdle();
        assertFalse(r.machine.runnable(), "asleep waiting for input");
    }

    @Test
    void aSaveResumesMidSession() throws IOException {
        Runner r = new Runner(JanetRomTest.machine(2));
        StringBuilder out = new StringBuilder();
        until(r, out, "\n> ");
        r.type("(def answer 1234)\n".getBytes(StandardCharsets.UTF_8));
        until(r, out, "\n1234\n> ");
        r.awaitIdle();
        r.requestSave();
        r.tick(false);
        Machine.Saved saved = r.takeSaved();
        assertNotNull(saved);
        // the running computer goes on and changes its RAM; the save mustn't see that
        r.type("(def answer 99)\n".getBytes(StandardCharsets.UTF_8));
        until(r, out, "\n99\n> ");

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        saved.write(file);
        Machine m = JanetRomTest.machine(2);
        assertTrue(m.load(Machine.Saved.read(new ByteArrayInputStream(file.toByteArray()))));
        Runner back = new Runner(m);
        back.type("answer\n".getBytes(StandardCharsets.UTF_8));
        StringBuilder out2 = new StringBuilder();
        until(back, out2, "\n1234\n> ");
    }

    @Test
    void aChangedRomColdBoots() throws IOException {
        Machine m = JanetRomTest.machine(2);
        Machine.Saved saved = m.save();
        Machine other = new Machine();
        other.mem.setRom(0, new mods.eln.robotics.computer.cpu.Rom(new byte[4096]));
        other.mem.setRam(1, Machine.RAM_CHIP);
        other.mem.setRam(2, Machine.RAM_CHIP);
        assertFalse(other.load(saved));
        assertEquals(0x400, other.cpu.pc);
    }

    @Test
    void aChangedRomColdBootsAndRunsJanetAgain() throws IOException {
        Machine m = JanetRomTest.machine(2);
        StringBuilder out = new StringBuilder();
        Runner r = new Runner(m);
        until(r, out, "\n> ");
        r.awaitIdle();
        Machine.Saved saved = m.save();
        // the same chips, but the ROM image changed (a mod update): one byte of padding past its end
        byte[] rom = JanetRomTest.class.getResourceAsStream("/eln/computer/rom/janet.rom").readAllBytes();
        Machine other = new Machine();
        other.mem.setRom(0, new mods.eln.robotics.computer.cpu.Rom(java.util.Arrays.copyOf(rom, rom.length + 4)));
        other.mem.setRam(1, Machine.RAM_CHIP);
        other.mem.setRam(2, Machine.RAM_CHIP);
        assertFalse(other.load(saved));
        until(new Runner(other), new StringBuilder(), "Janet 1.41.2 on Electrical Age");
    }
}
