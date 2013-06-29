package mods.eln.electricalbreaker;

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
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeElectricalResistor;
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


public class ElectricalBreakerElement extends SixNodeElement {

	public ElectricalBreakerElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
    	electricalLoadList.add(aLoad);
    	electricalLoadList.add(bLoad);
    	electricalProcessList.add(switchResistor);
    	thermalProcessList.add(cutProcess);

    	this.descriptor = (ElectricalBreakerDescriptor) descriptor;
	}


	public ElectricalBreakerDescriptor descriptor;
	public NodeElectricalLoad aLoad = new NodeElectricalLoad("aLoad");
	public NodeElectricalLoad bLoad = new NodeElectricalLoad("bLoad");
	public ElectricalResistor switchResistor = new ElectricalResistor(aLoad, bLoad);
	public ElectricalBreakerCutProcess cutProcess = new ElectricalBreakerCutProcess(this);
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);
	LRDU front;
	public float voltageMax = 0,voltageMin = -2;
	
	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	
	boolean switchState = false;
	double currantMax = 0;
	boolean nbtBoot = false;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value>>0) & 0x3);
        switchState = nbt.getBoolean(str + "switchState");
        voltageMax = nbt.getFloat(str + "voltageMax");
        voltageMin = nbt.getFloat(str + "voltageMin");
        nbtBoot = true;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front",(byte) ((front.toInt()<<0)));
		nbt.setBoolean(str + "switchState", switchState);
		nbt.setFloat(str + "voltageMax",voltageMax);
        nbt.setFloat(str + "voltageMin",voltageMin);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front.left() == lrdu) return aLoad;
		if(front.right() == lrdu) return bLoad;
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
		if(front.left() == lrdu) return Node.maskElectricalAll;
		if(front.right() == lrdu) return Node.maskElectricalAll;

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("Ua", aLoad.Uc) + Utils.plotVolt("Ub", bLoad.Uc) + Utils.plotVolt("I", aLoad.getCurrent()) ;
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
			stream.writeBoolean(switchState);
	    	stream.writeFloat( (voltageMax));
	    	stream.writeFloat( (voltageMin));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void setSwitchState(boolean state)
	{
		if(state == switchState) return;
		switchState = state;
		refreshSwitchResistor();
		needPublish(); 
	}
	public void refreshSwitchResistor()
	{
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null || switchState == false)
		{
			switchResistor.highImpedance();
		}
		else
		{
			cableDescriptor.applyTo(switchResistor);
		}
	}
	
	public boolean getSwitchState()
	{
		return switchState;
	}
	@Override
	public void initialize() {

    	computeElectricalLoad();
    	
    	setSwitchState(switchState);
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
	}
	
	public ElectricalCableDescriptor cableDescriptor = null;
	
	public void computeElectricalLoad()
	{
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		
		if(!nbtBoot)setSwitchState(false);
		nbtBoot = false;
		
		cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null)
		{
			aLoad.highImpedance();
			bLoad.highImpedance();	
		}
		else
		{
			cableDescriptor.applyTo(aLoad, false);
			cableDescriptor.applyTo(bLoad, false);
			currantMax = cableDescriptor.getImax();
		}
		refreshSwitchResistor();

		
		
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

	public static final byte setVoltageMaxId = 1;
	public static final byte setVoltageMinId = 2;
	public static final byte toogleSwitchId = 3;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte())
			{
			case setVoltageMaxId:
				voltageMax = stream.readFloat();
				needPublish();
				break;
			case setVoltageMinId:
				voltageMin = stream.readFloat();
				needPublish();
				break;
			case toogleSwitchId:
				setSwitchState(! getSwitchState());
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
		return new ElectricalBreakerContainer(player, inventory);
	}
	
	
	
}
