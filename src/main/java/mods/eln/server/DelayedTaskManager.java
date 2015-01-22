package mods.eln.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.ore.OreDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.server.FMLServerHandler;

public class DelayedTaskManager {

	public DelayedTaskManager() {
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}

	LinkedList<ITask> tasks = new LinkedList<DelayedTaskManager.ITask>();
	
	public void clear(){
		tasks.clear();
	}
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if(event.phase != Phase.END) return;
		ArrayList<ITask> cpy = new ArrayList<DelayedTaskManager.ITask>(tasks);
		tasks.clear();
		for(ITask t : cpy){
			t.run();
		}
	}
	
	interface ITask{
		void run();
	}

	public void add(ITask t) {
		tasks.add(t);
		
	}
	
}
