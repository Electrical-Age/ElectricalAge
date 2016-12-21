package mods.eln.transparentnode.transformer;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;
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

public class TransformerElement extends TransparentNodeElement {
	public NbtElectricalLoad primaryLoad = new NbtElectricalLoad("primaryLoad");
	public NbtElectricalLoad secondaryLoad = new NbtElectricalLoad("secondaryLoad");

	public VoltageSource primaryVoltageSource = new VoltageSource("primaryVoltageSource");
	public VoltageSource secondaryVoltageSource = new VoltageSource("secondaryVoltageSource");

	public TransformerInterSystemProcess interSystemProcess = new TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource);
    public Transformer transformer = new Transformer();

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);

	float primaryMaxCurrent = 0;
	float secondaryMaxCurrent = 0;
	TransformerDescriptor transformerDescriptor;
	SoundLooper highLoadSoundLooper;
	boolean isIsolator = false;
	
	public TransformerElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		
		electricalLoadList.add(primaryLoad);
		electricalLoadList.add(secondaryLoad);
		//electricalComponentList.add(primaryVoltageSource);
		//electricalComponentList.add(secondaryVoltageSource);
        //electricalComponentList.add(transformer);
		WorldExplosion exp = new WorldExplosion(this).machineExplosion();
		slowProcessList.add(voltagePrimaryWatchdog.set(primaryLoad).set(exp));
		slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp));

		transformerDescriptor = (TransformerDescriptor)descriptor;
		highLoadSoundLooper = new SoundLooper(this) {
			@Override
			public SoundCommand mustStart() {
				if (primaryMaxCurrent != 0 && secondaryMaxCurrent != 0) {
					float load = (float)Math.max(primaryLoad.getI() / primaryMaxCurrent, secondaryLoad.getI() / secondaryMaxCurrent);
					if (load > transformerDescriptor.minimalLoadToHum)
						return transformerDescriptor.highLoadSound.copy().mulVolume(0.1f * (load - transformerDescriptor.minimalLoadToHum) / (1 - transformerDescriptor.minimalLoadToHum), 1f).smallRange();
				} 
					
				return null;
			}
		};
		slowProcessList.add(highLoadSoundLooper);
	}

	VoltageStateWatchDog voltagePrimaryWatchdog = new VoltageStateWatchDog(), voltageSecondaryWatchdog = new VoltageStateWatchDog();

	@Override
	public void disconnectJob() {
		super.disconnectJob();
		if(isIsolator)
			Eln.simulator.mna.removeProcess(interSystemProcess);
	}

	@Override
	public void connectJob() {
		if(isIsolator)
			Eln.simulator.mna.addProcess(interSystemProcess);
		super.connectJob();
	}

	@Override
	public void onBreakElement() {
		// node.dropInventory(inventory);
		super.onBreakElement();
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down) return null;
		if (side == front.left()) return primaryLoad;
		if (side == front.right()) return secondaryLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (lrdu == lrdu.Down)
		{
			if (side == front.left()) return NodeBase.maskElectricalPower;
			if (side == front.right()) return NodeBase.maskElectricalPower;
			if (side == front && !grounded) return NodeBase.maskElectricalPower;
			if (side == front.back() && !grounded) return NodeBase.maskElectricalPower;
		}
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		if (side == front.left()) return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", -primaryLoad.getCurrent());
		if (side == front.right()) return Utils.plotVolt("US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", -secondaryLoad.getCurrent());

		return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", transformer.aCurrentState.state)
            + Utils.plotVolt("  US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", transformer.bCurrentState.state);

	}

	@Override
	public String thermoMeterString(Direction side) {
		return null;
	}

	@Override
	public void initialize() {
		applyIsolation();
		computeInventory();
		connect();
	}

	public void computeInventory()
	{
		ItemStack primaryCable = inventory.getStackInSlot(TransformerContainer.primaryCableSlotId);
		ItemStack secondaryCable = inventory.getStackInSlot(TransformerContainer.secondaryCableSlotId);
		ItemStack core = inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId);
		ElectricalCableDescriptor primaryCableDescriptor = null, secondaryCableDescriptor = null;

		// tranformerProcess.setEnable(primaryCable != null && core != null && secondaryCable != null);
	
		if (primaryCable != null) {
			primaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(primaryCable);
		}
		if (secondaryCable != null) {
			secondaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(secondaryCable);
		}

		if (primaryCableDescriptor != null)
			voltagePrimaryWatchdog.setUNominal(primaryCableDescriptor.electricalNominalVoltage);
		else
			voltagePrimaryWatchdog.setUNominal(1000000);

		if (secondaryCableDescriptor != null)
			voltageSecondaryWatchdog.setUNominal(secondaryCableDescriptor.electricalNominalVoltage);
		else
			voltageSecondaryWatchdog.setUNominal(1000000);

		double coreFactor = 1;
		if (core != null) {
			FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

			coreFactor = coreDescriptor.cableMultiplicator;
		}

		if (primaryCable == null || core == null) {
			primaryLoad.highImpedance();
			primaryMaxCurrent = 0;
		}
		else {
			primaryCableDescriptor.applyTo(primaryLoad, coreFactor);
			primaryMaxCurrent = (float)primaryCableDescriptor.electricalMaximalCurrent;
		}

		if (secondaryCable == null || core == null) {
			secondaryLoad.highImpedance();
			secondaryMaxCurrent = 0;
		}
		else {
			secondaryCableDescriptor.applyTo(secondaryLoad, coreFactor);
			secondaryMaxCurrent = (float)secondaryCableDescriptor.electricalMaximalCurrent;
		}

		if (primaryCable != null && secondaryCable != null)
		{
			transformer.setRatio(1.0 * secondaryCable.stackSize / primaryCable.stackSize);
			interSystemProcess.setRatio(1.0 * secondaryCable.stackSize / primaryCable.stackSize);
			/*
			 * tranformerProcess.setIMax( 2 * primaryCableDescriptor.electricalNominalPower / primaryCableDescriptor.electricalNominalVoltage, 2 * secondaryCableDescriptor.electricalNominalPower / secondaryCableDescriptor.electricalNominalVoltage);
			 */
		}
		else
		{
			transformer.setRatio(1);
			interSystemProcess.setRatio(1);
			// tranformerProcess.setIMax(
		}
	}


	public void applyIsolation(){
		electricalComponentList.remove(transformer);
		electricalComponentList.remove(primaryVoltageSource);
		electricalComponentList.remove(secondaryVoltageSource);
		primaryLoad.remove(primaryVoltageSource);
		secondaryLoad.remove(secondaryVoltageSource);
		primaryLoad.remove(transformer);
		secondaryLoad.remove(transformer);

		if(isIsolator) {
			primaryVoltageSource.connectTo(primaryLoad,null);
			secondaryVoltageSource.connectTo(secondaryLoad,null);
			electricalComponentList.add(primaryVoltageSource);
			electricalComponentList.add(secondaryVoltageSource);
		}else {
			transformer.connectTo(primaryLoad,secondaryLoad);
			electricalComponentList.add(transformer);
		}
	}

	public void inventoryChange(IInventory inventory)
	{
		disconnect();
		computeInventory();
		connect();
		needPublish();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {

		return false;
	}

	@Override
	public boolean hasGui() {

		return true;
	}

	@Override
	public Container newContainer(Direction side, EntityPlayer player) {

		return new TransformerContainer(player, inventory);
	}

	public float getLightOpacity() {

		return 1.0f;
	}

	@Override
	public IInventory getInventory() {

		return inventory;
	}

	@Override
	public void onGroundedChangedByClient() {

		super.onGroundedChangedByClient();
		computeInventory();
		reconnect();
	}
	public static final byte toogleIsIsolator = 0x1;

	@Override
	public byte networkUnserialize(DataInputStream stream) {
		switch(super.networkUnserialize(stream)) {
			case toogleIsIsolator:
				disconnect();
				isIsolator = ! isIsolator;
				applyIsolation();
				reconnect();
				needPublish();
				break;
		}
		return 0;
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {

		super.networkSerialize(stream);
		try {
			if (inventory.getStackInSlot(0) == null)
				stream.writeByte(0);
			else
				stream.writeByte(inventory.getStackInSlot(0).stackSize);
			if (inventory.getStackInSlot(1) == null)
				stream.writeByte(0);
			else
				stream.writeByte(inventory.getStackInSlot(1).stackSize);

			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.primaryCableSlotId));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.secondaryCableSlotId));

			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
			stream.writeBoolean(isIsolator);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}


	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("isIsolated",isIsolator);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		isIsolator = nbt.getBoolean("isIsolated");
	}

	@Override
	public Map<String, String> getWaila(){
		Map<String, String> info = new HashMap<String, String>();
		info.put(I18N.tr("Ratio"), Utils.plotValue(transformer.getRatio()));
		info.put(I18N.tr("Isolated"), isIsolator ? I18N.tr("Yes") : I18N.tr("No"));
		if(Eln.wailaEasyMode){
			FerromagneticCoreDescriptor core =
				(FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(
					inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId));
			if(core != null){
				info.put(I18N.tr("Core factor"), Utils.plotValue(core.cableMultiplicator));
			}
			info.put("Voltages", "\u00A7a" + Utils.plotVolt("", primaryLoad.getU()) + " " +
				"\u00A7e" + Utils.plotVolt("", secondaryLoad.getU()));
		}
		return info;
	}
}
