package mods.eln.gui;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
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
	
	public GuiHelperContainer(
			GuiScreen screen, 
			int xSize, int ySize,
			int xInv,int yInv) {
		super(screen, xSize, ySize);
		this.xInv = xInv;
		this.yInv = yInv;
	}
	public int xInv, yInv;

	@Override
	void draw(int x, int y, float f) {
		// TODO Auto-generated method stub
		super.draw(x, y, f);
	//	screen.drawTexturedModalRect(xInv, yInv, par3, par4, par5, par6)
	}
}
