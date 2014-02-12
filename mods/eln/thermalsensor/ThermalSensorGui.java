package mods.eln.thermalsensor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import org.lwjgl.opengl.GL11;

import mods.eln.electricasensor.ElectricalSensorElement;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
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


		if(render.descriptor.temperatureOnly == false)
		{

			
			powerType = newGuiButton( 8, 8,70, "Power");
	        temperatureType = newGuiButton(176-8-70,8,70, "Temperature");

			int x = -15,y = 13;
			validate = newGuiButton(x+8 + 50 + 4 + 50 + 4 -26,y+ (166-84)/2 - 8,50, "validate");
			
			lowValue = newGuiTextField(x+8 + 50 + 4-26, y+(166-84)/2+3, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{"Input value for low level output"});
	        
	        highValue = newGuiTextField(x+8 + 50 + 4-26,y+ (166-84)/2 -12, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{"Input value for high level output"});
		}
		else
		{
			int x = 0,y = 0;
			validate = newGuiButton(x+8 + 50 + 4 + 50 + 4 -26,y+ (166-84)/2 - 8,50, "validate");
			
			lowValue = newGuiTextField(x+8 + 50 + 4-26, y+(166-84)/2+3, 50);
	        lowValue.setText(render.lowValue);
	        lowValue.setComment(new String[]{"Input value for low level output"});
	        
	        highValue = newGuiTextField(x+8 + 50 + 4-26,y+ (166-84)/2 -12, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(new String[]{"Input value for high level output"});
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
				render.clientSetFloat(ElectricalSensorElement.setValueId, lowVoltage - (float)PhysicalConstant.Tamb,highVoltage - (float)PhysicalConstant.Tamb);
			} catch(ParseException e)
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
		if(render.descriptor.temperatureOnly == false){
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
	}


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}
  

	
   
	
}
