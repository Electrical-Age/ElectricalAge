package mods.eln.misc;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class TileEntityDestructor implements ITickHandler {
	ArrayList<TileEntity> destroyList = new ArrayList<TileEntity>();
	
	public TileEntityDestructor() {
		TickRegistry.registerTickHandler(this, Side.SERVER);
		
	}
	
	public void clear()
	{
		destroyList.clear();
	}
	public void add(TileEntity tile){
		destroyList.add(tile);
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		for(TileEntity t : destroyList){
			if(t.worldObj != null && t.worldObj.getBlockTileEntity(t.xCoord, t.yCoord, t.zCoord) == t){
				t.worldObj.setBlock(t.xCoord, t.yCoord, t.zCoord, 0);
				System.out.println("destroy light at " + t.xCoord + " " + t.yCoord + " " +  t.zCoord);
			}
		}
		destroyList.clear();
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub

	}

	@Override
	public EnumSet<TickType> ticks() {
		// TODO Auto-generated method stub
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "rondoudou2";
	}


}
