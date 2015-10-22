package mods.eln.sixnode.electricalrelay;

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
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalRelayElement extends SixNodeElement {

    public ElectricalRelayDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public Resistor switchResistor = new Resistor(aLoad, bLoad);
    public NbtElectricalGateInput gate = new NbtElectricalGateInput("gate");
    public ElectricalRelayGateProcess gateProcess = new ElectricalRelayGateProcess(this, "GP", gate);

    VoltageStateWatchDog voltageWatchDogA = new VoltageStateWatchDog();
    VoltageStateWatchDog voltageWatchDogB = new VoltageStateWatchDog();
    //ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    boolean switchState = false, defaultOutput = false;

    public ElectricalCableDescriptor cableDescriptor = null;

    public static final byte toogleOutputDefaultId = 3;

	public ElectricalRelayElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	this.descriptor = (ElectricalRelayDescriptor) descriptor;
    	
		front = LRDU.Left;
    	electricalLoadList.add(aLoad);
    	electricalLoadList.add(bLoad);
    	electricalComponentList.add(switchResistor);
    	electricalProcessList.add(gateProcess);
    	electricalLoadList.add(gate);
    	
    	electricalComponentList.add(new Resistor(bLoad, null).pullDown());
    	electricalComponentList.add(new Resistor(aLoad, null).pullDown());

    	//slowProcessList.add(currentWatchDog);
    	slowProcessList.add(voltageWatchDogA);
    	slowProcessList.add(voltageWatchDogB);
    	
    	WorldExplosion exp = new WorldExplosion(this).cableExplosion();
    	
    	//currentWatchDog.set(switchResistor).setIAbsMax(this.descriptor.cable.electricalMaximalCurrent).set(exp);
    	voltageWatchDogA.set(aLoad).setUNominal(this.descriptor.cable.electricalNominalVoltage).set(exp);
    	voltageWatchDogB.set(bLoad).setUNominal(this.descriptor.cable.electricalNominalVoltage).set(exp);
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
        defaultOutput = nbt.getBoolean("defaultOutput");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte)((front.toInt() << 0)));
		nbt.setBoolean("switchState", switchState);
		nbt.setBoolean("defaultOutput", defaultOutput);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front.left() == lrdu) return aLoad;
		if (front.right() == lrdu) return bLoad;
		if (front == lrdu) return gate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front.left() == lrdu) return descriptor.cable.getNodeMask();
		if (front.right() == lrdu) return descriptor.cable.getNodeMask();
		if (front == lrdu) return NodeBase.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("Ua:", aLoad.getU()) + Utils.plotVolt("Ub:", bLoad.getU()) + Utils.plotAmpere("I:", aLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(switchState);
			stream.writeBoolean(defaultOutput);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSwitchState(boolean state) {
		if (state == switchState) return;
		switchState = state;
		refreshSwitchResistor();
		play(new SoundCommand("random.click").mulVolume(0.1F, 2.0F).smallRange());
		needPublish(); 
	}
	
	public void refreshSwitchResistor() {
		if(!switchState) {
			switchResistor.ultraImpedance();
		} else {
			descriptor.applyTo(switchResistor);
		}
	}
	
	public boolean getSwitchState() {
		return switchState;
	}
	
	@Override
	public void initialize() {
    	computeElectricalLoad();
    	
    	setSwitchState(switchState);
    	refreshSwitchResistor();
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
	}

	public void computeElectricalLoad() {
		descriptor.applyTo(aLoad);
		descriptor.applyTo(bLoad);		
		refreshSwitchResistor();
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
    	return false;
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte()) {
                case toogleOutputDefaultId:
                    defaultOutput = ! defaultOutput;
                    needPublish();
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
}
