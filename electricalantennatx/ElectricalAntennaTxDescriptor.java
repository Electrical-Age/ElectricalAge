package mods.eln.electricalantennatx;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNode.FrontType;

public class ElectricalAntennaTxDescriptor extends TransparentNodeDescriptor{

	public ElectricalAntennaTxDescriptor(
			String name,
			int rangeMax,
			double electricalPowerRatioEffStart,double electricalPowerRatioEffEnd,
			double electricalNominalVoltage,double electricalNominalPower,
			double electricalMaximalVoltage,double electricalMaximalPower,
			ElectricalCableDescriptor cable
			) {
		super(name, ElectricalAntennaTxElement.class, ElectricalAntennaTxRender.class);
		this.rangeMax = rangeMax;
		this.electricalNominalVoltage = electricalNominalVoltage;
		this.electricalNominalPower = electricalNominalPower;
		this.electricalMaximalVoltage = electricalMaximalVoltage;
		this.electricalMaximalPower = electricalMaximalPower;
		this.electricalPowerRatioEffStart = electricalPowerRatioEffStart;
		this.electricalPowerRatioEffEnd = electricalPowerRatioEffEnd;
		this.cable = cable;
		
		electricalPowerRatioLostOffset = 1.0 - electricalPowerRatioEffStart;
		electricalPowerRatioLostPerBlock = (electricalPowerRatioEffStart-electricalPowerRatioEffEnd) / rangeMax;
		
		electricalNominalInputR = electricalNominalVoltage*electricalNominalVoltage / electricalNominalPower;
	}
	
	@Override
	public FrontType getFrontType() {
		// TODO Auto-generated method stub
		return FrontType.PlayerView;
	}
	
	int rangeMax;
	double electricalPowerRatioEffStart, electricalPowerRatioEffEnd;
	double electricalPowerRatioLostOffset, electricalPowerRatioLostPerBlock;
	double electricalNominalVoltage, electricalNominalPower;
	double electricalMaximalVoltage, electricalMaximalPower;
	double electricalNominalInputR;
	ElectricalCableDescriptor cable;
	
}
