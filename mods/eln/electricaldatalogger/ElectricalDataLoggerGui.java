package mods.eln.electricaldatalogger;

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
import mods.eln.gui.IGuiObject;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
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

public class ElectricalDataLoggerGui extends GuiContainerEln implements GuiTextFieldElnObserver{

	public ElectricalDataLoggerGui(EntityPlayer player, IInventory inventory,ElectricalDataLoggerRender render) {
		super(new ElectricalDataLoggerContainer(player, inventory));
		this.render = render;
	}


	GuiButton resetBt,voltageType,currentType,powerType,celsuisTyp,percentTyp,config,printBt,pause;
	GuiTextFieldEln samplingPeriode,maxValue,minValue;
	ElectricalDataLoggerRender render;
	

	enum State {display,config};
	State state = State.display;
	
	
	void displayEntry()
	{
		config.displayString = "config";
		config.drawButton = true;
		pause.drawButton = true;
		resetBt.drawButton = true;
		voltageType.drawButton = false;
		percentTyp.drawButton = false;
		currentType.drawButton = false;
		powerType.drawButton = false;
		celsuisTyp.drawButton = false;
		samplingPeriode.setVisible(false);
		maxValue.setVisible(false);
		minValue.setVisible(false);
		printBt.drawButton = true;
		state = State.display;
	}
	
	void configEntry()
	{	


		pause.drawButton = false;
		config.drawButton = true;
		config.displayString = "return to display";
		resetBt.drawButton = false;
		printBt.drawButton = true;
		voltageType.drawButton = true;
		percentTyp.drawButton = true;
		currentType.drawButton = true;
		powerType.drawButton = true;
		celsuisTyp.drawButton = true;
		samplingPeriode.setVisible(true);	
		maxValue.setVisible(true);
		minValue.setVisible(true);
		state = State.config;
	}
	
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        voltageType = newGuiButton( 176/2-60-2, 8+20+2-2,60,  "Voltage");
        currentType = newGuiButton( 176/2+2, 8+20+2-2,60,  "Current");
		resetBt = newGuiButton( 176/2-50, 8 + 20 + 2-2,48, "Reset");
		powerType = newGuiButton( 176/2-60-2, 8+40+4-2,60,  "Power");
		celsuisTyp = newGuiButton( 176/2+2, 8+40+4-2,60,  "Celsuis");
		percentTyp = newGuiButton( 176/2-30, 8+60+6-2,60,  "Percent");
		config = newGuiButton( 176/2-50, 8-2,100, "");
		printBt = newGuiButton( 176/2-48/2, 123,48, "Print");
		pause = newGuiButton( 176/2 + 2, 8+20+2-2,48,"");
		
		
		samplingPeriode = newGuiTextField( 30, 101, 50);
        samplingPeriode.setText(  render.log.samplingPeriod);
        samplingPeriode.setComment(new String[]{"Sampling period"});
        
        maxValue = newGuiTextField( 176 - 50 - 30, 101-7, 50);
        maxValue.setText(render.log.maxValue);
        maxValue.setComment(new String[]{"Y axis max"});
        
        minValue = newGuiTextField( 176 - 50 - 30, 101+8, 50);
        minValue.setText(render.log.minValue);
        minValue.setComment(new String[]{"Y axe min"});
       

        displayEntry();
	}
	


    @Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);

		try{
	    	if(object == resetBt)
	    	{
	    		render.clientSend(ElectricalDataLoggerElement.resetId);
	    	}
	    	else if(object == pause)
	    	{
	    		render.clientSend(ElectricalDataLoggerElement.tooglePauseId);
	    	}
	    	else if(object == printBt)
	    	{
	    		render.clientSend(ElectricalDataLoggerElement.printId);
	    	}
	    	else if(object == currentType)
	    	{
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.currentType);
	    	}
	    	else if(object == voltageType)
	    	{
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.voltageType);
	    	}
	    	else if(object == percentTyp)
	    	{
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.percentType);
	    	}
	    	else if(object == powerType)
	    	{
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.powerType);
	    	}
	    	else if(object == celsuisTyp)
	    	{
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.celsiusType);
	    	}
	    	else if(object == config)
	    	{ 
	    		switch(state)
	    		{
				case config:
					displayEntry();
					break;
				case display:
					configEntry();
					break;
				default:
					break;
	    		
	    		}
	    		
	    	}
	    	else if(object == maxValue)
			{
				render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, NumberFormat.getInstance().parse(maxValue.getText()).floatValue());
			}
			else if(object == minValue)
			{
				render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, NumberFormat.getInstance().parse(minValue.getText()).floatValue());
			}
			else if(object == samplingPeriode)
			{
				float value = NumberFormat.getInstance().parse(samplingPeriode.getText()).floatValue();
				if(value < 0.05f) value = 0.05f;
				samplingPeriode.setText(value);
				
				render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, value);
			}
		} catch(ParseException e)
		{

		}
		
    }
   
    
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	powerType.enabled = true;
    	currentType.enabled = true;
    	voltageType.enabled = true;
    	celsuisTyp.enabled = true;
    	percentTyp.enabled = true;
    	
    	switch(render.log.unitType)
    	{
    	case DataLogs.currentType:
    		currentType.enabled = false;
    		break;
    	case DataLogs.voltageType:
    		voltageType.enabled = false;
    		break;
    	case DataLogs.powerType:
    		powerType.enabled = false;
    		break;
    	case DataLogs.celsiusType:
    		celsuisTyp.enabled = false;
    		break;		
    	case DataLogs.percentType:
    		percentTyp.enabled = false;
    		break;		
    	}
    	if(render.pause)
    		pause.displayString = "Continue";
    	else
    		pause.displayString = "Pause";
    	
    	boolean a = inventorySlots.getSlot(ElectricalDataLoggerContainer.paperSlotId).getStack() != null;
    	boolean b = inventorySlots.getSlot(ElectricalDataLoggerContainer.printSlotId).getStack() == null;
    	printBt.enabled = a && b;

    }
    
    
    @Override
    protected void postDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.postDraw(f, x, y);
    	
    	if(state == State.display)
    	{
			GL11.glColor4f(1f, 0f, 0f, 1f);
			
	        GL11.glPushMatrix();
		        GL11.glTranslatef(guiLeft + 8, guiTop + 60, 0);
		        GL11.glScalef(50, 50, 1f);
		        render.log.draw(2.9f,1.2f,"");
	        GL11.glPopMatrix();
    	}
    }

	

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 230,8,148);
	}
	
}
