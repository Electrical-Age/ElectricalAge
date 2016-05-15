package mods.eln.sixnode.electricasensor;

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
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalSensorElement extends SixNodeElement {

    VoltageStateWatchDog voltageWatchDog = new VoltageStateWatchDog();
    //ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    public ElectricalSensorDescriptor descriptor;
    public NbtElectricalLoad aLoad, bLoad;
    public NbtElectricalLoad outputGate = new NbtElectricalLoad("outputGate");
    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    public ElectricalSensorProcess slowProcess = new ElectricalSensorProcess(this);

    public Resistor resistor;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    static final byte dirNone = 0, dirAB = 1, dirBA = 2;
    byte dirType = dirNone;
    static final byte powerType = 0, currantType = 1, voltageType = 2;
    int typeOfSensor = voltageType;
    float lowValue = 0, highValue = 50;

    public static final byte setTypeOfSensorId = 1;
    public static final byte setValueId = 2;
    public static final byte setDirType = 3;

	public ElectricalSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ElectricalSensorDescriptor) descriptor;

		aLoad = new NbtElectricalLoad("aLoad");
		electricalLoadList.add(aLoad);
		WorldExplosion exp = new WorldExplosion(this).cableExplosion();

		if (!this.descriptor.voltageOnly) {
			bLoad = new NbtElectricalLoad("bLoad");
			resistor = new Resistor(aLoad, bLoad);
			electricalLoadList.add(bLoad);
			electricalComponentList.add(resistor);

		//	slowProcessList.add(currentWatchDog);
		//	currentWatchDog.set(resistor).set(exp);
			
		}
		electricalLoadList.add(outputGate);
		electricalComponentList.add(outputGateProcess);
		electricalProcessList.add(slowProcess);

		slowProcessList.add(voltageWatchDog);
		voltageWatchDog.set(aLoad).set(exp);
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
		typeOfSensor = nbt.getByte("typeOfSensor");
		lowValue = nbt.getFloat("lowValue");
		highValue = nbt.getFloat("highValue");
		dirType = nbt.getByte("dirType");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) (front.toInt() << 0));
		nbt.setByte("typeOfSensor", (byte) typeOfSensor);
		nbt.setFloat("lowValue", lowValue);
		nbt.setFloat("highValue", highValue);
		nbt.setByte("dirType", dirType);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (!descriptor.voltageOnly) {
			if (front.left() == lrdu) return aLoad;
			if (front.right() == lrdu) return bLoad;
			if (front == lrdu) return outputGate;
		} else {
			if (front.inverse() == lrdu) return aLoad;
			if (front == lrdu) return outputGate;
		}
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		boolean cable = inventory.getStackInSlot(ElectricalSensorContainer.cableSlotId) != null;
		if (!descriptor.voltageOnly) {
			if (front.left() == lrdu && cable) return NodeBase.maskElectricalAll;
			if (front.right() == lrdu && cable) return NodeBase.maskElectricalAll;
			if (front == lrdu) return NodeBase.maskElectricalOutputGate;
		} else {
			if (front.inverse() == lrdu && cable) return NodeBase.maskElectricalAll;
			if (front == lrdu) return NodeBase.maskElectricalOutputGate;
		}
		return 0;
	}

	@Override
	public String multiMeterString() {
		if (!descriptor.voltageOnly)
			return Utils.plotUIP(aLoad.getU(), aLoad.getCurrent());
		else
			return Utils.plotVolt("Uin:", aLoad.getU()) + Utils.plotVolt("Uout:", outputGate.getU());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte(typeOfSensor);
			stream.writeFloat(lowValue);
			stream.writeFloat(highValue);
			stream.writeByte(dirType);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(ElectricalSensorContainer.cableSlotId));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		Eln.instance.signalCableDescriptor.applyTo(outputGate);
		computeElectricalLoad();
		Eln.applySmallRs(aLoad);
		if (bLoad != null) Eln.applySmallRs(bLoad);
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
		reconnect();
	}

	public void computeElectricalLoad() {
		//if (!descriptor.voltageOnly)
		{
			ItemStack cable = inventory.getStackInSlot(ElectricalSensorContainer.cableSlotId);
			ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);

			if (cableDescriptor == null) {
				if (resistor != null) resistor.highImpedance();
			//	currentWatchDog.setIAbsMax(100000);
				voltageWatchDog.setUNominal(1000000000);
			} else {
				if (resistor != null) cableDescriptor.applyTo(resistor, 2);
			//	currentWatchDog.setIAbsMax(cableDescriptor.electricalMaximalCurrent);
				voltageWatchDog.setUNominal(cableDescriptor.electricalNominalVoltage);
			}
		}
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

		//front = LRDU.fromInt((front.toInt() + 1) & 3);
		return false;
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch (stream.readByte()) {
                case setTypeOfSensorId:
                    typeOfSensor = stream.readByte();
                    needPublish();
                    break;
                case setValueId:
                    lowValue = stream.readFloat();
                    highValue = stream.readFloat();
                    if (lowValue == highValue) highValue += 0.0001;
                    needPublish();
                    break;
                case setDirType:
                    dirType = stream.readByte();
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
		return new ElectricalSensorContainer(player, inventory, descriptor);
	}
}
