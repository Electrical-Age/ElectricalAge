package mods.eln.robotics.computer.hw;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** A computer driven as the world drives it: ticks on this thread, slices on the pool, saves between slices. */
class RunnerTest {
    /** Tick (as the server would, 1 ms apart so slices finish) until cond holds for the screen. */
    static void until(Runner r, ScreenProbe s, java.util.function.BooleanSupplier cond) {
        for (int t = 0; t < 2000; t++) {
            r.tick(true);
            s.feed(r.takeEditor(), r.takeTerminal());
            if (cond.getAsBoolean()) return;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        fail("not reached; log: " + s.log() + "; console: " + s.console);
    }

    @Test
    void bootsOnThePoolAndAnswers() throws IOException {
        Runner r = new Runner(JanetRomTest.machine(2));
        ScreenProbe s = new ScreenProbe();
        until(r, s, s::ready);
        r.toEditor(s.submit("(* 6 7)"));
        until(r, s, () -> s.done() && s.log().contains("\n42\n"));
        r.awaitIdle();
        assertFalse(r.machine.runnable(), "asleep waiting for input");
    }

    @Test
    void aSaveResumesMidSession() throws IOException {
        Runner r = new Runner(JanetRomTest.machine(2));
        ScreenProbe s = new ScreenProbe();
        until(r, s, s::ready);
        r.toEditor(s.submit("(def answer 1234)"));
        until(r, s, () -> s.done() && s.log().contains("\n1234\n"));
        r.awaitIdle();
        r.requestSave();
        r.tick(false);
        Machine.Saved saved = r.takeSaved();
        assertNotNull(saved);
        // the running computer goes on and changes its RAM; the save mustn't see that
        r.toEditor(s.submit("(def answer 99)"));
        until(r, s, () -> s.done() && s.log().contains("\n99\n"));

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        saved.write(file);
        Machine m = JanetRomTest.machine(2);
        assertTrue(m.load(Machine.Saved.read(new ByteArrayInputStream(file.toByteArray()))));
        Runner back = new Runner(m);
        ScreenProbe s2 = new ScreenProbe();
        // the host keeps the screen's scene with the save and replays it (the computer doesn't redraw)
        s2.feed(String.join("\n", s.scene.replay()).concat("\n").getBytes(StandardCharsets.UTF_8), new byte[0]);
        back.toEditor(s2.submit("answer"));
        until(back, s2, () -> s2.log().contains("> answer\n1234\n"));
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
        Runner r = new Runner(m);
        ScreenProbe s = new ScreenProbe();
        until(r, s, s::ready);
        r.awaitIdle();
        Machine.Saved saved = m.save();
        // the same chips, but the ROM image changed (a mod update): one byte of padding past its end
        byte[] rom = JanetRomTest.class.getResourceAsStream("/eln/computer/rom/janet.rom").readAllBytes();
        Machine other = new Machine();
        other.mem.setRom(0, new mods.eln.robotics.computer.cpu.Rom(java.util.Arrays.copyOf(rom, rom.length + 4)));
        other.mem.setRam(1, Machine.RAM_CHIP);
        other.mem.setRam(2, Machine.RAM_CHIP);
        assertFalse(other.load(saved));
        ScreenProbe s2 = new ScreenProbe();
        until(new Runner(other), s2, () -> s2.console.indexOf("Janet 1.41.2 on Electrical Age") >= 0);
    }

    @Test
    void aKickAnswersWithinTheTick() throws IOException, InterruptedException {
        Runner r = new Runner(JanetRomTest.machine(2));
        ScreenProbe s = new ScreenProbe();
        until(r, s, s::ready);
        r.awaitIdle();
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        r.afterSlice = done::countDown;
        // no tick() from here: the kick alone runs the computer, on what is left of this tick's budget
        r.toEditor(s.submit("(+ 40 2)"));
        r.kick();
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS), "a slice ran");
        r.awaitIdle();
        assertTrue(r.collectScreen());
        s.feed(r.takeEditor(), r.takeTerminal());
        assertTrue(s.log().contains("> (+ 40 2)\n42\n"), s.log());
    }
}
