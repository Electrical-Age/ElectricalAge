package mods.eln.electricalantennarx;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNode.FrontType;

public class ElectricalAntennaRxDescriptor extends TransparentNodeDescriptor{

	public ElectricalAntennaRxDescriptor(
			String name,
			double electricalNominalVoltage,double electricalNominalPower,
			double electricalMaximalVoltage,double electricalMaximalPower,
			ElectricalCableDescriptor cable
			) {
		super(name, ElectricalAntennaRxElement.class, ElectricalAntennaRxRender.class);
		this.electricalNominalVoltage = electricalNominalVoltage;
		this.electricalNominalPower = electricalNominalPower;
		this.electricalMaximalVoltage = electricalMaximalVoltage;
		this.electricalMaximalPower = electricalMaximalPower;
		this.cable = cable;
		
	}
	
	@Override
	public FrontType getFrontType() {
		// TODO Auto-generated method stub
		return FrontType.PlayerView;
	}
	

	double electricalNominalVoltage, electricalNominalPower;
	double electricalMaximalVoltage, electricalMaximalPower;
	double electricalNominalInputR;
	ElectricalCableDescriptor cable;
}
