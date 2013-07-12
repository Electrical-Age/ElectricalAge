package mods.eln.gui;

import net.minecraft.client.gui.GuiScreen;

public class HelperStdContainer extends GuiHelperContainer{

	public HelperStdContainer(GuiScreen screen) {
		super(screen, 176, 166,8,84, "stdcontainer.png");
		// TODO Auto-generated constructor stub
	}

	public void drawProcess(int x,int y,float value)
	{
		drawTexturedModalRect(x, y,177,31 , (int) (22), 15);
		drawTexturedModalRect(x, y,177,14 , (int) (22*value), 15);
	}
}

