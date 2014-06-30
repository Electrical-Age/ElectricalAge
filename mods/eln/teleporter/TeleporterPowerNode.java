package mods.eln.teleporter;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.INodeInfo;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;

public class TeleporterPowerNode extends GhostNode{

	@Override
	public void initializeFromThat(Direction front,
			EntityLivingBase entityLiving, ItemStack itemStack) {
		connect();
		
	}

	@Override
	public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
		if(e == null) return 0;
		if(directionA == Direction.YP || directionA == Direction.YN) return 0;
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
		return e.powerLoad;
	}

	@Override
	public void initializeFromNBT() {
		// TODO Auto-generated method stub
		
	}

	void setElement(TeleporterElement e)
	{
		this.e = e;
		//reconnect();
	}
	
	TeleporterElement e;




}
