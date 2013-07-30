package mods.eln.mppt;

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
import mods.eln.node.NodeElectricalPowerSource;
import mods.eln.node.NodeElectricalResistor;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeVoltageWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TransformerProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MpptElement extends TransparentNodeElement{
	public NodeElectricalLoad positivePrimaryLoad = new NodeElectricalLoad("positivePrimaryLoad");
	public NodeElectricalLoad negativePrimaryLoad = new NodeElectricalLoad("negativePrimaryLoad");
	public NodeElectricalLoad positiveSecondaryLoad = new NodeElectricalLoad("positiveSecondaryLoad");
	public NodeElectricalLoad negativeSecondaryLoad = new NodeElectricalLoad("negativeSecondaryLoad");

	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");

	public ElectricalLoadHeatThermalLoadProcess positivePrimaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(positivePrimaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess negativePrimaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(negativePrimaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess positiveSecondaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(positiveSecondaryLoad,thermalLoad);
	public ElectricalLoadHeatThermalLoadProcess negativeSecondaryLoadETProcess = new ElectricalLoadHeatThermalLoadProcess(negativeSecondaryLoad,thermalLoad);
	
	public NodeElectricalResistor inResistor = new NodeElectricalResistor("inResistor",positivePrimaryLoad, negativePrimaryLoad);
	public NodeElectricalPowerSource outPowerSource = new NodeElectricalPowerSource("outPowerSource",positiveSecondaryLoad, negativeSecondaryLoad);
	
	public MpptSlowProcess mpptElectricalProcess = new MpptSlowProcess(this);
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0, 64, this);
	
	MpptDescriptor descriptor;
	
	
	
	public MpptElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (MpptDescriptor) descriptor;
		
	   	electricalLoadList.add(positivePrimaryLoad);
	   	electricalLoadList.add(negativePrimaryLoad);
	   	electricalLoadList.add(positiveSecondaryLoad);
	   	electricalLoadList.add(negativeSecondaryLoad);
	   	
	   	electricalProcessList.add(inResistor);
	   	electricalProcessList.add(outPowerSource);
	   	
	   	
	   	thermalLoadList.add(thermalLoad);
    	
	   	thermalProcessList.add(positivePrimaryLoadETProcess);
	   	thermalProcessList.add(negativePrimaryLoadETProcess);
	   	thermalProcessList.add(positiveSecondaryLoadETProcess);
	   	thermalProcessList.add(negativeSecondaryLoadETProcess);
           	
	   	slowProcessList.add(mpptElectricalProcess);
	}

	@Override
	public void onBreakElement() {
	//	node.dropInventory(inventory);
		super.onBreakElement();
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front) return positivePrimaryLoad;
		if(side == front.back()) return positiveSecondaryLoad;
		if(side == front.left() && ! grounded) return negativePrimaryLoad;
		if(side == front.right() && ! grounded) return negativeSecondaryLoad;
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
			if(side == front) return NodeBase.maskElectricalPower;	
			if(side == front.back()) return NodeBase.maskElectricalPower;	
			if(side == front.left() && ! grounded) return NodeBase.maskElectricalPower;
			if(side == front.right() && ! grounded) return NodeBase.maskElectricalPower;
		}
		return NodeBase.maskThermal;
	}


	
	@Override
	public String multiMeterString(Direction side) {
		if(side == front)return  Utils.plotVolt("UP+",positivePrimaryLoad.Uc) + Utils.plotAmpere("IP+",positivePrimaryLoad.getCurrent());
		if(side == front.back())return  Utils.plotVolt("US+",positiveSecondaryLoad.Uc) + Utils.plotAmpere("IS+",positiveSecondaryLoad.getCurrent());
		if(side == front.left() && grounded == false)return  Utils.plotVolt("UP-",negativePrimaryLoad.Uc) + Utils.plotAmpere("IP-",negativePrimaryLoad.getCurrent());
		if(side == front.right() && grounded == false)return Utils.plotVolt("US-",negativeSecondaryLoad.Uc) + Utils.plotAmpere("IS-",negativeSecondaryLoad.getCurrent());
		return "";

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return  Utils.plotCelsius("T",thermalLoad.Tc);
	}

	
	@Override
	public void initialize() {

  		
  		descriptor.applylToIn(positivePrimaryLoad,false);
  		descriptor.applylToIn(negativePrimaryLoad,grounded);
  		descriptor.applylToOut(positiveSecondaryLoad,false);
  		descriptor.applylToOut(negativeSecondaryLoad,grounded);
		
  		
  		if(fromNbt == false)
  		{
  			outPowerSource.setUmax(descriptor.outUmin);
  		}

		setPhysicalValue();
		
		connect();
    			
	}
	
	public void setPhysicalValue()
	{


	}

    public void inventoryChange(IInventory inventory)
    {
    	setPhysicalValue();
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
		return new MpptContainer(player, inventory);
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
	
	
	
	
	public static final byte unserializeUTarget = 1;

	
	
	@Override
	public byte networkUnserialize(DataInputStream stream) {
		byte packetType = super.networkUnserialize(stream);
		try {
			switch(packetType)
			{
			case unserializeUTarget:
				mpptElectricalProcess.setUtarget(stream.readFloat());				
				needPublish();
				break;

			default:
				return packetType;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unserializeNulldId;
	}
	
	

	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeFloat((float) mpptElectricalProcess.getUtarget());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	boolean fromNbt = false;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		fromNbt = true;
		super.readFromNBT(nbt, str);
	}
}
