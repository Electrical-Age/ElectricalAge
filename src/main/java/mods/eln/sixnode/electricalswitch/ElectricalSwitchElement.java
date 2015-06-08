package mods.eln.sixnode.electricalswitch;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalSwitchElement extends SixNodeElement {

    public ElectricalSwitchDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public ResistorSwitch switchResistor = new ResistorSwitch("switchRes", aLoad, bLoad);

    VoltageStateWatchDog voltageWatchDogA = new VoltageStateWatchDog();
    VoltageStateWatchDog voltageWatchDogB = new VoltageStateWatchDog();
//	ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    boolean switchState = false;

	public ElectricalSwitchElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

		switchResistor.mustUseUltraImpedance();
    	electricalLoadList.add(aLoad);
    	electricalLoadList.add(bLoad);
    	electricalComponentList.add(switchResistor);
    	electricalComponentList.add(new Resistor(bLoad, null).pullDown());
    	electricalComponentList.add(new Resistor(aLoad, null).pullDown());

    	this.descriptor = (ElectricalSwitchDescriptor) descriptor;
    	
    	WorldExplosion exp = new WorldExplosion(this).cableExplosion();

    //	slowProcessList.add(currentWatchDog);
    	slowProcessList.add(voltageWatchDogA);
    	slowProcessList.add(voltageWatchDogB);
    	
    	//currentWatchDog.set(switchResistor).setIAbsMax(this.descriptor.maximalPower/this.descriptor.nominalVoltage).set(exp);
    	voltageWatchDogA.set(aLoad).setUNominalMirror(this.descriptor.nominalVoltage).set(exp);
    	voltageWatchDogB.set(bLoad).setUNominalMirror(this.descriptor.nominalVoltage).set(exp);
	}

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        switchState = nbt.getBoolean("switchState");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte)((front.toInt() << 0)));
		nbt.setBoolean("switchState", switchState);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return aLoad;
		if (front.inverse() == lrdu) return bLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		//return thermalLoad;
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return descriptor.getNodeMask();
		if (front.inverse() == lrdu) return descriptor.getNodeMask();

		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("Ua:", aLoad.getU()) + Utils.plotVolt("Ub:", bLoad.getU()) + Utils.plotAmpere("I:", aLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		//return Utils.plotCelsius("T:",thermalLoad.Tc);
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(switchState);
	    	stream.writeShort((short)((aLoad.getU()) * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short)((bLoad.getU()) * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short)(aLoad.getCurrent() * NodeBase.networkSerializeIFactor));
	    	//stream.writeShort((short)(thermalLoad.Tc * NodeBase.networkSerializeTFactor));
	    	stream.writeShort(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSwitchState(boolean state) {
		switchState = state;
		switchResistor.setState(state);
		needPublish();
	}
	
	@Override
	public void initialize() {
    	//descriptor.thermal.applyTo(thermalLoad);
      	
    	descriptor.applyTo(aLoad);
    	descriptor.applyTo(bLoad);
    	
    	switchResistor.setR(new Resistance(descriptor.electricalRs));
    	
    	setSwitchState(switchState);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		} else if (Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	}
    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) { 
    		return false;
    	}
    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	} else {
			setSwitchState(!switchState);
			//playSoundEffect("random.click", 0.3F, 0.6F);
			play(new SoundCommand("random.click").mulVolume(0.3F, 0.6f).smallRange());
			return true;
		}
		//front = LRDU.fromInt((front.toInt()+1)&3);
	}
}
