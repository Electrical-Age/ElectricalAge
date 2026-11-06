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
    final ScreenProbe screen = new ScreenProbe();
    /** The REPL's log and the console since the last mark(). */
    final StringBuilder out = new StringBuilder();
    int logMark, consoleMark;
    final List<Packet> sent = new ArrayList<>();

    @BeforeEach
    void boot() throws IOException {
        m = JanetRomTest.machine(3);
        m.bus.setState(200, true, null);
        for (int t = 0; t < 80 && !screen.ready(); t++) {
            m.tick();
            m.run(1_000_000);
            screen.feed(m);
        }
        assertTrue(screen.ready(), "the REPL's tab is up");
        mark();
    }

    void mark() {
        logMark = screen.log().length();
        consoleMark = screen.console.length();
        out.setLength(0);
    }

    /**
     * Ticks of 1M instructions until the log (or console) shows want; returns the ticks taken. A want ending in "> " (the
     * old prompt) also waits for the submission to finish.
     */
    int tick(int max, String want) {
        boolean prompt = want != null && want.endsWith("\n> ");
        String w = prompt ? want.substring(0, want.length() - 2) : want;
        for (int t = 1; t <= max; t++) {
            m.tick();
            m.run(1_000_000);
            screen.feed(m);
            sent.addAll(m.bus.take());
            String log = screen.log();
            out.setLength(0);
            out.append(log, Math.min(logMark, log.length()), log.length()).append(screen.console.substring(consoleMark));
            if (want != null && (w.equals("\n") || out.indexOf(w) >= 0) && (!prompt || screen.done())) return t;
        }
        if (want != null) fail("no \"" + want + "\"; got: " + out + " [log " + screen.log() + "] [tabs " + screen.scene.tabs.keySet() + " done " + screen.done() + "]");
        return max;
    }

    /** The Break button on tab t, as the host passes it on: the interrupt, then the event. */
    void breakTab(int t) {
        m.pressBreak();
        m.editor.give(("(break " + t + ")\n").getBytes(StandardCharsets.UTF_8));
    }

    void type(String s) {
        mark();
        m.editor.give(screen.submit(s));
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
        assertTrue(s.endsWith("\nfirst\nsecond\n:done\n"), s);
    }

    @Test
    void breakStopsALoopThatNeverEnds() {
        type("(while true)");
        tick(3, null);
        assertTrue(m.runnable(), "busy");
        breakTab(1);
        tick(5, "stopped by Break");
        tick(5, "\n> ");
        type("(+ 2 2)");
        tick(5, "\n4\n> ");
    }

    @Test
    void breakStopsALongSleep() {
        type("(sleep 1000)");
        tick(3, null);
        breakTab(1);
        tick(5, "stopped by Break");
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
        assertTrue(out.toString().contains("write-protected"), out.toString());
        type("(disk/exists? \"a:/x.txt\")");
        tick(20, "\nfalse\n> ");
    }

    @Test
    void mainJanetRunsAtBoot() {
        Floppy f = new Floppy(java.util.UUID.randomUUID(), null, false);
        m.floppy.insert(0, f);
        type("(do (disk/format :a) (spit \"a:/main.janet\" \"(def x (do (sleep 0.2) 40)) (print (+ x 2))\") :ok)");
        tick(60, ":ok\n> ");
        m.reset(Machine.RESET_POWER);
        mark();
        tick(80, "Running A:/main.janet");
        for (int t = 0; t < 40 && !screen.log(2).contains("42"); t++) tick(1, null);
        assertTrue(screen.log(2).contains("42"), "it prints in its own tab: " + screen.log(2));
        assertEquals("main.janet", screen.scene.tabs.get(2).title);
    }

    @Test
    void waitingInsideTryWorks() {
        type("(try (device/read 40 1) ([e] (string \"caught \" e)))");
        tick(40, "caught no reply from 40");
        type("(do (var x (try (device/read 40 1) ([_] 7))) x)");
        tick(40, "\n7\n> ");
    }

    @Test
    void breakWithNothingAtTheReplStopsThePrograms() {
        type("(do (spawn (fn [] (forever (sleep 0.2) (print \"tick\")))) (bus/on :press (fn [p] (print \"press\"))) :started)");
        tick(10, ":started\n> ");
        tick(10, "tick");
        breakTab(1);
        tick(5, "stopped 1 task and the bus handlers");
        mark();
        m.bus.give(List.of(new Packet(0, 23, PacketType.PRESS, 0, 0)));
        tick(20, null);
        assertFalse(out.toString().contains("tick") || out.toString().contains("press"), out.toString());
        type("(+ 1 1)");
        tick(5, "\n2\n> ");
    }

    @Test
    void processesHaveTabsAndBreakStopsTheirs() {
        type("(proc/start \"ticker\" (fn [] (forever (print \"tick\") (sleep 0.2))))");
        tick(10, "\n2\n> ");
        for (int t = 0; t < 20 && !screen.log(2).contains("tick"); t++) tick(1, null);
        assertTrue(screen.log(2).contains("tick"), screen.log(2));
        breakTab(2);
        tick(5, null);
        assertTrue(screen.log(2).contains("Break: stopped 1 task"), screen.log(2));
        type("(proc/list)");
        tick(5, "@[(2 \"ticker\" 0)]\n> ");
        m.editor.give("(close 2)\n".getBytes(StandardCharsets.UTF_8));
        tick(3, null);
        assertNull(screen.scene.tabs.get(2), "closed");
    }

    @Test
    void editorTabsSaveHunksAndRefuseStaleRevisions() {
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), null, false));
        type("(do (disk/format :a) (spit \"a:/t.janet\" \"one\\ntwo\\nthree\") (edit \"a:/t.janet\"))");
        tick(60, "\n2\n> ");
        var ed = screen.scene.tabs.get(2).elements.get(4);
        assertEquals("editor", ed.kind);
        assertEquals("one\ntwo\nthree", ed.msg.get(10));
        String rev = (String) ed.msg.get(9);
        // the client replaced line 1 ("two") with two lines
        m.editor.give(("(save 2 4 7 \"" + rev + "\" [[1 1 [\"TWO\" \"2b\"]]])\n").getBytes(StandardCharsets.UTF_8));
        String newRev = null;
        for (int t = 0; t < 20 && newRev == null; t++) {
            byte[] d = m.editor.take();
            String text = new String(d, StandardCharsets.UTF_8);
            screen.feed(d, m.terminal.take());
            int k = text.indexOf("(saved 2 4 7 ");
            if (k >= 0) newRev = text.substring(k + 13, text.indexOf(")", k));
            m.tick();
            m.run(1_000_000);
        }
        assertNotNull(newRev, "saved");
        type("(slurp \"a:/t.janet\")");
        tick(10, "@\"one\\nTWO\\n2b\\nthree\"\n> ");
        // a save against the old revision is refused, with the file as it is now
        m.editor.give(("(save 2 4 8 \"" + rev + "\" [[0 1 []]])\n").getBytes(StandardCharsets.UTF_8));
        boolean conflict = false;
        for (int t = 0; t < 20 && !conflict; t++) {
            m.tick();
            m.run(1_000_000);
            conflict = new String(m.editor.take(), StandardCharsets.UTF_8).contains("(conflict 2 4 8 ");
        }
        assertTrue(conflict);
    }

    @Test
    void theInspectorShowsTasksStacksAndValues() {
        type("(proc/start \"sleeper\" (fn [] (var secret 42) (sleep 100) secret))");
        tick(20, "\n2\n> ");
        type("(inspect)");
        tick(20, "\n3\n> ");
        var list = screen.scene.tabs.get(3).elements.get(2);
        String rows = list.msg.get(7).toString();
        assertTrue(rows.contains("sleeper (tab 2)") && rows.contains("task pending"), rows);
        // select the task row (the third: REPL, sleeper, its task), then its frames, then a frame's slots
        int task = ((java.util.List<?>) list.msg.get(7)).indexOf(((java.util.List<?>) list.msg.get(7)).stream().filter(r -> r.toString().startsWith("  task")).findFirst().get());
        m.editor.give(("(select 3 2 " + task + ")\n").getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        String frames = screen.scene.tabs.get(3).elements.get(2).msg.get(7).toString();
        assertTrue(frames.contains("#0 sleep") && frames.contains("#1"), frames);
        m.editor.give("(select 3 2 1)\n".getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        String slots = screen.scene.tabs.get(3).elements.get(2).msg.get(7).toString();
        assertTrue(slots.contains("= 42"), slots);
        // the header says where it is, whole; Back goes up a level at a time
        String header = screen.scene.tabs.get(3).elements.get(1).msg.get(6).toString();
        assertTrue(header.startsWith("2: #1"), header);
        m.editor.give("(press 3 3)\n".getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        assertEquals(frames, screen.scene.tabs.get(3).elements.get(2).msg.get(7).toString());
        m.editor.give("(press 3 3)\n".getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        assertTrue(screen.scene.tabs.get(3).elements.get(2).msg.get(7).toString().contains("sleeper (tab 2)"));
        // (inspect) again brings the same tab forward instead of opening another
        type("(inspect)");
        tick(20, "\n3\n> ");
        assertNull(screen.scene.tabs.get(4));
        assertEquals(3, screen.scene.focus);
    }

    @Test
    void filesOpensAnEditor() {
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), null, false));
        type("(do (disk/format :a) (spit \"a:/x.janet\" \"(print 1)\") (files))");
        tick(60, "\n2\n> ");
        m.editor.give("(select 2 2 0)\n".getBytes(StandardCharsets.UTF_8));
        for (int t = 0; t < 20 && screen.scene.tabs.get(3) == null; t++) tick(1, null);
        assertNotNull(screen.scene.tabs.get(3), "an editor tab; console: " + screen.console);
        assertEquals("(print 1)", screen.scene.tabs.get(3).elements.get(4).msg.get(10));
    }

    @Test
    void aRawAreaGetsKeysAndClicks() {
        type("(do (def t (proc/new-tab \"game\")) (ui/raw t 1 0 0 20 5 (fn [k m] (ui/text t 2 0 0 :white (string \"key \" k \" \" m))) (fn [x y] (ui/text t 3 0 1 :white (string \"click \" x \" \" y)))) t)");
        tick(20, "\n2\n> ");
        assertEquals("raw", screen.scene.tabs.get(2).elements.get(1).kind);
        m.editor.give("(key 2 1 \"left\" 2)\n(click 2 1 4 3)\n".getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        assertEquals("key left 2", screen.scene.tabs.get(2).elements.get(2).msg.get(6));
        assertEquals("click 4 3", screen.scene.tabs.get(2).elements.get(3).msg.get(6));
    }

    /** The handbook's example (janet.md, "Drawing your own screen"), as printed there. */
    @Test
    void theHandbookCounterWorks() {
        type("(proc/start \"counter\" (fn [] (def t (proc/tab)) (var n 0) (defn show [] (ui/text t 2 2 1 :yellow (string \"count: \" n))) (ui/clear t) (ui/button t 3 2 3 5 \"+1\" (fn [] (++ n) (show))) (ui/button t 4 8 3 5 \"-1\" (fn [] (-- n) (show))) (show) (forever (sleep 60))))");
        tick(20, "\n2\n> ");
        assertEquals("count: 0", screen.scene.tabs.get(2).elements.get(2).msg.get(6));
        m.editor.give("(press 2 3)\n(press 2 3)\n(press 2 4)\n".getBytes(StandardCharsets.UTF_8));
        tick(5, null);
        assertEquals("count: 1", screen.scene.tabs.get(2).elements.get(2).msg.get(6));
    }
}
