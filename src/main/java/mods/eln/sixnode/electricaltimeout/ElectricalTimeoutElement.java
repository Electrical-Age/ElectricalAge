package mods.eln.sixnode.electricaltimeout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalTimeoutElement extends SixNodeElement {

	public ElectricalTimeoutElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Left;
    	electricalLoadList.add(inputGate);
    	electricalLoadList.add(outputGate);
    	electricalComponentList.add(outputGateProcess);
    	thermalProcessList.add(slowProcess);

    	this.descriptor = (ElectricalTimeoutDescriptor) descriptor;
	}

	public ElectricalTimeoutDescriptor descriptor;

	public NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate",false);	
	public NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");	
	public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
	
	public ElectricalTimeoutProcess slowProcess = new ElectricalTimeoutProcess(this);
	
	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	double timeOutCounter = 0, timeOutValue = 2;
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        timeOutValue = nbt.getFloat("timeOutValue");
        timeOutCounter = nbt.getFloat("timeOutCounter");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte)((front.toInt() << 0)));
		nbt.setFloat("timeOutValue", (float) timeOutValue);
		nbt.setFloat("timeOutCounter", (float) timeOutCounter);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu) return inputGate;
		if(front.inverse() == lrdu) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return NodeBase.maskElectricalInputGate;
		if(front.inverse() == lrdu) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return inputGate.plot("Input:") + outputGate.plot("Output:");
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) timeOutValue);
			stream.writeFloat((float) timeOutCounter);
			stream.writeBoolean(slowProcess.inputState);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		
		if(Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		//front = LRDU.fromInt((front.toInt() + 1)&3);
    	return false;
	}

	public static final byte resetId = 1;
	public static final byte setTimeOutValueId = 2;
	public static final byte setId = 3;
	
	void set() {
		timeOutCounter = timeOutValue;
		needPublish();
	}
	
	void reset() {
		timeOutCounter = 0.0;
		needPublish();
	}
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte()) {
			case resetId:
				reset();
				break;			
			case setId:
				set();
				break;
			case setTimeOutValueId:
				timeOutValue = stream.readFloat();
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
