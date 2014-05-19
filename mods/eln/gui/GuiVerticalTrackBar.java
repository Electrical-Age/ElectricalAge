package mods.eln.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
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
	@Override
	public int getYMax() {
		// TODO Auto-generated method stub
		return yPosition + height;
	}
	
    IGuiObjectObserver observer;
    public void setObserver(IGuiObjectObserver observer)
    {
    	this.observer = observer;
    }
	public GuiVerticalTrackBar(int xPosition,int yPosition,int width,int height,GuiHelper helper) 
	{
		this.width = width;
		this.height = height;
		this.xPosition = xPosition;
		this.yPosition = yPosition;	
		enable = true;
		this.helper = helper;	
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
    public void setStepId(int stepId) {
		this.stepId = stepId;
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
    		//Utils.println("mouseClicked");
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
		//Utils.println("mouseMovedOrUp "+ x + " " + y + " " + which);
		if(enable && visible && drag && which == 0)
		{
			mouseMove(x, y);
			if(observer != null) observer.guiObjectEvent(this);
			Utils.println("New Value : " + getValue());
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
	
	
	
	public void drawBase(float par1, int x, int y)
	{
		if(! visible) return;

		drawRect(xPosition, yPosition-2,xPosition + width,yPosition + height+2,0xFF404040);
		drawRect(xPosition+1, yPosition-1,xPosition + width-1,yPosition + height+1,0xFF606060);
		drawRect(xPosition+2, yPosition,xPosition + width-2,yPosition + height,0xFF808080);

	}
	public void drawBare(float par1, int x, int y)
	{
		if(! visible) return;

 
		drawRect(xPosition - 2, getCursorPosition()-2,xPosition + width + 2,getCursorPosition() + 2,0xFF202020);
		drawRect(xPosition - 1, getCursorPosition()-1,xPosition + width + 1,getCursorPosition() + 1,0xFF606060);
		
	}
	
	
	ArrayList<String> comment = new ArrayList<String>();
	GuiHelper helper;
	public void setComment(String[] comment)
	{
		this.comment.clear();
		for(String str : comment)
		{
			this.comment.add(str);
		}
	}
	
	public void setComment(int line,String comment)
	{
		if(this.comment.size() < line + 1)
			this.comment.add(line, comment);
		else
			this.comment.set(line, comment);
	}
	@Override
	public void idraw(int x, int y, float f) {
		// TODO Auto-generated method stub
		drawBase(f, x, y);
		drawBare(f, x, y);
		
		
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
	@Override
	public void idraw2(int x, int y) {
		// TODO Auto-generated method stub
		if(visible == true && (x >= xPosition && y >= yPosition && x < xPosition + width && y < yPosition + height) || drag)
		{
			int px,py;
			px = xPosition - helper.getHoveringTextWidth(comment,Minecraft.getMinecraft().fontRenderer)/2;
			py = yPosition + height + 20/* - helper.getHoveringTextHeight(comment,Minecraft.getMinecraft().fontRenderer)*/;
			helper.drawHoveringText(comment, px, py, Minecraft.getMinecraft().fontRenderer);
		}
				
	}
	
	@Override
	public void translate(int x, int y) {
		this.xPosition += x;
		this.yPosition += y;
	}
	public float getStepId() {
		// TODO Auto-generated method stub
		return stepId;
	}
}
