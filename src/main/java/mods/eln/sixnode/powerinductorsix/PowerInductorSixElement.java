package mods.eln.sixnode.powerinductorsix;

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
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.primitives.Inductance;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class PowerInductorSixElement extends SixNodeElement {

	PowerInductorSixDescriptor descriptor;
	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

	Inductor inductor = new Inductor("inductor", positiveLoad, negativeLoad);

    boolean fromNbt = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

	public PowerInductorSixElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
		super(SixNode, side, descriptor);
		this.descriptor = (PowerInductorSixDescriptor) descriptor;

		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		electricalComponentList.add(inductor);
		positiveLoad.setAsMustBeFarFromInterSystem();
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (lrdu == front.right()) return positiveLoad;
		if (lrdu == front.left()) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (lrdu == front.right()) return NodeBase.maskElectricalPower;
		if (lrdu == front.left()) return NodeBase.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U", Math.abs(inductor.getU().getValue())) + Utils.plotAmpere("I", inductor.getCurrent().getValue());
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
		setupPhysical();
	}

	@Override
	public void inventoryChanged() {
		super.inventoryChanged();
		setupPhysical();
	}
    
	public void setupPhysical() {
		double rs = descriptor.getRsValue(inventory);
		inductor.setL(new Inductance(descriptor.getlValue(inventory)));
		positiveLoad.setRs(rs);
		negativeLoad.setRs(rs);

		if (fromNbt) {
			fromNbt = false;
		} else {
			inductor.resetStates();
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return onBlockActivatedRotate(entityPlayer);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		fromNbt = true;
	}

	@Override
	public IInventory getInventory() {
		return inventory;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new PowerInductorSixContainer(player, inventory);
	}
}
