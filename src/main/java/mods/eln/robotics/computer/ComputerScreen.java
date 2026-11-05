package mods.eln.robotics.computer;

import com.mojang.blaze3d.platform.InputConstants;
import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mods.eln.i18n.I18N.tr;

/**
 * The computer's screen: this client's copy of its scene (ComputerRender.scene), drawn in character cells (Baughn:
 * the client draws everything, the prompt included, so typing never waits for the network). Tabs sit beside the title
 * and switch here, without asking the computer. Lines are edited here, with history and completion from the names the
 * computer sent; Enter sends the line and shows it greyed until the computer has taken it; Ctrl+C is Break for the tab
 * (Baughn). Logs and lists scroll here. Under it: Reset, its state and address, the chip slots and drives.
 */
public class ComputerScreen extends ElnScreen {
    public static final int WIDTH = 330, HEIGHT = 346, CELL = 6, LINE = 10;
    /** The scene's area: cells from (AREA_X, AREA_Y), in panel pixels. */
    public static final int AREA_X = 9, AREA_Y = 22;
    public static final int SLOTS_Y = 234, INV_X = 84, INV_Y = 264;   // (346 px high: fits a 720 px window at GUI scale 2)
    static final int BOTTOM_Y = AREA_Y + Scene.DEFAULT_H * LINE + 6;
    final ComputerRender render;

    /** The tab shown, and the element with the keyboard. */
    int tab = -1, focusId = -1;
    int seenFocus = -1;
    /** The player picked the tab shown (the Console tab is only the default while there is no REPL). */
    boolean chose;
    /** What only this player has: each line's edit, each log's and list's scroll. */
    final Map<String, LineEdit> edits;
    final Map<String, Integer> scrolls = new HashMap<>();
    static long seq;
    String hint = "";

    static final class LineEdit {
        final StringBuilder text = new StringBuilder();
        int cursor;
        final List<String> history = new ArrayList<>();
        int historyAt = -1;
        /** A submission not yet taken by the computer (shown greyed), and its number. */
        String pending;
        long pendingSeq;
    }

    public ComputerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.render = (ComputerRender) render;
        @SuppressWarnings("unchecked") Map<String, LineEdit> e = (Map<String, LineEdit>) (Map<?, ?>) this.render.screenState;
        edits = e;   // (kept in the render: a half-typed line survives closing the screen, AdventurAgent)
    }

    @Override
    protected void init() {
        super.init();
        button(9, BOTTOM_Y, 50, () -> tr("Reset"), () -> render.clientSendId(ComputerElement.RESET_ID),
            tr("Start again from slot 1's chip (the RAM keeps what it held)."));
        button(61, BOTTOM_Y, 62, () -> render.switchedOn ? tr("Power off") : tr("Power on"),
            () -> render.clientSendId(ComputerElement.POWER_ID),
            tr("Its power switch. Off, it stops and its RAM forgets; on, it starts from slot 1's chip."));
        field(268, BOTTOM_Y, 36, () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(ComputerElement.SET_ADDRESS_ID, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its RS485 address, 17 to 255: the source of the packets it sends."));
        Minecraft.getInstance().onTextInputFocusChange(this, true);
    }

    @Override
    public void removed() {
        Minecraft.getInstance().onTextInputFocusChange(this, false);
        super.removed();
    }

    // ---- the scene

    Scene scene() {
        return render.scene;
    }

    Scene.Tab currentTab() {
        Scene s = scene();
        if (s.focus != seenFocus && s.tabs.containsKey(s.focus)) {
            seenFocus = s.focus;
            tab = s.focus;
        }
        if (!s.tabs.containsKey(tab) || (tab == 0 && !chose && s.tabs.containsKey(1))) {
            tab = s.tabs.containsKey(1) ? 1 : s.tabs.isEmpty() ? -1 : s.tabs.keySet().iterator().next();
            focusId = -1;
        }
        return tab < 0 ? null : s.tabs.get(tab);
    }

    static String key(int t, int id) {
        return t + "/" + id;
    }

    LineEdit edit(int id) {
        return edits.computeIfAbsent(key(tab, id), k -> new LineEdit());
    }

    /** The focused line, or the tab's first line. */
    Scene.Element focusedLine(Scene.Tab t) {
        if (t == null) return null;
        Scene.Element e = t.elements.get(focusId);
        if (e != null && e.kind.equals("line")) return e;
        for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet())
            if (en.getValue().kind.equals("line")) {
                focusId = en.getKey();
                return en.getValue();
            }
        return null;
    }

    static int colour(Object kw, int dflt) {
        if (!(kw instanceof Form.Keyword k)) return dflt;
        String n = k.name().replace('-', '_');
        int c = mods.eln.bus.BusChannel.byName(n);
        return c < 0 ? dflt : 0xFF000000 | mods.eln.bus.BusChannel.rgb(c);
    }

    int cx(int x) {
        return leftPos + AREA_X + x * CELL;
    }

    int cy(int y) {
        return topPos + AREA_Y + y * LINE;
    }

    void cells(GuiGraphicsExtractor g, String s, int x, int y, int maxCells, int colour) {
        for (int k = 0, c = 0; k < s.length() && c < maxCells; c++) {
            int cp = s.codePointAt(k);
            String ch = new String(Character.toChars(cp));
            k += Character.charCount(cp);
            if (cp == ' ') continue;
            g.text(font, ch, cx(x + c) + (CELL - font.width(ch)) / 2, cy(y) + 1, colour, false);
        }
    }

    // ---- drawing

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Scene s = scene();
        int x0 = leftPos + AREA_X - 4, y0 = topPos + AREA_Y - 3;
        g.fill(x0, y0, x0 + s.w * CELL + 8, y0 + s.h * LINE + 6, 0xFF101418);
        Scene.Tab t = currentTab();
        drawTabs(g, s);
        if (t == null) {
            cells(g, render.status == ComputerElement.NO_CHIPS ? tr("No chips") : render.status == ComputerElement.OFF ? tr("Off") : "...", 1, 1, s.w, 0xFF6A7A6A);
        } else {
            Scene.Element line = focusedLine(t);
            for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) draw(g, en.getKey(), en.getValue(), en.getValue() == line);
        }
        if (!hint.isEmpty()) {   // the completion choices, over the row above the line
            g.fill(cx(0), cy(s.h - 2), cx(s.w), cy(s.h - 1), 0xFF2A2E38);
            g.text(font, font.plainSubstrByWidth(hint, s.w * CELL), cx(0) + 1, cy(s.h - 2) + 1, 0xFFB0B0C0, false);
        }
        label(g, 9, SLOTS_Y - 11, tr("Expansion modules"));
        label(g, 9 + 8 * 18 + 14, SLOTS_Y - 11, tr("Floppy drives"));
        // the module slots as ISA-style edge connectors (Baughn), under the item slots
        for (int k = 0; k < ComputerElement.CHIP_SLOTS; k++) {
            int sx = leftPos + 9 + 18 * k, sy = topPos + SLOTS_Y + 11;   // (inside the slot, under the module)
            g.fill(sx + 1, sy, sx + 15, sy + 4, 0xFF2A2A2A);
            for (int p = 0; p < 6; p++) g.fill(sx + 2 + 2 * p, sy + 1, sx + 3 + 2 * p, sy + 3, 0xFFC8A040);
        }
        label(g, 127, BOTTOM_Y + 6, render.stateTextClient());
        label(g, 222, BOTTOM_Y + 6, tr("Address"));
    }

    /** The tabs, beside the title (inventory mods draw their sort buttons at the top right). */
    void drawTabs(GuiGraphicsExtractor g, Scene s) {
        int x = leftPos + 60;
        for (Map.Entry<Integer, Scene.Tab> en : s.tabs.entrySet()) {
            String title = en.getValue().title.isEmpty() ? String.valueOf(en.getKey()) : en.getValue().title;
            int w = font.width(title) + 8;
            boolean on = en.getKey() == tab;
            g.fill(x, topPos + 5, x + w, topPos + 17, on ? 0xFF101418 : 0xFF8B8B8B);
            g.text(font, title, x + 4, topPos + 7, on ? 0xFF9FE8A0 : 0xFF202020, false);
            x += w + 2;
        }
    }

    void draw(GuiGraphicsExtractor g, int id, Scene.Element e, boolean focused) {
        List<Object> m = e.msg;
        int x = e.x(), y = e.y();
        switch (e.kind) {
            case "box" -> {
                int w = (int) Form.num(m, 5, 1), h = (int) Form.num(m, 6, 1);
                g.fill(cx(x), cy(y), cx(x + w), cy(y + h), colour(m.size() > 7 ? m.get(7) : null, 0xFF404040));
            }
            case "text" -> {
                String s = Form.str(m, 6, "");
                if (m.size() > 7 && m.get(7) != null)
                    g.fill(cx(x), cy(y), cx(x + s.codePointCount(0, s.length())), cy(y + 1), colour(m.get(7), 0));
                cells(g, s, x, y, scene().w - x, colour(m.size() > 5 ? m.get(5) : null, 0xFFE6E6E6));
            }
            case "log" -> drawLog(g, id, e, x, y, (int) Form.num(m, 5, 1), (int) Form.num(m, 6, 1));
            case "line" -> drawLine(g, id, e, x, y, (int) Form.num(m, 5, 1), Form.str(m, 6, ""), focused);
            case "button" -> {
                int w = (int) Form.num(m, 5, 4);
                String label = Form.str(m, 6, "");
                g.fill(cx(x), cy(y), cx(x + w), cy(y + 1), 0xFF5A5A66);
                cells(g, label, x + Math.max(0, (w - label.length()) / 2), y, w, 0xFFFFFFFF);
            }
            case "list" -> {
                int w = (int) Form.num(m, 5, 1), h = (int) Form.num(m, 6, 1);
                List<?> items = m.size() > 7 && m.get(7) instanceof List<?> l ? l : List.of();
                int off = scrolls.getOrDefault(key(tab, id), 0);
                g.fill(cx(x), cy(y), cx(x + w), cy(y + h), 0xFF181C22);
                for (int r = 0; r < h && off + r < items.size(); r++)
                    cells(g, String.valueOf(items.get(off + r)), x, y + r, w, 0xFFD0D0D0);
            }
            default -> cells(g, "[" + e.kind + "]", x, y, 20, 0xFF707070);   // (editors and raw areas come next)
        }
    }

    /** A log's last lines, wrapped at its width, scrolled back by this player. */
    void drawLog(GuiGraphicsExtractor g, int id, Scene.Element e, int x, int y, int w, int h) {
        List<List<Scene.Run>> rows = new ArrayList<>();
        int need = h + scrolls.getOrDefault(key(tab, id), 0);
        var it = e.lines.descendingIterator();
        while (it.hasNext() && rows.size() < need) {
            List<List<Scene.Run>> wrapped = wrap(it.next(), w);
            for (int k = wrapped.size() - 1; k >= 0 && rows.size() < need; k--) rows.add(0, wrapped.get(k));
        }
        int off = Math.max(0, Math.min(scrolls.getOrDefault(key(tab, id), 0), Math.max(0, rows.size() - h)));
        int first = Math.max(0, rows.size() - h - off);
        for (int r = 0; r < h && first + r < rows.size(); r++) {
            int c = 0;
            for (Scene.Run run : rows.get(first + r)) {
                cells(g, run.text(), x + c, y + r, w - c, colour(Form.kw(run.colour()), 0xFFE6E6E6));
                c += run.text().codePointCount(0, run.text().length());
            }
        }
    }

    static List<List<Scene.Run>> wrap(List<Scene.Run> line, int w) {
        List<List<Scene.Run>> out = new ArrayList<>();
        List<Scene.Run> cur = new ArrayList<>();
        int col = 0;
        for (Scene.Run r : line) {
            String t = r.text();
            while (!t.isEmpty()) {
                int room = w - col;
                if (room <= 0) {
                    out.add(cur);
                    cur = new ArrayList<>();
                    col = 0;
                    room = w;
                }
                int n = Math.min(room, t.length());
                cur.add(new Scene.Run(r.colour(), t.substring(0, n)));
                col += n;
                t = t.substring(n);
            }
        }
        out.add(cur);
        return out;
    }

    void drawLine(GuiGraphicsExtractor g, int id, Scene.Element e, int x, int y, int w, String prompt, boolean focused) {
        LineEdit le = edit(id);
        // the computer took the pending submission: it is in the log now
        if (le.pending != null && e.busySeq >= le.pendingSeq) le.pending = null;
        boolean busy = le.pending != null || (e.busy && le.pendingSeq > 0 && e.busySeq >= le.pendingSeq);
        cells(g, prompt, x, y, w, 0xFF8FB0FF);
        int px = prompt.codePointCount(0, prompt.length());
        int room = w - px - 1;
        if (le.pending != null) {
            cells(g, le.pending, x + px, y, room, 0xFF707070);
        } else {
            String t = le.text.toString();
            int start = Math.max(0, le.cursor - room + 1);
            cells(g, t.substring(Math.min(start, t.length())), x + px, y, room, 0xFFE6E6E6);
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0)
                g.fill(cx(x + px + le.cursor - start), cy(y) + 1, cx(x + px + le.cursor - start) + 1, cy(y + 1) - 1, 0xFFE6E6E6);
        }
        if (busy) cells(g, "*", x + w - 1, y, 1, 0xFFE0C060);
    }

    // ---- input

    @Override
    public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
        Scene s = scene();
        double mx = ev.x(), my = ev.y();
        int x = leftPos + 60;
        for (Map.Entry<Integer, Scene.Tab> en : s.tabs.entrySet()) {
            String title = en.getValue().title.isEmpty() ? String.valueOf(en.getKey()) : en.getValue().title;
            int w = font.width(title) + 8;
            if (mx >= x && mx < x + w && my >= topPos + 5 && my < topPos + 17) {
                if (ev.button() == InputConstants.MOUSE_BUTTON_RIGHT && en.getKey() > 1)
                    render.screenEvent(new Form.Symbol("close"), (long) en.getKey());   // (right-click closes a process's tab)
                else {
                    tab = en.getKey();
                    chose = true;
                    focusId = -1;
                }
                return true;
            }
            x += w + 2;
        }
        Scene.Tab t = currentTab();
        if (t != null && mx >= cx(0) && my >= cy(0) && mx < cx(s.w) && my < cy(s.h)) {
            int col = (int) ((mx - cx(0)) / CELL), row = (int) ((my - cy(0)) / LINE);
            for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) {
                Scene.Element e = en.getValue();
                List<Object> m = e.msg;
                int ex = e.x(), ey = e.y(), w = (int) Form.num(m, 5, 1);
                int h = e.kind.equals("line") || e.kind.equals("button") ? 1 : (int) Form.num(m, 6, 1);
                if (col < ex || col >= ex + w || row < ey || row >= ey + h) continue;
                switch (e.kind) {
                    case "line" -> focusId = en.getKey();
                    case "button" -> render.screenEvent(new Form.Symbol("press"), (long) tab, (long) en.getKey());
                    case "list" -> {
                        int index = row - ey + scrolls.getOrDefault(key(tab, en.getKey()), 0);
                        render.screenEvent(new Form.Symbol("select"), (long) tab, (long) en.getKey(), (long) index);
                    }
                    default -> {
                        continue;
                    }
                }
                setFocused(null);   // (the keyboard is the screen's again, not the address field's)
                return true;
            }
        }
        return super.mouseClicked(ev, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        Scene.Tab t = currentTab();
        if (t != null) {
            int col = (int) ((mx - cx(0)) / CELL), row = (int) ((my - cy(0)) / LINE);
            for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) {
                Scene.Element e = en.getValue();
                if (!e.kind.equals("log") && !e.kind.equals("list")) continue;
                int ex = e.x(), ey = e.y(), w = (int) Form.num(e.msg, 5, 1), h = (int) Form.num(e.msg, 6, 1);
                if (col < ex || col >= ex + w || row < ey || row >= ey + h) continue;
                String k = key(tab, en.getKey());
                int step = e.kind.equals("log") ? (sy > 0 ? 3 : -3) : (sy > 0 ? -1 : 1);
                scrolls.put(k, Math.max(0, scrolls.getOrDefault(k, 0) + step));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    boolean fieldHasKeys() {
        return getFocused() instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        Scene.Tab t = currentTab();
        Scene.Element line = focusedLine(t);
        if (line == null || !event.isAllowedChatCharacter() || fieldHasKeys()) return super.charTyped(event);
        LineEdit le = edit(focusId);
        if (le.text.length() < 1000) {
            String c = event.codepointAsString();
            le.text.insert(le.cursor, c);
            le.cursor += c.length();
        }
        hint = "";
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent ev) {
        if (fieldHasKeys()) return super.keyPressed(ev);
        Scene.Tab t = currentTab();
        Scene.Element line = focusedLine(t);
        int k = ev.key();
        if (k == InputConstants.KEY_C && ev.hasControlDownWithQuirk() && !ev.hasShiftDown()) {
            if (tab >= 0) render.screenEvent(new Form.Symbol("break"), (long) tab);   // (on any tab, a line or not)
            return true;
        }
        if (line == null || k == InputConstants.KEY_ESCAPE) return super.keyPressed(ev);
        LineEdit le = edit(focusId);
        var clip = Minecraft.getInstance().keyboardHandler;
        if (ev.isPaste()) {
            String p = clip.getClipboard().replace("\r", "").replace("\n", " ");
            p = p.substring(0, Math.min(p.length(), Math.max(0, 1000 - le.text.length())));
            le.text.insert(le.cursor, p);
            le.cursor += p.length();
            return true;
        }
        // Ctrl+C is Break for this tab, as in a terminal (Baughn, above); Ctrl+Shift+C copies the line
        if (k == InputConstants.KEY_C && ev.hasControlDownWithQuirk()) {
            clip.setClipboard(le.text.toString());
            return true;
        }
        if (k == InputConstants.KEY_A && ev.hasControlDownWithQuirk()) {   // (start of the line, as readline)
            le.cursor = 0;
            return true;
        }
        if (k == InputConstants.KEY_E && ev.hasControlDownWithQuirk()) {
            le.cursor = le.text.length();
            return true;
        }
        switch (k) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> submit(le);
            case InputConstants.KEY_BACKSPACE -> {
                if (le.cursor > 0) le.text.deleteCharAt(--le.cursor);
            }
            case InputConstants.KEY_DELETE -> {
                if (le.cursor < le.text.length()) le.text.deleteCharAt(le.cursor);
            }
            case InputConstants.KEY_LEFT -> le.cursor = Math.max(0, le.cursor - 1);
            case InputConstants.KEY_RIGHT -> le.cursor = Math.min(le.text.length(), le.cursor + 1);
            case InputConstants.KEY_HOME -> le.cursor = 0;
            case InputConstants.KEY_END -> le.cursor = le.text.length();
            case InputConstants.KEY_UP, InputConstants.KEY_DOWN -> {
                if (le.history.isEmpty()) return true;
                if (k == InputConstants.KEY_UP) le.historyAt = le.historyAt < 0 ? le.history.size() - 1 : Math.max(0, le.historyAt - 1);
                else le.historyAt = le.historyAt < 0 || le.historyAt + 1 >= le.history.size() ? -1 : le.historyAt + 1;
                le.text.setLength(0);
                if (le.historyAt >= 0) le.text.append(le.history.get(le.historyAt));
                le.cursor = le.text.length();
            }
            case InputConstants.KEY_TAB -> complete(le, t);
            default -> {
                return true;   // (every other key belongs to the line while it has the keyboard: no inventory key closing it)
            }
        }
        return true;
    }

    void submit(LineEdit le) {
        if (le.pending != null) return;   // (the last one isn't taken yet)
        String text = le.text.toString();
        le.pendingSeq = ++seq;
        le.pending = text;
        if (!text.isBlank() && (le.history.isEmpty() || !le.history.getLast().equals(text))) le.history.add(text);
        le.historyAt = -1;
        le.text.setLength(0);
        le.cursor = 0;
        hint = "";
        for (var en : scrolls.entrySet()) en.setValue(0);
        render.screenEvent(new Form.Symbol("submit"), (long) tab, (long) focusId, le.pendingSeq, text);
    }

    /** Completion from the names the computer sent (Tab): the common part, and the choices as a hint. */
    void complete(LineEdit le, Scene.Tab t) {
        int start = le.cursor;
        while (start > 0 && "()[]{} \"'".indexOf(le.text.charAt(start - 1)) < 0) start--;
        String word = le.text.substring(start, le.cursor);
        if (word.isEmpty()) return;
        List<String> found = new ArrayList<>();
        for (List<String> names : List.of(t.dict, scene().baseDict))
            for (String n : names) if (n.startsWith(word) && !found.contains(n)) found.add(n);
        if (found.isEmpty()) {
            hint = "";
            return;
        }
        String common = found.getFirst();
        for (String n : found) {
            int i = 0;
            while (i < common.length() && i < n.length() && common.charAt(i) == n.charAt(i)) i++;
            common = common.substring(0, i);
        }
        le.text.insert(le.cursor, common.substring(word.length()));
        le.cursor += common.length() - word.length();
        hint = found.size() == 1 ? "" : found.size() + ": " + String.join(" ", found.subList(0, Math.min(found.size(), 12)))
            + (found.size() > 12 ? " ..." : "");
    }
}
