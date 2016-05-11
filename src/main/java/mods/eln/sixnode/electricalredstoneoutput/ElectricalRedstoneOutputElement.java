package mods.eln.sixnode.electricalredstoneoutput;

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
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalRedstoneOutputElement extends SixNodeElement {

    public NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate");
    public ElectricalRedstoneOutputSlowProcess slowProcess = new ElectricalRedstoneOutputSlowProcess(this);

    int redstoneValue = 0;

	public ElectricalRedstoneOutputElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(inputGate);
    	slowProcessList.add(slowProcess);
	}

	@Override
	public int isProvidingWeakPower() {
		return redstoneValue;
	}
	
	public boolean refreshRedstone() { 
		int newValue = (int)(inputGate.getU() * 15.0 / Eln.SVU + 0.5);
		if (newValue != redstoneValue) {
			redstoneValue = newValue;
			notifyNeighbor();
			needPublish();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canConnectRedstone() {
		return true;
	}
	
	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        redstoneValue = nbt.getInteger("redstoneValue");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setInteger("redstoneValue", redstoneValue);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu.left()) return inputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu.left()) return NodeBase.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U:", inputGate.getU()) + Utils.plotAmpere("I:", inputGate.getCurrent()) ;
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte(redstoneValue);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}
}
