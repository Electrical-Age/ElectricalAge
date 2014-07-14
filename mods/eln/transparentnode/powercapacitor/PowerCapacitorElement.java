package mods.eln.transparentnode.powercapacitor;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PowerSourceBipole;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class PowerCapacitorElement extends TransparentNodeElement {

	PowerCapacitorDescriptor descriptor;
	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

	Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
	Resistor dischargeResistor = new Resistor(positiveLoad,negativeLoad);

	public PowerCapacitorElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (PowerCapacitorDescriptor) descriptor;

		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		electricalComponentList.add(capacitor);
		electricalComponentList.add(dischargeResistor);
		electricalProcessList.add(new SlowProcess());
		positiveLoad.setAsMustBeFarFromInterSystem();
	}
	
	
	
	class SlowProcess implements IProcess{
		@Override
		public void process(double time) {
			dischargeResistor.setR(stdDischargeResistor);
		}
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
		return Utils.plotAmpere("I", capacitor.getCurrent());
	}

	@Override
	public String thermoMeterString(Direction side) {
		return null;
	}

	@Override
	public void initialize() {
		Eln.applySmallRs(positiveLoad);
		Eln.applySmallRs(negativeLoad);

		setupPhysical();
		
		
		connect();
	}

	@Override
	public void inventoryChange(IInventory inventory) {
		// TODO Auto-generated method stub
		super.inventoryChange(inventory);
		setupPhysical();
	}
	
	double stdDischargeResistor;
	
	boolean fromNbt = false;
	public void setupPhysical() {
		//double rs = descriptor.getRsValue(inventory);

		double eOld = capacitor.getE();
		capacitor.setC(descriptor.getCValue(inventory));
		//positiveLoad.setRs(rs);
		//negativeLoad.setRs(rs);
		stdDischargeResistor = descriptor.dischargeTao/capacitor.getC();
		
		
		if(fromNbt){
			dischargeResistor.setR(stdDischargeResistor);
			fromNbt = false;
		}else{
			double deltaE = capacitor.getE()-eOld;
			if(deltaE < 0){
				dischargeResistor.setR(stdDischargeResistor);
			}else{
				double egualiseR = Math.pow(dischargeResistor.getU(),2)/(deltaE/Eln.simulator.electricalPeriod);
				dischargeResistor.setR(1/(1/egualiseR+1/stdDischargeResistor));
			}
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
		return new PowerCapacitorContainer(player, inventory);
	}

}
