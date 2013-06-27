package mods.eln.electricaltimout;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalTimeoutGui extends GuiContainer{

	public ElectricalTimeoutGui(EntityPlayer player, IInventory inventory,ElectricalTimeoutRender render) {
		super(new ElectricalTimeoutContainer(player, inventory));
		this.render = render;
	}


	GuiButton set,reset;
	GuiTextField timeoutValue;
	ElectricalTimeoutRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        reset = new GuiButton(1, 100, 50 + 20, "reset");
		set = new GuiButton(1, 100, 50, "set");

		buttonList.add(reset);	
		buttonList.add(set);	
		
		timeoutValue = new GuiTextField(this.fontRenderer, 120, 140+20, 103, 12);
		this.timeoutValue.setTextColor(-1);
        this.timeoutValue.setDisabledTextColour(-1);
        this.timeoutValue.setEnableBackgroundDrawing(true);
        this.timeoutValue.setMaxStringLength(30);
        timeoutValue.setText( String.format("%3.2f", render.timeoutValue));
        

	}
	
	@Override
	protected void keyTyped(char par1, int par2)
    {
		if (this.timeoutValue.textboxKeyTyped(par1, par2))
        {
			
        }
		else if(par1 == '\r')
		{
			timeoutValue.setFocused(false);
			sendTimeoutValue();		
		}
        else
        {
            super.keyTyped(par1, par2);
        }
    }
    protected void mouseClicked(int par1, int par2, int par3)
    {
        boolean focus;
        focus = timeoutValue.isFocused();
        this.timeoutValue.mouseClicked(par1, par2, par3);
        if(focus == true && timeoutValue.isFocused() == false)
        {
        	sendTimeoutValue();
        }
        super.mouseClicked(par1, par2, par3);

    }

    void sendTimeoutValue()
    {
		try{
			float value = Float.parseFloat (timeoutValue.getText());
			render.clientSetFloat(ElectricalTimeoutElement.setTimeOutValueId,value);
		} catch(NumberFormatException e)
		{

		}	   	
    }
    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == set)
    	{
    		render.clientSend(ElectricalTimeoutElement.setId);
    	}
    	else if(par1GuiButton == reset)
    	{
    		render.clientSend(ElectricalTimeoutElement.resetId);
    	}

    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
    	
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        this.timeoutValue.drawTextBox();

               
    }

	
    public boolean doesGuiPauseGame()
    {
        return false;
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		// TODO Auto-generated method stub
		
	}
	
}
