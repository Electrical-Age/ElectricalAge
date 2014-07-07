package mods.eln.sixnode.electricalentitysensor;

import java.io.DataOutputStream;
import java.io.IOException;

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
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class ElectricalEntitySensorElement extends SixNodeElement {

	ElectricalEntitySensorDescriptor descriptor;
	public ElectricalEntitySensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
	
    	electricalLoadList.add(outputGate);
    	electricalComponentList.add(outputGateProcess);
    	slowProcessList.add(slowProcess);
    	this.descriptor = (ElectricalEntitySensorDescriptor) descriptor;
	}
	public NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
	public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
	public ElectricalEntitySensorSlowProcess slowProcess = new ElectricalEntitySensorSlowProcess(this);

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(front == lrdu.left()) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return onBlockActivatedRotate(entityPlayer);
	}

	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public IInventory getInventory() {
		return inventory;
	}
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalEntitySensorContainer(player, inventory);
	}
	
	@Override
	protected void inventoryChanged() {
		
		super.inventoryChanged();
		needPublish();
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(slowProcess.state);
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(ElectricalEntitySensorContainer.filterId));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}
