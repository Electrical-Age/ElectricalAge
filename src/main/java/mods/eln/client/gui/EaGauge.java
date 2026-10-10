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
 * value's fraction, coloured by zone (green in the nominal band, amber above, red past the limit), text on top.
 */
public class EaGauge extends AbstractWidget {
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
        int color = f >= redFrom ? 0xFFC03030 : f >= amberFrom ? 0xFFD0A020 : 0xFF40A040;
        int fill = (int) Math.round((width - 2) * f);
        if (fill > 0) g.fill(getX() + 1, getY() + 1, getX() + 1 + fill, getY() + height - 1, color);
        Component t = text.get();
        var font = Minecraft.getInstance().font;
        g.text(font, t, getX() + (width - font.width(t)) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
