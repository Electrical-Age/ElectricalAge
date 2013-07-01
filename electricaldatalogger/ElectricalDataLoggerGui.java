package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
		printBt.drawButton = false;
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

        voltageType = newGuiButton( 100, 10,100,  "voltageType");
        currentType = newGuiButton( 100, 30,100,  "currentType");
		resetBt = newGuiButton( 100, 50,100, "reset");
		powerType = newGuiButton( 100, 50,100,  "powerType");
		celsuisTyp = newGuiButton( 100, 70,100,  "celsuisType");
		percentTyp = newGuiButton( 100, 90,100,  "percentType");
		config = newGuiButton( 100, 120,100, "");
		printBt = newGuiButton( 100, 10,100, "Print");
		pause = newGuiButton( 100, 30,100,"");
		
		
		samplingPeriode = newGuiTextField( 120, 140+20, 103);
        samplingPeriode.setText(  render.log.samplingPeriod);
        
        maxValue = newGuiTextField( 120, 140, 103);
        maxValue.setText(render.log.maxValue);
        
        minValue = newGuiTextField( 120, 120, 103);
        minValue.setText(render.log.minValue);
        

        displayEntry();
	}
	

    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        this.samplingPeriode.mouseClicked(par1, par2, par3);
        this.maxValue.mouseClicked(par1, par2, par3);
        this.minValue.mouseClicked(par1, par2, par3);
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
				render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, Float.parseFloat(maxValue.getText()));
			}
			else if(object == minValue)
			{
				render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, Float.parseFloat(minValue.getText()));
			}
			else if(object == samplingPeriode)
			{
				render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, Float.parseFloat(samplingPeriode.getText()));
			}
		} catch(NumberFormatException e)
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
    	
		GL11.glColor4f(1f, 0f, 0f, 1f);
		
        GL11.glPushMatrix();
	        GL11.glTranslatef(50, 100, 0);
	        GL11.glScalef(50, 50, 1f);
	        render.log.draw(2.8f,1f);
        GL11.glPopMatrix();
    }

	

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 230,8,148, "electricaldatalogger.png");
	}
	
}
