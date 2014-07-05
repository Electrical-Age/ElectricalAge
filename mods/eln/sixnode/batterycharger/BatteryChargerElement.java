package mods.eln.sixnode.batterycharger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class BatteryChargerElement extends SixNodeElement {

	public BatteryChargerDescriptor descriptor;
	
	public NodeElectricalLoad powerLoad = new NodeElectricalLoad("powerLoad");
	public BatteryChargerSlowProcess slowProcess = new BatteryChargerSlowProcess();
	Resistor powerResistor = new Resistor(powerLoad,null);
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(5, 64, this);
	
	@Override
	public IInventory getInventory() {
		return inventory;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new BatteryChargerContainer(player, inventory);
	}
	
	public String channel = "Default channel";
	
	public BatteryChargerElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (BatteryChargerDescriptor) descriptor;
		
		electricalLoadList.add(powerLoad);
		electricalComponentList.add(powerResistor);
		slowProcessList.add(slowProcess);

		front = LRDU.Down;
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu) return powerLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return NodeBase.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
		descriptor.applyTo(powerLoad);
	}
	
	@Override
	protected void inventoryChanged() {
		super.inventoryChanged();
		needPublish(); 
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		if(Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}

	boolean powerOn = false;
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("powerOn", powerOn);
		nbt.setDouble("energyCounter", slowProcess.energyCounter);

	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerOn = nbt.getBoolean( "powerOn");
		slowProcess.energyCounter = nbt.getDouble( "energyCounter");
	}

	public static final byte toogleCharge = 1;
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()) {
			case toogleCharge:
				powerOn = ! powerOn;
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
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(powerOn);
			stream.writeFloat((float) powerLoad.getU());
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(0));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(1));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(2));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(3));
			
			stream.writeByte(charged);
			stream.writeByte(presence);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	byte charged,presence;
	
	class BatteryChargerSlowProcess implements IProcess {
		double energyCounter = 0;
		
		@Override
		public void process(double time) {
			byte oldCharged = charged;
			charged = 0;
			presence = 0;
			if(powerOn == false) {
				descriptor.setRp(powerResistor, false);
			}
			else {
				ItemStack booster = (inventory.getStackInSlot(BatteryChargerContainer.boosterSlotId));
				double boost = 1.0;
				double eff = 1.0;
				if(booster != null) {
					boost = Math.pow(1.25, booster.stackSize);
					eff = Math.pow(0.9, booster.stackSize);
				}
				
				energyCounter += powerResistor.getP() * time * eff;
				
				for(int idx = 0; idx < 4; idx++) {
					ItemStack stack = inventory.getStackInSlot(idx);
					Object o = Utils.getItemObject(stack);
					if(o instanceof IItemEnergyBattery){
						IItemEnergyBattery b = (IItemEnergyBattery) o;
						double e = Math.min(Math.min(energyCounter, b.getChargePower(stack) * time * boost), b.getEnergyMax(stack) - b.getEnergy(stack));
						b.setEnergy(stack, b.getEnergy(stack) + e);
						energyCounter -= e;
					}
				}
				
				if(energyCounter < descriptor.nominalPower * time * 2 * boost) {
					//double target = descriptor.nominalPower * time * 2;
					double power = Math.min(descriptor.nominalPower * boost, (descriptor.nominalPower * time * 2 * boost - energyCounter) / time);
					powerResistor.setR(Math.max(powerLoad.getU() * powerLoad.getU() / power,descriptor.Rp / boost));
					
				}
				else {
					descriptor.setRp(powerResistor, false);
				}
			}
			for(int idx = 0; idx < 4; idx++) {
				ItemStack stack = inventory.getStackInSlot(idx);
				Object o = Utils.getItemObject(stack);
				if(o instanceof IItemEnergyBattery) {
					IItemEnergyBattery b = (IItemEnergyBattery) o;
					if(b.getEnergy(stack) == b.getEnergyMax(stack)){
						charged += 1 << idx;
					}
					presence += 1 << idx;
				}
			}
			
			if(charged != oldCharged) 
				needPublish();
		}
	}
}
