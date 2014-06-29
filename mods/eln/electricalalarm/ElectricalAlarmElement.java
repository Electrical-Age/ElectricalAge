package mods.eln.electricalalarm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalAlarmElement extends SixNodeElement {

	ElectricalAlarmDescriptor descriptor;
	public ElectricalAlarmElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		front = LRDU.Down;
    	electricalLoadList.add(inputGate);
    	slowProcessList.add(slowProcess);
    	this.descriptor = (ElectricalAlarmDescriptor) descriptor;
	}

	public NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate",true);
	public ElectricalAlarmSlowProcess slowProcess = new ElectricalAlarmSlowProcess(this);
	LRDU front;
	
	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt ) {
		super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value>>0) & 0x3);
        mute = nbt.getBoolean("mute");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) ((front.toInt()<<0)));
		nbt.setBoolean("mute", mute);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu) return inputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu) return NodeBase.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U:", inputGate.getU()) + Utils.plotAmpere("I:", inputGate.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	boolean warm = false;
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte( (front.toInt()<<4) + (warm ? 1 : 0));
			stream.writeBoolean(mute);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setWarm(boolean value) {
		if(warm != value) {
			warm = value;
			sixNode.recalculateLightValue();
			needPublish();
		}
	}
	
	@Override
	public void initialize() {
	}

	public int getLightValue() {
		return warm ? descriptor.light : 0;
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
		return false;
	}
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	boolean mute = false;
	
	public static final byte clientSoundToggle = 1;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()){
			case clientSoundToggle:
				mute = ! mute;
				needPublish();
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
