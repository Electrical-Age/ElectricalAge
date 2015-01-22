package mods.eln.sim.nbt;

import net.minecraft.init.Blocks;
import mods.eln.Eln;
import mods.eln.node.NodeBase;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.BatterySlowProcess;
import mods.eln.sim.ThermalLoad;

public class NbtBatterySlowProcess extends BatterySlowProcess {
	NodeBase node;
	float explosionRadius = 2;
	
	public NbtBatterySlowProcess(NodeBase node,BatteryProcess batteryProcess,ThermalLoad thermalLoad) {
		super(batteryProcess,thermalLoad);
		this.node = node;
		
	}

	@Override
	public void destroy() {
		node.physicalSelfDestruction(explosionRadius);
	}

}
