package mods.eln.gui;

import net.minecraft.client.gui.GuiScreen;

public class HelperStdContainerSmall extends GuiHelperContainer {

    public HelperStdContainerSmall(GuiScreen screen) {
        super(screen, 176, 140, 8, 84 + 140 - 166);
    }

    public void drawProcess(int x, int y, float value) {
        drawTexturedModalRect(x, y, 177, 31, (int) (22), 15);
        drawTexturedModalRect(x, y, 177, 14, (int) (22 * value), 15);
    }
}
