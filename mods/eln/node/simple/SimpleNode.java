package mods.eln.node.simple;

import java.util.ArrayList;

import cpw.mods.fml.common.registry.GameRegistry.ObjectHolder;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.INodeInfo;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.nbt.NbtThermalLoad;

public abstract class SimpleNode extends NodeBase {

	public EntityPlayerMP removedByPlayer;

	@Override
	public void initializeFromThat(Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
		initialize();
	}

	@Override
	public void initializeFromNBT() {
		initialize();
	}

	public abstract void initialize();

	
	
	
	public ArrayList<IProcess> slowProcessList = new ArrayList<IProcess>(4);

	public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
	public ArrayList<State> electricalLoadList = new ArrayList<State>(4);

	public ArrayList<IProcess> thermalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
	public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);

	@Override
	public void connectJob()
	{
		super.connectJob();
		
		Eln.simulator.addAllSlowProcess(slowProcessList);

		Eln.simulator.addAllElectricalComponent(electricalComponentList);
		for (State load : electricalLoadList)
			Eln.simulator.addElectricalLoad(load);
		Eln.simulator.addAllElectricalProcess(electricalProcessList);

		Eln.simulator.addAllThermalConnection(thermalConnectionList);
		for (NbtThermalLoad load : thermalLoadList)
			Eln.simulator.addThermalLoad(load);
		Eln.simulator.addAllThermalProcess(thermalProcessList);
	}
	
	@Override
	public void disconnectJob()
	{
		super.disconnectJob();

		
		Eln.simulator.removeAllSlowProcess(slowProcessList);

		Eln.simulator.removeAllElectricalComponent(electricalComponentList);
		for (State load : electricalLoadList)
			Eln.simulator.removeElectricalLoad(load);
		Eln.simulator.removeAllElectricalProcess(electricalProcessList);

		Eln.simulator.removeAllThermalConnection(thermalConnectionList);
		for (NbtThermalLoad load : thermalLoadList)
			Eln.simulator.removeThermalLoad(load);
		Eln.simulator.removeAllThermalProcess(thermalProcessList);
	}

	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		for (State electricalLoad : electricalLoadList) {
			if (electricalLoad instanceof INBTTReady) ((INBTTReady) electricalLoad).readFromNBT(nbt, "");
		}

		for (NbtThermalLoad thermalLoad : thermalLoadList) {
			thermalLoad.readFromNBT(nbt, "");
		}

		for (Component c : electricalComponentList)
			if (c instanceof INBTTReady)
				((INBTTReady) c).readFromNBT(nbt, "");

		for (IProcess process : slowProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
		}
		for (IProcess process : electricalProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
		}
		for (IProcess process : thermalProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).readFromNBT(nbt, "");
		}
	}

	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		for (State electricalLoad : electricalLoadList) {
			if (electricalLoad instanceof INBTTReady) ((INBTTReady) electricalLoad).writeToNBT(nbt, "");
		}

		for (NbtThermalLoad thermalLoad : thermalLoadList) {
			thermalLoad.writeToNBT(nbt, "");
		}

		for (Component c : electricalComponentList)
			if (c instanceof INBTTReady)
				((INBTTReady) c).writeToNBT(nbt, "");

		for (IProcess process : slowProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
		}
		for (IProcess process : electricalProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
		}
		for (IProcess process : thermalProcessList) {
			if (process instanceof INBTTReady) ((INBTTReady) process).writeToNBT(nbt, "");
		}

	}
}
