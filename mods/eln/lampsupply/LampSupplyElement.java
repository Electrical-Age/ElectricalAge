package mods.eln.lampsupply;

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
import mods.eln.sim.ThermalLoad;

public class LampSupplyElement extends SixNodeElement {


	

	public static HashMap<String, ArrayList<LampSupplyElement>> channelMap = new HashMap<String, ArrayList<LampSupplyElement>>(); 
	
	//NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");

	public LampSupplyDescriptor descriptor;
	
	public NodeElectricalLoad powerLoad = new NodeElectricalLoad("powerLoad");
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new LampSupplyContainer(player, inventory);
	}
	
	public String channel = "Default channel";
	
	public LampSupplyElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(powerLoad);

		front = LRDU.Down;
		this.descriptor = (LampSupplyDescriptor) descriptor;
		channelRegister(this);
	}

	static void channelRegister(LampSupplyElement tx)
	{
		String channel = tx.channel;
		ArrayList<LampSupplyElement> list = channelMap.get(channel);
		if(list == null) 
			channelMap.put(channel,list =  new ArrayList<LampSupplyElement>());
		list.add(tx);
	}
	
	static void channelRemove(LampSupplyElement tx)
	{
		String channel = tx.channel;
		ArrayList<LampSupplyElement> list = channelMap.get(channel);
		if(list == null) return;
		list.remove(tx);
		if(list.size() == 0)
			channelMap.remove(channel);
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSupplyContainer.cableSlotId) == null) return null;
		if(front == lrdu) return powerLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSupplyContainer.cableSlotId) == null) return null;
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSupplyContainer.cableSlotId) == null) return 0;
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
		setupFromInventory();
	}
	
	@Override
	protected void inventoryChanged() {
		// TODO Auto-generated method stub
		super.inventoryChanged();
		setupFromInventory();
		reconnect();
		needPublish();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		channelRemove(this);
		super.destroy();
	}
	
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setString("channel", channel);
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		
		channelRemove(this);
		
		super.readFromNBT(nbt);
		channel = nbt.getString( "channel");
		
		channelRegister(this);
		
	}



	void setupFromInventory(){
		ItemStack cableStack = inventory.getStackInSlot(LampSupplyContainer.cableSlotId);
		if(cableStack != null){
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
			desc.applyTo(powerLoad,false);
		}
		else{
			powerLoad.highImpedance();
		}
	}

	
	public static final byte setChannelId = 1;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()){
			case setChannelId:
				channelRemove(this);
				channel = stream.readUTF();
				needPublish();
				channelRegister(this);
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
			stream.writeUTF(channel);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(LampSupplyContainer.cableSlotId));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
