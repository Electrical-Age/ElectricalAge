package mods.eln.sixnode.electricalwindsensor;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;



public class ElectricalWindSensorElement extends SixNodeElement{

	ElectricalWindSensorDescriptor descriptor;
	public ElectricalWindSensorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
	
    	electricalLoadList.add(outputGate);
    	electricalComponentList.add(outputGateProcess);
    	slowProcessList.add(slowProcess);
    	slowProcessList.add(publishProcess);
    	this.descriptor = (ElectricalWindSensorDescriptor) descriptor;
	}
	public NodeElectricalGateOutput outputGate = new NodeElectricalGateOutput("outputGate");
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);
	public ElectricalWindSensorSlowProcess slowProcess = new ElectricalWindSensorSlowProcess(this);
	public NodePeriodicPublishProcess publishProcess = new NodePeriodicPublishProcess(sixNode, 5, 5);
	

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front == lrdu.left()) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4
		if(front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent()) ;
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}



	@Override
	public void initialize() {
	
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return onBlockActivatedRotate(entityPlayer);
	}


	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) Utils.getWind(sixNode.coordonate.world(), sixNode.coordonate.y));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}















