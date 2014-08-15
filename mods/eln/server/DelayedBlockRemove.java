package mods.eln.server;

import java.util.HashSet;

import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.server.DelayedTaskManager.ITask;

public class DelayedBlockRemove implements ITask{
	private static HashSet<Coordonate> blocks = new HashSet<Coordonate>(); 
	
	private DelayedBlockRemove(Coordonate c) {
		this.c = c;
	}
	
	
	public static void clear(){
		blocks.clear();
	}
	
	
	public static void add(Coordonate c){
		if(blocks.contains(c))return;
		blocks.add(c);
		Eln.delayedTask.add(new DelayedBlockRemove(c));
	}
	
	Coordonate c;
	
	
	@Override
	public void run() {
		blocks.remove(c);
		c.setBlock(Blocks.air);
	}

}
