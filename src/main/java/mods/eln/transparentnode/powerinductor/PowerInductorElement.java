package mods.eln.transparentnode.powerinductor;

import java.io.DataInputStream;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class PowerInductorElement extends TransparentNodeElement {

	PowerInductorDescriptor descriptor;
	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

	Inductor inductor = new Inductor("inductor", positiveLoad, negativeLoad);

	public PowerInductorElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (PowerInductorDescriptor) descriptor;

		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		electricalComponentList.add(inductor);
		positiveLoad.setAsMustBeFarFromInterSystem();
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down) return null;
		if (side == front.left()) return positiveLoad;
		if (side == front.right()) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down) return 0;
		if (side == front.left()) return node.maskElectricalPower;
		if (side == front.right()) return node.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotAmpere("I", inductor.getCurrent());
	}

	@Override
	public String thermoMeterString(Direction side) {
		return null;
	}

	@Override
	public void initialize() {
		//Eln.applySmallRs(positiveLoad);
		//Eln.applySmallRs(negativeLoad);

		setupPhysical();
		
		connect();
	}

	@Override
	public void inventoryChange(IInventory inventory) {
		super.inventoryChange(inventory);
		setupPhysical();
	}
	
	
	
	boolean fromNbt = false;
	public void setupPhysical() {
		double rs = descriptor.getRsValue(inventory);
		inductor.setL(descriptor.getlValue(inventory));
		positiveLoad.setRs(rs);
		negativeLoad.setRs(rs);
		
		if(fromNbt){
			fromNbt = false;
		}else{
			inductor.resetStates();
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {

		return false;
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

	public void networkSerialize(java.io.DataOutputStream stream)
	{
		super.networkSerialize(stream);
		/*
		 * try {
		 * 
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */
	}

	public static final byte unserializePannelAlpha = 0;

	public byte networkUnserialize(DataInputStream stream) {

		byte packetType = super.networkUnserialize(stream);
		/*
		 * try { switch(packetType) {
		 * 
		 * 
		 * default: return packetType; } } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */
		return unserializeNulldId;
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);

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
		return new PowerInductorContainer(player, inventory);
	}

}
