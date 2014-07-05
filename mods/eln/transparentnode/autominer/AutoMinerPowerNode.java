package mods.eln.transparentnode.autominer;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.INodeInfo;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class AutoMinerPowerNode extends GhostNode{
	Direction front;
	@Override
	public void initializeFromThat(Direction front,
			EntityLivingBase entityLiving, ItemStack itemStack) {
		
		this.front = front;
		
		
		connect();
	}

	@Override
	public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
		if(e == null) return 0;
		if(directionA != front) return 0;
		if(lrduA != LRDU.Down) return 0;
		return maskElectricalPower;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
		if(e == null) return null;
		return e.inPowerLoad;
	}

	@Override
	public void initializeFromNBT() {
		// TODO Auto-generated method stub
		
	}

	void setElement(AutoMinerElement e)
	{
		this.e = e;
		//reconnect();
	}
	
	
	public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt, String str) {
		front.writeToNBT(nbt, str + "front");
	}
	
	public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt, String str) {
		front = front.readFromNBT(nbt, str + "front");		
	}
	
	AutoMinerElement e;

}
