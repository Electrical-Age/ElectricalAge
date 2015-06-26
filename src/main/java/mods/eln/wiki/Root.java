package mods.eln.wiki;

import mods.eln.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class Root extends Default {

    public Root(GuiScreen preview) {
        super(preview);

    }


    @Override
    public void initGui() {

        super.initGui();

        int y = 8;
        for (Entry<String, ArrayList<ItemStack>> groupe : Data.groupes.entrySet()) {
            y = addStackGroupe(groupe.getValue(), groupe.getKey(), y);
        }

    }

    static final int stackPerLine = 10;

    int addStackGroupe(List<ItemStack> list, String name, int y) {
        int idx = 0;
        extender.add(new GuiLabel(8, y, name));
        y += 10;
        for (ItemStack stack : list) {
            GuiItemStack gui = new GuiItemStack((idx % stackPerLine) * 18 + 8, y + (idx / stackPerLine) * 18, stack, helper);
            extender.add(gui);
            idx++;
        }
        y += ((idx - 1) / stackPerLine + 1) * 18 + 6;
        return y;
    }

}
