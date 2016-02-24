package mods.eln.sixnode.electricalbreaker;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalBreakerElement extends SixNodeElement {

    public ElectricalBreakerDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public Resistor switchResistor = new Resistor(aLoad, bLoad);
    public ElectricalBreakerCutProcess cutProcess = new ElectricalBreakerCutProcess(this);

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public float voltageMax = (float) Eln.SVU, voltageMin = 0;

    boolean switchState = false;
    double currantMax = 0;
    boolean nbtBoot = false;

    public ElectricalCableDescriptor cableDescriptor = null;

    public static final byte setVoltageMaxId = 1;
    public static final byte setVoltageMinId = 2;
    public static final byte toogleSwitchId = 3;

	public ElectricalBreakerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

		front = LRDU.Down;
    	electricalLoadList.add(aLoad);
    	electricalLoadList.add(bLoad);
    	electricalComponentList.add(switchResistor);
    	electricalComponentList.add(new Resistor(bLoad, null).pullDown());
    	electricalComponentList.add(new Resistor(aLoad, null).pullDown());

    	electricalProcessList.add(cutProcess);

    	this.descriptor = (ElectricalBreakerDescriptor) descriptor;
	}

	public SixNodeElementInventory getInventory() {
		return inventory;
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
        voltageMax = nbt.getFloat("voltageMax");
        voltageMin = nbt.getFloat("voltageMin");
        nbtBoot = true;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) ((front.toInt() << 0)));
		nbt.setBoolean("switchState", switchState);
		nbt.setFloat("voltageMax", voltageMax);
        nbt.setFloat("voltageMin", voltageMin);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return aLoad;
		if (front.inverse() == lrdu) return bLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId) == null) return 0;
		if (front == lrdu) return NodeBase.maskElectricalAll;
		if (front.inverse() == lrdu) return NodeBase.maskElectricalAll;

		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("Ua:", aLoad.getU()) + Utils.plotVolt("Ub:", bLoad.getU()) + Utils.plotVolt("I:", aLoad.getCurrent());
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
	    	stream.writeFloat((voltageMax));
	    	stream.writeFloat((voltageMin));

	    	Utils.serialiseItemStack(stream, inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSwitchState(boolean state) {
		if (state == switchState) return;
		play(new SoundCommand("random.click").mulVolume(0.3F, 0.6f).smallRange());
		switchState = state;
		refreshSwitchResistor();
		needPublish(); 
	}
	
	public void refreshSwitchResistor() {
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if (cableDescriptor == null || !switchState) {
			switchResistor.ultraImpedance();
		} else {
			cableDescriptor.applyTo(switchResistor);
		}
	}
	
	public boolean getSwitchState() {
		return switchState;
	}
	
	@Override
	public void initialize() {
    	computeElectricalLoad();
    	setSwitchState(switchState);
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
		reconnect();
	}

	public void computeElectricalLoad() {
		ItemStack cable = inventory.getStackInSlot(ElectricalBreakerContainer.cableSlotId);
		
		if (!nbtBoot)setSwitchState(false);
		nbtBoot = false;
		
		cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
		if (cableDescriptor == null) {
			aLoad.highImpedance();
			bLoad.highImpedance();	
		} else {
			cableDescriptor.applyTo(aLoad);
			cableDescriptor.applyTo(bLoad);
			currantMax = cableDescriptor.electricalMaximalCurrent;
		}
		refreshSwitchResistor();
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			
			return true;	
		} else if (Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	}

    	if (Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	}
    	if (Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem())) {
    		return false;
    	} else {
			//setSwitchState(true);
			//return true;
		}
		//front = LRDU.fromInt((front.toInt() + 1)&3);
    	return false;
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte()) {
                case setVoltageMaxId:
                    voltageMax = stream.readFloat();
                    needPublish();
                    break;
                case setVoltageMinId:
                    voltageMin = stream.readFloat();
                    needPublish();
                    break;
                case toogleSwitchId:
                    setSwitchState(! getSwitchState());
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
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalBreakerContainer(player, inventory);
	}
}
