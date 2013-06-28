package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiTextFieldEln;
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

public class ElectricalDataLoggerGui extends GuiContainer implements GuiTextFieldElnObserver{

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

        voltageType = new GuiButton(1, 100, 10,100, 20, "voltageType");
        currentType = new GuiButton(1, 100, 30,100, 20, "currentType");
		resetBt = new GuiButton(1, 100, 50,100, 20, "reset");
		powerType = new GuiButton(1, 100, 50,100, 20, "powerType");
		celsuisTyp = new GuiButton(1, 100, 70,100, 20, "celsuisType");
		percentTyp = new GuiButton(1, 100, 90,100, 20, "percentType");
		config = new GuiButton(1, 100, 120,100, 20, "");
		printBt = new GuiButton(1, 100, 10,100, 20, "Print");
		pause = new GuiButton(1, 100, 30,100, 20, "");
		
		buttonList.add(powerType);
		buttonList.add(voltageType);
		buttonList.add(currentType);	
		buttonList.add(celsuisTyp);
		buttonList.add(config);
		buttonList.add(printBt);
		buttonList.add(pause);
		buttonList.add(percentTyp);
		
		buttonList.add(resetBt);	
		
		samplingPeriode = new GuiTextFieldEln(this.fontRenderer, 120, 140+20, 103, 12);
		samplingPeriode.setObserver(this);
        samplingPeriode.setText( String.format("%3.2f", render.log.samplingPeriod));
        
        maxValue = new GuiTextFieldEln(this.fontRenderer, 120, 140, 103, 12);
		maxValue.setObserver(this);
        maxValue.setText( String.format("%3.2f", render.log.maxValue));
        
        minValue = new GuiTextFieldEln(this.fontRenderer, 120, 120, 103, 12);
		minValue.setObserver(this);
        minValue.setText( String.format("%3.2f", render.log.minValue));
        

        displayEntry();
	}
	
	@Override
	protected void keyTyped(char par1, int par2)
    {
		if (this.samplingPeriode.textboxKeyTyped(par1, par2))
        {

        }
        else if (this.maxValue.textboxKeyTyped(par1, par2))
        {

        }
        else if (this.minValue.textboxKeyTyped(par1, par2))
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
        this.samplingPeriode.mouseClicked(par1, par2, par3);
        this.maxValue.mouseClicked(par1, par2, par3);
        this.minValue.mouseClicked(par1, par2, par3);
    }


    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == resetBt)
    	{
    		render.clientSend(ElectricalDataLoggerElement.resetId);
    	}
    	else if(par1GuiButton == pause)
    	{
    		render.clientSend(ElectricalDataLoggerElement.tooglePauseId);
    	}
    	else if(par1GuiButton == printBt)
    	{
    		render.clientSend(ElectricalDataLoggerElement.printId);
    	}
    	else if(par1GuiButton == currentType)
    	{
    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.currentType);
    	}
    	else if(par1GuiButton == voltageType)
    	{
    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.voltageType);
    	}
    	else if(par1GuiButton == percentTyp)
    	{
    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.percentType);
    	}
    	else if(par1GuiButton == powerType)
    	{
    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.powerType);
    	}
    	else if(par1GuiButton == celsuisTyp)
    	{
    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.celsiusType);
    	}
    	else if(par1GuiButton == config)
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
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {

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
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        this.samplingPeriode.drawTextBox();
        this.maxValue.drawTextBox();
        this.minValue.drawTextBox();
             
        
		GL11.glLineWidth(1f);
		GL11.glColor4f(1f, 0f, 0f, 1f);
		
        GL11.glPushMatrix();
	        GL11.glTranslatef(50, 100, 0);
	        GL11.glScalef(50, 50, 1f);
	        render.log.draw(2.8f,1f);
        GL11.glPopMatrix();

    }

	
    public boolean doesGuiPauseGame()
    {
        return false;
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void textFieldNewValue(GuiTextFieldEln textField, String value) {
		
		float valuef = 0;
		try{
			valuef = Float.parseFloat(value);
		} catch(NumberFormatException e)
		{

		}	

		if(textField == maxValue)
		{
			render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, valuef);
		}
		else if(textField == minValue)
		{
			render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, valuef);
		}
		else if(textField == samplingPeriode)
		{
			render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, valuef);
		}
		
	/*
	 * 			float lowVoltage,highVoltage;
			
			try{
				lowVoltage = Float.parseFloat (samplingPeriode.getText());
				highVoltage = Float.parseFloat (highValue.getText());
				render.clientSetFloat(ElectricalDataLoggerElement.setValueId, lowVoltage,highVoltage);
			} catch(NumberFormatException e)
			{

			}	
	 */
	}
	
}
