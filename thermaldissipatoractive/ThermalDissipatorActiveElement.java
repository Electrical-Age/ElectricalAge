package mods.eln.thermaldissipatoractive;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;

public class ThermalDissipatorActiveElement extends TransparentNodeElement{
	ThermalDissipatorActiveDescriptor descriptor;
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	ThermalDissipatorActiveSlowProcess slowProcess = new ThermalDissipatorActiveSlowProcess(this);
	NodeThermalWatchdogProcess watchdog;
	
	
	public ThermalDissipatorActiveElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		thermalLoadList.add(thermalLoad);
		electricalLoadList.add(positiveLoad);
		slowProcessList.add(slowProcess);
		this.descriptor = (ThermalDissipatorActiveDescriptor) descriptor;
		watchdog = new NodeThermalWatchdogProcess(node, this.descriptor, this.descriptor, thermalLoad);
		slowProcessList.add(watchdog);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(side == front && lrdu == LRDU.Down) return positiveLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(side == Direction.YN || side == Direction.YP || lrdu != lrdu.Down) return null;
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(side == Direction.YN || side == Direction.YP  || lrdu != lrdu.Down) return 0;
		if(side == front) return node.maskElectricalPower;
		return node.maskThermal;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U : ", positiveLoad.Uc) + Utils.plotAmpere("I : ", positiveLoad.getCurrent());
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T : ", thermalLoad.Tc) + Utils.plotPower("P : ",thermalLoad.getPower());
	}

	@Override
	public void initialize() {
		descriptor.applyTo(thermalLoad);
		descriptor.applyTo(positiveLoad);
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}

}
