package mods.eln.transparentnode.powercapacitor;

import java.io.DataInputStream;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog;
import mods.eln.sim.process.destruct.WorldExplosion;
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
	PunkProcess punkProcess = new PunkProcess();
	BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);
	
	public PowerCapacitorElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (PowerCapacitorDescriptor) descriptor;

		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		electricalComponentList.add(capacitor);
		electricalComponentList.add(dischargeResistor);
		electricalProcessList.add(punkProcess);
		slowProcessList.add(watchdog);
		
		watchdog.set(new WorldExplosion(this).machineExplosion());
		positiveLoad.setAsMustBeFarFromInterSystem();
	}
	
	
	
	class PunkProcess implements IProcess{
		double eLeft = 0;
		double eLegaliseResistor;
		
		@Override
		public void process(double time) {
			if(eLeft <= 0){
				eLeft = 0;
				dischargeResistor.setR(stdDischargeResistor);
			}else{
				eLeft -= dischargeResistor.getP()*time;
				dischargeResistor.setR(eLegaliseResistor);
			}
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
		super.inventoryChange(inventory);
		setupPhysical();
	}
	
	double stdDischargeResistor;
	
	boolean fromNbt = false;
	public void setupPhysical() {
		double eOld = capacitor.getE();
		capacitor.setC(descriptor.getCValue(inventory));
		stdDischargeResistor = descriptor.dischargeTao/capacitor.getC();
		
		watchdog.setUNominal(descriptor.getUNominalValue(inventory));
		punkProcess.eLegaliseResistor = Math.pow(descriptor.getUNominalValue(inventory),2)/400;
		
		if(fromNbt){
			dischargeResistor.setR(stdDischargeResistor);
			fromNbt = false;
		}else{
			double deltaE = capacitor.getE()-eOld;
			punkProcess.eLeft += deltaE;
			if(deltaE < 0){
				dischargeResistor.setR(stdDischargeResistor);
			}else{
				dischargeResistor.setR(punkProcess.eLegaliseResistor);
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
		nbt.setDouble("punkELeft",punkProcess.eLeft);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		punkProcess.eLeft = nbt.getDouble("punkELeft");
		if(Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0;
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
