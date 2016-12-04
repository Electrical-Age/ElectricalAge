package mods.eln.sixnode.batterycharger;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
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
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.ResistorPowerWatchdog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BatteryChargerElement extends SixNodeElement {

	public BatteryChargerDescriptor descriptor;
	
	public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
	public BatteryChargerSlowProcess slowProcess = new BatteryChargerSlowProcess();
	Resistor powerResistor = new Resistor(powerLoad, null);

	SixNodeElementInventory inventory = new SixNodeElementInventory(5, 64, this);

	VoltageStateWatchDog voltageWatchDog = new VoltageStateWatchDog();
	ResistorPowerWatchdog powerWatchDog = new ResistorPowerWatchdog();

    public String channel = "Default channel";

    boolean invChanged = false;

    boolean powerOn = false;

    public static final byte toogleCharge = 1;

    byte charged, presence;

    @Override
	public IInventory getInventory() {
		return inventory;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new BatteryChargerContainer(player, inventory);
	}

	public BatteryChargerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (BatteryChargerDescriptor) descriptor;
		
		electricalLoadList.add(powerLoad);
		electricalComponentList.add(powerResistor);
		slowProcessList.add(slowProcess);

		WorldExplosion exp = new WorldExplosion(this).machineExplosion();
		slowProcessList.add(voltageWatchDog.set(powerLoad).setUNominal(this.descriptor.nominalVoltage).set(exp));
		//slowProcessList.add(powerWatchDog.set(powerResistor).setPmax(this.descriptor.nominalPower * 3).set(exp));
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return powerLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return NodeBase.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
	}

	@Override
	public Map<String, String> getWaila() {
		Map<String,String> info = new HashMap<String,String>();
		info.put(I18N.TR("Charge Current"), Utils.plotAmpere("", powerLoad.getCurrent()));
		if (Eln.wailaEasyMode) {
			info.put(I18N.TR("Voltage"), Utils.plotVolt("", powerLoad.getU()));
			info.put(I18N.TR("Power"), Utils.plotPower("", powerLoad.getI() * powerLoad.getU()));
		}
		return info;
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
		descriptor.applyTo(powerLoad);
	}
	
	@Override
	protected void inventoryChanged() {
		super.inventoryChanged();
		//needPublish(); 
		invChanged = true;
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

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("powerOn", powerOn);
		nbt.setDouble("energyCounter", slowProcess.energyCounter);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerOn = nbt.getBoolean( "powerOn");
		slowProcess.energyCounter = nbt.getDouble("energyCounter");
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		
		try {
			switch(stream.readByte()) {
			    case toogleCharge:
				    powerOn = !powerOn;
				    needPublish();
				    break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(powerOn);
			stream.writeFloat((float) powerLoad.getU());
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(0));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(1));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(2));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(3));
			
			stream.writeByte(charged);
			stream.writeByte(presence);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class BatteryChargerSlowProcess implements IProcess {
		double energyCounter = 0;
		double timeout = 0;

        @Override
		public void process(double time) {
			timeout -= time;
			if (timeout > 0) return;
			timeout = 1;
			time = 1;

			byte oldCharged = charged;
			charged = 0;
			presence = 0;
			if (!powerOn) {
				descriptor.setRp(powerResistor, false);
			} else {
				ItemStack booster = (inventory.getStackInSlot(BatteryChargerContainer.boosterSlotId));
				double boost = 1.0;
				double eff = 1.0;
				if (booster != null) {
					boost = Math.pow(1.25, booster.stackSize);
					eff = Math.pow(0.9, booster.stackSize);
				}
				
				energyCounter += powerResistor.getP() * time * eff;
				
				for (int idx = 0; idx < 4; idx++) {
					ItemStack stack = inventory.getStackInSlot(idx);
					Object o = Utils.getItemObject(stack);
					if (o instanceof IItemEnergyBattery) {
						IItemEnergyBattery b = (IItemEnergyBattery) o;
						double e = Math.min(Math.min(energyCounter, b.getChargePower(stack) * time * boost), b.getEnergyMax(stack) - b.getEnergy(stack));
						b.setEnergy(stack, b.getEnergy(stack) + e);
						energyCounter -= e;
					}
				}
				
				if (energyCounter < descriptor.nominalPower * time * 2 * boost) {
					//double target = descriptor.nominalPower * time * 2;
					double power = Math.min(descriptor.nominalPower * boost, (descriptor.nominalPower * time * 2 * boost - energyCounter) / time);
					powerResistor.setR(Math.max(powerLoad.getU() * powerLoad.getU() / power, descriptor.Rp / boost));
				} else {
					descriptor.setRp(powerResistor, false);
				}
			}
			for (int idx = 0; idx < 4; idx++) {
				ItemStack stack = inventory.getStackInSlot(idx);
				Object o = Utils.getItemObject(stack);
				if (o instanceof IItemEnergyBattery) {
					IItemEnergyBattery b = (IItemEnergyBattery) o;
					if (b.getEnergy(stack) == b.getEnergyMax(stack)) {
						charged += 1 << idx;
					}
					presence += 1 << idx;
				}
			}
			
			if (charged != oldCharged)
				needPublish();
			if (invChanged) {
				invChanged = false;
				needPublish();
			}
		}
	}
}
