package mods.eln.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class GuiVerticalTrackBar extends Gui{
	

    public int width,height,xPosition,yPosition;

    public boolean enable = false;
    boolean drag = false;	
    
    public float min = 0f,max = 1.0f;
    int stepId = 0,stepIdMax = 10;
    
    
	public GuiVerticalTrackBar(int xPosition,int yPosition,int width,int height) 
	{
		this.width = width;
		this.height = height;
		this.xPosition = xPosition;
		this.yPosition = yPosition;	
	}
	
	public void setEnable(boolean enable)
	{
		this.enable = enable;
		if(enable == false) drag = false;
	}

    void stepLimit()
    {
		if(stepId < 0) stepId = 0;
		if(stepId > stepIdMax) stepId = stepIdMax;   	
    }
    public void setStepIdMax(int stepIdMax) {
		this.stepIdMax = stepIdMax;
		stepLimit();
	}
	
	public void setValue(float value)
	{
		if(!drag) 
		{
			this.stepId = (int) ((value - min)/(max-min)*stepIdMax + 0.5);
			stepLimit();
		}
	}
	
	public float getValue() {
		return min + (max-min) * stepId/stepIdMax;
	}
	
	public boolean mouseClicked(int x, int y, int which)
    {
    	if(enable && which == 0 && x > xPosition && y > yPosition && x < xPosition + width && y < yPosition + height)
    	{
    		//System.out.println("mouseClicked");
    		drag = enable;
    		return true;
    	}
    	return false;
    }

    /**
     * Called when the mouse is moved or a mouse button is released.  Signature: (mouseX, mouseY, which) which==-1 is
     * mouseMove, which==0 or which==1 is mouseUp
     */
	public boolean mouseMovedOrUp(int x, int y, int which)
    {
		//System.out.println("mouseMovedOrUp "+ x + " " + y + " " + which);
		if(drag && which == 0)
		{
			mouseMove(x, y);

			System.out.println("New Value : " + getValue());
			drag = false;
			return true;
		}
    	return false;
    }

	public void setRange(float min, float max)
	{
		this.min = min;
		this.max = max;
		stepLimit();
	}
	
	public void mouseMove(int x,int y)
	{
		if(drag)
		{
			stepId = (int) ((1.0 - (double)(y - yPosition)/height + 1.0/stepIdMax/2.0) * stepIdMax);
			
			stepLimit();
			/*
			value = max + ((float)(y-yPosition)) / (height) * (min-max);
			
			if(value < min) value = min;
			if(value > max) value = max;*/
		}
	}
	
	public int getCursorPosition()
	{
		return (int) (yPosition + height - 1.0*stepId/stepIdMax * height);
	}
	public int getCursorPositionForValue(float value)
	{
		value-=min;
		int yCalc = (int)( yPosition + height - (value / (max - min)) * height);
		if(yCalc < yPosition) yCalc = yPosition;
		if(yCalc > yPosition + height) yCalc = yPosition + height;
		return yCalc;
	}
	
	
	
	public void draw(float par1, int x, int y)
	{
		//if(! enable) return;
		
		drawRect(xPosition, yPosition,xPosition + width,yPosition + height,0x80808080);
		drawRect(xPosition - 2, getCursorPosition(),xPosition + width + 2,getCursorPosition() + 1,0xFFFFFFFF);
	}
}
