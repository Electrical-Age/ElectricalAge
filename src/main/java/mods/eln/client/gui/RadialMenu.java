package mods.eln.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A radial menu centred on the screen, for a held tool's modes (first: the chisel's brush, client/ChiselRadial).
 * Rings of segments round a centre; each segment picks one choice. Opened while a key is held: releasing the key
 * picks the segment under the mouse and commits; a click picks a segment and stays open while the key is held (so one
 * opening can set a choice on every ring), otherwise commits. A click on the centre commits, Esc cancels. Opened by a
 * tap (key already up): clicks pick and commit; pressing the key again commits.
 */
public class RadialMenu extends Screen {
    /** One choice: its label, whether it is the current choice (highlighted), and what picking it does. */
    public record Segment(Component label, BooleanSupplier current, Runnable pick) {
    }

    /** Radii (GUI pixels): the centre disc, the gap between rings, a ring's width. */
    static final int CENTRE = 22, GAP = 3, RING = 34;
    static final int COL_RING = 0x90101010, COL_HOVER = 0xB0505050, COL_CURRENT = 0xA0285A28, COL_CURRENT_HOVER = 0xC0408040,
        COL_CENTRE = 0xA0000000, COL_TEXT = 0xFFFFFFFF, COL_TEXT_DIM = 0xFFB0B0B0;

    final List<List<Segment>> rings;
    final Supplier<Component> centre;
    final KeyMapping holdKey;
    final Runnable commit;
    boolean held;
    double mouseX, mouseY;

    /**
     * rings: innermost first, segment 0 of each centred at the top and going clockwise. centre: the text in the middle
     * (the choices so far). held: opened while holdKey is down. commit: runs once when the choices are taken.
     */
    public RadialMenu(Component title, List<List<Segment>> rings, Supplier<Component> centre, KeyMapping holdKey, boolean held, Runnable commit) {
        super(title);
        this.rings = rings;
        this.centre = centre;
        this.holdKey = holdKey;
        this.held = held;
        this.commit = commit;
    }

    static int inner(int ring) {
        return CENTRE + GAP + ring * (RING + GAP);
    }

    /**
     * The ring and segment at (dx, dy) from the centre (screen axes, y down), given each ring's segment count:
     * {ring, segment}, ring -1 = the centre; null between rings or outside. Segment i of n spans the angle
     * (i - 1/2 .. i + 1/2) * 360/n clockwise from the top.
     */
    public static int[] at(double dx, double dy, int[] counts) {
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < CENTRE) return new int[]{-1, 0};
        for (int k = 0; k < counts.length; k++) {
            if (r < inner(k) || r >= inner(k) + RING || counts[k] == 0) continue;
            double step = 2 * Math.PI / counts[k];
            double a = Math.atan2(dx, -dy) + step / 2;
            a = (a % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
            return new int[]{k, Math.min(counts[k] - 1, (int) (a / step))};
        }
        return null;
    }

    int[] counts() {
        int[] c = new int[rings.size()];
        for (int k = 0; k < c.length; k++) c[k] = rings.get(k).size();
        return c;
    }

    int[] hovered() {
        return at(mouseX - width / 2.0, mouseY - height / 2.0, counts());
    }

    @Override
    protected void init() {
        mouseX = width / 2.0;
        mouseY = height / 2.0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No blur or dimming: the world stays visible round the menu (subtitles still drawn, as Screen does). */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        mouseX = mx;
        mouseY = my;
        int cx = width / 2, cy = height / 2, outer = inner(rings.size() - 1) + RING;
        int[] counts = counts(), hover = hovered();
        // the rings, a row of spans at a time (GuiGraphicsExtractor only fills rectangles); 1-pixel gaps between segments
        for (int y = -outer; y < outer; y++) {
            int runStart = -outer, runCol = 0;
            for (int x = -outer; x <= outer; x++) {
                int col = x == outer ? 0 : colourAt(x + 0.5, y + 0.5, counts, hover);
                if (col != runCol) {
                    if (runCol != 0) g.fill(cx + runStart, cy + y, cx + x, cy + y + 1, runCol);
                    runStart = x;
                    runCol = col;
                }
            }
        }
        for (int k = 0; k < rings.size(); k++) {
            var ring = rings.get(k);
            double r = inner(k) + RING / 2.0;
            for (int i = 0; i < ring.size(); i++) {
                double ang = i * 2 * Math.PI / ring.size();
                var s = ring.get(i);
                g.centeredText(font, s.label(), cx + (int) Math.round(r * Math.sin(ang)), cy - (int) Math.round(r * Math.cos(ang)) - 4,
                    s.current().getAsBoolean() ? COL_TEXT : COL_TEXT_DIM);
            }
        }
        g.centeredText(font, centre.get(), cx, cy - 4, COL_TEXT);
        g.centeredText(font, title, cx, cy - outer - 12, COL_TEXT);
    }

    int colourAt(double x, double y, int[] counts, int[] hover) {
        double r = Math.sqrt(x * x + y * y);
        int[] at = at(x, y, counts);
        if (at == null) return 0;
        if (at[0] < 0) return r < CENTRE - 1 ? COL_CENTRE : 0;
        double step = 2 * Math.PI / counts[at[0]];
        if (counts[at[0]] > 1) {
            double a = Math.atan2(x, -y) + step / 2;
            a = (a % step + step) % step;   // angle from the segment's start
            if (Math.min(a, step - a) * r < 1) return 0;
        }
        boolean h = hover != null && hover[0] == at[0] && hover[1] == at[1];
        boolean cur = rings.get(at[0]).get(at[1]).current().getAsBoolean();
        return cur ? (h ? COL_CURRENT_HOVER : COL_CURRENT) : h ? COL_HOVER : COL_RING;
    }

    /** Picks the segment under the mouse (if any), commits and closes. */
    void finish() {
        int[] h = hovered();
        if (h != null && h[0] >= 0) rings.get(h[0]).get(h[1]).pick().run();
        commit.run();
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (holdKey.matchesMouse(e)) {
            if (!held) finish();   // (as keyPressed: the key bound to a mouse button)
            return true;
        }
        if (e.button() != 0) return false;
        int[] h = at(e.x() - width / 2.0, e.y() - height / 2.0, counts());
        if (h == null) return true;
        if (h[0] < 0 || !held) {
            mouseX = e.x();
            mouseY = e.y();
            finish();
        } else rings.get(h[0]).get(h[1]).pick().run();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (held && holdKey.matchesMouse(e)) {
            finish();
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (holdKey.matches(e)) {
            if (!held) finish();   // (opened by a tap: the key again takes the choices; while held, key repeats)
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(KeyEvent e) {
        if (held && holdKey.matches(e)) {
            finish();
            return true;
        }
        return super.keyReleased(e);
    }
}
