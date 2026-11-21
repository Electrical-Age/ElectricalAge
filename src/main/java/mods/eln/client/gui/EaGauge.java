package mods.eln.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A horizontal read-only bar in an inset (P4b; replaces 1.12's vertical progress and working-zone bars): fill from the
 * value's fraction, coloured by zone (green in the nominal band, amber above, red past the limit), text on top. Its
 * static chargeBar is the battery-charge bar the HUDs draw (the belt's, the bike's).
 */
public class EaGauge extends AbstractWidget {
    public static final int GREEN = 0xFF40A040, AMBER = 0xFFD0A020, RED = 0xFFC03030;
    /** A charge bar's rim and inside. */
    public static final int RIM = 0xFF9AA4AE, INSIDE = 0xFF15181C;

    final DoubleSupplier fraction;
    final Supplier<Component> text;
    /** Fraction where amber starts, and where red starts (>1 disables). */
    final double amberFrom, redFrom;

    public EaGauge(int x, int y, int w, int h, DoubleSupplier fraction, Supplier<Component> text, double amberFrom, double redFrom) {
        super(x, y, w, h, Component.empty());
        this.fraction = fraction;
        this.text = text;
        this.amberFrom = amberFrom;
        this.redFrom = redFrom;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ElnScreen.INSET, getX(), getY(), width, height);
        double f = Math.max(0, Math.min(1, fraction.getAsDouble()));
        int color = f >= redFrom ? RED : f >= amberFrom ? AMBER : GREEN;
        int fill = (int) Math.round((width - 2) * f);
        if (fill > 0) g.fill(getX() + 1, getY() + 1, getX() + 1 + fill, getY() + height - 1, color);
        Component t = text.get();
        var font = Minecraft.getInstance().font;
        g.text(font, t, getX() + (width - font.width(t)) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF, true);
    }

    /** A battery's charge colour: green above half, amber above a fifth, red below (the bike's thresholds). */
    public static int chargeColor(double share) {
        return share > 0.5 ? GREEN : share > 0.2 ? AMBER : RED;
    }

    /**
     * A battery's charge in the box (x, y, w, h), rim included: a 1-pixel rim, a dark inside, and the charge filled
     * from the left, or from the bottom when vertical, in chargeColor; at least a pixel while any is left.
     */
    public static void chargeBar(GuiGraphicsExtractor g, int x, int y, int w, int h, double share, boolean vertical) {
        g.fill(x, y, x + w, y + h, RIM);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSIDE);
        double s = Math.max(0, Math.min(1, share));
        int len = vertical ? h - 2 : w - 2;
        int fill = s > 0 ? Math.max(1, (int) Math.round(len * s)) : 0;
        if (fill == 0) return;
        int c = chargeColor(s);
        if (vertical) g.fill(x + 1, y + h - 1 - fill, x + w - 1, y + h - 1, c);
        else g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, c);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
