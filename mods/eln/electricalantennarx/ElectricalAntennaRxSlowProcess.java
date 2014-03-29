package mods.eln.electricalantennarx;

import net.minecraft.block.Block;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeManager;
import mods.eln.sim.IProcess;

public class ElectricalAntennaRxSlowProcess implements IProcess {
	ElectricalAntennaRxElement element;
	
	public ElectricalAntennaRxSlowProcess(ElectricalAntennaRxElement element) {
		this.element = element;
	}
	
	@Override
	public void process(double time) {
		if(element.powerSrc.getP() > element.descriptor.electricalMaximalPower)
		{
			element.node.physicalSelfDestruction(2.0f);
		}
		if(element.powerOut.Uc > element.descriptor.electricalMaximalVoltage)
		{
			element.node.physicalSelfDestruction(2.0f);
		}
	}
}
