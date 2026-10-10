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
        List<String> ls = lines.apply(menu.deviceInventory);
        if (layout != null) {
            for (int i = 0; i < ls.size(); i++) {
                var r = layout.get("text", "line" + i);
                if (r != null) label(g, r.x(), r.y(), ls.get(i));
            }
            return;
        }
        int y = menu.spec.inventoryY() < 60 ? 6 : 18; // 1.12 drew them at the top of the small panels
        for (String l : ls) {
            label(g, 8, y, l);
            y += 10;
        }
    }
}
