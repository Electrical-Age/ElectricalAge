package mods.eln.client.gui;

import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.function.Function;

/** A slot screen with value lines computed from the device's slots (1.12 resistor, capacitor, inductor GUIs). */
public class InfoScreen extends ElnScreen {
    final Function<Container, List<String>> lines;

    public InfoScreen(ElnNodeMenu menu, Inventory inventory, Component title, Function<Container, List<String>> lines) {
        super(menu, inventory, title);
        this.lines = lines;
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int y = 18;
        for (String l : lines.apply(menu.deviceInventory)) {
            label(g, 8, y, l);
            y += 10;
        }
    }
}
