package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public abstract class GhostNode extends NodeBase{

	@Override
	public short getBlockId() {
		// TODO Auto-generated method stub
		return (short) Utils.getBlockId(Eln.ghostBlock);
	}

	@Override
	public Block getBlock() {
		// TODO Auto-generated method stub
		return Eln.ghostBlock;
	}

	@Override
	public boolean mustBeSaved() {
		// TODO Auto-generated method stub
		return false;
	}

}
