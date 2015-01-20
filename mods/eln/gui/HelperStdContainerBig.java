package mods.eln.gui;

import net.minecraft.client.gui.GuiScreen;

public class HelperStdContainerBig extends GuiHelperContainer {

	public HelperStdContainerBig(GuiScreen screen) {
		super(screen, 176, 214, 8, 84 + 214 - 166, "stdcontainerbig.png");
	}

	public void drawProcess(int x, int y, float value) {
		drawTexturedModalRect(x, y, 177, 31, (int) (22), 15);
		drawTexturedModalRect(x, y, 177, 14, (int) (22 * value), 15);
	}
}
