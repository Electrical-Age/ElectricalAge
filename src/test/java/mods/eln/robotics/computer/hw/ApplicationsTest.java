package mods.eln.robotics.computer.hw;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stdlib's import, load, getline and data packets, and the Applications floppy's programs (rom/disks/applications),
 * the networked ones on two computers whose bus ports are joined by the test.
 */
class ApplicationsTest extends StdlibTest {
    static byte[] applications() throws IOException {
        try (var in = ApplicationsTest.class.getResourceAsStream("/eln/computer/disks/applications.img")) {
            assertNotNull(in, "the Applications image in the resources");
            return in.readAllBytes();
        }
    }

    void formatA() {
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), null, false));
        type("(do (disk/format :a) (spit \"a:/lib.janet\" \"(defn twice [x] (* 2 x))\") :ok)");
        tick(60, ":ok\n> ");
    }

    @Test
    void importFindsFilesOnTheFloppies() {
        formatA();
        type("(import a:/lib)");
        tick(40, "\n> ");
        type("(lib/twice 21)");
        tick(20, "\n42\n> ");
    }

    @Test
    void loadDefinesIntoTheRepl() {
        formatA();
        type("(load \"a:/lib.janet\")");
        tick(40, "\n> ");
        type("(twice 4)");
        tick(20, "\n8\n> ");
    }

    @Test
    void getlineReadsTheReplsLine() {
        type("(getline \"name? \")");
        tick(5, null);
        m.editor.give(screen.submit("Ada"));
        tick(20, "@\"Ada\\n\"");
        assertTrue(out.toString().contains("name? Ada"), out.toString());
        tick(20, "\n> ");   // (the form's last busy mark covers the line getline took: the screen shows no pending line)
        type("(+ 1 2)");
        tick(20, "\n3\n> ");
    }

    @Test
    void readingStdinDoesNotHang() {
        type("(file/read stdin :line)");
        tick(20, "\nnil\n> ");
    }

    @Test
    void busHandlersEndWithTheirProcess() {
        type("(def hits @[])");
        tick(10, "\n> ");
        type("(proc/start \"p\" (fn [] (bus/on :press (fn [p] (array/push hits (p :index)))) (forever (sleep 1))))");
        tick(20, "\n2\n> ");
        m.bus.give(List.of(new Packet(0, 23, PacketType.PRESS, 4, 0)));
        tick(5, null);
        type("(length hits)");
        tick(10, "\n1\n> ");
        type("(proc/close 2)");
        tick(10, "\n> ");
        m.bus.give(List.of(new Packet(0, 23, PacketType.PRESS, 5, 0)));
        tick(5, null);
        type("(length hits)");
        tick(10, "\n1\n> ");
    }

    @Test
    void dataPacketsCarryBytes() {
        type("(bus/send 30 :data 7 \"hello\")");
        tick(10, "\ntrue\n> ");
        assertEquals(Packet.data(30, 200, 7, "hello".getBytes(StandardCharsets.UTF_8)), sent.get(0));
        type("(bus/send 30 :data 7 (string/repeat \"x\" 65))");
        tick(10, "64 bytes at most");
        tick(10, "\n> ");
        type("(bus/wait)");
        tick(3, null);
        byte[] b = new byte[64];
        for (int i = 0; i < b.length; i++) b[i] = (byte) (i + 1);
        m.bus.give(List.of(Packet.data(200, 41, 3, b)));
        tick(10, ":type :data");
        assertTrue(out.toString().contains(":index 3"), out.toString());
        assertTrue(out.toString().contains("\\x01\\x02\\x03"), out.toString());
    }

    @Test
    void dataPacketsKeepTheirWireForm() {
        Packet p = Packet.data(0, 40, 9, "a post".getBytes(StandardCharsets.UTF_8));
        assertEquals(p, Packet.decode(p.encode()));
        assertEquals(3 + 2 + 6, p.encode().length);
        assertNull(Packet.decode(new byte[]{0, 40, 9, 1, 5, 1, 2}), "a data packet shorter than it says");
        assertEquals(64, new Packet(0, 40, PacketType.DATA, 0, 0, new byte[100]).dataLength(), "cut to 64 bytes");
    }

    // ---- the programs

    /** Element id of tab t, or null. */
    static Scene.Element element(ScreenProbe s, int t, int id) {
        Scene.Tab tab = s.scene.tabs.get(t);
        return tab == null ? null : tab.elements.get(id);
    }

    /** A text element's text, or "". */
    static String text(ScreenProbe s, int t, int id) {
        Scene.Element e = element(s, t, id);
        return e == null || e.msg.size() < 7 ? "" : e.msg.get(6).toString();
    }

    /** Log id of tab t as text. */
    static String log(ScreenProbe s, int t, int id) {
        Scene.Element e = element(s, t, id);
        if (e == null) return "";
        StringBuilder b = new StringBuilder();
        for (List<Scene.Run> line : e.lines) {
            for (Scene.Run r : line) b.append(r.text());
            b.append('\n');
        }
        return b.toString();
    }

    /** What a failing test shows of a screen: the REPL, tab 2's texts and logs. */
    static String dump(ScreenProbe s) {
        StringBuilder b = new StringBuilder("[repl " + s.log() + "] [console " + s.console + "]");
        Scene.Tab t = s.scene.tabs.get(2);
        if (t != null)
            for (var en : t.elements.entrySet()) {
                String k = en.getValue().kind;
                if (k.equals("text") || k.equals("line")) b.append(" [").append(en.getKey()).append(' ').append(en.getValue().msg).append(']');
                if (k.equals("log")) b.append(" [log ").append(en.getKey()).append(' ').append(log(s, 2, en.getKey())).append(']');
            }
        return b.toString();
    }

    static byte[] event(Object... parts) {
        return (Form.print(parts) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    static byte[] key(int t, int id, String k) {
        return event(new Form.Symbol("key"), (long) t, (long) id, k, 0L);
    }

    void run(int ticks) {
        for (int t = 0; t < ticks; t++) {
            m.tick();
            m.run(1_000_000);
            screen.feed(m);
            sent.addAll(m.bus.take());
        }
    }

    void start(String file) throws IOException {
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), applications(), false));
        type("(proc/start \"" + file + "\" run-file \"a:/" + file + ".janet\")");
        tick(40, "\n2\n> ");
    }

    @Test
    void theMenuStartsAProgram() throws IOException {
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), applications(), false));
        type("(proc/start \"menu\" run-file \"a:/startup.janet\")");
        tick(40, "\n2\n> ");
        run(20);
        assertEquals("Applications", text(screen, 2, 1));
        m.editor.give(event(new Form.Symbol("press"), 2L, 10L));   // Calculator
        run(40);
        assertNotNull(element(screen, 3, 30), "the calculator's tape in tab 3");
    }

    @Test
    void calculatorAddsAndKeepsATape() throws IOException {
        start("calculator");
        run(20);
        for (long id : new long[]{110, 143, 111, 144})   // 7 + 8 =
            m.editor.give(event(new Form.Symbol("press"), 2L, id));
        run(10);
        assertEquals("15", text(screen, 2, 3).trim());
        assertTrue(log(screen, 2, 30).contains("= 15"), dump(screen));
        for (String k : new String[]{"*", "3", "enter"}) m.editor.give(key(2, 1, k));   // typed: * 3 Enter
        run(10);
        assertEquals("45", text(screen, 2, 3).trim());
        for (long id : new long[]{110, 113, 140, 144})   // 7 / 0 = shows Error, C clears it
            m.editor.give(event(new Form.Symbol("press"), 2L, id));
        run(10);
        assertEquals("Error", text(screen, 2, 3).trim());
        m.editor.give(event(new Form.Symbol("press"), 2L, 104L));
        run(5);
        assertEquals("0", text(screen, 2, 3).trim());
    }

    @Test
    void snakeMovesAndDies() throws IOException {
        start("snake");
        run(5);
        assertTrue(text(screen, 2, 16).contains("oo@"), "the snake on row 6: " + text(screen, 2, 16));
        m.editor.give(key(2, 1, "up"));
        run(50);
        assertTrue(text(screen, 2, 2).startsWith("game over"), text(screen, 2, 2));
    }

    // ---- two computers on one bus

    /** A second computer, with its own screen, joined to the first by the test's bus. */
    static final class Other {
        final Machine m;
        final ScreenProbe screen = new ScreenProbe();
        final int address;

        Other(int address) throws IOException {
            this.address = address;
            m = JanetRomTest.machine(3);
            m.bus.setState(address, true, null);
            for (int t = 0; t < 80 && !screen.ready(); t++) {
                m.tick();
                m.run(1_000_000);
                screen.feed(m);
            }
            assertTrue(screen.ready());
        }

        void type(String s) {
            m.editor.give(screen.submit(s));
        }
    }

    /** Ticks both computers, passing each one's packets to the other, until done says so; the ticks taken. */
    int both(Other o, int max, BooleanSupplier done) {
        for (int t = 1; t <= max; t++) {
            m.tick();
            m.run(1_000_000);
            screen.feed(m);
            o.m.tick();
            o.m.run(1_000_000);
            o.screen.feed(o.m);
            List<Packet> a = m.bus.take(), b = o.m.bus.take();
            List<Packet> toO = new ArrayList<>(), toM = new ArrayList<>();
            for (Packet p : a) if (p.target() == 0 || p.target() == o.address) toO.add(p);
            for (Packet p : b) if (p.target() == 0 || p.target() == 200) toM.add(p);
            if (!toO.isEmpty()) o.m.bus.give(toO);
            if (!toM.isEmpty()) m.bus.give(toM);
            if (done != null && done.getAsBoolean()) return t;
        }
        if (done != null) fail("not done after " + max + " ticks; " + dump(screen) + "\n/// " + dump(o.screen));
        return max;
    }

    void startBoth(Other o, String file) throws IOException {
        o.m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), applications(), false));
        m.floppy.insert(0, new Floppy(java.util.UUID.randomUUID(), applications(), false));
        String run = "(proc/start \"" + file + "\" run-file \"a:/" + file + ".janet\")";
        type(run);
        o.type(run);
        both(o, 60, () -> element(screen, 2, 1) != null && element(o.screen, 2, 1) != null);
    }

    @Test
    void gorillasPairsUpAndThrowsOnBothScreens() throws IOException {
        Other o = new Other(201);
        startBoth(o, "gorillas");
        both(o, 200, () -> text(screen, 2, 3).contains("You 0 : 0 Them") && text(o.screen, 2, 3).contains("You 0 : 0 Them"));
        // the same city on both
        for (int id = 10; id < 20; id++) {
            Scene.Element a = element(screen, 2, id), b = element(o.screen, 2, id);
            assertEquals(a == null ? null : a.msg, b == null ? null : b.msg, "building " + id);
        }
        // the lower address throws first
        both(o, 20, () -> text(screen, 2, 4).startsWith("Angle"));
        assertTrue(text(o.screen, 2, 4).startsWith("Their throw"), text(o.screen, 2, 4));
        for (String k : new String[]{"4", "5", "enter", "6", "0", "enter"}) m.editor.give(key(2, 1, k));
        both(o, 40, () -> text(o.screen, 2, 4).startsWith("They threw at 45 degrees, speed 60"));
        assertTrue(text(screen, 2, 4).startsWith("You threw at 45 degrees, speed 60"), text(screen, 2, 4));
        // then it is the other's turn, on both
        both(o, 600, () -> text(o.screen, 2, 4).startsWith("Angle") || text(o.screen, 2, 4).contains("win"));
        long busy = 0;
        for (int t = 0; t < 20; t++) {
            o.m.tick();
            busy += o.m.run(1_000_000);
            o.screen.feed(o.m);
        }
        System.out.println("gorillas, waiting for a key: " + busy / 20 + " instructions a tick");
    }

    @Test
    void bbsPostsReachTheOtherComputerAndStayOnItsFloppy() throws IOException {
        Other o = new Other(201);
        startBoth(o, "bbs");
        both(o, 40, () -> element(screen, 2, 4) != null);
        String post = "Selling 64 copper cables, ask at the depot. " + "x".repeat(150);
        m.editor.give(event(new Form.Symbol("submit"), 2L, 4L, 1L, post));
        both(o, 200, () -> log(o.screen, 2, 3).contains(post.substring(0, 40)));
        assertTrue(log(o.screen, 2, 3).replace("\n", "").contains(post), "the whole post: " + log(o.screen, 2, 3));
        // a second board, and a post back
        o.m.editor.give(event(new Form.Symbol("submit"), 2L, 4L, 1L, "/board trade"));
        o.m.editor.give(event(new Form.Symbol("submit"), 2L, 4L, 2L, "Buying steam turbines"));
        both(o, 200, () -> text(screen, 2, 1).contains("2 posts"));
        o.type("(string (slurp \"a:/bbs/posts.jdn\"))");
        both(o, 40, () -> o.screen.log().contains("Buying steam turbines"));
        assertTrue(o.screen.log().contains("Selling 64 copper"), o.screen.log());
    }
}
