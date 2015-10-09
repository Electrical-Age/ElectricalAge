package mods.eln.transparentnode.electricalmachine;

import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ElectricalStackMachineProcess.ElectricalStackMachineProcessObserver;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalMachineElement extends TransparentNodeElement implements ElectricalStackMachineProcessObserver {

	TransparentNodeElementInventory inventory;

	NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");	
	Resistor electricalResistor = new Resistor(electricalLoad,null);	
	
	//NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	
	ElectricalStackMachineProcess slowRefreshProcess;
	
	//ElectricalResistorHeatThermalLoad heatingProcess = new ElectricalResistorHeatThermalLoad(electricalResistor, thermalLoad);
	
	//VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad motorWatchdog = new VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad(inventory, motorSlotId, electricalLoad);
	
	ElectricalMachineSlowProcess slowProcess = new ElectricalMachineSlowProcess(this);
	boolean powerOn = false;
	ElectricalMachineDescriptor descriptor;
	public int inSlotId = 0, outSlotId = 0, boosterSlotId = 1;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    double efficiency = 1.0;

    public ElectricalMachineElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (ElectricalMachineDescriptor) descriptor;
		inSlotId += this.descriptor.outStackCount;
		boosterSlotId += this.descriptor.outStackCount;
		inventory = new ElectricalMachineInventory(2 + this.descriptor.outStackCount, 64, this);
		
		slowRefreshProcess = new ElectricalStackMachineProcess(
				inventory, inSlotId, outSlotId, this.descriptor.outStackCount,
				electricalResistor, Double.POSITIVE_INFINITY, this.descriptor.recipe);
		
		electricalLoadList.add(electricalLoad);
		electricalComponentList.add(electricalResistor);
		//thermalLoadList.add(thermalLoad);
		slowProcessList.add(slowRefreshProcess);
		//thermalProcessList.add(heatingProcess);
		slowProcessList.add(slowProcess);
		slowRefreshProcess.setObserver(this);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2, 1));
		
		WorldExplosion exp = new WorldExplosion(this).machineExplosion();
		slowProcessList.add(voltageWatchdog.set(electricalLoad).setUNominal(this.descriptor.nominalU).set(exp));
	}
    
	@Override
	public IInventory getInventory() {
		return inventory;
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalMachineContainer(this.node, player, inventory, descriptor);
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return electricalLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down) return 0;
		if (!descriptor.powerLrdu(side, front)) return 0;
		return NodeBase.maskElectricalPower;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getCurrent());
	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return null;//Utils.plotCelsius("T", thermalLoad.Tc);
	}

	@Override
	public void initialize() {
		inventoryChange(getInventory());
		connect();
	}
	
	@Override
	public void inventoryChange(IInventory inventory) {
		super.inventoryChange(inventory);
		setPhysicalValue();
		needPublish();
	}
	
	public void setPhysicalValue() {
		ItemStack stack;
		
		int boosterCount = 0;
		stack = getInventory().getStackInSlot(boosterSlotId);
		if (stack != null) {
			boosterCount = stack.stackSize;
		}
		double speedUp = Math.pow(descriptor.boosterSpeedUp, boosterCount);
		slowRefreshProcess.setEfficiency(Math.pow(descriptor.boosterEfficiency, boosterCount));
		slowRefreshProcess.setSpeedUp(speedUp);
		
		//descriptor.applyTo(thermalLoad);
		descriptor.applyTo(electricalLoad);
		descriptor.applyTo(slowRefreshProcess);
		
		//thermalLoad.setRp(thermalLoad.Rp / speedUp);
		//electricalLoad.setRp(electricalLoad.getRp() / Math.pow(descriptor.boosterSpeedUp, boosterCount));
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}

	public void networkSerialize(java.io.DataOutputStream stream) {
		super.networkSerialize(stream);
		double fPower = electricalResistor.getP() / descriptor.nominalP;
		if (electricalResistor.getP() < 11) fPower = 0.0;
		if (fPower > 1.9) fPower = 1.9;
		try {
			stream.writeByte((int)(fPower * 64));
			serialiseItemStack(stream, inventory.getStackInSlot(inSlotId));
			serialiseItemStack(stream, inventory.getStackInSlot(outSlotId));
			stream.writeFloat((float) slowRefreshProcess.processState());
			stream.writeFloat((float) slowRefreshProcess.processStatePerSecond());
			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
			stream.writeFloat((float)(electricalLoad.getU() / descriptor.nominalU));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("powerOn", powerOn);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerOn = nbt.getBoolean("powerOn");
	}

	@Override
	public void done(ElectricalStackMachineProcess who) {
		needPublish();
		if (descriptor.endSound != null)
			play(descriptor.endSound);
	}
}
