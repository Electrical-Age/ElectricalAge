package mods.eln.electricasensor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
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

public class ElectricalSensorGui extends GuiContainerEln{

	public ElectricalSensorGui(EntityPlayer player, IInventory inventory,ElectricalSensorRender render) {
		super(new ElectricalSensorContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,voltageType,currentType,powerType;
	GuiTextFieldEln lowValue,highValue;
	ElectricalSensorRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

 		validate = newGuiButton(150, 50,100, "validate");
		if(render.descriptor.voltageOnly == false)
		{
			voltageType = newGuiButton(150, 50 + 20,100, "voltageType");
			currentType = newGuiButton(150, 50 + 40,100, "currentType");
			powerType = newGuiButton(150, 50 + 60,100, "powerType");
		}

		lowValue = newGuiTextField(120, 10, 103);
        lowValue.setText(render.lowValue);
        
        highValue = newGuiTextField(120, 24, 103);
        highValue.setText(render.highValue);
        

	}
	




    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == validate)
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
    	else if(object == currentType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.currantType);
    	}
    	else if(object == voltageType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.voltageType);
    	}
    	else if(object == powerType)
    	{
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.powerType);
    	}
    }
   
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
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

    }



	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 176, 166, "electricalsensor.png");
	}
	
}
