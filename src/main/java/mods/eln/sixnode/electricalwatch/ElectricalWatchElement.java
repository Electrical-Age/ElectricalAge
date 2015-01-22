package mods.eln.sixnode.electricalwatch;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class ElectricalWatchElement extends SixNodeElement {

	ElectricalWatchDescriptor descriptor;
	public ElectricalWatchElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

    	slowProcessList.add(slowProcess);
    	this.descriptor = (ElectricalWatchDescriptor) descriptor;
	}
	public ElectricalWatchSlowProcess slowProcess = new ElectricalWatchSlowProcess(this);


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
		return 0;
	}

	@Override
	public String multiMeterString() {
		return "";
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
		//return onBlockActivatedRotate(entityPlayer);
		return false;
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
		return new ElectricalWatchContainer(player, inventory);
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
			stream.writeBoolean(slowProcess.upToDate);
			stream.writeLong(slowProcess.oldDate);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}
