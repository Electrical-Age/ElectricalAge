package mods.eln.sixnode.powercapacitorsix;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.primitives.Capacitance;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class PowerCapacitorSixElement extends SixNodeElement {

	PowerCapacitorSixDescriptor descriptor;
	NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
	NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

	Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
	Resistor dischargeResistor = new Resistor(positiveLoad, negativeLoad);
	PunkProcess punkProcess = new PunkProcess();
	BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    double stdDischargeResistor;

    boolean fromNbt = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public PowerCapacitorSixElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
		super(SixNode, side, descriptor);
		this.descriptor = (PowerCapacitorSixDescriptor) descriptor;

		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		electricalComponentList.add(capacitor);
		electricalComponentList.add(dischargeResistor);
		electricalProcessList.add(punkProcess);
		slowProcessList.add(watchdog);
		
		watchdog.set(new WorldExplosion(this).machineExplosion());
		positiveLoad.setAsMustBeFarFromInterSystem();
	}

	class PunkProcess implements IProcess {
		double eLeft = 0;
		double eLegaliseResistor;
		
		@Override
		public void process(double time) {
			if (eLeft <= 0) {
				eLeft = 0;
				dischargeResistor.setR(new Resistance(stdDischargeResistor));
			} else {
				eLeft -= dischargeResistor.getP().getValue() * time;
				dischargeResistor.setR(new Resistance(eLegaliseResistor));
			}
		}
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
		return Utils.plotVolt("U", Math.abs(capacitor.getU().getValue())) + Utils.plotAmpere("I", capacitor.getCurrent().getValue());
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
		Eln.applySmallRs(positiveLoad);
		Eln.applySmallRs(negativeLoad);

		setupPhysical();
	}

	@Override
	public void inventoryChanged() {
		super.inventoryChanged();
		setupPhysical();
	}

	public void setupPhysical() {
		double eOld = capacitor.getE().getValue();
		capacitor.setC(new Capacitance(descriptor.getCValue(inventory)));
		stdDischargeResistor = descriptor.dischargeTao / capacitor.getC().getValue();
		
		watchdog.setUNominal(descriptor.getUNominalValue(inventory));
		punkProcess.eLegaliseResistor = Math.pow(descriptor.getUNominalValue(inventory), 2) / 400;
		
		if (fromNbt) {
			dischargeResistor.setR(new Resistance(stdDischargeResistor));
			fromNbt = false;
		} else {
			double deltaE = capacitor.getE().getValue() - eOld;
			punkProcess.eLeft += deltaE;
			if (deltaE < 0) {
				dischargeResistor.setR(new Resistance(stdDischargeResistor));
			} else {
				dischargeResistor.setR(new Resistance(punkProcess.eLegaliseResistor));
			}
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return onBlockActivatedRotate(entityPlayer);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setDouble("punkELeft", punkProcess.eLeft);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		punkProcess.eLeft = nbt.getDouble("punkELeft");
		if(Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0;
		fromNbt = true;
	}

	public void networkSerialize(java.io.DataOutputStream stream) {
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
		return new PowerCapacitorSixContainer(player, inventory);
	}
}
