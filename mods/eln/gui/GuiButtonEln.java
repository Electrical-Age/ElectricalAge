package mods.eln.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiButtonEln extends GuiButton implements IGuiObject{


	IGuiObjectObserver observer;
    public GuiButtonEln(int x, int y, int width,int height, String str)
    {
    	super(0, x, y, width, height, str);
    }
    public void setObserver(IGuiObjectObserver observer)
    {
    	this.observer = observer;
    }
	@Override
	public void idraw(int x, int y, float f) {
		// TODO Auto-generated method stub
		drawButton(Minecraft.getMinecraft(), x, y);
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
        if (mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	Minecraft.getMinecraft().sndManager.playSoundFX("random.click", 1.0F, 1.0F);
        	if(observer != null)
        	{
        		observer.guiObjectEvent(this);
        	}
        }
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
	public void idraw2(int x, int y) {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public void translate(int x, int y) {
		this.xPosition += x;
		this.yPosition += y;
	}
}
