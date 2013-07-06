package mods.eln.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.XRandR.Screen;

import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiTextFieldEln extends GuiTextField implements IGuiObject{
	int xPos,yPos,width,height;
	public GuiTextFieldEln(FontRenderer par1FontRenderer, int par2, int par3,
			int par4, int par5,GuiHelper helper) {
		super(par1FontRenderer, par2, par3, par4, par5);
		setTextColor(-1);
        setDisabledTextColour(-1);
        setEnableBackgroundDrawing(true);
        setMaxStringLength(30);
        xPos = par2;
        yPos = par3;
        width = par4;
        height = par5;
        this.helper = helper;
	}
	GuiTextFieldElnObserver observer;
	
	public void setObserver(GuiTextFieldElnObserver observer)
	{
		this.observer = observer;
	}
	
	public interface GuiTextFieldElnObserver{
		void textFieldNewValue(GuiTextFieldEln textField,String value);
		
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
	public void setText(float value)
	{
		setText( String.format("%3.2f", value));
	}
	
	private boolean enabled = true;
	
	@Override
	public void setEnabled(boolean par1) {
		enabled = par1;
		super.setEnabled(par1);
	}
	
	public boolean getEnabled()
	{
		return enabled;
	}

    public boolean textboxKeyTyped(char par1, int par2)
    {
        if (getEnabled() && this.isFocused())
        {
        	if(par1 == '\r')
        	{
        		setFocused(false);
        		return true;
        	}
        	
        }
        return super.textboxKeyTyped(par1, par2);
    }
    
    
    @Override
    public void setFocused(boolean par1) {
    	if(isFocused() == true && par1 == false && observer != null) observer.textFieldNewValue(this,this.getText());
    	super.setFocused(par1);
    }

	@Override
	public void idraw(int x, int y, float f) {
		this.drawTextBox();

	}

	@Override
	public boolean ikeyTyped(char key, int code) {
		return this.textboxKeyTyped(key, code);
	}

	@Override
	public void imouseClicked(int x, int y, int code) {
		this.mouseClicked(x, y, code);
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
		if(isFocused() == false && getVisible() == true && x >= xPos && y >= yPos && x < xPos + width && y < yPos + height)
			helper.drawHoveringText(comment, x, y, Minecraft.getMinecraft().fontRenderer);
				
	}
	

}
