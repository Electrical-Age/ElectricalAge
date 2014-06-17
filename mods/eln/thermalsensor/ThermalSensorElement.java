package mods.eln.thermalsensor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

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
import mods.eln.thermalcable.ThermalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ThermalSensorElement extends SixNodeElement {

	public ThermalSensorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
    	thermalLoadList.add(thermalLoad);
    	electricalLoadList.add(outputGate);
    	electricalProcessList.add(outputGateProcess);
    	thermalProcessList.add(slowProcess);

    	this.descriptor = (ThermalSensorDescriptor) descriptor;
	}


	public ThermalSensorDescriptor descriptor;
	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	public NodeElectricalLoad outputGate = new NodeElectricalLoad("outputGate");
	
	public NodeElectricalGateOutputProcess outputGateProcess = new NodeElectricalGateOutputProcess("outputGateProcess",outputGate);
	public ThermalSensorProcess slowProcess = new ThermalSensorProcess(this);
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);
	LRDU front;

	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	
	static final byte powerType = 0,temperatureType = 1;
	int typeOfSensor = temperatureType; 
	float lowValue = 0,highValue = 50;


	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value>>0) & 0x3);
        typeOfSensor = nbt.getByte("typeOfSensor");
        lowValue = nbt.getFloat("lowValue");
        highValue = nbt.getFloat("highValue");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setByte("front",(byte) ((front.toInt()<<0)));
		nbt.setByte("typeOfSensor", (byte) typeOfSensor);
		nbt.setFloat("lowValue", lowValue);
		nbt.setFloat("highValue", highValue);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub

		if(front == lrdu) return outputGate;
		
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(descriptor.temperatureOnly ==false)
		{
			if(inventory.getStackInSlot(ThermalSensorContainer.cableSlotId) != null){
				if(front.left() == lrdu) return thermalLoad;
				if(front.right() == lrdu) return thermalLoad;
			}
		}
		else
		{
			if(front.inverse() == lrdu) return thermalLoad;
		}
		return  null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4

		if(descriptor.temperatureOnly==false)
		{
			if(inventory.getStackInSlot(ThermalSensorContainer.cableSlotId) != null){
				if(front.left() == lrdu) return NodeBase.maskThermal;
				if(front.right() == lrdu) return NodeBase.maskThermal;
			}
			if(front == lrdu) return  NodeBase.maskElectricalOutputGate;
		}
		else
		{
			if(inventory.getStackInSlot(ThermalSensorContainer.cableSlotId) != null){
				if(front.inverse() == lrdu) return NodeBase.maskThermal;
			}
			if(front == lrdu) return  NodeBase.maskElectricalOutputGate;
		}
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return "";//Utils.plotUIP(electricalLoad.Uc, electricalLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T :", thermalLoad.Tc);
	}


	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte( (front.toInt()<<4) + typeOfSensor);
			stream.writeFloat(lowValue);
			stream.writeFloat(highValue);
			Utils.serialiseItemStack(stream,inventory.getStackInSlot(ThermalSensorContainer.cableSlotId));
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
		reconnect();
	}

	public void computeElectricalLoad()
	{
		ItemStack cable = inventory.getStackInSlot(ThermalSensorContainer.cableSlotId);
		
		
		ThermalCableDescriptor cableDescriptor = (ThermalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null)
		{
			thermalLoad.setHighImpedance();
		}
		else
		{
			cableDescriptor.setThermalLoad(thermalLoad);
		}

	}
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		else if(Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return false;
    	}
    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{ 
    		return false;
    	}
    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
    	{
    		return false;
    	}    
    	else
		{
			//setSwitchState(true);
			//return true;
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
		return new ThermalSensorContainer(player, inventory);
	}
	
	
	
}
