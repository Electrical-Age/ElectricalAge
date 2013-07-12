package mods.eln.node;

import mods.eln.sim.BatteryProcess;
import mods.eln.sim.BatterySlowProcess;
import mods.eln.sim.ThermalLoad;

public class NodeBatterySlowProcess extends BatterySlowProcess {
	Node node;
	float explosionRadius = 0;
	
	public NodeBatterySlowProcess(Node node,BatteryProcess batteryProcess,ThermalLoad thermalLoad) {
		super(batteryProcess,thermalLoad);
		this.node = node;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		node.physicalSelfDestruction(explosionRadius);
	}

}
