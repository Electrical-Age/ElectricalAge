





package mods.eln.inductor;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;


import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalInductor;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.DiodeHeatingThermalLoadProcess;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


public class InductorElement extends SixNodeElement{

	public InductorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
    	electricalLoadList.add(postiveLoad);
    	electricalLoadList.add(negativeLoad);
    	electricalProcessList.add(inductor);
    	this.descriptor = (InductorDescriptor) descriptor;
	}


	public InductorDescriptor descriptor;
	public NodeElectricalLoad postiveLoad = new NodeElectricalLoad("postiveLoad");
	public NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	public NodeElectricalInductor inductor = new NodeElectricalInductor(postiveLoad, negativeLoad, "inductor");


	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front == lrdu) return postiveLoad;
		if(front.inverse() == lrdu) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4
		if(front == lrdu) return descriptor.cable.getNodeMask();
		if(front.inverse() == lrdu) return descriptor.cable.getNodeMask();

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}




	@Override
	public void initialize() {
		// TODO Auto-generated method stub
 
		descriptor.applyTo(negativeLoad);
		descriptor.applyTo(postiveLoad);
		descriptor.applyTo(inductor);
	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{	
		return super.onBlockActivatedRotate(entityPlayer);
	}
	

}
