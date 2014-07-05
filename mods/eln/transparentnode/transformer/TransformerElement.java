package mods.eln.transparentnode.transformer;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeVoltageSource;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class TransformerElement extends TransparentNodeElement {
	public NodeElectricalLoad primaryLoad = new NodeElectricalLoad("primaryLoad");
	public NodeElectricalLoad secondaryLoad = new NodeElectricalLoad("secondaryLoad");

	public NodeVoltageSource primaryVoltageSource = new NodeVoltageSource(primaryLoad, null, "primaryVoltageSource");
	public NodeVoltageSource secondaryVoltageSource = new NodeVoltageSource(secondaryLoad, null, "secondaryVoltageSource");

	public TransformerInterSystemProcess interSystemProcess = new TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource);

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);

	public TransformerElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		electricalLoadList.add(primaryLoad);
		electricalLoadList.add(secondaryLoad);
		electricalComponentList.add(primaryVoltageSource);
		electricalComponentList.add(secondaryVoltageSource);
		
		WorldExplosion exp = new WorldExplosion(this).machineExplosion();
		slowProcessList.add(voltagePrimaryWatchdog.set(primaryLoad).set(exp));
		slowProcessList.add(voltageSecondaryWatchdog.set(primaryLoad).set(exp));

	}

	VoltageStateWatchDog voltagePrimaryWatchdog = new VoltageStateWatchDog(),voltageSecondaryWatchdog = new VoltageStateWatchDog();
	@Override
	public void disconnectJob() {
		super.disconnectJob();
		Eln.simulator.mna.removeProcess(interSystemProcess);
	}

	@Override
	public void connectJob() {
		Eln.simulator.mna.addProcess(interSystemProcess);
		super.connectJob();
	}

	@Override
	public void onBreakElement() {
		//	node.dropInventory(inventory);
		super.onBreakElement();
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front.left()) return primaryLoad;
		if(side == front.right()) return secondaryLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu == lrdu.Down)
		{
			if(side == front.left()) return NodeBase.maskElectricalPower;
			if(side == front.right()) return NodeBase.maskElectricalPower;
			if(side == front && !grounded) return NodeBase.maskElectricalPower;
			if(side == front.back() && !grounded) return NodeBase.maskElectricalPower;
		}
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		if(side == front.left()) return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", primaryLoad.getCurrent());
		if(side == front.right()) return Utils.plotVolt("US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", secondaryLoad.getCurrent());
		return "";

	}

	@Override
	public String thermoMeterString(Direction side) {
		return null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		/*switch (type) {
			case 0:
				tranformerProcess.setRatio(2);
				break;
			case 1:
				tranformerProcess.setRatio(-2);
				break;
			case 2:
				tranformerProcess.setRatio(10);
				break;
		
			default:
				break;
		}*/
		//tranformerProcess.setRatio(2);	

		computeInventory();

		connect();

	}

	public void computeInventory()
	{
		ItemStack primaryCable = inventory.getStackInSlot(TransformerContainer.primaryCableSlotId);
		ItemStack secondaryCable = inventory.getStackInSlot(TransformerContainer.secondaryCableSlotId);
		ItemStack core = inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId);
		ElectricalCableDescriptor primaryCableDescriptor = null, secondaryCableDescriptor = null;

		//tranformerProcess.setEnable(primaryCable != null && core != null && secondaryCable != null);
		
		if(primaryCable != null) {
			primaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(primaryCable);
		}
		if(secondaryCable != null) {
			secondaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(secondaryCable);
		}

		if(primaryCableDescriptor != null)
			voltagePrimaryWatchdog.setUNominal(primaryCableDescriptor.electricalNominalVoltage);
		else
			voltagePrimaryWatchdog.setUNominal(1000000);
		
		if(secondaryCableDescriptor != null)
			voltageSecondaryWatchdog.setUNominal(secondaryCableDescriptor.electricalNominalVoltage);
		else
			voltageSecondaryWatchdog.setUNominal(1000000);
		
		double coreFactor = 1;
		if(core != null)	{
			FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

			coreFactor = coreDescriptor.cableMultiplicator;
		}

		if(primaryCable == null || core == null)
			primaryLoad.highImpedance();
		else
			primaryCableDescriptor.applyTo(primaryLoad, coreFactor);

		if(secondaryCable == null || core == null)
			secondaryLoad.highImpedance();
		else
			secondaryCableDescriptor.applyTo(secondaryLoad, coreFactor);

		if(primaryCable != null && secondaryCable != null)
		{
			interSystemProcess.setRatio(1.0 * secondaryCable.stackSize / primaryCable.stackSize);
			/*tranformerProcess.setIMax(
					2 * primaryCableDescriptor.electricalNominalPower / primaryCableDescriptor.electricalNominalVoltage,
					2 * secondaryCableDescriptor.electricalNominalPower / secondaryCableDescriptor.electricalNominalVoltage);
*/
		}
		else
		{
			interSystemProcess.setRatio(1);
			//tranformerProcess.setIMax(
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new TransformerContainer(player, inventory);
	}

	public float getLightOpacity() {
		// TODO Auto-generated method stub
		return 1.0f;
	}

	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}

	@Override
	public void onGroundedChangedByClient() {
		// TODO Auto-generated method stub
		super.onGroundedChangedByClient();
		computeInventory();
		reconnect();
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			if(inventory.getStackInSlot(0) == null)
				stream.writeByte(0);
			else
				stream.writeByte(inventory.getStackInSlot(0).stackSize);
			if(inventory.getStackInSlot(1) == null)
				stream.writeByte(0);
			else
				stream.writeByte(inventory.getStackInSlot(1).stackSize);

			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.primaryCableSlotId));
			Utils.serialiseItemStack(stream, inventory.getStackInSlot(TransformerContainer.secondaryCableSlotId));

			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
