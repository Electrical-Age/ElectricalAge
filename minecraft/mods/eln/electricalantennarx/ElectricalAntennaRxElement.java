package mods.eln.electricalantennarx;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeElectricalPowerSource;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class ElectricalAntennaRxElement extends TransparentNodeElement{

	ElectricalAntennaRxSlowProcess slowProcess = new ElectricalAntennaRxSlowProcess(this);
	
	NodeElectricalLoad powerOut = new NodeElectricalLoad("powerOut");
	NodeElectricalGateInput signalIn = new NodeElectricalGateInput("signalIn");

	NodeElectricalPowerSource powerSrc = new NodeElectricalPowerSource("powerSrc", powerOut, ElectricalLoad.groundLoad);
	public double getSignal()
	{
		return signalIn.getBornedU();
	}
	public void setPowerOut(double power)
	{
		powerSrc.setP(power);
	}
	
	public void rxDisconnect()
	{
		powerSrc.setP(0.0);
	}
	
	LRDU rot = LRDU.Up;
	Coordonate rxCoord = null;
	ElectricalAntennaRxDescriptor descriptor;
	public ElectricalAntennaRxElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		slowProcessList.add(slowProcess);
	
		electricalLoadList.add(powerOut);
		electricalLoadList.add(signalIn);		
		electricalProcessList.add(powerSrc);
		
		this.descriptor = (ElectricalAntennaRxDescriptor) descriptor;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(front.getInverse() != side.applyLRDU(lrdu)) return null;
		
		if(side == front.applyLRDU(rot.left())) return powerOut;
		if(side == front.applyLRDU(rot.right())) return signalIn;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(front.getInverse() != side.applyLRDU(lrdu)) return 0;
		
		if(side == front.applyLRDU(rot.left())) return Node.maskElectricalPower;
		if(side == front.applyLRDU(rot.right())) return Node.maskElectricalInputGate;
		
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
		descriptor.cable.applyTo(powerOut,false);
		powerSrc.setUmax(descriptor.electricalMaximalVoltage*2);
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			rot = rot.getNextClockwise();
			node.reconnect();
			return true;	
		}
		return false;
	}

	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);

		rot = LRDU.readFromNBT(nbt, str + "rot");
		
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);

		rot.writeToNBT(nbt, str + "rot");
	}
	



	public boolean mustHaveFloor()
	{
		return false;
	}
	
	public boolean mustHaveCeiling()
	{
		return false;
	}
	public boolean mustHaveWall()
	{
		return false;
	}
	public boolean mustHaveWallFrontInverse()
	{
		return true;
	}
}
