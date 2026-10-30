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
import java.util.function.Consumer;

/**
 * The signal processor's quick reference (board #45), drawn as a handbook page held beside the editor: the functions
 * by category, each with its signature, what it does and an example, in small print. A click on an entry inserts its
 * call into the editor; the wheel scrolls.
 */
public class ReferencePane extends AbstractWidget {
    static final float SCALE = 0.75f;
    static final int PAPER = 0xFFEFE6CC, EDGE = 0xFFB8A880, INK = 0xFF2C2A24, FAINT = 0xFF6E6858, HEAD = 0xFF7A3418, SIG = 0xFF1E3A70, HOVER = 0xFFE2D6B4;
    final Consumer<String> insert;
    int scroll;

    /** An entry as drawn: its lines and where it sits (unscaled px from the page's top). */
    record Placed(FunctionReference.Entry entry, int top, int bottom, List<FormattedCharSequence> meaning) {
    }

    public ReferencePane(int x, int y, int w, int h, Consumer<String> insert) {
        super(x, y, w, h, Component.literal("reference"));
        this.insert = insert;
    }

    int inner() {
        return (int) ((width - 10) / SCALE);
    }

    /** Lay the page out: headings and entries, in page px (scaled). */
    List<Object[]> layout(Font font) {
        List<Object[]> out = new ArrayList<>();
        float y = 16;
        String cat = "";
        for (var e : FunctionReference.ENTRIES) {
            if (!e.category().equals(cat)) {
                cat = e.category();
                y += 3;
                out.add(new Object[]{cat, y});
                y += 10 * SCALE + 1;
            }
            var meaning = font.split(Component.literal(e.meaning()), inner());
            float h = (9 * (2 + meaning.size())) * SCALE + 3;
            out.add(new Object[]{new Placed(e, (int) y, (int) (y + h), meaning), y});
            y += h;
        }
        return out;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX(), y0 = getY();
        // the page: paper with a darker edge, a little shadow at the spine
        g.fill(x0 + 2, y0 + 2, x0 + width + 2, y0 + height + 2, 0x40000000);
        g.fill(x0, y0, x0 + width, y0 + height, EDGE);
        g.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, PAPER);
        g.fill(x0 + 1, y0 + 1, x0 + 3, y0 + height - 1, 0xFFDCD0B0);
        g.text(font, "Quick reference", x0 + 6, y0 + 4, HEAD, false);
        g.enableScissor(x0 + 1, y0 + 14, x0 + width - 1, y0 + height - 1);
        Placed hovered = null;
        for (Object[] o : layout(font)) {
            float y = (Float) o[1] - scroll;
            if (o[0] instanceof String cat) {
                drawScaled(g, font, Component.literal(cat).getVisualOrderText(), x0 + 5, y0 + y, HEAD);
                g.fill(x0 + 5, (int) (y0 + y + 8), x0 + width - 5, (int) (y0 + y + 8) + 1, EDGE);
                continue;
            }
            Placed p = (Placed) o[0];
            int top = y0 + p.top() - scroll, bottom = y0 + p.bottom() - scroll;
            if (mouseX >= x0 && mouseX < x0 + width && mouseY >= Math.max(top, y0 + 14) && mouseY < bottom && mouseY < y0 + height) {
                hovered = p;
                g.fill(x0 + 3, top, x0 + width - 2, bottom - 1, HOVER);
            }
            float ly = top;
            drawScaled(g, font, Component.literal(p.entry().signature()).getVisualOrderText(), x0 + 6, ly, SIG);
            ly += 9 * SCALE;
            for (var m : p.meaning()) {
                drawScaled(g, font, m, x0 + 6, ly, INK);
                ly += 9 * SCALE;
            }
            drawScaled(g, font, Component.literal("e.g. " + p.entry().example()).getVisualOrderText(), x0 + 6, ly, FAINT);
        }
        g.disableScissor();
        if (hovered != null) g.setTooltipForNextFrame(Component.literal("Click to insert " + hovered.entry().insert().trim()), mouseX, mouseY);
    }

    static void drawScaled(GuiGraphicsExtractor g, Font font, FormattedCharSequence s, float x, float y, int colour) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SCALE, SCALE);
        g.text(font, s, 0, 0, colour, false);
        g.pose().popMatrix();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        Font font = Minecraft.getInstance().font;
        for (Object[] o : layout(font))
            if (o[0] instanceof Placed p && event.y() >= getY() + p.top() - scroll && event.y() < getY() + p.bottom() - scroll && event.y() >= getY() + 14) {
                insert.accept(p.entry().insert());
                return;
            }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!isMouseOver(x, y)) return false;
        Font font = Minecraft.getInstance().font;
        var l = layout(font);
        int end = l.isEmpty() ? 0 : ((Placed) l.get(l.size() - 1)[0]).bottom();
        scroll = Math.max(0, Math.min(Math.max(0, end - height + 6), scroll - (int) Math.signum(sy) * 12));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
