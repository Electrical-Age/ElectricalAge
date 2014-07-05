package mods.eln.sixnode.lampsocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import mods.eln.Eln;
import mods.eln.item.LampDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.MonsterPopFreeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class LampSocketElement extends SixNodeElement {

	LampSocketDescriptor socketDescriptor = null;

	public LampSocketElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.socketDescriptor = (LampSocketDescriptor) descriptor;

		lampProcess.alphaZ = this.socketDescriptor.alphaZBoot;
		//electricalLoadList.add(positiveLoad);
		//electricalComponentList.add(lampResistor);
		thermalLoadList.add(thermalLoad);
		slowProcessList.add(lampProcess);
		slowProcessList.add(monsterPopFreeProcess);
	}

	public MonsterPopFreeProcess monsterPopFreeProcess = new MonsterPopFreeProcess(sixNode.coordonate, 15);
	public NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	//public NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");

	public LampSocketProcess lampProcess = new LampSocketProcess(this);
	public Resistor lampResistor = new Resistor(positiveLoad, null);

	boolean poweredByLampSupply = true;
	boolean grounded = true;

	SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

	LampDescriptor lampDescriptor = null;
	//ElectricalCableDescriptor cableDescriptor = null;
	public String channel = lastSocketName;

	public static String lastSocketName = "Default channel";

	@Override
	public IInventory getInventory()
	{
		return inventory;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		byte value = nbt.getByte("front");
		front = LRDU.fromInt((value >> 0) & 0x3);
		grounded = (value & 4) != 0;

		setPoweredByLampSupply(nbt.getBoolean("poweredByLampSupply"));
		channel = nbt.getString("channel");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) ((front.toInt() << 0) + (grounded ? 4 : 0)));
		nbt.setBoolean("poweredByLampSupply", poweredByLampSupply);
		nbt.setString("channel", channel);
	}

	static final int setGroundedId = 1;
	static final int setAlphaZId = 2;
	static final int tooglePowerSupplyType = 3, setChannel = 4;

	public void networkUnserialize(DataInputStream stream)
	{
		try {
			switch (stream.readByte())
			{
			case setGroundedId:
				grounded = stream.readByte() != 0 ? true : false;
				computeElectricalLoad();
				reconnect();
				break;
			case setAlphaZId:
				lampProcess.alphaZ = stream.readFloat();
				needPublish();
				break;
			case tooglePowerSupplyType:
				setPoweredByLampSupply(!poweredByLampSupply);

				reconnect();
				break;
			case setChannel:
				channel = stream.readUTF();
				lastSocketName = channel;
				needPublish();
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void setPoweredByLampSupply(boolean b) {
		poweredByLampSupply = b;
	}

	@Override
	public void disconnectJob() {
		// TODO Auto-generated method stub
		super.disconnectJob();

		electricalLoadList.remove(positiveLoad);
		electricalComponentList.remove(lampResistor);
		positiveLoad.state = 0;
	}

	@Override
	public void connectJob() {
		// TODO Auto-generated method stub
		if(!poweredByLampSupply) {
			electricalLoadList.add(positiveLoad);
			electricalComponentList.add(lampResistor);
		}
		super.connectJob();
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
		reconnect();
	}

	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new LampSocketContainer(player, inventory, socketDescriptor);
	}

	public static boolean canBePlacedOnSide(Direction side, int type)
	{
		return true;
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null) return null;
		if(poweredByLampSupply) return null;
		// TODO Auto-generated method stub
		if(grounded) return positiveLoad;

		//if(front == lrdu) return positiveLoad;
		//if(front == lrdu.inverse()) return negativeLoad;

		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null) return null;
		if(poweredByLampSupply) return null;
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null) return 0;
		if(poweredByLampSupply) return 0;
		if(grounded) return NodeBase.maskElectricalPower;

		if(front == lrdu) return NodeBase.maskElectricalPower;
		if(front == lrdu.inverse()) return NodeBase.maskElectricalPower;

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotVolt("U:", positiveLoad.getU()) + Utils.plotAmpere("I:", lampResistor.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T:", thermalLoad.Tc);
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeByte((grounded ? (1 << 6) : 0));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.lampSlotId));
			stream.writeFloat((float) lampProcess.alphaZ);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.cableSlotId));
			stream.writeBoolean(poweredByLampSupply);
			stream.writeUTF(channel);
			stream.writeBoolean(isConnectedToLampSupply);
			stream.writeByte(lampProcess.light);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

		computeElectricalLoad();

		thermalLoad.Rs = 100000f;
		thermalLoad.C = 0.5f;
		thermalLoad.Rp = 5.0f;

	}

	public void computeElectricalLoad()
	{
		ItemStack lamp = inventory.getStackInSlot(LampSocketContainer.lampSlotId);
		ItemStack cable = inventory.getStackInSlot(LampSocketContainer.cableSlotId);

		ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if(cableDescriptor == null)
		{
			positiveLoad.highImpedance();
			//negativeLoad.highImpedance();
		}
		else
		{
			cableDescriptor.applyTo(positiveLoad);
			//cableDescriptor.applyTo(negativeLoad, grounded,5);
		}

		lampDescriptor = (LampDescriptor) Utils.getItemObject(lamp);
		if(lampDescriptor == null)
		{
			lampResistor.setR(Double.POSITIVE_INFINITY);
		}
		else
		{
			lampDescriptor.applyTo(lampResistor);
		}

	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz)
	{
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			front = front.getNextClockwise();
			reconnect();
			return true;
		}

		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if(currentItemStack != null)
		{
			Item item = currentItemStack.getItem();
		}
		return false;
	}

	public int getLightValue()
	{
		return lampProcess.getBlockLight();
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
		lampProcess.destructor();
	}

	void setIsConnectedToLampSupply(boolean value) {
		if(isConnectedToLampSupply != value) {
			isConnectedToLampSupply = value;
			needPublish();
		}
	}

	boolean isConnectedToLampSupply = false;

}
