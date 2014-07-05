package mods.eln.sixnode.electricallightsensor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalLightSensorElement extends SixNodeElement {

	ElectricalLightSensorDescriptor descriptor;
	public ElectricalLightSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	electricalLoadList.add(outputGate);
    	electricalComponentList.add(outputGateProcess);
    	slowProcessList.add(slowProcess);
    	this.descriptor = (ElectricalLightSensorDescriptor) descriptor;
	}
	
	public NodeElectricalGateOutput outputGate = new NodeElectricalGateOutput("outputGate");
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess", outputGate);
	public ElectricalLightSensorSlowProcess slowProcess = new ElectricalLightSensorSlowProcess(this);

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu.left()) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return onBlockActivatedRotate(entityPlayer);
	}
}
