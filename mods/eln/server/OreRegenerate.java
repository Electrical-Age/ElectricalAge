package mods.eln.server;

import java.util.LinkedList;
import java.util.Random;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.ore.OreDescriptor;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

public class OreRegenerate {

	public OreRegenerate() {
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}

	public static class Job{
		World world; Chunk chunk;
		public Job(World world, Chunk chunk) {
			this.world = world;
			this.chunk = chunk;
		}
		
	}
	
	
	LinkedList<Job> jobs = new LinkedList<Job>();
	
	public void clear(){
		jobs.clear();
	}
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if(event.phase != Phase.START) return;
		if(jobs.size() != 0){
			Job j = jobs.pop();
			if(Eln.instance.saveConfig.reGenOre == false) return; 
			if(j.world.getChunkProvider().chunkExists(j.chunk.xPosition, j.chunk.zPosition) == false) return;
			for(int y = 0;y < 60;y+=2){
				for(int z = y & 1;z < 16;z+=2){
					for(int x = y & 1;x < 16;x+=2){
						if(j.chunk.getBlock(x, y, z) == Eln.oreBlock){
						//	Utils.println("NO Regenrate ore ! left " + jobs.size());
							return;
						}
					}					
				}			
			}
			
			for(OreDescriptor d : Eln.oreItem.descriptors){
				d.generate(j.world.rand, j.chunk.xPosition, j.chunk.zPosition, j.world, null, null);
			}
			//Utils.println("Regenrate ore ! left " + jobs.size());

			return;
		}
	}
	
	@SubscribeEvent
	public void chunkLoad(ChunkEvent.Load e) {
		if(e.world.isRemote == true) return;
		jobs.push(new Job(e.world,e.getChunk()));
	}
		
}
