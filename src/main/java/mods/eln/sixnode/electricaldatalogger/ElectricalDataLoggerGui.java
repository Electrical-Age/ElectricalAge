package mods.eln.sixnode.electricaldatalogger;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.text.ParseException;

public class ElectricalDataLoggerGui extends GuiContainerEln implements GuiTextFieldElnObserver {

    GuiButton resetBt, voltageType, energyType, currentType, powerType, celsiusType, percentType, config, printBt, pause;
    GuiTextFieldEln samplingPeriode, maxValue, minValue, yCursorValue;
    ElectricalDataLoggerRender render;

    enum State {display,config}
    State state = State.display;

	public ElectricalDataLoggerGui(EntityPlayer player, IInventory inventory, ElectricalDataLoggerRender render) {
		super(new ElectricalDataLoggerContainer(player, inventory));
		this.render = render;
	}

	void displayEntry() {
		config.displayString = "Config";
		config.visible = true;
		pause.visible = true;
		resetBt.visible = true;
		voltageType.visible = false;
		energyType.visible = false;
		percentType.visible = false;
		currentType.visible = false;
		powerType.visible = false;
		celsiusType.visible = false;
		samplingPeriode.setVisible(false);
		maxValue.setVisible(false);
		minValue.setVisible(false);
		printBt.visible = true;
		state = State.display;
	}
	
	void configEntry() {	
		pause.visible = false;
		config.visible = true;
		config.displayString = "Return to Display";
		resetBt.visible = false;
		printBt.visible = true;
		voltageType.visible = true;
		energyType.visible = true;
		percentType.visible = true;
		currentType.visible = true;
		powerType.visible = true;
		celsiusType.visible = true;
		samplingPeriode.setVisible(true);	
		maxValue.setVisible(true);
		minValue.setVisible(true);
		state = State.config;
	}
	
	@Override
	public void initGui() {
		super.initGui();

        voltageType = newGuiButton(176 / 2 - 60 - 2, 8 + 20 + 2 - 2, 60, "Voltage [V]");
        currentType = newGuiButton(176 / 2 + 2, 8 + 20 + 2 - 2, 60, "Current [A]");
		resetBt = newGuiButton(176 / 2 - 50, 8 + 20 + 2 - 2, 48, "Reset");
		powerType = newGuiButton(176 / 2 - 60 - 2, 8 + 40 + 4 - 2, 60, "Power [W]");
		celsiusType = newGuiButton(176 / 2 + 2, 8 + 40 + 4 - 2, 60, "Degrees Celsius [°C]");
		percentType = newGuiButton(176 / 2 - 60 - 2, 8 + 60 + 6 - 2, 60, "Percent [-]%");
		energyType = newGuiButton(176 / 2 + 2, 8 + 60 + 6 - 2, 60, "Energy [J]");
		config = newGuiButton(176 / 2 - 50, 8 - 2, 100, "");
		printBt = newGuiButton(176 / 2 - 48 / 2, 123, 48, "Print");
		pause = newGuiButton(176 / 2 + 2, 8 + 20 + 2 - 2, 48, "");
		
		samplingPeriode = newGuiTextField(30, 101, 50);
        samplingPeriode.setText(render.log.samplingPeriod);
        samplingPeriode.setComment(new String[]{"Sampling period"});
        
        maxValue = newGuiTextField(176 - 50 - 30, 101 - 7, 50);
        maxValue.setText(render.log.maxValue);
        maxValue.setComment(new String[]{"Y-axis Max"});
        
        minValue = newGuiTextField(176 - 50 - 30, 101 + 8, 50);
        minValue.setText(render.log.minValue);
        minValue.setComment(new String[]{"Y-axis Min"});
       
        displayEntry();
	}
	
    @Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
		try {
	    	if (object == resetBt) {
	    		render.clientSend(ElectricalDataLoggerElement.resetId);
	    	} else if (object == pause) {
	    		render.clientSend(ElectricalDataLoggerElement.tooglePauseId);
	    	} else if (object == printBt) {
	    		render.clientSend(ElectricalDataLoggerElement.printId);
	    	} else if (object == currentType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.currentType);
	    	} else if (object == voltageType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.voltageType);
	    	} else if (object == energyType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.energyType);
	    	} else if (object == percentType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.percentType);
	    	} else if (object == powerType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.powerType);
	    	} else if (object == celsiusType) {
	    		render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.celsiusType);
	    	} else if (object == config) {
	    		switch(state) {
                    case config:
                        displayEntry();
                        break;
                    case display:
                        configEntry();
                        break;
                    default:
                        break;
	    		}
	    	} else if (object == maxValue) {
				render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, NumberFormat.getInstance().parse(maxValue.getText()).floatValue());
			} else if (object == minValue) {
				render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, NumberFormat.getInstance().parse(minValue.getText()).floatValue());
			} else if (object == samplingPeriode) {
				float value = NumberFormat.getInstance().parse(samplingPeriode.getText()).floatValue();
				if (value < 0.05f) value = 0.05f;
				samplingPeriode.setText(value);
				
				render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, value);
			}
		} catch(ParseException e) {
		}
    }
   
    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	powerType.enabled = true;
    	currentType.enabled = true;
    	voltageType.enabled = true;
    	celsiusType.enabled = true;
    	percentType.enabled = true;
    	energyType.enabled = true;
    	    	
    	switch(render.log.unitType) {
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
                celsiusType.enabled = false;
                break;
            case DataLogs.percentType:
                percentType.enabled = false;
                break;
            case DataLogs.energyType:
                energyType.enabled = false;
                break;
    	}
    	
    	if (render.pause)
    		pause.displayString = "Continue";
    	else
    		pause.displayString = "Pause";
    	
    	boolean a = inventorySlots.getSlot(ElectricalDataLoggerContainer.paperSlotId).getStack() != null;
    	boolean b = inventorySlots.getSlot(ElectricalDataLoggerContainer.printSlotId).getStack() == null;
    	printBt.enabled = a && b;
    }
    
    @Override
    protected void postDraw(float f, int x, int y) {
    	super.postDraw(f, x, y);
    	
    	if (state == State.display) {
			GL11.glColor4f(1f, 0f, 0f, 1f);
			
	        GL11.glPushMatrix();
		        GL11.glTranslatef(guiLeft + 8, guiTop + 60, 0);
		        GL11.glScalef(50, 50, 1f);
		        render.log.draw(2.9f, 1.2f, "");
	        GL11.glPopMatrix();
    	}
    }

	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 230, 8, 148);
	}
}
