package mods.eln.node.six;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.misc.*;
import mods.eln.misc.Coordinate;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sound.IPlayer;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public abstract class SixNodeElement implements GhostObserver, IPlayer {
	//private static Class[] idToClass = new Class[256];
	//private static Class[] idToRenderClass = new Class[256];

	public ArrayList<IProcess> slowProcessList = new ArrayList<IProcess>(4);

	public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
	public ArrayList<NbtElectricalLoad> electricalLoadList = new ArrayList<NbtElectricalLoad>(4);

	public ArrayList<IProcess> thermalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<IProcess> thermalSlowProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
	public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);

	public SixNode sixNode;
	public Direction side;
	public SixNodeDescriptor sixNodeElementDescriptor;

	public int isProvidingWeakPower()
	{
		return 0;
	}

	protected void inventoryChanged() {
		

	}

	public void play(SoundCommand s) {
		s.addUuid(getUuid());
		s.set(sixNode.coordinate);
		s.play();
	}

	public Coordinate getCoordonate() {
		return sixNode.coordinate;
	}

	protected boolean onBlockActivatedRotate(EntityPlayer entityPlayer)
	{
		if(Utils.isPlayerUsingWrench(entityPlayer))
		{
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;
		}
		return false;
	}

	public void sendPacketToAllClient(ByteArrayOutputStream bos) {
		sixNode.sendPacketToAllClient(bos);
	}
	public void sendPacketToAllClient(ByteArrayOutputStream bos,double range) {
		sixNode.sendPacketToAllClient(bos,range);
	}

	public void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player)
	{
		sixNode.sendPacketToClient(bos, player);
	}

	public void notifyNeighbor()
	{
		sixNode.notifyNeighbor();
	}

	public void connectJob()
	{
		Eln.simulator.addAllElectricalComponent(electricalComponentList);
		Eln.simulator.addAllThermalConnection(thermalConnectionList);

		for(NbtElectricalLoad load : electricalLoadList)
			Eln.simulator.addElectricalLoad(load);
		for(NbtThermalLoad load : thermalLoadList)
			Eln.simulator.addThermalLoad(load);


		for(IProcess process : slowProcessList)
			Eln.simulator.addSlowProcess(process);
		for(IProcess process : electricalProcessList)
			Eln.simulator.addElectricalProcess(process);
		for(IProcess process : thermalProcessList)
			Eln.simulator.addThermalFastProcess(process);
		for(IProcess process : thermalSlowProcessList)
			Eln.simulator.addThermalSlowProcess(process);

	}

	public void networkUnserialize(DataInputStream stream)
	{

	}

	public void networkUnserialize(DataInputStream stream, EntityPlayerMP player)
	{
		networkUnserialize(stream);
	}

	public int getLightValue()
	{
		return 0;
	}

	public boolean hasGui()
	{
		return false;
	}

	public IInventory getInventory()
	{
		return null;
	}

	public Container newContainer(Direction side, EntityPlayer player)
	{
		return null;
	}

	public SixNodeElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor)
	{
		this.sixNode = sixNode;
		this.side = side;
		this.sixNodeElementDescriptor = descriptor;
		this.itemStackDamageId = sixNode.sideElementIdList[side.getInt()];

		if(descriptor.hasGhostGroup())
			Eln.ghostManager.addObserver(this);
	}

	public void preparePacketForClient(DataOutputStream stream)
	{
		sixNode.preparePacketForClient(stream, this);
	}

	public abstract ElectricalLoad getElectricalLoad(LRDU lrdu);

	public abstract ThermalLoad getThermalLoad(LRDU lrdu);

	public abstract int getConnectionMask(LRDU lrdu);

	public abstract String multiMeterString();

	public abstract String thermoMeterString();

	public LRDU front = LRDU.Up;

	private int itemStackDamageId;

	public void networkSerialize(DataOutputStream stream)
	{

		try {
			stream.writeByte(sixNode.lrduElementMask.get(side).mask + (front.dir << 4));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	/* public void initializeFromThat(SixNodeDescriptor descriptor)
	 {
	 	this.descriptor = descriptor;
	 	initialize();
	 }*/
	public abstract void initialize();



	@Override
	public void stop(int uuid) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
		DataOutputStream stream = new DataOutputStream(bos);

		try {
			stream.writeByte(Eln.packetDestroyUuid);
			stream.writeInt(uuid);

			sendPacketToAllClient(bos);
		} catch (IOException e) {
			
			e.printStackTrace();

		}
	}

	public void destroy(EntityPlayerMP entityPlayer)
	{
		if(useUuid()) {
			stop(uuid);
		}

		if(sixNodeElementDescriptor.hasGhostGroup()) {
			Eln.ghostManager.removeObserver(sixNode.coordinate);
			sixNodeElementDescriptor.getGhostGroup(side, front).erase(sixNode.coordinate);
		}

		sixNode.dropInventory(getInventory());
		//	getCoordinate().world().getWorldInfo().
		if(Utils.mustDropItem(entityPlayer))
			sixNode.dropItem(getDropItemStack());
	}

	public abstract boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz);

	/*
	public void onBreakElement()
	{
		sixNode.dropInventory(getInventory());
		
	}*/

	public ItemStack getDropItemStack()
	{
		return new ItemStack(Eln.sixNodeBlock, 1, itemStackDamageId); //sixNode.sideElementIdList[side.getInt()]
	}

	public void readFromNBT(NBTTagCompound nbt)
	{

		int idx;

		front = front.readFromNBT(nbt, "sixFront");

		IInventory inv = getInventory();
		if(inv != null)
		{
			Utils.readFromNBT(nbt, "inv", inv);
		}

		idx = 0;
		for(NbtElectricalLoad electricalLoad : electricalLoadList)
		{
			electricalLoad.readFromNBT(nbt, "");
		}

		for(NbtThermalLoad thermalLoad : thermalLoadList)
		{
			thermalLoad.readFromNBT(nbt, "");
		}
		
		for(Component c : electricalComponentList)	
			if(c instanceof INBTTReady)
				((INBTTReady) c).readFromNBT(nbt, "");
		
		
		for(IProcess process : slowProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).readFromNBT(nbt, "");
		}
		for(IProcess process : electricalProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).readFromNBT(nbt, "");
		}
		for(IProcess process : thermalProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).readFromNBT(nbt, "");
		}
		for(IProcess process : thermalSlowProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).readFromNBT(nbt, "");
		}

	}

	public void writeToNBT(NBTTagCompound nbt)
	{

		int idx;

		front.writeToNBT(nbt, "sixFront");

		IInventory inv = getInventory();
		if(inv != null)
		{
			Utils.writeToNBT(nbt, "inv", inv);
		}

		idx = 0;
		for(NbtElectricalLoad electricalLoad : electricalLoadList)
		{
			electricalLoad.writeToNBT(nbt, "");
		}

		for(NbtThermalLoad thermalLoad : thermalLoadList)
		{
			thermalLoad.writeToNBT(nbt, "");
		}

		for(Component c : electricalComponentList)	
			if(c instanceof INBTTReady)
				((INBTTReady) c).writeToNBT(nbt, "");
		
		
		for(IProcess process : slowProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).writeToNBT(nbt, "");
		}
		for(IProcess process : electricalProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).writeToNBT(nbt, "");
		}
		for(IProcess process : thermalProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).writeToNBT(nbt, "");
		}		
		for(IProcess process : thermalSlowProcessList)
		{
			if(process instanceof INBTTReady)
				((INBTTReady) process).writeToNBT(nbt, "");
		}

	}

	public void reconnect()
	{
		sixNode.reconnect();
	}

	public void needPublish()
	{
		sixNode.setNeedPublish(true);
	}

	public void disconnectJob()
	{
		Eln.simulator.removeAllElectricalComponent(electricalComponentList);
		Eln.simulator.removeAllThermalConnection(thermalConnectionList);

		for(NbtElectricalLoad load : electricalLoadList)
			Eln.simulator.removeElectricalLoad(load);
		for(NbtThermalLoad load : thermalLoadList)
			Eln.simulator.removeThermalLoad(load);

		for(IProcess process : slowProcessList)
			Eln.simulator.removeSlowProcess(process);
		for(IProcess process : electricalProcessList)
			Eln.simulator.removeElectricalProcess(process);
		for(IProcess process : thermalProcessList)
			Eln.simulator.removeThermalFastProcess(process);
		for(IProcess process : thermalSlowProcessList)
			Eln.simulator.removeThermalSlowProcess(process);

	}

	public boolean canConnectRedstone() {
		
		return false;
	}

	public Coordinate getGhostObserverCoordonate()
	{
		return sixNode.coordinate;

	}

	public void ghostDestroyed(int UUID)
	{
		if(UUID == sixNodeElementDescriptor.getGhostGroupUuid()) {
			selfDestroy();
		}
	}

	public boolean ghostBlockActivated(int UUID, EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz)
	{
		if(UUID == sixNodeElementDescriptor.getGhostGroupUuid()) {
			sixNode.onBlockActivated(entityPlayer, this.side, vx, vy, vz);
		}
		return false;
	}

	private void selfDestroy() {
		sixNode.deleteSubBlock(null, side);
	}

	private int uuid = 0;

	public int getUuid() {
		if(uuid == 0) {
			uuid = Utils.getUuid();
		}
		return uuid;
	}

	public boolean useUuid() {
		return uuid != 0;
	}

	public void globalBoot() {

	}

	public void unload() {

	}

	public boolean playerAskToBreak() {
		return true;
	}

}
