package mods.eln.sixnode.rs485cable;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;

public class Rs485CableElement extends SixNodeElement {

	public Rs485CableDescriptor descriptor;
	
	public Rs485CableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (Rs485CableDescriptor) descriptor;
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
		return NodeBase.maskRs485;
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
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
	}
}
