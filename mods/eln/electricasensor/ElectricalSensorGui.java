package mods.eln.electricasensor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
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

public class ElectricalSensorGui extends GuiContainerEln{

	public ElectricalSensorGui(EntityPlayer player, IInventory inventory,ElectricalSensorRender render) {
		super(new ElectricalSensorContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,voltageType,currentType,powerType,dirType;
	GuiTextFieldEln lowValue,highValue;
	ElectricalSensorRender render;
	

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

 		
		if(render.descriptor.voltageOnly == false)
		{
			voltageType = newGuiButton(8, 8,50, "Voltage");
			currentType = newGuiButton(8, 8+24,50, "Current");
			powerType = newGuiButton(8, 8+48,50, "Power");
			dirType = newGuiButton(8+50+4, 8+48, 50, "");

			int x = 0,y = -12;
			validate = newGuiButton(x+8 + 50 + 4 + 50 + 4,y+ (166-84)/2 - 9,50, "Validate");
			
			lowValue = newGuiTextField(x+8 + 50 + 4,y+ (166-84)/2+3, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{"Minimum input"});
	        
	        highValue = newGuiTextField(x+8 + 50 + 4,y+ (166-84)/2 -13, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{"Maximum input"});
		}
		else
		{
			validate = newGuiButton(8 + 50 + 4 + 50 + 4 -26, (166-84)/2 - 8,50, "Validate");
			
			lowValue = newGuiTextField(8 + 50 + 4-26, (166-84)/2+3, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{"Minimum input"});
	        
	        highValue = newGuiTextField(8 + 50 + 4-26, (166-84)/2 -12, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{"Maximum input"});
		}
	}
	




    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == validate)
    	{
			float lowVoltage,highVoltage;
			
			try{
				lowVoltage = NumberFormat.getInstance().parse(lowValue.getText()).floatValue();
				highVoltage = NumberFormat.getInstance().parse(highValue.getText()).floatValue();
				render.clientSetFloat(ElectricalSensorElement.setValueId, lowVoltage,highVoltage);
			} catch(ParseException e)
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
    	else if(object == dirType)
    	{
    		render.dirType = (byte) ((render.dirType + 1)%3);
    		render.clientSetByte(ElectricalSensorElement.setDirType, render.dirType);
    	}
    }
   
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	if(render.descriptor.voltageOnly == false)
    	{
    		switch(render.dirType)
    		{
    		case ElectricalSensorElement.dirNone:
    			dirType.displayString = "\u00a72\u25CF\u00a77 <=> \u00a71\u25CF";
    			break;
    		case ElectricalSensorElement.dirAB:
    			dirType.displayString = "\u00a72\u25CF\u00a77 => \u00a71\u25CF";
    			break;
    		case ElectricalSensorElement.dirBA:
    			dirType.displayString = "\u00a72\u25CF\u00a77 <= \u00a71\u25CF";
    			break;
    		}
    		
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
    }



	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}
	
}
