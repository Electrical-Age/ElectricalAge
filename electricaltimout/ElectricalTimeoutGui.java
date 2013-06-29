package mods.eln.electricaltimout;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
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

public class ElectricalTimeoutGui extends GuiScreenEln{

	public ElectricalTimeoutGui(EntityPlayer player,ElectricalTimeoutRender render) {
		this.render = render;
	}


	GuiButton set,reset;
	GuiTextFieldEln timeoutValue;
	ElectricalTimeoutRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        reset = newGuiButton( 100, 50 + 20,50, "reset");
		set = newGuiButton(100, 50,50, "set");

		timeoutValue = newGuiTextField(120, 140+20, 103);

        timeoutValue.setText( render.timeoutValue);
        

	}

    
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == set)
    	{
    		render.clientSend(ElectricalTimeoutElement.setId);
    	}
    	else if(object == reset)
    	{
    		render.clientSend(ElectricalTimeoutElement.resetId);
    	}
    	else if(object == timeoutValue)
    	{
    		try{
    			float value = Float.parseFloat (timeoutValue.getText());
    			render.clientSetFloat(ElectricalTimeoutElement.setTimeOutValueId,value);
    		} catch(NumberFormatException e)
    		{

    		}	
    	}
    }
   

	



	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 176, 166, "electricaltimeout.png");
	}
	
}
