package mods.eln.robotics.computer.hw;

import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A test's view of a computer's screen, as a client would have it: the display channel's messages applied to a Scene,
 * and the console's bytes. The REPL is tab 1: its log is element 1, its line element 2.
 */
final class ScreenProbe {
    final Scene scene = new Scene();
    final StringBuilder console = new StringBuilder();
    private final StringBuilder partial = new StringBuilder();
    long seq;

    void feed(byte[] display, byte[] terminal) {
        console.append(new String(terminal, StandardCharsets.UTF_8));
        partial.append(new String(display, StandardCharsets.UTF_8));
        int nl;
        while ((nl = partial.indexOf("\n")) >= 0) {
            String line = partial.substring(0, nl);
            partial.delete(0, nl + 1);
            List<Object> m = Form.parse(line);
            if (m != null) scene.apply(m);
        }
    }

    void feed(Machine m) {
        feed(m.editor.take(), m.terminal.take());
    }

    /** The REPL's log as text. */
    String log() {
        Scene.Tab t = scene.tabs.get(1);
        Scene.Element e = t == null ? null : t.elements.get(1);
        if (e == null) return "";
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (List<Scene.Run> line : e.lines) {
            if (!first) b.append('\n');
            first = false;
            for (Scene.Run r : line) b.append(r.text());
        }
        return b.toString();
    }

    /** The REPL's line is up. */
    boolean ready() {
        Scene.Tab t = scene.tabs.get(1);
        return t != null && t.elements.containsKey(2);
    }

    /** The last submission is finished (its busy mark cleared). */
    boolean done() {
        Scene.Tab t = scene.tabs.get(1);
        Scene.Element e = t == null ? null : t.elements.get(2);
        return e != null && e.busySeq >= seq && !e.busy;
    }

    /** The message submitting text to the REPL's line, as the client sends it. */
    byte[] submit(String text) {
        return (Form.print(new Form.Symbol("submit"), 1L, 2L, ++seq, text) + "\n").getBytes(StandardCharsets.UTF_8);
    }
}
