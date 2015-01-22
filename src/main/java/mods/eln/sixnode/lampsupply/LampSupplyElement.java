package mods.eln.sixnode.lampsupply;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketContainer;

public class LampSupplyElement extends SixNodeElement {

	public static HashMap<String, ArrayList<LampSupplyElement>> channelMap = new HashMap<String, ArrayList<LampSupplyElement>>();

	//NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");
	public LampSupplyDescriptor descriptor;

	public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
	public Resistor loadResistor = new Resistor(powerLoad, null);
	public IProcess lampSupplySlowProcess = new LampSupplySlowProcess();

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

	@Override
	public IInventory getInventory() {
		
		return inventory;
	}

	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		
		return new LampSupplyContainer(player, inventory);
	}

	public String channel = "Default channel";


	public LampSupplyElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(powerLoad);
		electricalComponentList.add(loadResistor);
		slowProcessList.add(lampSupplySlowProcess);
		loadResistor.highImpedance();
		front = LRDU.Down;
		this.descriptor = (LampSupplyDescriptor) descriptor;
		
		slowProcessList.add(voltageWatchdog);
		voltageWatchdog
		 .set(powerLoad)
		 .set(new WorldExplosion(this).cableExplosion());
		
		channelRegister(this);
	}

	
	VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();
	
	class LampSupplySlowProcess implements IProcess{

		@Override
		public void process(double time) {
			loadResistor.setR(1/RpStack);
			RpStack = 0;
		}
		
	}
	
	
	static void channelRegister(LampSupplyElement tx)
	{
		String channel = tx.channel;
		ArrayList<LampSupplyElement> list = channelMap.get(channel);
		if(list == null)
			channelMap.put(channel, list = new ArrayList<LampSupplyElement>());
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
		
		return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		
		return null;
	}

	@Override
	public void initialize() {
		setupFromInventory();
	}

	@Override
	protected void inventoryChanged() {
		
		super.inventoryChanged();
		sixNode.disconnect();
		setupFromInventory();
		sixNode.connect();
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
	public void destroy(EntityPlayerMP entityPlayer) {	
		super.destroy(entityPlayer);
		unregister();
	}

	
	@Override
	public void unload() {
		super.unload();
		channelRemove(this);
	}
	
	void unregister(){
		channelRemove(this);	
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		
		super.writeToNBT(nbt);
		nbt.setString("channel", channel);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		channelRemove(this);

		super.readFromNBT(nbt);
		channel = nbt.getString("channel");

		channelRegister(this);

	}

	void setupFromInventory() {
		ItemStack cableStack = inventory.getStackInSlot(LampSupplyContainer.cableSlotId);
		if(cableStack != null) {
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
			desc.applyTo(powerLoad);
			voltageWatchdog.setUNominal(desc.electricalNominalVoltage);
		} else {
			voltageWatchdog.setUNominal(10000);
			powerLoad.highImpedance();
		}
	}

	public static final byte setChannelId = 1;

	@Override
	public void networkUnserialize(DataInputStream stream) {

		super.networkUnserialize(stream);

		try {
			switch (stream.readByte()) {
			case setChannelId:
				channelRemove(this);
				channel = stream.readUTF();
				needPublish();
				channelRegister(this);
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
			stream.writeUTF(channel);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(LampSupplyContainer.cableSlotId));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	double RpStack = 0;

	public void addToRp(double r) {
		RpStack += 1 / r;
	}
	
	public int getRange(){
		return getRange(descriptor,inventory);
	}

	private int getRange(LampSupplyDescriptor desc,SixNodeElementInventory inventory2) {
		ItemStack stack = inventory.getStackInSlot(LampSupplyContainer.cableSlotId);
		if(stack == null) return desc.range;
		return desc.range + stack.stackSize;
	}
}
