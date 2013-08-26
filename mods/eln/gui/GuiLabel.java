package mods.eln.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class GuiLabel extends Gui implements IGuiObject{

	private String text;
	int color;
	FontRenderer font;
	int xPos,yPos;
	
	public GuiLabel(int x,int y,String text) {
		this.text = text;
		font = Minecraft.getMinecraft().fontRenderer;
		color = 0x00FFFFFF;
		xPos = x;
		yPos = y;
	}
	
	
	@Override
	public void idraw(int x, int y, float f) {
		drawString(font, text, xPos, yPos, color);
	}

	@Override
	public void idraw2(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imouseMove(int x, int y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imouseMovedOrUp(int x, int y, int witch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void translate(int x, int y) {
		xPos += x;
		yPos += y;
	}

	@Override
	public int getYMax() {
		// TODO Auto-generated method stub
		return yPos + 10;
	}

}
