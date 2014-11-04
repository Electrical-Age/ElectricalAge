package mods.eln.sixnode.electricasensor;

import java.text.NumberFormat;
import java.text.ParseException;

import mods.eln.Translator;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class ElectricalSensorGui extends GuiContainerEln{

	public ElectricalSensorGui(EntityPlayer player, IInventory inventory,ElectricalSensorRender render) {
		super(new ElectricalSensorContainer(player, inventory,render.descriptor));
		this.render = render;
	}


	GuiButton validate,voltageType,currentType,powerType,dirType;
	GuiTextFieldEln lowValue,highValue;
	ElectricalSensorRender render;
	

	@Override
	public void initGui() {
		
		super.initGui();

 		
		if(render.descriptor.voltageOnly == false)
		{
			voltageType = newGuiButton(8, 8,50, Translator.translate("eln.core.voltage"));
			currentType = newGuiButton(8, 8+24,50, Translator.translate("eln.core.current"));
			powerType = newGuiButton(8, 8+48,50, Translator.translate("eln.core.power"));
			dirType = newGuiButton(8+50+4, 8+48, 50, "");

			int x = 0,y = -12;
			validate = newGuiButton(x+8 + 50 + 4 + 50 + 4,y+ (166-84)/2 - 9,50, Translator.translate("eln.core.validate"));
			
			lowValue = newGuiTextField(x+8 + 50 + 4,y+ (166-84)/2+3, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{Translator.translate("eln.core.tile.probe.probedvalue"),Translator.translate("eln.core.tile.probe.hint0percent")});
	        
	        highValue = newGuiTextField(x+8 + 50 + 4,y+ (166-84)/2 -13, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{Translator.translate("eln.core.tile.probe.probedvalue"),Translator.translate("eln.core.tile.probe.hint100percent")});
		}
		else
		{
			validate = newGuiButton(8+50 + 4, 10,50, Translator.translate("eln.core.validate"));
			
			lowValue = newGuiTextField(8, 6+16, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{Translator.translate("eln.core.tile.probe.probedvoltage"),Translator.translate("eln.core.tile.probe.hint0percent")});
	        
	        highValue = newGuiTextField(8,6, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{Translator.translate("eln.core.tile.probe.probedvoltage"),Translator.translate("eln.core.tile.probe.hint100percent")});
		}
	}
	




    @Override
    public void guiObjectEvent(IGuiObject object) {
    	
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
		if(render.descriptor.voltageOnly == false)	
			return new HelperStdContainer(this);
		else
			return new GuiHelperContainer(this, 176, 166-45,8,84-45);
	}
	
}
