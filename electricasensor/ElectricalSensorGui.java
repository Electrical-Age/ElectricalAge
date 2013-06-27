package mods.eln.electricasensor;

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

public class ElectricalSensorGui extends GuiContainer{

	public ElectricalSensorGui(EntityPlayer player, IInventory inventory,ElectricalSensorRender render) {
		super(new ElectricalSensorContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,voltageType,currentType,powerType;
	GuiTextField lowValue,highValue;
	ElectricalSensorRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        voltageType = new GuiButton(1, 100, 50 + 20, "voltageType");
        currentType = new GuiButton(1, 100, 50 + 40, "currentType");
		validate = new GuiButton(1, 100, 50, "validate");
		powerType = new GuiButton(1, 100, 50 + 60, "powerType");
		if(render.descriptor.voltageOnly == false)
		{
			buttonList.add(powerType);
			buttonList.add(voltageType);
			buttonList.add(currentType);	
		}
		buttonList.add(validate);	
		
		lowValue = new GuiTextField(this.fontRenderer, 120, 140+20, 103, 12);
		this.lowValue.setTextColor(-1);
        this.lowValue.setDisabledTextColour(-1);
        this.lowValue.setEnableBackgroundDrawing(true);
        this.lowValue.setMaxStringLength(30);
        lowValue.setText( String.format("%3.2f", render.lowValue));
        
        highValue = new GuiTextField(this.fontRenderer, 120, 140, 103, 12);
		this.highValue.setTextColor(-1);
        this.highValue.setDisabledTextColour(-1);
        this.highValue.setEnableBackgroundDrawing(true);
        this.highValue.setMaxStringLength(30);
        highValue.setText( String.format("%3.2f", render.highValue));
        

	}
	
	@Override
	protected void keyTyped(char par1, int par2)
    {
		if (this.lowValue.textboxKeyTyped(par1, par2))
        {

        }
        else if (this.highValue.textboxKeyTyped(par1, par2))
        {

        }
        else
        {
            super.keyTyped(par1, par2);
        }
    }
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        this.lowValue.mouseClicked(par1, par2, par3);
        this.highValue.mouseClicked(par1, par2, par3);
    }


    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == validate)
    	{
			float lowVoltage,highVoltage;
			
			try{
				lowVoltage = Float.parseFloat (lowValue.getText());
				highVoltage = Float.parseFloat (highValue.getText());
				render.clientSetFloat(ElectricalSensorElement.setValueId, lowVoltage,highVoltage);
			} catch(NumberFormatException e)
			{

			}
    	}
    	else if(par1GuiButton == currentType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.currantType);
    	}
    	else if(par1GuiButton == voltageType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.voltageType);
    	}
    	else if(par1GuiButton == powerType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.powerType);
    	}
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
    	if(render.typeOfSensor == ElectricalSensorElement.currantType)
    	{
        	powerType.enabled = true;
        	currentType.enabled = false;
        	voltageType.enabled = true;
    	}
    	else if(render.typeOfSensor == ElectricalSensorElement.voltageType)
    	{
    		powerType.enabled = true;
        	currentType.enabled = true;
        	voltageType.enabled = false;
    	}
    	else if(render.typeOfSensor == ElectricalSensorElement.powerType)
    	{
        	powerType.enabled = false;
        	currentType.enabled = true;
        	voltageType.enabled = true;
    	}
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        this.lowValue.drawTextBox();
        this.highValue.drawTextBox();
               
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
