package mods.eln.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class GuiVerticalProgressBar  extends Gui implements IGuiObject {
	boolean visible;
	public int width,height,xPosition,yPosition;
	float value;
	public void setValue(float newValue)
	{
		this.value = newValue;
		if(value < 0f)value = 0f;
		if(value > 1f)value = 1f;
	}
	public void setColor(float r,float g,float b)
	{
		color = (0xFF<<24) + (((int)(r*255))<<16)+ (((int)(g*255))<<8)+ (((int)(b*255))<<0);
	}
	@Override
	public int getYMax() {
		
		return yPosition + height;
	}
	
	int color;
	public GuiVerticalProgressBar(int xPosition,int yPosition,int width,int height,GuiHelper helper) 
	{
		this.width = width;
		this.height = height;
		this.xPosition = xPosition;
		this.yPosition = yPosition;	
		visible = true;
		setColor(1,1,1);
		this.helper = helper;	
	}
	
	ArrayList<String> comment = new ArrayList<String>();
	GuiHelper helper;
	public void setComment(String[] comment)
	{
		for(String str : comment)
		{
			this.comment.add(str);
		}
	}
	@Override
	public void idraw(int x, int y, float f) {
		
		if(! visible) return;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
       // this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + 1 * 20, this.width / 2, this.height);
       // this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + 1 * 20, this.width / 2, this.height);

        //this.drawTexturedModalRect(this.xPosition + (int)(0.2 * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
       // this.drawTexturedModalRect(this.xPosition + (int)(0.2 * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);

		drawRect(xPosition, yPosition,xPosition + width,yPosition + height,0xFF404040);
		drawRect(xPosition+1, yPosition+1,xPosition + width-1,yPosition + height-1,0xFF606060);
		drawRect(xPosition+2, yPosition+2,xPosition + width-2,yPosition + height-2,0xFF808080);

		int yPos = (int) (-value * (height-4) + yPosition + height - 2);
		drawRect(xPosition +2, yPos,xPosition + width - 2, yPosition + height-2,color);

	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		
		return false;
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		
		
	}

	@Override
	public void imouseMove(int x, int y) {
		
		
	}

	@Override
	public void imouseMovedOrUp(int x, int y, int witch) {
		
		
	}
	@Override
	public void idraw2(int x, int y) {
		
		if(visible == true && x >= xPosition && y >= yPosition && x < xPosition + width && y < yPosition + height)
			helper.drawHoveringText(comment, x, y, Minecraft.getMinecraft().fontRenderer);
				
	}
	public void setComment(int line,String comment)
	{
		if(this.comment.size() < line + 1)
			this.comment.add(line, comment);
		else
			this.comment.set(line, comment);
	}
	public double getValue() {
		
		return value;
	}
	@Override
	public void translate(int x, int y) {
		this.xPosition += x;
		this.yPosition += y;
	}
}
