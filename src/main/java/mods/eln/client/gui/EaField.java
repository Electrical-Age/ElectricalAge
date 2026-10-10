package mods.eln.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** A text box that commits on Enter or when it loses focus (1.12 GuiTextFieldEln). */
public class EaField extends EditBox {
    final Consumer<String> commit;
    String committed;

    public EaField(int x, int y, int w, String initial, Consumer<String> commit) {
        super(Minecraft.getInstance().font, x, y, w, 18, Component.empty());
        this.commit = commit;
        setMaxLength(64);
        setValue(initial);
        committed = initial;
    }

    void fire() {
        if (!getValue().equals(committed)) {
            committed = getValue();
            commit.accept(getValue());
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
            || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
            fire();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void setFocused(boolean focused) {
        if (isFocused() && !focused) fire();
        super.setFocused(focused);
    }

    /** Follow the device's value unless the player is editing. */
    public void setFromDevice(String v) {
        if (isFocused() || v == null || v.equals(committed)) return;
        committed = v;
        setValue(v);
    }

    /** A float from the text (locale decimal mark or a dot), or NaN. */
    public static float parse(String s) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }
}
