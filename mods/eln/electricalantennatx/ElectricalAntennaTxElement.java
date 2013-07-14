package mods.eln.electricalantennatx;

import java.io.DataOutputStream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.electricalantennarx.ElectricalAntennaRxElement;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeManager;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class ElectricalAntennaTxElement extends TransparentNodeElement{

	ElectricalAntennaTxSlowProcess slowProcess = new ElectricalAntennaTxSlowProcess(this);
	

	NodeElectricalLoad powerIn = new NodeElectricalLoad("powerIn");
	NodeElectricalGateInput commandIn = new NodeElectricalGateInput("commandIn");
	NodeElectricalGateOutput signalOut = new NodeElectricalGateOutput("signalOut");
	NodeElectricalGateOutputProcess signalOutProcess = new NodeElectricalGateOutputProcess("signalOutProcess", signalOut);
	
	ElectricalAntennaTxElectricalProcess electricalProcess = new ElectricalAntennaTxElectricalProcess(this);
	
	LRDU rot = LRDU.Down;
	
	boolean placeBoot = true;
	
	ElectricalAntennaTxDescriptor descriptor;
	public ElectricalAntennaTxElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		slowProcessList.add(slowProcess);
		
		electricalLoadList.add(powerIn);
		electricalLoadList.add(commandIn);
		electricalLoadList.add(signalOut);		
		electricalProcessList.add(signalOutProcess);
		electricalProcessList.add(electricalProcess);
		
		this.descriptor = (ElectricalAntennaTxDescriptor) descriptor;
	}
	
	Coordonate rxCoord = null;
	ElectricalAntennaRxElement rxElement = null;
	double powerEfficency = 0.0;

	public void txDisconnect()
	{
		ElectricalAntennaRxElement rx = getRxElement();
		
		if(rx != null) rx.rxDisconnect();
		rxCoord = null;
		rxElement = null;
	}
	
	ElectricalAntennaRxElement getRxElement()
	{
		if(rxCoord == null) return null;
		if(rxElement == null)
		{
			Node node = NodeManager.instance.getNodeFromCoordonate(rxCoord);
			if(node != null && node instanceof TransparentNode && ((TransparentNode)node).element instanceof ElectricalAntennaRxElement)
				rxElement =  (ElectricalAntennaRxElement) ((TransparentNode)node).element;
			else
			{
				rxCoord = null;
				System.out.println("ASSERT ElectricalAntennaRxElement getRxElement()");
			}
		}
		return rxElement;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(front.getInverse() != side.applyLRDU(lrdu)) return null;
		
		if(side == front.applyLRDU(rot)) return powerIn;
		if(side == front.applyLRDU(rot.left())) return signalOut;
		if(side == front.applyLRDU(rot.right())) return commandIn;
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
		
		if(side == front.applyLRDU(rot)) return Node.maskElectricalPower;
		if(side == front.applyLRDU(rot.left())) return Node.maskElectricalOutputGate;
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

	void calculatePowerInRp()
	{
		double cmd = commandIn.getNormalized();
		if(cmd == 0.0)
			powerIn.setRp(1000000000.0);
		else
			powerIn.setRp(descriptor.electricalNominalInputR / cmd);
	}
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		descriptor.cable.applyTo(powerIn, false);
		calculatePowerInRp();
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
		if(nbt.getBoolean(str + "rxCoordValid") == true)
		{
			rxCoord = new Coordonate();
			rxCoord.readFromNBT(nbt, str + "rxCoord");
		}
		rot = LRDU.readFromNBT(nbt, str + "rot");
		placeBoot = false;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		if(rxCoord == null)
			nbt.setBoolean(str + "rxCoordValid", false);
		else
		{
			nbt.setBoolean(str + "rxCoordValid", true);
			rxCoord.writeToNBT(nbt, str + "rxCoord");	
		}
		rot.writeToNBT(nbt, str + "rot");
		
	}
	@Override
	public void onBreakElement() {
		// TODO Auto-generated method stub
		txDisconnect();
		super.onBreakElement();
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
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		rot.serialize(stream);
		node.lrduCubeMask.getTranslate(front.getInverse()).serialize(stream);
	}
}
