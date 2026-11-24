package mods.eln.robotics.computer;

import com.mojang.blaze3d.platform.InputConstants;
import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;
import mods.eln.robotics.computer.screen.SceneCells;
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
 * The computer's screen, a Wire display (Baughn 2026-11-18: Dynamo; the client draws everything, so nothing the hand
 * does waits for the network). It composes this client's copy of the scene (ComputerRender.scene) in character cells
 * (SceneCells: the desk, the windows back to front, the screen bar) and acts on it at once: a window is dragged by its
 * title row, sized by its corner, raised or lowered by a click or its depth box, activated by a click; the menus of the
 * active window open on the screen bar while the right button is held. Each of these is applied to its own copy and
 * reported to the computer (moved, raised, lowered, active, pick, closed), which passes it to the other displays: the
 * last report wins (eventual consistency, Baughn 1572606714631561411).
 * <p>
 * Lines are edited here, with history and completion from the names the computer sent; Enter sends the line and shows
 * it greyed until the computer has taken it; Ctrl+C is Break for the active window (Baughn). Logs and lists scroll
 * here. Under it: Reset, power, its state and address, the chip slots and drives.
 */
// Inventory Profiles Next (optional, client) must not sort here: its R fired on every r typed, and moved the ROM chip
@org.anti_ad.mc.ipn.api.IPNIgnore
public class ComputerScreen extends ElnScreen {
    public static final int WIDTH = 498, HEIGHT = 358, CELL = 6, LINE = 10;
    /** The screen's cells start at (AREA_X, AREA_Y), in panel pixels. */
    public static final int AREA_X = 9, AREA_Y = 12;
    /**
     * Under the 80 x 25 cells (Baughn 2026-11-18): on the left the slots, then the buttons; on the right the inventory
     * (498 x 358: fits 640 x 360, a 720 px window at GUI scale 2; a smaller window takes a smaller scale, ElnScreen).
     * The inventory sits 14 px under the cells: inventory mods draw their buttons just above its corner (AdventurAgent).
     */
    public static final int SLOTS_Y = AREA_Y + Scene.DEFAULT_H * LINE + 20, INV_X = WIDTH - 9 - 162, INV_Y = AREA_Y + Scene.DEFAULT_H * LINE + 14;
    static final int BOTTOM_Y = SLOTS_Y + 24;
    /**
     * The layout for the screen's size now (Baughn: two text modes, 80 x 25 and 120 x 40): the slots, buttons and
     * inventory sit under the cells, so they move with its size. Set by layout(); the constants above are 80 x 25's.
     */
    int slotsY = SLOTS_Y, invX = INV_X, invY = INV_Y, bottomY = BOTTOM_Y, laidW = Scene.DEFAULT_W, laidH = Scene.DEFAULT_H;
    final ComputerRender render;

    /** This display's active window (the keyboard's), and the element in it with the keyboard. */
    int active = -1, focusId = -1;
    long seenActivate = -1;
    /** What only this player has: each line's edit, each log's and list's scroll. */
    final Map<String, LineEdit> edits;
    final Map<String, Integer> scrolls = new HashMap<>();
    String hint = "";

    /** A window being dragged (MOVE) or sized (SIZE) by this player: its new geometry so far. */
    static final int MOVE = 1, SIZE = 2;
    int dragMode, dragWin, dragX, dragY, dragW, dragH, placesSeen;
    double dragFromX, dragFromY;

    /** An editor element's state on this client: the revision and text it was opened at, the text now, a save. */
    static final class EditorState {
        /** The editor message this state was made from (a new one is a new opening: the state starts again). */
        Object opened;
        String baseRev, baseText = "", text = "", savingText, status = "";
        long savingSeq;
    }

    final Map<String, mods.eln.client.gui.CodeEditor> editorWidgets = new HashMap<>();
    static final JanetLanguage JANET = new JanetLanguage();

    static final class LineEdit {
        final StringBuilder text = new StringBuilder();
        int cursor;
        final List<String> history = new ArrayList<>();
        int historyAt = -1;
        /** A submission not yet taken by the computer (shown greyed), and its number. */
        String pending;
        long pendingSeq, pendingAt;
    }

    public ComputerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, widthFor((ComputerRender) render), heightFor((ComputerRender) render));
        this.render = (ComputerRender) render;
        @SuppressWarnings("unchecked") Map<String, LineEdit> e = (Map<String, LineEdit>) (Map<?, ?>) this.render.screenState;
        edits = e;   // (kept in the render: a half-typed line survives closing the screen, AdventurAgent)
        active = this.render.scene.active;
        seenActivate = this.render.scene.activateSeq;
        titleLabelY = -10000;      // (the screen bar is its title)
        inventoryLabelY = -10000;  // (no room for the inventory's title above it)
    }

    @Override
    protected boolean fitsGuiScale() {
        return true;
    }

    /** The address field's column: right of the drives, and of their label (its width differs by language). */
    int addressX() {
        int drives = 9 + 8 * 18 + 14;
        return Math.max(drives + 2 * 18 + 14, drives + font.width(tr("Floppy drives")) + 10);
    }

    /** The panel's width and height for the computer's screen size now. */
    static int widthFor(ComputerRender r) {
        return Math.max(WIDTH, 2 * AREA_X + r.scene.w * CELL);
    }

    static int heightFor(ComputerRender r) {
        return AREA_Y + r.scene.h * LINE + 14 + 76 + 6;
    }

    /** Moves the slots and the inventory under the cells (the panel was sized for them when the screen was made). */
    void layout() {
        laidW = render.scene.w;
        laidH = render.scene.h;
        slotsY = AREA_Y + laidH * LINE + 20;
        invY = AREA_Y + laidH * LINE + 14;
        invX = imageWidth - 9 - 162;
        bottomY = slotsY + 24;
        for (int k = 0; k < ComputerElement.CHIP_SLOTS; k++) menu.moveDeviceSlotTo(k, 9 + 18 * k, slotsY);
        for (int k = 0; k < 2; k++) menu.moveDeviceSlotTo(ComputerElement.DRIVE_A + k, 9 + 18 * 8 + 14 + 18 * k, slotsY);
        menu.movePlayerInventory(invX, invY);
    }

    @Override
    protected void init() {
        layout();
        super.init();
        button(9, bottomY, 50, () -> tr("Reset"), () -> render.clientSendId(ComputerElement.RESET_ID));
        button(61, bottomY, 62, () -> render.switchedOn ? tr("Power off") : tr("Power on"),
            () -> render.clientSendId(ComputerElement.POWER_ID));
        field(addressX(), slotsY, 36, () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(ComputerElement.SET_ADDRESS_ID, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its RS485 address, 17 to 255: the source of the packets it sends."));
        Minecraft.getInstance().onTextInputFocusChange(this, true);
    }

    EditorState editorState(int t, int id) {
        return (EditorState) render.editorStates.computeIfAbsent("ed/" + t + "/" + id, k -> new EditorState());
    }

    // ---- the scene and this display's view of it

    /** The pens the computer's scene draws with (Prefs). */
    mods.eln.robotics.computer.screen.Pens pens() {
        return mods.eln.robotics.computer.screen.Pens.of(scene().pens);
    }

    Scene scene() {
        return render.scene;
    }

    /** The active window, taking the computer's (or another display's) newest activation. */
    Scene.Window activeWindow() {
        Scene s = scene();
        if (s.activateSeq != seenActivate) {
            seenActivate = s.activateSeq;
            if (s.windows.containsKey(s.active) && active != s.active) {
                active = s.active;
                focusId = -1;
            }
        }
        if (!s.windows.containsKey(active) || s.windows.get(active).minimized) {
            // (gone or minimised: the frontmost window that isn't a backdrop or minimised, else none)
            active = -1;
            for (int k = s.order.size() - 1; k >= 0 && active < 0; k--) {
                Scene.Window w = s.windows.get(s.order.get(k));
                if (!w.has("backdrop") && !w.minimized) active = s.order.get(k);
            }
            focusId = -1;
        }
        return active < 0 ? null : s.windows.get(active);
    }

    /** This display changed its copy of the scene (a move, a raise): the monitor repaints. */
    void changed() {
        render.sceneVersion++;
    }

    static String key(int t, int id) {
        return t + "/" + id;
    }

    LineEdit edit(int id) {
        return edits.computeIfAbsent(key(active, id), k -> new LineEdit());
    }

    /** The raw area that has the keyboard: the one clicked last, or the window's first if it has no line. */
    Scene.Element focusedRaw(Scene.Window t) {
        if (t == null) return null;
        Scene.Element e = t.elements.get(focusId);
        if (e != null && (e.kind.equals("raw") || e.kind.equals("line"))) return e.kind.equals("raw") ? e : null;
        Scene.Element raw = null;
        for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) {
            if (en.getValue().kind.equals("line")) return null;
            if (raw == null && en.getValue().kind.equals("raw")) {
                raw = en.getValue();
                focusId = en.getKey();
            }
        }
        return raw;
    }

    /** The focused line, or the window's first line. */
    Scene.Element focusedLine(Scene.Window t) {
        if (t == null) return null;
        Scene.Element e = t.elements.get(focusId);
        if (e != null && e.kind.equals("line")) return e;
        if (e != null && e.kind.equals("raw")) return null;   // (anything else focused: the window's first line)
        for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet())
            if (en.getValue().kind.equals("line")) {
                focusId = en.getKey();
                return en.getValue();
            }
        return null;
    }

    /** A raw area's name for a key that types no character (null: none, or one charTyped sends). */
    static String keyName(int k, boolean ctrlOrAlt) {
        if (ctrlOrAlt && k >= InputConstants.KEY_A && k <= InputConstants.KEY_Z) return String.valueOf((char) ('a' + k - InputConstants.KEY_A));
        if (k >= InputConstants.KEY_F1 && k <= InputConstants.KEY_F12) return "f" + (k - InputConstants.KEY_F1 + 1);
        return switch (k) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> "enter";
            case InputConstants.KEY_BACKSPACE -> "backspace";
            case InputConstants.KEY_TAB -> "tab";
            case InputConstants.KEY_DELETE -> "delete";
            case InputConstants.KEY_INSERT -> "insert";
            case InputConstants.KEY_LEFT -> "left";
            case InputConstants.KEY_RIGHT -> "right";
            case InputConstants.KEY_UP -> "up";
            case InputConstants.KEY_DOWN -> "down";
            case InputConstants.KEY_HOME -> "home";
            case InputConstants.KEY_END -> "end";
            case InputConstants.KEY_PAGEUP -> "pageup";
            case InputConstants.KEY_PAGEDOWN -> "pagedown";
            default -> null;
        };
    }

    int cx(int x) {
        return leftPos + AREA_X + x * CELL;
    }

    int cy(int y) {
        return topPos + AREA_Y + y * LINE;
    }

    int col(double mx) {
        return (int) Math.floor((mx - cx(0)) / CELL);
    }

    int row(double my) {
        return (int) Math.floor((my - cy(0)) / LINE);
    }

    boolean onScreen(double mx, double my) {
        Scene s = scene();
        return mx >= cx(0) && my >= cy(0) && mx < cx(s.w) && my < cy(s.h);
    }

    // ---- editors: code editor widgets over editor elements this display can show whole

    /** Is window wid's rectangle (x, y, w, h in cells) on top everywhere (no other window over it)? */
    boolean uncovered(int wid, int x, int y, int w, int h) {
        Scene s = scene();
        for (int yy = y; yy < y + h; yy++)
            for (int xx = x; xx < x + w; xx++) if (s.windowAt(xx, yy) != wid) return false;
        return true;
    }

    /** The editor elements of the active window, when nothing covers them, as code editor widgets placed over them. */
    @SuppressWarnings("unchecked")
    void syncEditors() {
        Scene.Window t = render.lit() && dragMode == 0 && render.operator && !render.usingCells ? activeWindow() : null;   // (off, a drag, a watcher: none)
        java.util.Set<String> want = new java.util.HashSet<>();
        if (t != null && t.ask == null)
            for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) {
                Scene.Element e = en.getValue();
                if (!e.kind.equals("editor")) continue;
                int ex = t.x + e.x(t), ey = t.y + e.y(t), ew = e.size(5, t, 10), eh = e.size(6, t, 5);
                if (ew < 4 || eh < 2 || !uncovered(active, ex, ey, ew, eh)) continue;
                String k = active + "/" + en.getKey();
                want.add(k);
                List<Object> m = e.msg;
                EditorState st = editorState(active, en.getKey());
                boolean replaced = false;
                if (!m.equals(st.opened) && st.opened instanceof List<?> om && Form.str((List<Object>) om, 8, "").equals(Form.str(m, 8, ""))
                    && st.savingText != null && st.savingText.equals(Form.str(m, 10, "")))
                    st.opened = m;   // (this player's own save, as the scene now keeps it: what they typed since stays)
                if (!m.equals(st.opened)) {
                    // a new editor message: the file opened again (a reused window number, AdventurAgent): start from it.
                    // (by content: the same message replayed when the screen is opened again keeps the unsaved text)
                    st.opened = m;
                    st.baseRev = Form.str(m, 9, "");
                    st.baseText = st.text = Form.str(m, 10, "");
                    st.status = "";
                    replaced = true;
                }
                var w = editorWidgets.get(k);
                // (its last row is left for the save status, which the widget would cover)
                int px = cx(ex), py = cy(ey), pw = ew * CELL, ph = Math.max(1, eh - 1) * LINE;
                if (w == null) {
                    boolean janet = "janet".equals(Form.kwName(m, 7, ""));
                    w = new mods.eln.client.gui.CodeEditor(px, py, pw, ph, st.text, 32768, janet ? JANET : PLAIN, s2 -> {
                        st.text = s2;
                        st.status = "";
                    });
                    editorWidgets.put(k, addRenderableWidget(w));
                } else {
                    if (w.getX() != px || w.getY() != py) w.setPosition(px, py);
                    if (w.getWidth() != pw || w.getHeight() != ph) w.setSize(pw, ph);
                    if (replaced && !w.buffer.text().equals(st.text)) w.buffer.setText(st.text);
                }
                // (an open menu would be drawn under the widget: it hides, keeping its text, cursor and bar, so a pick
                // from the menu still finds it)
                w.visible = !menuOpen;
            }
        // states of editors whose window is gone are forgotten
        render.editorStates.keySet().removeIf(key -> !scene().windows.containsKey(Integer.parseInt(key.split("/")[1])));
        for (var it = editorWidgets.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (!want.contains(en.getKey())) {
                removeWidget(en.getValue());
                it.remove();
            }
        }
    }

    /** Plain text (no colours, no checks). */
    static final mods.eln.gui.code.LanguageAdapter PLAIN = new mods.eln.gui.code.LanguageAdapter() {
        @Override
        public List<List<Token>> tokenize(String text) {
            List<List<Token>> out = new ArrayList<>();
            for (String l : text.split("\n", -1)) out.add(List.of());
            return out;
        }

        @Override
        public List<Diagnostic> validate(String text) {
            return List.of();
        }
    };

    @Override
    protected void containerTick() {
        super.containerTick();
        if (render.scene.w != laidW || render.scene.h != laidH) {   // (the text mode changed: a screen of the new size)
            minecraft.gui.setScreen(new ComputerScreen(menu, minecraft.player.getInventory(), title, render));
            return;
        }
        if (render.places != placesSeen) {   // (the spec: a program's place wins over a drag in progress)
            placesSeen = render.places;
            if (dragMode != 0 && dragWin == render.placedWin) dragMode = 0;
        }
        syncEditors();
        for (List<Object> m; (m = render.replies.poll()) != null; ) {
            int t = (int) Form.num(m, 1, -1), id = (int) Form.num(m, 2, -1);
            EditorState st = editorState(t, id);
            if (Form.num(m, 3, -1) != st.savingSeq) continue;
            if (Form.verb(m).equals("saved")) {
                st.baseRev = Form.str(m, 4, "");
                st.baseText = st.savingText;
                st.status = tr("Saved");
            } else {   // conflict: refused, and refused again (Baughn 2026-11-24): the player's text stays, the file too
                st.status = tr("Not saved: the file changed on the disk. Save As keeps your text");
            }
        }
        // lines the computer took are in its log now; ones it never took (off, stuck, refused) come back after 10 s
        for (var en : edits.entrySet()) {
            LineEdit le = en.getValue();
            String[] p = en.getKey().split("/");
            if (le.pending == null || p.length != 2) continue;
            Scene.Window w = scene().windows.get(Integer.parseInt(p[0]));
            Scene.Element e = w == null ? null : w.elements.get(Integer.parseInt(p[1]));
            if (e != null && e.busySeq >= le.pendingSeq) le.pending = null;
            else if (System.currentTimeMillis() - le.pendingAt > 10_000) {
                if (le.text.isEmpty()) le.text.append(le.pending);
                le.cursor = le.text.length();
                le.pending = null;
            }
        }
    }

    /** Ctrl+S in an editor: its changes as hunks against the revision it was opened (or last saved) at. */
    void save(String key) {
        String[] tid = key.split("/");
        int t = Integer.parseInt(tid[0]), id = Integer.parseInt(tid[1]);
        EditorState st = editorState(t, id);
        Object change = whole(t, id) ? st.text : hunks(st.baseText, st.text);
        if (Form.print(new Form.Symbol("save"), (long) t, (long) id, render.seq + 1, st.baseRev == null ? "" : st.baseRev, change).length()
            > mods.eln.robotics.computer.screen.ScreenHost.MAX_SAVE - 16) {   // (the server would drop it, and "Saving..." would stay; 16: the :gen added on sending)
            st.status = tr("Too big to save in one go: save in smaller steps, or split the file");
            return;
        }
        st.savingSeq = ++render.seq;
        st.savingText = st.text;
        st.status = tr("Saving...");
        render.screenEvent(new Form.Symbol("save"), (long) t, (long) id, st.savingSeq, st.baseRev == null ? "" : st.baseRev, change);
    }

    /** Does the editor's program want whole texts in its saves (Wire 2: :whole true after its text), not hunks? */
    boolean whole(int t, int id) {
        Scene.Window w = scene().windows.get(t);
        Scene.Element e = w == null ? null : w.elements.get(id);
        return e != null && mods.eln.robotics.computer.screen.ScreenHost.whole(e);
    }

    /** One hunk covering what changed between the common first and last lines (none if nothing did). */
    static List<Object> hunks(String base, String now) {
        String[] a = base.split("\n", -1), b = now.split("\n", -1);
        int p = 0;
        while (p < a.length && p < b.length && a[p].equals(b[p])) p++;
        if (p == a.length && p == b.length) return List.of();
        int q = 0;
        while (q < a.length - p && q < b.length - p && a[a.length - 1 - q].equals(b[b.length - 1 - q])) q++;
        return List.of(List.of((long) p, (long) (a.length - p - q), List.of(java.util.Arrays.copyOfRange(b, p, b.length - q))));
    }

    @Override
    public void removed() {
        Minecraft.getInstance().onTextInputFocusChange(this, false);
        super.removed();
    }

    // ---- drawing

    /** What only this player has, for the cells: the line being edited, scrolling, the editors drawn as widgets. */
    final SceneCells.Local local = new SceneCells.Local() {
        @Override
        public String lineText(int win, int id) {
            LineEdit le = edits.get(key(win, id));
            if (le == null) return null;
            if (le.pending != null) return le.pending;
            Scene.Window w = scene().windows.get(win);
            Scene.Element e = w == null ? null : w.elements.get(id);
            if (e == null) return le.text.toString();
            String prompt = Form.str(e.msg, 6, "");
            int room = e.size(5, w, 1) - prompt.codePointCount(0, prompt.length()) - 1;
            int start = Math.max(0, le.cursor - room + 1);
            return le.text.substring(Math.min(start, le.text.length()));
        }

        @Override
        public int scroll(int win, int id) {
            return scrolls.getOrDefault(key(win, id), 0);
        }

        @Override
        public boolean ownEditor(int win, int id) {
            return editorWidgets.containsKey(win + "/" + id);
        }

        @Override
        public String editorText(int win, int id) {
            var w = editorWidgets.get(win + "/" + id);
            return w != null && !w.visible ? w.buffer.text() : null;   // (hidden under a menu: drawn as cells, as it stands)
        }

        @Override
        public boolean selected(String key) {
            return key.equals(selected);
        }

        @Override
        public String clock() {
            return ComputerScreen.clock(scene().clockHours);
        }
    };

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Scene s = scene();
        int x0 = leftPos + AREA_X - 4, y0 = topPos + AREA_Y - 3;
        g.fill(x0, y0, x0 + s.w * CELL + 8, y0 + s.h * LINE + 6, 0xFF101418);
        if (render.lit() && render.usingCells && render.cells != null) cells(g, render.cells);   // (a watcher: the composed picture)
        else if (render.lit()) {
            Scene.Window t = activeWindow();
            SceneCells c = SceneCells.of(s, local, active);
            cells(g, c);
            bevels(g, c, s);
            if (t != null) overlays(g, t);
            if (dragMode != 0) dragOutline(g);
            if (menuOpen) menus(g);
        }
        if (!render.operator) {   // (a watcher: Baughn 2026-11-24, one user at a time)
            String who = render.operatorName.isEmpty() ? tr("someone else") : render.operatorName;
            String msg = tr("Watching: in use by %1$", who);
            int by = y0 + s.h * LINE + 6 - LINE;
            g.fill(x0, by, x0 + s.w * CELL + 8, by + LINE, 0xE0303848);
            g.text(font, msg, x0 + (s.w * CELL + 8 - font.width(msg)) / 2, by + 1, 0xFFFFE070, false);
        }
        // (off, or no chips: a dark screen, no text: Baughn 2026-11-18; the state shows under it)
        label(g, 9, slotsY - 11, tr("Expansion modules"));
        label(g, 9 + 8 * 18 + 14, slotsY - 11, tr("Floppy drives"));
        // the module slots as ISA-style edge connectors (Baughn), under the item slots
        for (int k = 0; k < ComputerElement.CHIP_SLOTS; k++) {
            int sx = leftPos + 9 + 18 * k, sy = topPos + slotsY + 11;   // (inside the slot, under the module)
            g.fill(sx + 1, sy, sx + 15, sy + 4, 0xFF2A2A2A);
            for (int p = 0; p < 6; p++) g.fill(sx + 2 + 2 * p, sy + 1, sx + 3 + 2 * p, sy + 3, 0xFFC8A040);
        }
        label(g, 127, bottomY + 6, render.stateTextClient());
        label(g, addressX(), slotsY - 11, tr("Address"));   // (over its field, beside the drives: Baughn)
    }

    /** The composed cells: each row's paper in runs, then the pictures over the cells they show on, then the glyphs. */
    void cells(GuiGraphicsExtractor g, SceneCells c) {
        for (int y = 0; y < c.h; y++) {
            int x = 0;
            while (x < c.w) {
                int p = c.paper[y * c.w + x], e = x + 1;
                while (e < c.w && c.paper[y * c.w + e] == p) e++;
                g.fill(cx(x), cy(y), cx(e), cy(y + 1), p);
                x = e;
            }
        }
        for (SceneCells.Image im : c.images) {
            var tex = render.pixmapTexture(im.pixmap());
            if (tex == null) continue;
            for (int y = im.y(); y < im.y() + im.ch(); y++) {
                int x = im.x();
                while (x < im.x() + im.cw()) {   // (runs of cells it shows on, one blit each)
                    if (!im.shows(c, x, y)) {
                        x++;
                        continue;
                    }
                    int e = x + 1;
                    while (e < im.x() + im.cw() && im.shows(c, e, y)) e++;
                    float u0 = (x - im.x()) / (float) im.cw(), u1 = (e - im.x()) / (float) im.cw();
                    float v0 = (y - im.y()) / (float) im.ch(), v1 = (y + 1 - im.y()) / (float) im.ch();
                    g.blit(tex, cx(x), cy(y), cx(e), cy(y + 1), u0, u1, v0, v1);
                    x = e;
                }
            }
        }
        for (int y = 0; y < c.h; y++)
            for (int x = 0; x < c.w; x++) {
                int cp = c.glyph[y * c.w + x];
                if (cp == ' ') continue;
                int pp = c.paper[y * c.w + x];
                if ((cp == SceneCells.DEPTH || cp == SceneCells.CLOSE || cp == SceneCells.SIZE || cp == SceneCells.ICONIFY)
                    && (pp == pens().titleActive() || pp == pens().titleIdle())) continue;   // (a window's boxes: gadget())
                if (cp >= 0x2500 && cp <= 0x257F) boxGlyph(g, cp, x, y, c.ink[y * c.w + x]);
                else {
                    String ch = new String(Character.toChars(cp));
                    g.text(font, ch, cx(x) + (CELL - font.width(ch)) / 2, cy(y) + 1, c.ink[y * c.w + x], false);
                }
            }
    }

    // ---- shading (Baughn 1572640714167419132: the period's desks were skeuomorphic, not flat): bevels in pixels, drawn
    // only over cells that show of their window

    /** The button the pointer is holding down (drawn pressed in), or -1. */
    int pressWin = -1, pressId = -1;

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), gg = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
            bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | gg << 8 | bl;
    }

    /** A horizontal pixel line from x0 to x1 (exclusive) at y, where the cells under it are window wid's. */
    void hline(GuiGraphicsExtractor g, SceneCells c, int x0, int x1, int y, int ink, int wid) {
        int row = Math.floorDiv(y - cy(0), LINE);
        if (row < 0 || row >= c.h) return;
        for (int col = Math.max(0, Math.floorDiv(x0 - cx(0), CELL)); col < c.w && cx(col) < x1; col++) {
            if (c.owner[row * c.w + col] != wid) continue;
            int a = Math.max(x0, cx(col)), b = Math.min(x1, cx(col + 1));
            if (a < b) g.fill(a, y, b, y + 1, ink);
        }
    }

    void vline(GuiGraphicsExtractor g, SceneCells c, int x, int y0, int y1, int ink, int wid) {
        int col = Math.floorDiv(x - cx(0), CELL);
        if (col < 0 || col >= c.w) return;
        for (int row = Math.max(0, Math.floorDiv(y0 - cy(0), LINE)); row < c.h && cy(row) < y1; row++) {
            if (c.owner[row * c.w + col] != wid) continue;
            int a = Math.max(y0, cy(row)), b = Math.min(y1, cy(row + 1));
            if (a < b) g.fill(x, a, x + 1, b, ink);
        }
    }

    /** A bevel round the pixel rectangle: raised (light top and left, dark bottom and right) or sunk in. */
    void bevel(GuiGraphicsExtractor g, SceneCells c, int x0, int y0, int x1, int y1, boolean raised, int paper, int wid) {
        int light = mix(paper, 0xFFFFFFFF, 0.5f), dark = mix(paper, 0xFF000000, 0.55f);
        // (on a dark paper black doesn't show: the shadow side is a dim grey instead, the lit side brighter; the tape)
        int lum = ((paper >> 16) & 255) + ((paper >> 8) & 255) + (paper & 255);
        if (lum < 3 * 60) {
            dark = mix(paper, 0xFFFFFFFF, 0.16f);
            light = mix(paper, 0xFFFFFFFF, 0.55f);
        }
        int tl = raised ? light : dark, br = raised ? dark : light;
        hline(g, c, x0, x1, y0, tl, wid);
        vline(g, c, x0, y0, y1, tl, wid);
        hline(g, c, x0, x1, y1 - 1, br, wid);
        vline(g, c, x1 - 1, y0, y1, br, wid);
    }

    /** A bevel round cells (x, y, w, h), inset by in pixels. */
    void bevelCells(GuiGraphicsExtractor g, SceneCells c, int x, int y, int w, int h, int in, boolean raised, int wid) {
        if (w <= 0 || h <= 0 || x < 0 || y < 0 || x >= c.w || y >= c.h) return;
        bevel(g, c, cx(x) + in, cy(y) + in, cx(x + w) - in, cy(y + h) - in, raised, c.paper[y * c.w + x], wid);
    }

    /**
     * A window's close (c), depth (d) or size (s) box drawn over its cell, if the cell shows: a raised button with a
     * dot, two overlapping frames, or a small frame in a big one (the glyphs the monitor uses are wider than a cell).
     */
    void gadget(GuiGraphicsExtractor g, SceneCells c, int x, int y, char kind, int wid) {
        if (x < 0 || y < 0 || x >= c.w || y >= c.h || c.owner[y * c.w + x] != wid) return;
        int paper = c.paper[y * c.w + x], ink = mix(paper, 0xFF000000, 0.7f), lit = mix(paper, 0xFFFFFFFF, 0.6f);
        int px = cx(x), py = cy(y);
        g.fill(px, py, px + CELL, py + LINE, paper);
        bevel(g, c, px, py + 1, px + CELL, py + LINE - 1, true, paper, wid);
        switch (kind) {
            case 'c' -> {
                g.fill(px + 2, py + 4, px + 4, py + 6, ink);
                g.fill(px + 2, py + 4, px + 3, py + 5, lit);
            }
            case 'd' -> {
                g.outline(px + 1, py + 3, 3, 3, ink);
                g.fill(px + 3, py + 5, px + 5, py + 7, lit);
                g.outline(px + 2, py + 4, 3, 3, ink);
            }
            case 'i' -> g.fill(px + 1, py + 6, px + 5, py + 8, ink);   // (a bar at the bottom: down to an icon)
            default -> {
                g.outline(px + 1, py + 2, 4, 6, ink);
                g.outline(px + 1, py + 2, 2, 3, ink);
            }
        }
    }

    void bevels(GuiGraphicsExtractor g, SceneCells c, Scene s) {
        bevel(g, c, cx(0), cy(0), cx(c.w), cy(1), true, pens().bar(), SceneCells.OWNER_BAR);   // the screen bar
        for (int wid : s.order) {
            Scene.Window w = s.windows.get(wid);
            if (w == null) continue;
            if (w.framed()) {
                int[] o = w.outer();
                bevelCells(g, c, o[0], o[1], o[2], o[3], 0, true, wid);                 // the frame stands up
                bevelCells(g, c, w.x, w.y, w.cw, w.ch, -1, false, wid);                 // the content sits in it
                if (w.has("close")) gadget(g, c, w.x - 1, w.y - 1, 'c', wid);
                if (w.has("depth")) gadget(g, c, w.x + w.cw, w.y - 1, 'd', wid);
                if (w.has("iconify")) gadget(g, c, w.x + w.cw - 1, w.y - 1, 'i', wid);
                if (w.has("size")) gadget(g, c, w.x + w.cw, w.y + w.ch, 's', wid);
            }
            for (Map.Entry<Integer, Scene.Element> en : w.elements.entrySet()) {
                Scene.Element e = en.getValue();
                int ex = w.x + e.x(w), ey = w.y + e.y(w);
                switch (e.kind) {
                    case "button" -> bevelCells(g, c, ex, ey, e.size(5, w, 4), 1, 0, !(pressWin == wid && pressId == en.getKey()), wid);
                    // (fields sit in a pixel outside their cells: their first column's letters start at the cell's edge)
                    case "line" -> bevelCells(g, c, ex, ey, e.size(5, w, 1), 1, -1, false, wid);
                    case "list", "raw" -> bevelCells(g, c, ex, ey, e.size(5, w, 1), e.size(6, w, 1), -1, false, wid);
                    case "editor" -> {
                        if (!editorWidgets.containsKey(wid + "/" + en.getKey()))
                            bevelCells(g, c, ex, ey, e.size(5, w, 1), e.size(6, w, 1), -1, false, wid);
                    }
                    case "log" -> {   // (a log filling its window is the window's own text; one beside other things sits in)
                        int lw = e.size(5, w, 1), lh = e.size(6, w, 1);
                        if (lw < w.cw || lh < w.ch - 1) bevelCells(g, c, ex, ey, lw, lh, -1, false, wid);
                    }
                    default -> {
                    }
                }
            }
            if (w.ask != null) {
                int[][] lay = SceneCells.askLayout(w);
                bevelCells(g, c, lay[0][0], lay[0][1], lay[0][2], lay[0][3], 0, true, wid);
                int by = lay[0][1] + lay[0][3] - 2;
                for (int i = 1; i < lay.length; i++) bevelCells(g, c, lay[i][0], by, lay[i][1], 1, 0, true, wid);
            }
        }
    }

    /**
     * A box-drawing glyph drawn as lines through its cell, so frames join cell to cell (the font's glyphs are 6 x 8 and
     * leave gaps between rows of 10 px cells): single lines 1 px, double lines two 1 px lines.
     */
    void boxGlyph(GuiGraphicsExtractor g, int cp, int x, int y, int ink) {
        int px = cx(x), py = cy(y), mx = px + CELL / 2 - 1, my = py + LINE / 2 - 1;
        String s = String.valueOf((char) cp);
        boolean dbl = "═║╔╗╚╝╬╦╩╠╣".contains(s);
        boolean up = "│┘└┤├┼║╝╚╬╩╠╣".contains(s), down = "│┐┌┤├┼║╗╔╬╦╠╣".contains(s);
        boolean left = "─┘┐┤┴┬┼═╝╗╬╩╦╣".contains(s), right = "─└┌├┴┬┼═╚╔╬╩╦╠".contains(s);
        for (int o : dbl ? new int[]{-1, 1} : new int[]{0}) {
            if (up) g.fill(mx + o, py, mx + o + 1, my + 1, ink);
            if (down) g.fill(mx + o, my, mx + o + 1, py + LINE, ink);
            if (left) g.fill(px, my + o, mx + 1, my + o + 1, ink);
            if (right) g.fill(mx, my + o, px + CELL, my + o + 1, ink);
        }
    }

    /** The active window's cursor and completion hint, where nothing covers them, and its editors' save status. */
    void overlays(GuiGraphicsExtractor g, Scene.Window t) {
        Scene.Element line = focusedLine(t);
        if (line != null && t.ask == null && focusedRaw(t) == null) {
            LineEdit le = edit(focusId);
            int x = t.x + line.x(t), y = t.y + line.y(t);
            String prompt = Form.str(line.msg, 6, "");
            int px = prompt.codePointCount(0, prompt.length()), room = line.size(5, t, 1) - px - 1;
            int start = Math.max(0, le.cursor - room + 1), cur = x + px + le.cursor - start;
            if (le.pending == null && (System.currentTimeMillis() / 500) % 2 == 0 && scene().windowAt(cur, y) == active
                && cur < t.x + t.cw && y < t.y + t.ch)
                g.fill(cx(cur), cy(y) + 1, cx(cur) + 1, cy(y + 1) - 1, 0xFFE6E6E6);
            if (le.pending != null)   // (sent, not yet taken: underlined grey)
                g.fill(cx(x + px), cy(y + 1) - 1, cx(x + px + Math.min(room, le.pending.length())), cy(y + 1), 0xFF707070);
            if (!hint.isEmpty() && y - 1 >= t.y) {   // the completion choices, over the row above the line
                g.fill(cx(t.x), cy(y - 1), cx(t.x + t.cw), cy(y), 0xFF2A2E38);
                g.text(font, font.plainSubstrByWidth(hint, t.cw * CELL), cx(t.x) + 1, cy(y - 1) + 1, 0xFFB0B0C0, false);
            }
        }
        for (Map.Entry<Integer, Scene.Element> en : t.elements.entrySet()) {
            Scene.Element e = en.getValue();
            if (!e.kind.equals("editor") || !editorWidgets.containsKey(active + "/" + en.getKey())) continue;
            EditorState st = editorState(active, en.getKey());
            if (st.status.isEmpty()) continue;
            int ex = t.x + e.x(t), ey = t.y + e.y(t) + e.size(6, t, 1) - 1, ew = e.size(5, t, 1);
            g.fill(cx(ex), cy(ey), cx(ex + ew), cy(ey + 1), 0xFF2A2E38);
            g.text(font, font.plainSubstrByWidth(st.status, ew * CELL), cx(ex) + 1, cy(ey) + 1, 0xFFE0C060, false);
        }
    }

    void dragOutline(GuiGraphicsExtractor g) {
        Scene.Window w = scene().windows.get(dragWin);
        if (w == null) return;
        int f = w.framed() ? 1 : 0;
        g.outline(cx(dragX - f), cy(dragY - f), (dragW + 2 * f) * CELL, (dragH + 2 * f) * LINE, 0xFFFFFFFF);
    }

    // ---- the menu bar (Baughn 1572655325104832613: a regular one, the system menu first, listing the windows)

    /** The menu open on the bar (0: the system menu, k: the active window's menu k - 1), the item under the pointer. */
    boolean menuOpen, menuDragging;
    int menuAt = -1, itemAt = -1;
    /** The window each system menu item stands for (-1: not a window). */
    final List<Integer> systemWindows = new ArrayList<>();

    /** Menu m's items: [label, key] each, or null for a line; the system menu's from the windows open now. */
    List<String[]> menuItems(int m) {
        List<String[]> out = new ArrayList<>();
        if (m == 0) {
            systemWindows.clear();
            out.add(new String[]{"About Dynamo", ""});
            systemWindows.add(-1);
            out.add(null);
            systemWindows.add(-1);
            for (var en : scene().windows.entrySet()) {
                Scene.Window w = en.getValue();
                if (w.has("backdrop")) continue;
                String t = w.title.isEmpty() ? "Window " + en.getKey() : w.title;
                out.add(new String[]{w.minimized ? "[" + t + "]" : t, ""});
                systemWindows.add(en.getKey());
            }
            return out;
        }
        Scene.Window t = activeWindow();
        if (t == null || m - 1 >= t.menus.size() || !(t.menus.get(m - 1) instanceof List<?> l)) return out;
        for (int k = 1; k < l.size(); k++) {
            Object o = l.get(k);
            if (o instanceof List<?> it && !it.isEmpty())
                out.add(item(it));
            else out.add(null);
        }
        return out;
    }

    static List<String[]> windowMenuItems(Scene.Window t, int m) {
        List<String[]> out = new ArrayList<>();
        if (!(t.menus.get(m) instanceof List<?> l)) return out;
        for (int k = 1; k < l.size(); k++) {
            Object o = l.get(k);
            if (o instanceof List<?> it && !it.isEmpty())
                out.add(item(it));
        }
        return out;
    }

    /**
     * A menu item as the display needs it: label, Alt key, action and the editor it acts on (Wire 2:
     * ["Save" "s" :save 4]; the action, not the label, says what the display does, in any language).
     */
    static String[] item(List<?> it) {
        return new String[]{String.valueOf(it.get(0)), it.size() > 1 && it.get(1) instanceof String s ? s : "",
            it.size() > 2 && it.get(2) instanceof Form.Keyword kw ? kw.name() : "",
            it.size() > 3 && it.get(3) instanceof Long id ? String.valueOf(id) : ""};
    }

    static String menuTitle(Scene.Window t, int m) {
        return t.menus.get(m) instanceof List<?> l && !l.isEmpty() ? String.valueOf(l.get(0)) : "";
    }

    int menuWidth(List<String[]> items) {
        int w = 8;
        for (String[] it : items) if (it != null) w = Math.max(w, it[0].length() + (it[1].isEmpty() ? 2 : 7));
        return w;
    }

    /** The open menu's dropdown under its title. */
    void menus(GuiGraphicsExtractor g) {
        if (menuAt < 0) return;
        List<String> titles = SceneCells.menuTitles(scene(), active);
        if (menuAt >= titles.size()) return;
        int tx = SceneCells.menuX(titles, menuAt);
        String title = titles.get(menuAt);
        g.fill(cx(tx - 1), cy(0), cx(tx + title.length() + 1), cy(1), pens().barText());
        g.text(font, title, cx(tx), cy(0) + 1, pens().bar(), false);
        List<String[]> items = menuItems(menuAt);
        if (items.isEmpty()) return;
        int x = tx - 1, w = menuWidth(items);
        g.fill(cx(x), cy(1), cx(x + w), cy(1 + items.size()), pens().bar());
        // (it stands up: lit along its top and left, shadowed along its bottom and right)
        int x0 = cx(x), y0 = cy(1), x1 = cx(x + w), y1 = cy(1 + items.size());
        g.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
        g.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
        g.fill(x0, y1 - 1, x1, y1, 0xFF5A5448);
        g.fill(x1 - 1, y0, x1, y1, 0xFF5A5448);
        Scene.Window t = activeWindow();
        for (int i = 0; i < items.size(); i++) {
            String[] it = items.get(i);
            int y = 1 + i;
            if (it == null) {
                g.fill(cx(x) + 2, cy(y) + LINE / 2, cx(x + w) - 2, cy(y) + LINE / 2 + 1, pens().titleIdle());
                continue;
            }
            boolean off = menuAt > 0 && t != null && t.disabled.contains(title + "/" + it[0]), on = i == itemAt && !off;
            if (on) g.fill(cx(x) + 1, cy(y), cx(x + w) - 1, cy(y + 1), pens().barText());
            int ink = off ? 0xFF9A9A9A : on ? pens().bar() : pens().barText();
            g.text(font, it[0], cx(x + 1), cy(y) + 1, ink, false);
            if (!it[1].isEmpty()) g.text(font, "Alt-" + it[1].toUpperCase(), cx(x + w - 6), cy(y) + 1, ink, false);
        }
    }

    /** The menu title under cell (c, 0), or -1. */
    int titleAt(int c) {
        List<String> titles = SceneCells.menuTitles(scene(), active);
        for (int k = 0; k < titles.size(); k++) {
            int x = SceneCells.menuX(titles, k);
            if (c >= x - 1 && c < x + titles.get(k).length() + 1) return k;
        }
        return -1;
    }

    /** While a menu is open: a title under the pointer opens instead; the item under it is marked. */
    void menuHover(double mx, double my) {
        itemAt = -1;
        if (!onScreen(mx, my)) return;
        int c = col(mx), r = row(my);
        if (r == 0) {
            int k = titleAt(c);
            if (k >= 0) menuAt = k;
            return;
        }
        if (menuAt < 0) return;
        List<String> titles = SceneCells.menuTitles(scene(), active);
        List<String[]> items = menuItems(menuAt);
        int x = SceneCells.menuX(titles, menuAt) - 1, w = menuWidth(items);
        if (c >= x && c < x + w && r >= 1 && r < 1 + items.size()) itemAt = r - 1;
    }

    void closeMenu() {
        menuOpen = menuDragging = false;
        menuAt = itemAt = -1;
    }

    /** The item under the pointer picked: the system menu's here, the window's sent to its program. */
    void pickHovered() {
        List<String[]> items = menuItems(menuAt);
        String[] it = itemAt >= 0 && itemAt < items.size() ? items.get(itemAt) : null;
        int m = menuAt, i = itemAt;
        closeMenu();
        if (it == null) return;
        if (m == 0) {
            int wid = i < systemWindows.size() ? systemWindows.get(i) : -1;
            if (wid >= 0) restoreAndActivate(wid);
            else render.screenEvent(new Form.Symbol("pick"), 0L, SceneCells.SYSTEM_MENU, it[0]);
            return;
        }
        Scene.Window t = activeWindow();
        if (t != null) pick(t, menuTitle(t, m - 1), it);
    }

    void pick(Scene.Window t, String menu, String[] it) {
        if (t.disabled.contains(menu + "/" + it[0])) return;
        if (!editorAction(it[2], it[3])) return;   // (a Save As too big to send: no pick, or Ed would write the old text)
        render.screenEvent(new Form.Symbol("pick"), (long) active, menu, it[0]);
    }

    /**
     * Wire 2: a menu item's action acts on this display's editor before the pick goes to the program (Baughn
     * 2026-11-24: actions are tags, never labels): :save saves first (only this display holds the text; Ed's Save, Run and
     * Close carry it), :save-as sends the changes as a stash for the program to write under the new name, :find,
     * :find-next and :go-to-line open the editor's bar or find again, :name gives the keys to the line asking a name.
     * The item names the editor element; without one, the window's only editor.
     */
    boolean editorAction(String action, String element) {
        if (action.isEmpty()) return true;
        // (the editor the item names, or the window's only one; drawn or not: an editor covered by another window still
        // has its text here, and Close must still save it)
        String key = null;
        Scene.Window win = activeWindow();
        if (win == null) return true;
        if (!element.isEmpty()) {
            Scene.Element e = win.elements.get(Integer.parseInt(element));
            key = e != null && e.kind.equals("editor") ? active + "/" + element : null;
        } else
            for (var en : win.elements.entrySet())
                if (en.getValue().kind.equals("editor")) key = key == null ? active + "/" + en.getKey() : "";
        if (key == null || key.isEmpty()) return true;   // (none, or several and the item doesn't say which)
        var w = editorWidgets.get(key);
        EditorState st = editorState(active, Integer.parseInt(key.split("/")[1]));
        if (w == null && !action.equals("save") && !action.equals("save-as")) return true;   // (find and the like need it drawn)
        switch (action) {
            case "save" -> {
                if (st.baseText == null || !st.text.equals(st.baseText)) save(key);
            }
            case "save-as" -> {   // (its changes, kept aside by the program and written only under the new name)
                int eid = Integer.parseInt(key.split("/")[1]);
                Object change = whole(active, eid) ? st.text : hunks(st.baseText, st.text);
                if (Form.print(new Form.Symbol("stash"), (long) active, (long) eid, render.seq + 1, st.baseRev == null ? "" : st.baseRev, change).length()
                    > mods.eln.robotics.computer.screen.ScreenHost.MAX_SAVE - 16) {   // (dropped, Save As would write the old text)
                    st.status = tr("Too big to save in one go: save in smaller steps, or split the file");
                    return false;
                }
                render.screenEvent(new Form.Symbol("stash"), (long) active, (long) eid, ++render.seq,
                    st.baseRev == null ? "" : st.baseRev, change);
                if (w != null) w.setFocused(false);   // (the keys go to the line that asks for the name)
                setFocused(null);
            }
            case "name" -> {
                if (w != null) w.setFocused(false);
                setFocused(null);
            }
            case "find" -> {
                setFocused(w);
                w.openFind();
            }
            case "find-next" -> {
                setFocused(w);
                w.findNext();
            }
            case "go-to-line" -> {
                setFocused(w);
                w.openGoTo();
            }
            default -> {
            }
        }
        return true;
    }

    /** A window brought back: restored if minimised (reported), raised and given the keyboard. */
    void restoreAndActivate(int wid) {
        Scene.Window w = scene().windows.get(wid);
        if (w == null) return;
        if (w.minimized) {
            w.minimized = false;
            render.screenEvent(new Form.Symbol("restored"), (long) wid);
        }
        active = -2;   // (so activate() acts even if it was the active one before it was minimised)
        activate(wid);
    }

    /** Minimised here at once, and reported; the keyboard goes to the next window. */
    void minimize(int wid) {
        Scene.Window w = scene().windows.get(wid);
        if (w == null) return;
        w.minimized = true;
        if (active == wid) {
            active = -1;
            scene().active = -1;
        }
        selected = "min/" + wid;
        changed();
        render.screenEvent(new Form.Symbol("minimized"), (long) wid);
    }

    /** The icon selected on this display ("w/id" a desk icon, "min/w" a minimised window), or null. */
    String selected;

    /** The world's time of day for the clock, as the display sees it. */
    static String clock(int hours) {
        var level = Minecraft.getInstance().level;
        if (level == null) return "";
        long t = Math.floorMod(level.getOverworldClockTime() + 6000, 24000);
        int h = (int) (t / 1000), mi = (int) (t % 1000 * 60 / 1000);
        if (hours == 12) return String.format("%2d:%02d %s", h % 12 == 0 ? 12 : h % 12, mi, h < 12 ? "am" : "pm");
        return String.format("%02d:%02d", h, mi);
    }

    // ---- input

    /** This display makes window wid the active one: raised, and the computer and the other displays told. */
    void activate(int wid) {
        Scene s = scene();
        if (wid == active && !s.order.isEmpty() && s.order.getLast() == wid) return;
        active = wid;
        focusId = -1;
        hint = "";
        s.active = wid;
        s.raise(wid);
        changed();
        render.screenEvent(new Form.Symbol("active"), (long) wid);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
        Scene s = scene();
        double mx = ev.x(), my = ev.y();
        if (!render.operator && mx >= leftPos + AREA_X && my >= topPos + AREA_Y && mx < leftPos + AREA_X + s.w * CELL
            && my < topPos + AREA_Y + s.h * LINE) return true;   // (a watcher's clicks on the screen go nowhere)
        if (!render.lit() || !onScreen(mx, my)) {
            closeMenu();
            return super.mouseClicked(ev, doubleClick);
        }
        int c = col(mx), r = row(my);
        // the menu bar: a click on a title opens its menu (again: closes it); one on an open menu's item picks it
        if (menuOpen) {
            menuHover(mx, my);
            if (r == 0 && titleAt(c) >= 0) {
                if (titleAt(c) == menuAt && !menuDragging) closeMenu();
                else menuDragging = true;
            } else if (itemAt >= 0) pickHovered();
            else closeMenu();
            return true;
        }
        if (r == 0) {
            int k = titleAt(c);
            if (k >= 0) {
                menuOpen = menuDragging = true;
                menuAt = k;
                itemAt = -1;
            }
            return true;
        }
        int wid = s.windowAt(c, r);
        Scene.Window w = wid < 0 ? null : s.windows.get(wid);
        // the desk: minimised windows' icons (over backdrops), then a backdrop's own icons
        if (w == null || w.has("backdrop")) {
            int min = s.minimizedAt(c, r);
            if (min >= 0) {
                selected = "min/" + min;
                if (doubleClick) restoreAndActivate(min);
                return true;
            }
            if (w == null) {
                selected = null;
                return true;
            }
            for (Map.Entry<Integer, Scene.Element> en : w.elements.entrySet()) {
                Scene.Element e = en.getValue();
                if (!e.kind.equals("icon")) continue;
                int ex = w.x + e.x(w), ey = w.y + e.y(w);
                if (c < ex || r < ey || c >= ex + Scene.ICON_W || r >= ey + Scene.ICON_H) continue;
                selected = wid + "/" + en.getKey();
                if (doubleClick) render.screenEvent(new Form.Symbol("open"), (long) wid, (long) en.getKey());
                return true;
            }
            selected = null;
        }
        activate(wid);
        if (w.ask != null) {   // a requester: only its buttons
            int[][] lay = SceneCells.askLayout(w);
            int by = lay[0][1] + lay[0][3] - 2;
            for (int i = 1; i < lay.length; i++)
                if (r == by && c >= lay[i][0] && c < lay[i][0] + lay[i][1]) {
                    render.screenEvent(new Form.Symbol("answer"), (long) wid, Form.num(w.ask, 2, 0), (long) (i - 1));
                    w.ask = null;   // (gone at once here; the computer's unask follows)
                    changed();
                }
            return true;
        }
        if (w.framed() && r == w.y - 1) {   // the title row
            if (c == w.x - 1 && w.has("close")) render.screenEvent(new Form.Symbol("closed"), (long) wid);
            else if (c == w.x + w.cw - 1 && w.has("iconify")) minimize(wid);
            else if (c == w.x + w.cw && w.has("depth")) {
                s.lower(wid);
                changed();
                render.screenEvent(new Form.Symbol("lowered"), (long) wid);
            } else if (w.has("drag")) startDrag(MOVE, wid, w, mx, my);
            return true;
        }
        if (w.framed() && r == w.y + w.ch && c == w.x + w.cw && w.has("size")) {
            startDrag(SIZE, wid, w, mx, my);
            return true;
        }
        if (c < w.x || r < w.y || c >= w.x + w.cw || r >= w.y + w.ch) return true;   // (the frame)
        int col = c - w.x, row = r - w.y;
        // (the last made is drawn on top, so it is hit first: a button over a raw area)
        for (Map.Entry<Integer, Scene.Element> en : new ArrayList<>(w.elements.entrySet()).reversed()) {
            Scene.Element e = en.getValue();
            int ex = e.x(w), ey = e.y(w), ew = e.size(5, w, 1);
            int eh = e.kind.equals("line") || e.kind.equals("button") ? 1 : e.size(6, w, 1);
            if (col < ex || col >= ex + ew || row < ey || row >= ey + eh) continue;
            switch (e.kind) {
                case "line" -> focusId = en.getKey();
                case "raw" -> {
                    focusId = en.getKey();
                    render.screenEvent(new Form.Symbol("click"), (long) wid, (long) en.getKey(), (long) (col - ex), (long) (row - ey));
                }
                case "button" -> {
                    pressWin = wid;   // (drawn pressed in while held; it acts when let go over it, as the period's did)
                    pressId = en.getKey();
                }
                case "list" -> {
                    int index = row - ey + scrolls.getOrDefault(key(wid, en.getKey()), 0);
                    render.screenEvent(new Form.Symbol("select"), (long) wid, (long) en.getKey(), (long) index);
                }
                case "editor" -> {
                    syncEditors();
                    return super.mouseClicked(ev, doubleClick);   // (its widget takes it)
                }
                default -> {
                    continue;
                }
            }
            setFocused(null);   // (the keyboard is the screen's again, not the address field's)
            return true;
        }
        setFocused(null);
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (menuOpen) menuHover(mx, my);
        super.mouseMoved(mx, my);
    }

    void startDrag(int mode, int wid, Scene.Window w, double mx, double my) {
        dragMode = mode;
        dragWin = wid;
        dragX = w.x;
        dragY = w.y;
        dragW = w.cw;
        dragH = w.ch;
        dragFromX = mx;
        dragFromY = my;
        syncEditors();   // (editors hide while a window moves)
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
        if (menuOpen) {
            menuHover(ev.x(), ev.y());
            return true;
        }
        if (dragMode == 0) return super.mouseDragged(ev, dx, dy);
        Scene.Window w = scene().windows.get(dragWin);
        if (w == null) {
            dragMode = 0;
            return true;
        }
        int dc = (int) Math.round((ev.x() - dragFromX) / CELL), dr = (int) Math.round((ev.y() - dragFromY) / LINE);
        // (the outline follows the hand, kept on the screen as Scene.fit keeps windows; the window moves when it lets go)
        int x = w.x, y = w.y, cw = w.cw, ch = w.ch;
        if (dragMode == MOVE) {
            w.x += dc;
            w.y += dr;
        } else {
            w.cw += dc;
            w.ch += dr;
        }
        scene().fit(w);
        dragX = w.x;
        dragY = w.y;
        dragW = w.cw;
        dragH = w.ch;
        w.x = x;
        w.y = y;
        w.cw = cw;
        w.ch = ch;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent ev) {
        if (pressWin >= 0) {
            Scene.Window w = scene().windows.get(pressWin);
            Scene.Element e = w == null ? null : w.elements.get(pressId);
            int c = col(ev.x()), r = row(ev.y());
            if (e != null && e.kind.equals("button") && scene().windowAt(c, r) == pressWin) {
                int ex = w.x + e.x(w), ey = w.y + e.y(w);
                if (c >= ex && c < ex + e.size(5, w, 4) && r == ey)
                    render.screenEvent(new Form.Symbol("press"), (long) pressWin, (long) pressId);
            }
            pressWin = pressId = -1;
            return true;
        }
        if (menuOpen) {
            // pressed on a title and let go on an item: picked (the period's way); let go on the title: it stays open
            menuHover(ev.x(), ev.y());
            if (menuDragging && itemAt >= 0) pickHovered();
            menuDragging = false;
            return true;
        }
        if (dragMode != 0) {
            Scene.Window w = scene().windows.get(dragWin);
            dragMode = 0;
            if (w != null && (w.x != dragX || w.y != dragY || w.cw != dragW || w.ch != dragH)) {
                w.x = dragX;
                w.y = dragY;
                w.cw = dragW;
                w.ch = dragH;
                changed();
                render.screenEvent(new Form.Symbol("moved"), (long) dragWin, (long) w.x, (long) w.y, (long) w.cw, (long) w.ch);
            }
            syncEditors();
            return true;
        }
        return super.mouseReleased(ev);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        Scene s = scene();
        if (render.lit() && onScreen(mx, my)) {
            int c = col(mx), r = row(my), wid = s.windowAt(c, r);
            Scene.Window w = s.windows.get(wid);
            if (w != null) {
                int colIn = c - w.x, rowIn = r - w.y;
                for (Map.Entry<Integer, Scene.Element> en : w.elements.entrySet()) {
                    Scene.Element e = en.getValue();
                    if (!e.kind.equals("log") && !e.kind.equals("list")) continue;
                    int ex = e.x(w), ey = e.y(w), ew = e.size(5, w, 1), eh = e.size(6, w, 1);
                    if (colIn < ex || colIn >= ex + ew || rowIn < ey || rowIn >= ey + eh) continue;
                    String k = key(wid, en.getKey());
                    int step = e.kind.equals("log") ? (sy > 0 ? 3 : -3) : (sy > 0 ? -1 : 1);
                    scrolls.put(k, Math.max(0, scrolls.getOrDefault(k, 0) + step));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    boolean fieldHasKeys() {
        return getFocused() instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!render.operator) return false;
        String ed = focusedEditor();
        if (ed != null) return menuOpen || editorWidgets.get(ed).charTyped(event);   // (hidden under a menu: nothing)
        Scene.Window t = render.lit() ? activeWindow() : null;
        if (t == null || t.ask != null) return super.charTyped(event);
        if (!fieldHasKeys() && focusedRaw(t) != null) {
            render.screenEvent(new Form.Symbol("key"), (long) active, (long) focusId, event.codepointAsString(), 0L);   // (Shift is in the character)
            return true;
        }
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

    String focusedEditor() {
        for (var en : editorWidgets.entrySet()) if (en.getValue().isFocused()) return en.getKey();
        return null;
    }

    @Override
    public boolean keyReleased(KeyEvent ev) {
        Scene.Window t = render.operator && !fieldHasKeys() ? activeWindow() : null;
        if (t != null && t.ask == null && !menuOpen && focusedRaw(t) != null) {   // (Wire 2: games hear keys let go)
            int mods = (ev.hasShiftDown() ? 1 : 0) | (ev.hasControlDown() ? 2 : 0) | (ev.hasAltDown() ? 4 : 0);
            String name = keyName(ev.key(), true);
            if (name != null) {
                render.screenEvent(new Form.Symbol("keyup"), (long) active, (long) focusId, name, (long) mods);
                return true;
            }
        }
        return super.keyReleased(ev);
    }

    @Override
    public boolean keyPressed(KeyEvent ev) {
        if (fieldHasKeys() || !render.operator) return super.keyPressed(ev);
        int k = ev.key();
        if (menuOpen && k == InputConstants.KEY_ESCAPE) {
            closeMenu();
            return true;
        }
        Scene.Window t = render.lit() ? activeWindow() : null;
        // Alt+letter: the active window's menu item with that key, without opening the menus
        if (t != null && ev.hasAltDown() && k >= InputConstants.KEY_A && k <= InputConstants.KEY_Z) {
            String letter = String.valueOf((char) ('a' + k - InputConstants.KEY_A));
            for (int m = 0; m < t.menus.size(); m++)
                for (String[] it : windowMenuItems(t, m))
                    if (it != null && it[1].equalsIgnoreCase(letter)) {
                        pick(t, menuTitle(t, m), it);
                        return true;
                    }
        }
        if (menuOpen) return true;   // (a hidden editor under the menu takes nothing)
        String ed = focusedEditor();
        if (ed != null && k == InputConstants.KEY_ESCAPE && editorWidgets.get(ed).escape()) return true;   // (the find bar closes first)
        if (ed != null && k != InputConstants.KEY_ESCAPE) {
            if (k == InputConstants.KEY_S && ev.hasControlDownWithQuirk()) save(ed);
            else editorWidgets.get(ed).keyPressed(ev);
            return true;
        }
        if (t == null || t.ask != null) return super.keyPressed(ev);
        Scene.Element line = focusedLine(t);
        if (k == InputConstants.KEY_C && ev.hasControlDownWithQuirk() && !ev.hasShiftDown()) {
            render.screenEvent(new Form.Symbol("break"), (long) active);   // (on any window, a line or not)
            return true;
        }
        if (k != InputConstants.KEY_ESCAPE && focusedRaw(t) != null) {
            // (characters come through charTyped; Escape still closes the screen)
            int mods = (ev.hasShiftDown() ? 1 : 0) | (ev.hasControlDown() ? 2 : 0) | (ev.hasAltDown() ? 4 : 0);
            String name = keyName(k, (mods & 6) != 0);
            if (name != null) render.screenEvent(new Form.Symbol("key"), (long) active, (long) focusId, name, (long) mods);
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
        // Ctrl+C is Break for this window, as in a terminal (Baughn, above); Ctrl+Shift+C copies the line
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
        le.pendingSeq = ++render.seq;
        le.pending = text;
        le.pendingAt = System.currentTimeMillis();
        if (!text.isBlank() && (le.history.isEmpty() || !le.history.getLast().equals(text))) le.history.add(text);
        le.historyAt = -1;
        le.text.setLength(0);
        le.cursor = 0;
        hint = "";
        for (var en : scrolls.entrySet()) en.setValue(0);
        render.screenEvent(new Form.Symbol("submit"), (long) active, (long) focusId, le.pendingSeq, text);
    }

    /** Completion from the names the computer sent (Tab): the common part, and the choices as a hint. */
    void complete(LineEdit le, Scene.Window t) {
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
