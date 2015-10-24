package mods.eln.sixnode.wirelesssignal.repeater;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public class WirelessSignalRepeaterElement extends SixNodeElement {

	WirelessSignalRepeaterProcess slowProcess = new WirelessSignalRepeaterProcess(this);
	
	WirelessSignalRepeaterDescriptor descriptor;

    boolean fromNbt = false;
	
	public WirelessSignalRepeaterElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		
		this.descriptor = (WirelessSignalRepeaterDescriptor) descriptor;

		slowProcessList.add(slowProcess);
		
		front = LRDU.Down;

		IWirelessSignalSpot.spots.add(slowProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return null;
	}

	@Override
	public String thermoMeterString() {
		return null;
	}
    
	@Override
	public void globalBoot() {
		slowProcess.process(0.05);
	}

	@Override
	public void destroy(EntityPlayerMP entityPlayer) {
		unregister();
		super.destroy(entityPlayer);
	}
    
	@Override
	public void unload() {
		super.unload();
		unregister();
	}
	
	void unregister() {
		IWirelessSignalSpot.spots.remove(slowProcess);
	}

	@Override
	public void initialize() {
		if (!fromNbt) {
			slowProcess.process(0.05);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		fromNbt = true;
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
