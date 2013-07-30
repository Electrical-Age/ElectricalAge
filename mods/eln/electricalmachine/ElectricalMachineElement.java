package mods.eln.electricalmachine;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.ElectricalMotorDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorGrounded;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ElectricalResistorWithCounter;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ElectricalStackMachineProcess.ElectricalStackMachineProcessObserver;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.RegulatorThermalLoadToElectricalResistor;
import mods.eln.sim.RegulatorType;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalRegulator;
import mods.eln.sim.ThermalResistor;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class ElectricalMachineElement extends TransparentNodeElement implements ITemperatureWatchdogDescriptor,IThermalDestructorDescriptor,ElectricalStackMachineProcessObserver{

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3 , 64, this);

	
	

	
	NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");	
	ElectricalResistorGrounded electricalResistor = new ElectricalResistorGrounded(electricalLoad);	
	
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	
	ElectricalLoadHeatThermalLoadProcess heatEToTProcess = new ElectricalLoadHeatThermalLoadProcess(electricalLoad,thermalLoad);
	ElectricalStackMachineProcess slowRefreshProcess;
	
	ElectricalResistorHeatThermalLoad heatingProcess = new ElectricalResistorHeatThermalLoad(electricalResistor, thermalLoad);
	
	//VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad motorWatchdog = new VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad(inventory, motorSlotId, electricalLoad);
	
	NodeThermalWatchdogProcess thermalWatchdogProcess = new NodeThermalWatchdogProcess(this.node, this,this, thermalLoad);
	ElectricalMachineSlowProcess slowProcess = new ElectricalMachineSlowProcess(this);
	boolean powerOn = false;
	ElectricalMachineDescriptor descriptor;
	public ElectricalMachineElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (ElectricalMachineDescriptor) descriptor;
		
		slowRefreshProcess = new ElectricalStackMachineProcess(
				inventory,ElectricalMachineContainer.inSlotId,ElectricalMachineContainer.outSlotId,1,
				electricalResistor,Double.POSITIVE_INFINITY,this.descriptor.recipe);
		
		electricalLoadList.add(electricalLoad);
		electricalProcessList.add(electricalResistor);
		thermalLoadList.add(thermalLoad);
		thermalProcessList.add(heatEToTProcess);
		slowProcessList.add(slowRefreshProcess);
		thermalProcessList.add(heatingProcess);
		slowProcessList.add(thermalWatchdogProcess);
		slowProcessList.add(slowProcess);
		slowRefreshProcess.setObserver(this);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2, 1));
	}

	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalMachineContainer(this.node,player, inventory);
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {

		return electricalLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return 0;
		if(descriptor.powerLrdu(side, front) == false) return 0;
		return NodeBase.maskElectricalPower;
	}


	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotUIP(electricalLoad.Uc, electricalLoad.getCurrent());
	}
	
	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T", thermalLoad.Tc);
	}

	@Override
	public void initialize() {
	
		
		inventoryChange(getInventory());
		
		connect();
	}
	
	@Override
	public void inventoryChange(IInventory inventory) {
		// TODO Auto-generated method stub
		super.inventoryChange(inventory);

		
		setPhysicalValue();

		needPublish();

	}
	
	public void setPhysicalValue()
	{
		ItemStack stack;
		
		int boosterCount = 0;
		stack = getInventory().getStackInSlot(ElectricalMachineContainer.boosterSlotId);
		if(stack != null)
		{
			boosterCount = stack.stackSize;
		}
		double speedUp = Math.pow(descriptor.boosterSpeedUp, boosterCount);
		slowRefreshProcess.setEfficiency(Math.pow(descriptor.boosterEfficiency, boosterCount));
		slowRefreshProcess.setSpeedUp(speedUp);
		
		descriptor.applyTo(thermalLoad);
		descriptor.applyTo(electricalLoad);
		descriptor.applyTo(slowRefreshProcess);
		
		thermalLoad.setRp(thermalLoad.Rp / speedUp);
		//electricalLoad.setRp(electricalLoad.getRp()/ Math.pow(descriptor.boosterSpeedUp, boosterCount));
	}
	
	double efficiency = 1.0;
	

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}


	public void networkSerialize(java.io.DataOutputStream stream)
	{
		super.networkSerialize(stream);
		double fPower = electricalResistor.getP() / descriptor.nominalP;
		if(electricalResistor.getP() < 11) fPower = 0.0;
		if(fPower > 1.9)fPower = 1.9;
		try {
			stream.writeByte((int)(fPower*64));
			serialiseItemStack(stream, inventory.getStackInSlot(ElectricalMachineContainer.inSlotId));
			serialiseItemStack(stream, inventory.getStackInSlot(ElectricalMachineContainer.outSlotId));
			stream.writeFloat((float) slowRefreshProcess.processState());
			stream.writeFloat((float) slowRefreshProcess.processStatePerSecond());
			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
			stream.writeFloat((float)(electricalLoad.Uc / descriptor.nominalU));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setBoolean(str + "powerOn", powerOn);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		powerOn = nbt.getBoolean(str + "powerOn");
	}

	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 2;
	}

	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.05;
	}

	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return descriptor.thermal.warmLimit;
	}

	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return descriptor.thermal.coolLimit;
	}

	@Override
	public void done(ElectricalStackMachineProcess who) {
		// TODO Auto-generated method stub
		needPublish();
	}
	


}
