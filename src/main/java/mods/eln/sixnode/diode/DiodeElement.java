package mods.eln.sixnode.diode;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.DiodeHeatThermalLoad;
import mods.eln.sim.process.heater.ResistorHeatThermalLoad;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DiodeElement extends SixNodeElement {

    public DiodeDescriptor descriptor;
    public NbtElectricalLoad anodeLoad = new NbtElectricalLoad("anodeLoad");
    public NbtElectricalLoad catodeLoad = new NbtElectricalLoad("catodeLoad");
    public ResistorSwitch resistorSwitch = new ResistorSwitch("resistorSwitch", anodeLoad, catodeLoad);
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    public DiodeHeatThermalLoad heater = new DiodeHeatThermalLoad(resistorSwitch, thermalLoad);
    public ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    public DiodeProcess diodeProcess = new DiodeProcess(resistorSwitch);

    LRDU front;

    public DiodeElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

		this.descriptor = (DiodeDescriptor) descriptor;
		thermalLoad.setAsSlow();
		
		front = LRDU.Left;
		electricalLoadList.add(anodeLoad);
		electricalLoadList.add(catodeLoad);
		thermalLoadList.add(thermalLoad);
		electricalComponentList.add(resistorSwitch);
		electricalProcessList.add(diodeProcess);
		slowProcessList.add(thermalWatchdog.set(thermalLoad).set(this.descriptor.thermal).set(new WorldExplosion(this).cableExplosion()));
		thermalSlowProcessList.add(heater);
	}

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		byte value = nbt.getByte("front");
		front = LRDU.fromInt((value >> 0) & 0x3);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) ((front.toInt() << 0)));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return anodeLoad;
		if (front.inverse() == lrdu) return catodeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return descriptor.cable.getNodeMask();
		if (front.inverse() == lrdu) return descriptor.cable.getNodeMask();
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotVolt("U+:", anodeLoad.getU()) + Utils.plotVolt("U-:", catodeLoad.getU()) + Utils.plotAmpere("I:", anodeLoad.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return Utils.plotCelsius("T:", thermalLoad.Tc);
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte((front.toInt() << 4));
			stream.writeShort((short) ((anodeLoad.getU()) * NodeBase.networkSerializeUFactor));
			stream.writeShort((short) ((catodeLoad.getU()) * NodeBase.networkSerializeUFactor));
			stream.writeShort((short) (anodeLoad.getCurrent() * NodeBase.networkSerializeIFactor));
			stream.writeShort((short) (thermalLoad.Tc * NodeBase.networkSerializeTFactor));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		descriptor.applyTo(catodeLoad);
		descriptor.applyTo(anodeLoad);
		descriptor.applyTo(thermalLoad);
		descriptor.applyTo(resistorSwitch);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		if (currentItemStack != null) {
			Item item = currentItemStack.getItem();
			/*if (item== Eln.toolsSetItem) {
				colorCare = colorCare ^ 1;
				entityPlayer.addChatMessage("Wire color care " + colorCare);
				sixNode.reconnect();
			}
			if (item == Eln.brushItem) {
				if (currentItemStack.getItemDamage() < BrushItem.maximalUse) {
					color = currentItemStack.getItemDamage() & 0xF;
					
					currentItemStack.setItemDamage(currentItemStack.getItemDamage() + 16);
					
					sixNode.reconnect();
				} else {
					entityPlayer.addChatMessage("Brush is empty");
				}
			}*/
		}
		//front = LRDU.fromInt((front.toInt() + 1)&3);
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;
		}
		return false;
	}
}
