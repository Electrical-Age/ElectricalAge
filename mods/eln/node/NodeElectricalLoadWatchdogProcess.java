package mods.eln.node;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadWatchdogListener;
import mods.eln.sim.IProcess;

public class NodeElectricalLoadWatchdogProcess implements IProcess{
	ElectricalLoad electricalLoad;
	public double positiveLimit =  Double.POSITIVE_INFINITY;
	public double negativeLimit =  Double.NEGATIVE_INFINITY;
	private Node node;
	private float explosionStrength;
	

	public NodeElectricalLoadWatchdogProcess(ElectricalLoad electricalLoad,Node node,double positiveLimit,float explosionStrength)
	{
		this.electricalLoad = electricalLoad;
		this.node = node;
		this.explosionStrength = explosionStrength;
		this.negativeLimit = -10;
		this.positiveLimit = positiveLimit;
	}
	public NodeElectricalLoadWatchdogProcess(ElectricalLoad electricalLoad,Node node,ElectricalCableDescriptor cable)
	{
		this(electricalLoad, node, cable.electricalNominalVoltage*1.3, 2);
	}	
	@Override
	public void process(double time) {
		if(electricalLoad.Uc < negativeLimit) 
		{
			node.physicalSelfDestruction(explosionStrength);
			return; 
		}
		if(electricalLoad.Uc > positiveLimit) 
		{
			node.physicalSelfDestruction(explosionStrength);
			return; 
		}
	}
	
	

}
