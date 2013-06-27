package mods.eln.thermalsensor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.electricasensor.ElectricalSensorElement;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ThermalSensorGui extends GuiContainer{

	public ThermalSensorGui(EntityPlayer player, IInventory inventory,ThermalSensorRender render) {
		super(new ThermalSensorContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,temperatureType,powerType;
	GuiTextField lowValue,highValue;
	ThermalSensorRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        temperatureType = new GuiButton(1, 100, 50 + 20, "temperatureType");
		validate = new GuiButton(1, 100, 50, "validate");
		powerType = new GuiButton(1, 100, 50 + 40, "powerType");
		buttonList.add(powerType);
		buttonList.add(temperatureType);
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
				lowVoltage = (float) (Float.parseFloat (lowValue.getText()));
				highVoltage = (float) (Float.parseFloat (highValue.getText()));
				render.clientSetFloat(ElectricalSensorElement.setValueId, lowVoltage - (float)PhysicalConstant.Tamb,highVoltage - (float)PhysicalConstant.Tamb);
			} catch(NumberFormatException e)
			{

			}
    	}
    	else if(par1GuiButton == temperatureType)
    	{
    		render.clientSetByte(ThermalSensorElement.setTypeOfSensorId, ThermalSensorElement.temperatureType);
    	}
    	else if(par1GuiButton == powerType)
    	{
    		render.clientSetByte(ThermalSensorElement.setTypeOfSensorId, ThermalSensorElement.powerType);
    	}
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
    	if(render.typeOfSensor == ThermalSensorElement.temperatureType)
    	{
    		powerType.enabled = true;
        	temperatureType.enabled = false;
    	}
    	else if(render.typeOfSensor == ThermalSensorElement.powerType)
    	{
        	powerType.enabled = false;
        	temperatureType.enabled = true;
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
