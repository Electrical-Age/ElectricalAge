package mods.eln.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * A labelled horizontal slider over [min, max] in steps (P4b; replaces 1.12's vertical track bars). The value is sent
 * when the mouse is released or a key changes it, as 1.12 did; while not dragged it follows the device (setFromDevice).
 */
public class EaSlider extends AbstractSliderButton {
    final double min, max, step;
    final DoubleFunction<Component> label;
    final DoubleConsumer onCommit;
    private boolean held;

    public EaSlider(int x, int y, int w, double min, double max, double step, double initial,
                    DoubleFunction<Component> label, DoubleConsumer onCommit) {
        super(x, y, w, 20, Component.empty(), 0);
        this.min = min;
        this.max = max;
        this.step = step;
        this.label = label;
        this.onCommit = onCommit;
        this.value = toUnit(initial);
        updateMessage();
    }

    double toUnit(double v) {
        return Math.max(0, Math.min(1, (v - min) / (max - min)));
    }

    public double get() {
        double v = min + value * (max - min);
        if (step > 0) v = Math.round(v / step) * step;
        return Math.max(min, Math.min(max, v));
    }

    /** Follow the device's value unless the player is moving the slider. */
    public void setFromDevice(double v) {
        if (held || isFocused() && dragging) return;
        value = toUnit(v);
        updateMessage();
    }

    /** Re-read the label (it may show live values too). */
    public void refreshLabel() {
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(get()));
    }

    @Override
    protected void applyValue() {
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        held = true;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        held = false;
        onCommit.accept(get());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        boolean r = super.keyPressed(event);
        if (r) onCommit.accept(get());
        return r;
    }
}
