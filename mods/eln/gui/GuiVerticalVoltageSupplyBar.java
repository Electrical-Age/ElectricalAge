package mods.eln.gui;

import mods.eln.Translator;
import mods.eln.misc.Utils;

public class GuiVerticalVoltageSupplyBar extends GuiVerticalWorkingZoneBar {

	public GuiVerticalVoltageSupplyBar(int xPosition, int yPosition, int width,
			int height, GuiHelper helper) {
		super(xPosition, yPosition, width, height, helper);

		setMinMax(0f,1.3f);
		addZone(0.6f, 0xFF0000FF);
		addZone(0.3f, 0xFF0066FF);
		addZone(0.2f, 0xFF00FF00);
		addZone(0.1f, 0xFFFF6600);
		addZone(0.1f, 0xFFFF0000);
		
	}
	float nominalU = 1;
	public void setNominalU(float nominalU)
	{
		this.nominalU = nominalU;
	}
	

	public void setVoltage(float value) {
		
		super.setValue(value/nominalU);
		setComment(0,Utils.plotVolt(Translator.translate("eln.core.machine.vsupply")+":",value));
	}	
	
	@Override
	public void setValue(float value) {
	
		super.setValue(value);
	}
	
	public void setPower(float f) {
		
		setComment(1,Utils.plotPower(Translator.translate("eln.core.machine.psupply")+":",f));
	}


}
