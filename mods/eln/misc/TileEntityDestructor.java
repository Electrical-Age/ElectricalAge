package mods.eln.misc;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.relauncher.Side;

public class TileEntityDestructor {
	ArrayList<TileEntity> destroyList = new ArrayList<TileEntity>();
	
	public TileEntityDestructor() {
		FMLCommonHandler.instance().bus().register(this);
		
	}
	
	public void clear()
	{
		destroyList.clear();
	}
	public void add(TileEntity tile){
		destroyList.add(tile);
	}
	
	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if(event.type != Type.SERVER) return;
		for(TileEntity t : destroyList){
			if(t.getWorldObj() != null && t.getWorldObj().getTileEntity(t.xCoord, t.yCoord, t.zCoord) == t){
				t.getWorldObj().setBlockToAir(t.xCoord, t.yCoord, t.zCoord);
				Utils.println("destroy light at " + t.xCoord + " " + t.yCoord + " " +  t.zCoord);
			}
		}
		destroyList.clear();
	}



}
