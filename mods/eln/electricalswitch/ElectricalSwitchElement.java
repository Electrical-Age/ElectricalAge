package mods.eln.electricalswitch;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;


import mods.eln.Eln;
import mods.eln.misc.Coordonate;
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
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorCableHeatThermalLoad;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.sound.SoundServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalSwitchElement extends SixNodeElement implements ITemperatureWatchdogDescriptor, IThermalDestructorDescriptor, IVoltageWatchdogDescriptor, IVoltageDestructorDescriptor {

	public ElectricalSwitchElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	electricalLoadList.add(aLoad);
    	electricalLoadList.add(bLoad);
    	electricalProcessList.add(switchResistor);
    	thermalLoadList.add(thermalLoad);
    	//thermalProcessList.add(aETProcess);
    	//thermalProcessList.add(bETProcess);
    	thermalProcessList.add(switchResistorETProcess);
    	slowProcessList.add(thermalWatchdog);
    	slowProcessList.add(voltageAWatchdog);
    	slowProcessList.add(voltageBWatchdog);
    	this.descriptor = (ElectricalSwitchDescriptor) descriptor;
	}


	public ElectricalSwitchDescriptor descriptor;
	public NodeElectricalLoad aLoad = new NodeElectricalLoad("aLoad");
	public NodeElectricalLoad bLoad = new NodeElectricalLoad("bLoad");
	public ElectricalResistor switchResistor = new ElectricalResistor(aLoad, bLoad);
	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	//public ElectricalLoadHeatThermalLoadProcess aETProcess = new ElectricalLoadHeatThermalLoadProcess(aLoad, thermalLoad);
	//public ElectricalLoadHeatThermalLoadProcess bETProcess = new ElectricalLoadHeatThermalLoadProcess(bLoad, thermalLoad);
	public ElectricalResistorCableHeatThermalLoad switchResistorETProcess = new ElectricalResistorCableHeatThermalLoad(switchResistor, thermalLoad);

	public NodeThermalWatchdogProcess thermalWatchdog = new NodeThermalWatchdogProcess(sixNode, this, this, thermalLoad);
	public NodeVoltageWatchdogProcess voltageAWatchdog = new NodeVoltageWatchdogProcess(sixNode, this, this, aLoad);
	public NodeVoltageWatchdogProcess voltageBWatchdog = new NodeVoltageWatchdogProcess(sixNode, this, this, bLoad);
	
	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	boolean switchState = false;
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		super.readFromNBT(nbt, str);
        byte value = nbt.getByte(str + "front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        switchState = nbt.getBoolean(str + "switchState");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		super.writeToNBT(nbt, str);
		nbt.setByte(str + "front", (byte)((front.toInt() << 0)));
		nbt.setBoolean(str + "switchState", switchState);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu) return aLoad;
		if(front.inverse() == lrdu) return bLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return descriptor.getNodeMask();
		if(front.inverse() == lrdu) return descriptor.getNodeMask();

		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("Ua:", aLoad.Uc) + Utils.plotVolt("Ub:", bLoad.Uc) + Utils.plotVolt("I:", aLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return Utils.plotCelsius("T:",thermalLoad.Tc);
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(switchState);
	    	stream.writeShort((short)((aLoad.Uc) * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short)((bLoad.Uc) * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short)(aLoad.getCurrent() * NodeBase.networkSerializeIFactor));
	    	stream.writeShort((short)(thermalLoad.Tc * NodeBase.networkSerializeTFactor));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSwitchState(boolean state) {
		switchState = state;
		descriptor.applyTo(switchResistor,state);
		needPublish();
	}
	
	@Override
	public void initialize() {
    	descriptor.thermal.applyTo(thermalLoad);
      	
    	descriptor.applyTo(aLoad);
    	descriptor.applyTo(bLoad);
    	
    	setSwitchState(switchState);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Eln.playerManager.get(entityPlayer).getInteractEnable()) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
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
			setSwitchState(! switchState);
			//playSoundEffect("random.click", 0.3F, 0.6F);
			SoundServer.playFromCoord(getCoordonate(),"random.click", 0.3F, 0.6F, 1, 7);
			return true;
		}
		//front = LRDU.fromInt((front.toInt()+1)&3);
	}

	@Override
	public double getThermalDestructionMax() {
		return 2;
	}

	@Override
	public double getThermalDestructionStart() {
		return 1;
	}

	@Override
	public double getThermalDestructionPerOverflow() {
		return 0.2;
	}

	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		return 0.2;
	}

	@Override
	public double getTmax() {
		return descriptor.thermal.warmLimit;
	}

	@Override
	public double getTmin() {
		return descriptor.thermal.coolLimit;
	}

	@Override
	public double getVoltageDestructionMax() {
		return 2;
	}

	@Override
	public double getVoltageDestructionStart() {
		return 1;
	}

	@Override
	public double getVoltageDestructionPerOverflow() {
		return 0.2;
	}

	@Override
	public double getVoltageDestructionProbabilityPerOverflow() {
		return 0.2;
	}

	@Override
	public double getUmax() {
		return descriptor.maximalVoltage;
	}

	@Override
	public double getUmin() {
		return -descriptor.maximalVoltage;
	}
}
