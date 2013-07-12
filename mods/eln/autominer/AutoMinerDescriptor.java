package mods.eln.autominer;

import mods.eln.Eln;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElectricalLoadWatchdog;
import mods.eln.sim.ElectricalLoad;

public class AutoMinerDescriptor extends TransparentNodeDescriptor{

	public AutoMinerDescriptor(
			String name,
			double nominalVoltage,double maximalVoltage,
			double nominalPower,double nominalDropFactor,
			double pipeOperationTime,double pipeOperationEnergy
			
			) {
		super(name, AutoMinerElement.class, AutoMinerRender.class);
		this.nominalVoltage = nominalVoltage;
		this.maximalVoltage = maximalVoltage;
		this.pipeOperationTime = pipeOperationTime;
		this.pipeOperationEnergy = pipeOperationEnergy;
		pipeOperationPower = pipeOperationEnergy/pipeOperationTime;
		pipeOperationRp = nominalVoltage*nominalVoltage/pipeOperationPower;
		
		Rs = nominalVoltage*nominalVoltage / nominalPower  * nominalDropFactor;
	}

	double nominalVoltage,maximalVoltage;
	double pipeOperationTime,pipeOperationEnergy,pipeOperationPower;
	
	double pipeOperationRp;
	double Rs;
	
	public void applyTo(ElectricalLoad load)
	{
		load.setRs(Rs);
		load.setMinimalC(Eln.simulator);
	}
	
	
	public void applyTo(TransparentNodeElectricalLoadWatchdog watch)
	{
		watch.negativeLimit = -maximalVoltage * 0.1;
		watch.positiveLimit = maximalVoltage;
	}
	
	
	@Override
	public boolean mustHaveFloor() {
		// TODO Auto-generated method stub
		return false;
	}
}
