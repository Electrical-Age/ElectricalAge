package mods.eln.thermalsensor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.electricasensor.ElectricalSensorElement;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
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

public class ThermalSensorGui extends GuiContainerEln{

	public ThermalSensorGui(EntityPlayer player, IInventory inventory,ThermalSensorRender render) {
		super(new ThermalSensorContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,temperatureType,powerType;
	GuiTextFieldEln lowValue,highValue;
	ThermalSensorRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        temperatureType = newGuiButton(100, 50 + 20,50, "temperatureType");
		validate = newGuiButton(100, 50,50, "validate");
		powerType = newGuiButton( 100, 50 + 40,50, "powerType");

		
		lowValue = newGuiTextField(120, 140+20, 103);
        lowValue.setText(render.lowValue);
        
        highValue = newGuiTextField( 120, 140, 103);
        highValue.setText( render.highValue);
        

	}
	

	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
    	if(object == validate)
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
    	else if(object == temperatureType)
    	{
    		render.clientSetByte(ThermalSensorElement.setTypeOfSensorId, ThermalSensorElement.temperatureType);
    	}
    	else if(object == powerType)
    	{
    		render.clientSetByte(ThermalSensorElement.setTypeOfSensorId, ThermalSensorElement.powerType);
    	}
	}

	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);
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

	}


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 166,8,84, "thermalsensor.png");
	}
  

	
   
	
}
