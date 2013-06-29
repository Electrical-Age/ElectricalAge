package mods.eln.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class GuiVerticalTrackBar extends Gui implements IGuiObject{
	

    public int width,height,xPosition,yPosition;

    public boolean enable = false,visible = true;
    boolean drag = false;	
    
    public float min = 0f,max = 1.0f;
    int stepId = 0,stepIdMax = 10;
    
    
    public void setVisible(boolean visible)
    {
    	this.visible = visible;
    	if(visible == false) drag = false;
    }
    
    IGuiObjectObserver observer;
    void setObserver(IGuiObjectObserver observer)
    {
    	this.observer = observer;
    }
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
		if(enable == false || visible == false) drag = false;
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
    	if(enable  && visible && which == 0 && x > xPosition && y > yPosition && x < xPosition + width && y < yPosition + height)
    	{
    		//System.out.println("mouseClicked");
    		drag = true;
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
		if(enable && visible && drag && which == 0)
		{
			mouseMove(x, y);
			if(observer != null) observer.guiObjectEvent(this);
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
		}
	}
		
	public void imouseMove(int x,int y)
	{
		mouseMove(x, y);
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
		if(! visible) return;
		
		drawRect(xPosition, yPosition,xPosition + width,yPosition + height,0x80808080);
		drawRect(xPosition - 2, getCursorPosition(),xPosition + width + 2,getCursorPosition() + 1,0xFFFFFFFF);
	}

	@Override
	public void idraw(int x, int y, float f) {
		// TODO Auto-generated method stub
		draw(f, x, y);
	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		if(mouseClicked(x, y, code))
		{
			
		}
	}
	@Override
	public void imouseMovedOrUp(int x, int y, int witch) {
		// TODO Auto-generated method stub
		mouseMovedOrUp(x, y, witch);
	}
}
