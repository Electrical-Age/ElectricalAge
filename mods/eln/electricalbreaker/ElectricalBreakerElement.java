package mods.eln.electricalbreaker;

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

	public ElectricalBreakerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
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
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

	public float voltageMax = (float) Eln.SVU, voltageMin = 0;
	
	public SixNodeElementInventory getInventory() {
		return inventory;
	}

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	boolean switchState = false;
	double currantMax = 0;
	boolean nbtBoot = false;
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
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
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front", (byte) ((front.toInt()<<0)));
		nbt.setBoolean(str + "switchState", switchState);
		nbt.setFloat(str + "voltageMax", voltageMax);
        nbt.setFloat(str + "voltageMin", voltageMin);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu) return aLoad;
		if(front.inverse() == lrdu) return bLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId) == null) return 0;
		if(front == lrdu) return NodeBase.maskElectricalAll;
		if(front.inverse() == lrdu) return NodeBase.maskElectricalAll;

		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("Ua:", aLoad.Uc) + Utils.plotVolt("Ub:", bLoad.Uc) + Utils.plotVolt("I:", aLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(switchState);
	    	stream.writeFloat((voltageMax));
	    	stream.writeFloat((voltageMin));

	    	Utils.serialiseItemStack(stream, inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSwitchState(boolean state) {
		if(state == switchState) return;
		playSoundEffect("random.click", 0.3F, 0.6F);
		switchState = state;
		refreshSwitchResistor();
		needPublish(); 
	}
	
	public void refreshSwitchResistor() {
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null || switchState == false) {
			switchResistor.highImpedance();
		}
		else {
			cableDescriptor.applyTo(switchResistor);
		}
	}
	
	public boolean getSwitchState() {
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
		reconnect();
	}
	
	public ElectricalCableDescriptor cableDescriptor = null;
	
	public void computeElectricalLoad() {
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		
		if(!nbtBoot)setSwitchState(false);
		nbtBoot = false;
		
		cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null) {
			aLoad.highImpedance();
			bLoad.highImpedance();	
		}
		else {
			cableDescriptor.applyTo(aLoad, false);
			cableDescriptor.applyTo(bLoad, false);
			currantMax = cableDescriptor.getImax();
		}
		refreshSwitchResistor();
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Eln.playerManager.get(entityPlayer).getInteractEnable()) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			
			return true;	
		}
		else if(Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) { 
    		return false;
    	}
    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) { 
    		return false;
    	}
    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	}    
    	else {
			//setSwitchState(true);
			//return true;
		}
		//front = LRDU.fromInt((front.toInt() + 1)&3);
    	return false;
	}

	public static final byte setVoltageMaxId = 1;
	public static final byte setVoltageMinId = 2;
	public static final byte toogleSwitchId = 3;
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte()) {
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
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalBreakerContainer(player, inventory);
	}
}
