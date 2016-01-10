package mods.eln.sixnode.electricasensor;

import mods.eln.gui.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import java.text.NumberFormat;
import java.text.ParseException;

import static mods.eln.i18n.I18N.tr;

public class ElectricalSensorGui extends GuiContainerEln {

    GuiButton validate, voltageType, currentType, powerType, dirType;
    GuiTextFieldEln lowValue, highValue;
    ElectricalSensorRender render;

	public ElectricalSensorGui(EntityPlayer player, IInventory inventory, ElectricalSensorRender render) {
		super(new ElectricalSensorContainer(player, inventory, render.descriptor));
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();

		if (!render.descriptor.voltageOnly) {
			voltageType = newGuiButton(8, 8, 50, tr("Voltage"));
			currentType = newGuiButton(8, 8 + 24, 50, tr("Current"));
			powerType = newGuiButton(8, 8 + 48, 50, tr("Power"));
			dirType = newGuiButton(8 + 50 + 4, 8 + 48, 50, "");

			int x = 0, y = -12;
			validate = newGuiButton(x + 8 + 50 + 4 + 50 + 4, y + (166 - 84) / 2 - 9, 50, tr("Validate"));
			
			lowValue = newGuiTextField(x + 8 + 50 + 4, y + (166 - 84) / 2 + 3, 50);
	        lowValue.setText(render.lowValue);
			lowValue.setComment(tr("Measured value\ncorresponding\na 0% output.").split("\n"));

			highValue = newGuiTextField(x + 8 + 50 + 4, y + (166 - 84) / 2 - 13, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(tr("Measured value\ncorresponding\na 100% output.").split("\n"));
		} else {
			validate = newGuiButton(8 + 50 + 4, 10, 50, tr("Validate"));
			
			lowValue = newGuiTextField(8, 6 + 16, 50);
	        lowValue.setText(render.lowValue);
			lowValue.setComment(tr("Measured voltage\ncorresponding\na 0% output").split("\n"));

			highValue = newGuiTextField(8, 6, 50);
	        highValue.setText(render.highValue);
	        highValue.setComment(tr("Measured voltage\ncorresponding\na 100% output").split("\n"));
		}
	}

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if (object == validate) {
			float lowVoltage, highVoltage;
			
			try {
				lowVoltage = NumberFormat.getInstance().parse(lowValue.getText()).floatValue();
				highVoltage = NumberFormat.getInstance().parse(highValue.getText()).floatValue();
				render.clientSetFloat(ElectricalSensorElement.setValueId, lowVoltage, highVoltage);
			} catch (ParseException e) {
			}
    	} else if (object == currentType) {
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.currantType);
    	} else if (object == voltageType) {
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.voltageType);
    	} else if (object == powerType) {
    		render.clientSetByte(ElectricalSensorElement.setTypeOfSensorId, ElectricalSensorElement.powerType);
    	} else if (object == dirType) {
    		render.dirType = (byte) ((render.dirType + 1) % 3);
    		render.clientSetByte(ElectricalSensorElement.setDirType, render.dirType);
    	}
    }
   
    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if (!render.descriptor.voltageOnly) {
    		switch (render.dirType) {
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
    		
	    	if (render.typeOfSensor == ElectricalSensorElement.currantType) {
	        	powerType.enabled = true;
	        	currentType.enabled = false;
	        	voltageType.enabled = true;
	    	} else if (render.typeOfSensor == ElectricalSensorElement.voltageType) {
	    		powerType.enabled = true;
	        	currentType.enabled = true;
	        	voltageType.enabled = false;
	    	} else if (render.typeOfSensor == ElectricalSensorElement.powerType) {
	        	powerType.enabled = false;
	        	currentType.enabled = true;
	        	voltageType.enabled = true;
	    	}
    	}
    }

	@Override
	protected GuiHelperContainer newHelper() {
		if (!render.descriptor.voltageOnly)
			return new HelperStdContainer(this);
		else
			return new GuiHelperContainer(this, 176, 166 - 45, 8, 84 - 45);
	}
}
