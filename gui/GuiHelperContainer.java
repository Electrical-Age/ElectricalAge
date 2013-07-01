package mods.eln.gui;

import net.minecraft.client.gui.GuiScreen;

public class GuiHelperContainer extends GuiHelper{

	public GuiHelperContainer(
			GuiScreen screen, 
			int xSize, int ySize,
			int xInv,int yInv,
			String backgroundName) {
		super(screen, xSize, ySize, backgroundName);
		this.xInv = xInv;
		this.yInv = yInv;
	}
	public int xInv, yInv;
}
