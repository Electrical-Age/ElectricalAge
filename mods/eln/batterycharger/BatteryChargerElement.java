package mods.eln.batterycharger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.lampsocket.LampSocketContainer;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;

public class BatteryChargerElement extends SixNodeElement {


	


	public BatteryChargerDescriptor descriptor;
	
	public NodeElectricalLoad powerLoad = new NodeElectricalLoad("powerLoad");
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(4, 64, this);
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new BatteryChargerContainer(player, inventory);
	}
	
	public String channel = "Default channel";
	
	public BatteryChargerElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(powerLoad);

		front = LRDU.Down;
		this.descriptor = (BatteryChargerDescriptor) descriptor;
		
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
		// TODO Auto-generated method stub
		return Utils.plotUIP(powerLoad.Uc,powerLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		descriptor.applyTo(powerLoad,powerOn);
	}
	
	@Override
	protected void inventoryChanged() {
		// TODO Auto-generated method stub
		super.inventoryChanged();

		needPublish();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		if(Eln.playerManager.get(entityPlayer).getInteractEnable())
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}


	boolean powerOn = false;
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setBoolean(str + "powerOn", powerOn);

	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		super.readFromNBT(nbt, str);
		powerOn = nbt.getBoolean(str + "powerOn");
	}



	public static final byte toogleCharge = 1;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()){
			case toogleCharge:
				powerOn = ! powerOn;
				descriptor.applyTo(powerLoad,powerOn);
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
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(powerOn);
			stream.writeFloat((float) powerLoad.Uc);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(0));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(1));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(2));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(3));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	class BatteryChargerSlowProcess implements IProcess
	{

		@Override
		public void process(double time) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
