package mods.eln.robotics.computer.hw;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Janet ROM's stdlib (rom/janet/ea.janet) at its REPL: tasks, sleeping, Break, the bus. */
class StdlibTest {
    Machine m;
    final StringBuilder out = new StringBuilder();
    final List<Packet> sent = new ArrayList<>();

    @BeforeEach
    void boot() throws IOException {
        m = JanetRomTest.machine(3);
        m.bus.setState(200, true, null);
        tick(60, "\n> ");
    }

    /** Ticks of 1M instructions until the terminal shows want; returns the ticks taken. */
    int tick(int max, String want) {
        for (int t = 1; t <= max; t++) {
            m.tick();
            m.run(1_000_000);
            out.append(new String(m.terminal.take(), StandardCharsets.UTF_8));
            sent.addAll(m.bus.take());
            if (want != null && out.indexOf(want) >= 0) return t;
        }
        if (want != null) fail("no \"" + want + "\"; got: " + out);
        return max;
    }

    void type(String s) {
        out.setLength(0);
        m.terminal.give((s + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void sleepWaitsTheComputersTimeAndCostsNothing() {
        type("(do (sleep 1) :woke)");
        tick(3, null);
        assertFalse(m.runnable(), "asleep while the task sleeps");
        int t = tick(40, ":woke\n> ");
        assertTrue(t >= 15 && t <= 22, "about 20 ticks: " + t);
    }

    @Test
    void tasksRunSideBySide() {
        type("(do (spawn (fn [] (sleep 0.5) (print \"second\"))) (print \"first\") (sleep 1) :done)");
        tick(40, ":done\n> ");
        String s = out.toString();
        assertTrue(s.endsWith("\nfirst\nsecond\n:done\n> "), s);
    }

    @Test
    void breakStopsALoopThatNeverEnds() {
        type("(while true)");
        tick(3, null);
        assertTrue(m.runnable(), "busy");
        m.pressBreak();
        tick(5, "interrupted (Break)");
        tick(5, "\n> ");
        type("(+ 2 2)");
        tick(5, "\n4\n> ");
    }

    @Test
    void breakStopsALongSleep() {
        type("(sleep 1000)");
        tick(3, null);
        m.pressBreak();
        tick(5, "interrupted (Break)");
        tick(5, "\n> ");
    }

    @Test
    void anErrorShowsAndTheReplGoesOn() {
        type("(error \"boom\")");
        tick(5, "boom");
        tick(5, "\n> ");
        type("(foo)");
        tick(5, "unknown symbol foo");
    }

    @Test
    void busSendAndRawSend() {
        type("(do (bus/send 30 :write 0 5) (bus/raw-send 31 42 :power 0 1) (light/set 32 0.5) (bus/set :red 0.25) :ok)");
        tick(5, ":ok\n> ");
        assertEquals(List.of(new Packet(30, 200, PacketType.WRITE, 0, 5), new Packet(31, 42, PacketType.POWER, 0, 1),
            new Packet(32, 200, PacketType.POWER, 0, 0.5f), new Packet(15, 200, PacketType.SIGNAL, 0, 0.25f)), sent);
    }

    @Test
    void deviceReadWaitsForTheReply() {
        type("(device/read 40 1)");
        tick(3, null);
        assertEquals(new Packet(40, 200, PacketType.READ, 1, 0), sent.get(0));
        assertFalse(m.runnable(), "asleep waiting for the reply");
        m.bus.give(List.of(new Packet(200, 41, PacketType.REPLY, 1, 9), new Packet(200, 40, PacketType.REPLY, 1, 3.5f)));
        tick(5, "\n3.5\n> ");
    }

    @Test
    void deviceReadTimesOut() {
        type("(device/read 40 1)");
        int t = tick(40, "no reply from 40");
        assertTrue(t >= 18, "after a second: " + t);
    }

    @Test
    void pressHandlersRunAsTasks() {
        type("(bus/on-press 23 (fn [i] (print \"pressed \" i)))");
        tick(5, "\n> ");
        m.bus.give(List.of(new Packet(0, 23, PacketType.PRESS, 2, 0), new Packet(0, 24, PacketType.PRESS, 7, 0)));
        tick(5, "pressed 2");
        assertFalse(out.toString().contains("pressed 7"), out.toString());
    }

    @Test
    void levelsReadTheChannels() {
        double[] l = new double[16];
        l[14] = 0.75;
        m.bus.setState(200, true, l);
        type("(bus/level :red)");
        tick(5, "\n0.75\n> ");
    }

    @Test
    void floppiesFormatWriteReadList() {
        Floppy f = new Floppy(java.util.UUID.randomUUID(), null, false);
        m.floppy.insert(0, f);
        type("(do (disk/format :a \"WORK\") (spit \"a:/notes.txt\" \"hello\") (disk/mkdir \"a:/lib\") :ok)");
        tick(60, ":ok\n> ");
        type("[(slurp \"a:/notes.txt\") (disk/list) (disk/label :a)]");
        tick(20, "\n> ");
        assertTrue(out.toString().contains("(@\"hello\" @[\"notes.txt\" \"lib/\"] \"WORK\")"), out.toString());
        assertTrue(f.dirty());
    }

    @Test
    void noDiskAndUnformattedSaySo() {
        type("(slurp \"a:/x\")");
        tick(20, "\n> ");
        type("(disk/list)");
        tick(20, "no disk in the drive");
        m.floppy.insert(1, new Floppy(java.util.UUID.randomUUID(), null, false));
        type("(disk/list \"b:/\")");
        tick(20, "isn't formatted");
    }

    @Test
    void writeProtectedDisksRefuseWrites() {
        Floppy f = new Floppy(java.util.UUID.randomUUID(), null, false);
        m.floppy.insert(0, f);
        type("(disk/format :a)");
        tick(60, "\n> ");
        f.writeProtected = true;
        type("(spit \"a:/x.txt\" \"no\")");
        tick(20, "\n> ");
        type("(disk/exists? \"a:/x.txt\")");
        tick(20, "\nfalse\n> ");
    }

    @Test
    void mainJanetRunsAtBoot() {
        Floppy f = new Floppy(java.util.UUID.randomUUID(), null, false);
        m.floppy.insert(0, f);
        type("(do (disk/format :a) (spit \"a:/main.janet\" \"(spawn (fn [] (sleep 0.2) (print (+ 40 2))))\") :ok)");
        tick(60, ":ok\n> ");
        m.reset(Machine.RESET_POWER);
        out.setLength(0);
        tick(80, "42\n");
        assertTrue(out.toString().contains("Running A:/main.janet"), out.toString());
    }
}
