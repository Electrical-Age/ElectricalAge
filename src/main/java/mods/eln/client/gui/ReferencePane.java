package mods.eln.client.gui;

import mods.eln.sixnode.electricalmath.FunctionReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The signal processor's quick reference (board #45), drawn as a ruled handbook page held beside the editor, in the
 * game's own font, a line of text on each rule. The categories are blocks that open and close with a click, all
 * closed at first (Baughn 2026-10-30); a click on an open entry inserts its call into the editor. The wheel scrolls.
 */
public class ReferencePane extends AbstractWidget {
    static final int ROW = 10, TOP = 14;
    static final int PAPER = 0xFFEFE6CC, EDGE = 0xFFB8A880, RULE = 0x2A3060A0, MARGIN = 0x40C04040, INK = 0xFF2C2A24, FAINT = 0xFF7A7464,
        HEAD = 0xFF7A3418, SIG = 0xFF1E3A70, HOVER = 0x30A08040;
    final Consumer<String> insert;
    /** The open categories (the screen keeps them over a re-layout). */
    final Set<String> open;
    int scroll;

    /** A line of the page: its text and colour, and what a click on it does (a category to toggle, or an entry). */
    record Row(FormattedCharSequence text, int colour, String category, FunctionReference.Entry entry) {
    }

    public ReferencePane(int x, int y, int w, int h, Set<String> open, Consumer<String> insert) {
        super(x, y, w, h, Component.literal("reference"));
        this.open = open;
        this.insert = insert;
    }

    int textX() {
        return getX() + 12;
    }

    int textWidth() {
        return width - 18;
    }

    int visibleRows() {
        return (height - TOP - 2) / ROW;
    }

    List<Row> rows(Font font) {
        List<Row> out = new ArrayList<>();
        for (String cat : FunctionReference.CATEGORIES) {
            boolean isOpen = open.contains(cat);
            out.add(new Row(Component.literal((isOpen ? "- " : "+ ") + cat).getVisualOrderText(), HEAD, cat, null));
            if (!isOpen) continue;
            for (var e : FunctionReference.ENTRIES) {
                if (!e.category().equals(cat)) continue;
                for (var l : font.split(Component.literal(e.signature()), textWidth())) out.add(new Row(l, SIG, null, e));
                for (var l : font.split(Component.literal(e.meaning()), textWidth())) out.add(new Row(l, INK, null, e));
                for (var l : font.split(Component.literal("e.g. " + e.example()), textWidth())) out.add(new Row(l, FAINT, null, e));
            }
        }
        return out;
    }

    /** The row under a y, or -1. */
    int rowAt(double y) {
        if (y < getY() + TOP || y >= getY() + TOP + visibleRows() * ROW) return -1;
        return (int) ((y - getY() - TOP) / ROW) + scroll;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX(), y0 = getY();
        List<Row> rows = rows(font);
        scroll = Math.max(0, Math.min(scroll, rows.size() - visibleRows()));
        // the page: paper with a darker edge and a shadow, faint rules and a margin
        g.fill(x0 + 2, y0 + 2, x0 + width + 2, y0 + height + 2, 0x40000000);
        g.fill(x0, y0, x0 + width, y0 + height, EDGE);
        g.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, PAPER);
        for (int r = 0; r <= visibleRows(); r++) g.fill(x0 + 1, y0 + TOP + r * ROW - 1, x0 + width - 1, y0 + TOP + r * ROW, RULE);
        g.fill(x0 + 9, y0 + 1, x0 + 10, y0 + height - 1, MARGIN);
        g.text(font, "Quick reference", textX(), y0 + 3, HEAD, false);
        int hover = isMouseOver(mouseX, mouseY) ? rowAt(mouseY) : -1;
        Row hovered = hover >= 0 && hover < rows.size() ? rows.get(hover) : null;
        for (int r = 0; r < visibleRows() && r + scroll < rows.size(); r++) {
            Row row = rows.get(r + scroll);
            int y = y0 + TOP + r * ROW;
            // the hovered entry (all its lines) or category
            if (hovered != null && (row.entry() != null && row.entry() == hovered.entry() || row.category() != null && row == hovered))
                g.fill(x0 + 10, y, x0 + width - 1, y + ROW - 1, HOVER);
            g.text(font, row.text(), row.category() != null ? x0 + 4 : textX(), y + 1, row.colour(), false);
        }
        if (rows.size() > visibleRows()) {
            // where the page is: a thin bar at the edge
            int track = visibleRows() * ROW, bar = Math.max(8, track * visibleRows() / rows.size());
            int by = y0 + TOP + (track - bar) * scroll / Math.max(1, rows.size() - visibleRows());
            g.fill(x0 + width - 3, by, x0 + width - 1, by + bar, 0x60806040);
        }
        if (hovered != null && hovered.entry() != null)
            g.setTooltipForNextFrame(Component.literal("Click to insert " + hovered.entry().insert().trim()), mouseX, mouseY);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        List<Row> rows = rows(Minecraft.getInstance().font);
        int r = rowAt(event.y());
        if (r < 0 || r >= rows.size()) return;
        Row row = rows.get(r);
        if (row.category() != null) {
            if (!open.remove(row.category())) open.add(row.category());
        } else if (row.entry() != null) insert.accept(row.entry().insert());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!isMouseOver(x, y)) return false;
        scroll = Math.max(0, scroll - (int) Math.signum(sy) * 2);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
