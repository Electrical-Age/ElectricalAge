package mods.eln.electricalmath;

import net.minecraft.entity.player.EntityPlayer;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class ElectricalMathElement extends SixNodeElement {

	NodeElectricalGateOutput gateOutput = new NodeElectricalGateOutput("gateOutput");
	NodeElectricalGateOutputProcess gateOutputProcess = new NodeElectricalGateOutputProcess("gateOutputProcess",gateOutput);
	
	NodeElectricalGateInput[] gateInput = new NodeElectricalGateInput[]{new NodeElectricalGateInput("gateA"),new NodeElectricalGateInput("gateB"),new NodeElectricalGateInput("gateC")};

	
	public ElectricalMathElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(gateOutput);
		electricalLoadList.add(gateInput[0]);
		electricalLoadList.add(gateInput[1]);
		electricalLoadList.add(gateInput[2]);
		
		electricalProcessList.add(gateOutputProcess);
	}
	
	byte sideConnection[] = new byte[3];

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(lrdu == front) return gateOutput;
		if(lrdu == front.left()) return sideConnectionToLoad(0);
		if(lrdu == front.inverse()) return sideConnectionToLoad(1);
		if(lrdu == front.right()) return sideConnectionToLoad(2);
		return null;
	}
	
	public NodeElectricalGateInput sideConnectionToLoad(int index)
	{
		byte id = sideConnection[index];
		if(id < 0 || id >= 3) return null;	
		return gateInput[id];
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(lrdu == front) return Node.maskElectricalOutputGate;
		if(lrdu == front.left()) return sideConnectionToLoad(0) != null ? Node.maskElectricalInputGate : 0;
		if(lrdu == front.inverse()) return sideConnectionToLoad(1) != null ? Node.maskElectricalInputGate : 0;
		if(lrdu == front.right()) return sideConnectionToLoad(2) != null ? Node.maskElectricalInputGate : 0;
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
}
