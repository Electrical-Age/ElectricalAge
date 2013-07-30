package mods.eln.transformer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TransformerProcess;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TransformerElement extends TransparentNodeElement{
	public NodeElectricalLoad positivePrimaryLoad = new NodeElectricalLoad("positivePrimaryLoad");
	public NodeElectricalLoad negativePrimaryLoad = new NodeElectricalLoad("negativePrimaryLoad");
	public NodeElectricalLoad positiveSecondaryLoad = new NodeElectricalLoad("positiveSecondaryLoad");
	public NodeElectricalLoad negativeSecondaryLoad = new NodeElectricalLoad("negativeSecondaryLoad");

	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");

	public ElectricalLoadHeatThermalLoadProcess positivePrimaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(positivePrimaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess negativePrimaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(negativePrimaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess positiveSecondaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(positiveSecondaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess negativeSecondaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(negativeSecondaryLoad,thermalLoad);
	
	public TransformerProcess tranformerProcess = new TransformerProcess(positivePrimaryLoad,negativePrimaryLoad,positiveSecondaryLoad,negativeSecondaryLoad);
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);
	
	
	VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad primaryVoltageWatchdog = new VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad(inventory, TransformerContainer.primaryCableSlotId, positivePrimaryLoad, negativePrimaryLoad);
	VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad secondaryVoltageWatchdog = new VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad(inventory, TransformerContainer.secondaryCableSlotId, positiveSecondaryLoad, negativeSecondaryLoad);
	
	public TransformerElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
	   	electricalLoadList.add(positivePrimaryLoad);
	   	electricalLoadList.add(negativePrimaryLoad);
	   	electricalLoadList.add(positiveSecondaryLoad);
	   	electricalLoadList.add(negativeSecondaryLoad);
	   	
	   	thermalLoadList.add(thermalLoad);
    	
	   	thermalProcessList.add(positivePrimaryLoadETProcess);
	   	thermalProcessList.add(negativePrimaryLoadETProcess);
	   	thermalProcessList.add(positiveSecondaryLoadETProcess);
	   	thermalProcessList.add(negativeSecondaryLoadETProcess);
	   	
	   	slowProcessList.add(primaryVoltageWatchdog);
	   	slowProcessList.add(secondaryVoltageWatchdog);
           	
	   	electricalProcessList.add(tranformerProcess);
	}

	@Override
	public void onBreakElement() {
	//	node.dropInventory(inventory);
		super.onBreakElement();
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front.left()) return positivePrimaryLoad;
		if(side == front.right()) return positiveSecondaryLoad;
		if(side == front && ! grounded) return negativePrimaryLoad ;
		if(side == front.back() && ! grounded) return negativeSecondaryLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu == lrdu.Down)
		{
			if(side == front.left() ) return NodeBase.maskElectricalPower;	
			if(side == front.right()) return NodeBase.maskElectricalPower;	
			if(side == front&& ! grounded) return NodeBase.maskElectricalPower;
			if(side == front.back() && ! grounded) return NodeBase.maskElectricalPower;
		}
		return 0;
	}


	
	@Override
	public String multiMeterString(Direction side) {
		if(side == front.left())return  Utils.plotVolt("UP+",positivePrimaryLoad.Uc) + Utils.plotAmpere("IP+",positivePrimaryLoad.getCurrent());
		if(side == front.right())return  Utils.plotVolt("US+",positiveSecondaryLoad.Uc) + Utils.plotAmpere("IS+",positiveSecondaryLoad.getCurrent());
		if(side == front && grounded == false)return  Utils.plotVolt("UP-",negativePrimaryLoad.Uc) + Utils.plotAmpere("IP-",negativePrimaryLoad.getCurrent());
		if(side == front.back() && grounded == false)return Utils.plotVolt("US-",negativeSecondaryLoad.Uc) + Utils.plotAmpere("IS-",negativeSecondaryLoad.getCurrent());
		return "";

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return  Utils.plotCelsius("T",thermalLoad.Tc);
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
	   		

    	thermalLoad.Rs = 100000.0f;
    	thermalLoad.C = 1.0f;
    	thermalLoad.Rp = 1.0f;
          	

		
		computeInventory();
		
		connect();
    			
	}
	
	public void computeInventory()
	{
		ItemStack primaryCable = inventory.getStackInSlot(TransformerContainer.primaryCableSlotId);
		ItemStack secondaryCable = inventory.getStackInSlot(TransformerContainer.secondaryCableSlotId);
		ItemStack core = inventory.getStackInSlot(TransformerContainer.ferromagneticSlotId);
		
		tranformerProcess.setEnable(primaryCable != null && core != null && secondaryCable != null);
		
		if(primaryCable == null || core == null)
		{
			positivePrimaryLoad.highImpedance();
			negativePrimaryLoad.highImpedance();
			if(grounded) negativePrimaryLoad.groundedEnable(); 
		}
		else
		{
			ElectricalCableDescriptor primaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(primaryCable);	
			primaryCableDescriptor.applyTo( positivePrimaryLoad, false);
			primaryCableDescriptor.applyTo( negativePrimaryLoad, grounded);	
		}
		if(secondaryCable == null || core == null)
		{
			positiveSecondaryLoad.highImpedance();
			negativeSecondaryLoad.highImpedance();
			if(grounded) negativeSecondaryLoad.groundedEnable(); 
		}
		else
		{
			ElectricalCableDescriptor secondaryCableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(secondaryCable);
			secondaryCableDescriptor.applyTo( positiveSecondaryLoad, false);
			secondaryCableDescriptor.applyTo( negativeSecondaryLoad, grounded);	
		}		
		
		if(core != null)
		{
			FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);
			
			coreDescriptor.applyTo(positivePrimaryLoad);
			coreDescriptor.applyTo(negativePrimaryLoad);
			coreDescriptor.applyTo(positiveSecondaryLoad);
			coreDescriptor.applyTo(negativeSecondaryLoad);

		}

		if(primaryCable != null && secondaryCable != null)
		{
			tranformerProcess.setRatio(1.0 * secondaryCable.stackSize / primaryCable.stackSize);
		}
		else
		{
			tranformerProcess.setRatio(1);
		}
	}

    public void inventoryChange(IInventory inventory)
    {
    	computeInventory();
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
			if(inventory.getStackInSlot(0) == null) stream.writeByte(0);
			else stream.writeByte(inventory.getStackInSlot(0).stackSize);
			if(inventory.getStackInSlot(1) == null) stream.writeByte(0);
			else stream.writeByte(inventory.getStackInSlot(1).stackSize);
			
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
