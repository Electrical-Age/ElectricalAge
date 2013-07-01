package mods.eln.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;

public class GuiHelper {
	public GuiScreen screen;
	public int xSize,ySize;
	String backgroundName;
	public GuiHelper(	
			GuiScreen screen,
			int xSize,int ySize,
			String backgroundName
	) 
	{
		this.screen = screen;
		this.xSize = xSize;
		this.ySize = ySize;
		this.backgroundName = backgroundName;
	}
	
	
	GuiTextFieldEln newGuiTextField(int x,int y,int width)
	{
		GuiTextFieldEln o;
		o = new GuiTextFieldEln(
				Minecraft.getMinecraft().fontRenderer,
				screen.width/2 -xSize/2 + x , screen.height/2 -ySize/2 + y,
				width, 12
				);
		objectList.add(o);
		return o;
	}

	GuiButtonEln newGuiButton(int x,int y,int width,String name)
	{
		GuiButtonEln o;
		o = new GuiButtonEln(
				screen.width/2 -xSize/2 + x , screen.height/2 -ySize/2 + y,
				width, 20,
				name
				);
		objectList.add(o);
		return o;
	}

	GuiVerticalTrackBar newGuiVerticalTrackBar(int x,int y,int width,int height)
	{
		GuiVerticalTrackBar o;
		o = new GuiVerticalTrackBar(
				screen.width/2 -xSize/2 + x , screen.height/2 -ySize/2 + y,
				width, height
				);
		objectList.add(o);
		return o;
	}
	GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x,int y,int width,int height)
	{
		GuiVerticalTrackBarHeat o;
		o = new GuiVerticalTrackBarHeat(
				screen.width/2 -xSize/2 + x , screen.height/2 -ySize/2 + y,
				width, height
				);
		objectList.add(o);
		return o;
	}
	
	
	
	void add(IGuiObject o)
	{
		objectList.add(o);
	}
	ArrayList<IGuiObject> objectList = new ArrayList<IGuiObject>();
	void draw(int x, int y, float f)
	{
		Utils.drawGuiBackground(backgroundName, screen, xSize, ySize);
		for(IGuiObject o : objectList)
		{
			o.idraw(x, y, f);
		}
	}
    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height)
    {
		x += (screen.width - xSize) / 2;
		y += (screen.height - ySize) / 2;
		screen.drawTexturedModalRect(x, y, u, v, width, height);
    }
	protected void keyTyped(char key, int code)
    {
		for(IGuiObject o : objectList)
		{
			o.ikeyTyped(key, code);
		}
    }
    protected void mouseClicked(int x, int y, int code)
    {
		for(IGuiObject o : objectList)
		{
			o.imouseClicked(x, y, code);
		}
    }
    
    protected void mouseMove(int x,int y)
    {
		for(IGuiObject o : objectList)
		{
			o.imouseMove(x, y);
		}    	
    }
    protected void mouseMovedOrUp(int x, int y, int witch)
    {
		for(IGuiObject o : objectList)
		{
			o.imouseMovedOrUp(x, y, witch);
		}     	
    }

	public void drawString(int x, int y, int color, String str) {
		// TODO Auto-generated method stub
		Minecraft.getMinecraft().fontRenderer.drawString(str, screen.width/2 -xSize/2 + x , screen.height/2 -ySize/2 + y, color);
	}
}
