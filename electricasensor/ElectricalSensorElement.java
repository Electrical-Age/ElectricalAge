package mods.eln.electricasensor;

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
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
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


public class ElectricalSensorElement extends SixNodeElement {

	public ElectricalSensorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		//front = LRDU.Left;
    	this.descriptor = (ElectricalSensorDescriptor) descriptor;
	//	if(this.descriptor.voltageOnly == false)
			electricalLoad = new NodeElectricalLoad("electricalLoad");
		/*else
			electricalLoad = new NodeElectricalGateInput("inputGate");*/
		electricalLoadList.add(electricalLoad);
    	electricalLoadList.add(outputGate);
    	electricalProcessList.add(outputGateProcess);
    	thermalProcessList.add(slowProcess);
    	
    //	electricalLoad.setRp(100000000000000.0);

	}


	public ElectricalSensorDescriptor descriptor;
	public NodeElectricalLoad electricalLoad;
	public NodeElectricalLoad outputGate = new NodeElectricalLoad("outputGate");
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);
	public ElectricalSensorProcess slowProcess = new ElectricalSensorProcess(this);
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);


	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	
	static final byte powerType = 0,currantType = 1,voltageType = 2;
	int typeOfSensor = voltageType; 
	float lowValue = 0,highValue = 50;

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value>>0) & 0x3);
        typeOfSensor = nbt.getByte(str + "typeOfSensor");
        lowValue = nbt.getFloat(str + "lowValue");
        highValue = nbt.getFloat(str + "highValue");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front",(byte) ((front.toInt()<<0)));
		nbt.setByte(str + "typeOfSensor", (byte) typeOfSensor);
		nbt.setFloat(str + "lowValue", lowValue);
		nbt.setFloat(str + "highValue", highValue);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(descriptor.voltageOnly == false)
		{
			if(front.left() == lrdu) return electricalLoad;
			if(front.right() == lrdu) return electricalLoad;
			if(front == lrdu) return outputGate;
		}
		else
		{
			if(front == lrdu) return electricalLoad;
			if(front.inverse() == lrdu) return outputGate;
		}
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

		if(descriptor.voltageOnly == false)
		{
			if(front.left() == lrdu) return Node.maskElectricalAll;
			if(front.right() == lrdu) return Node.maskElectricalAll;
			if(front == lrdu) return Node.maskElectricalOutputGate;
		}
		else
		{
			if(front == lrdu) return Node.maskElectricalAll;
			if(front.inverse() == lrdu) return Node.maskElectricalOutputGate;
		}
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		if(descriptor.voltageOnly == false)
			return Utils.plotUIP(electricalLoad.Uc, electricalLoad.getCurrent());
		else
			return Utils.plotVolt("Uin", electricalLoad.Uc) + Utils.plotVolt("Uout", outputGate.Uc);

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
			stream.writeByte( typeOfSensor);
			stream.writeFloat(lowValue);
			stream.writeFloat(highValue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	@Override
	public void initialize() {
		
		Eln.instance.signalCableDescriptor.applyTo(outputGate, false);
    	computeElectricalLoad();
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
	}

	public void computeElectricalLoad()
	{
		
		//if(descriptor.voltageOnly == false)
		{
			ItemStack cable = inventory.getStackInSlot(ElectricalSensorContainer.cableSlotId);
			ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
			if(cableDescriptor == null)
			{
				electricalLoad.highImpedance();
			}
			else
			{
				cableDescriptor.applyTo(electricalLoad, false);
			}
		}

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

	public static final byte setTypeOfSensorId = 1;
	public static final byte setValueId = 2;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte())
			{
			case setTypeOfSensorId:
				typeOfSensor = stream.readByte();
				needPublish();
				break;
			case setValueId:
				lowValue = stream.readFloat();
				highValue = stream.readFloat();
				if(lowValue == highValue) highValue += 0.0001;
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
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalSensorContainer(player, inventory);
	}
	
	
	
}
