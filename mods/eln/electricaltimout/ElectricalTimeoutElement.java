package mods.eln.electricaltimout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import org.bouncycastle.crypto.modes.SICBlockCipher;


import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.item.LampDescriptor;
import mods.eln.lampsocket.LampSocketContainer;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.IVoltageDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeElectricalResistor;
import mods.eln.node.NodeElectricalSourceWithCurrentLimitationProcess;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.NodeVoltageWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalTimeoutElement extends SixNodeElement {

	public ElectricalTimeoutElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
    	electricalLoadList.add(inputGate);
    	electricalLoadList.add(outputGate);
    	electricalProcessList.add(outputGateProcess);
    	thermalProcessList.add(slowProcess);

    	this.descriptor = (ElectricalTimeoutDescriptor) descriptor;
	}


	public ElectricalTimeoutDescriptor descriptor;

	public NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");	
	public NodeElectricalGateOutput outputGate = new NodeElectricalGateOutput("outputGate");	
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);
	
	public ElectricalTimeoutProcess slowProcess = new ElectricalTimeoutProcess(this);
	
	


	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	

	double timeOutCounter = 0,timeOutValue = 2;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value>>0) & 0x3);
        timeOutValue = nbt.getFloat(str + "timeOutValue");
        timeOutCounter = nbt.getFloat(str + "timeOutCounter");

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front",(byte) ((front.toInt()<<0)));
		nbt.setFloat(str + "timeOutValue", (float) timeOutValue);
		nbt.setFloat(str + "timeOutCounter", (float) timeOutCounter);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front == lrdu) return inputGate;
		if(front.inverse() == lrdu) return outputGate;
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
		if(front == lrdu) return NodeBase.maskElectricalInputGate;
		if(front.inverse() == lrdu) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return inputGate.plot("input") + outputGate.plot("output");
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}


	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) timeOutValue);
			stream.writeFloat((float) timeOutCounter);
			stream.writeBoolean(slowProcess.inputState);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	@Override
	public void initialize() {
		

	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		//front = LRDU.fromInt((front.toInt()+1)&3);
    	return false;

	}

	public static final byte resetId = 1;
	public static final byte setTimeOutValueId = 2;
	public static final byte setId = 3;
	
	void set()
	{
		timeOutCounter = timeOutValue;
		needPublish();
	}
	void reset()
	{
		timeOutCounter = 0.0;
		needPublish();
	}
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte())
			{
			case resetId:
				reset();
				break;			
			case setId:
				set();
				break;
			case setTimeOutValueId:
				timeOutValue = stream.readFloat();
				needPublish();
				break;

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	

	
	
	
}
