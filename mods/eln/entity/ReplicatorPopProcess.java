package mods.eln.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import cpw.mods.fml.common.FMLCommonHandler;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.sim.IProcess;

public class ReplicatorPopProcess implements IProcess {

	public ReplicatorPopProcess() {

	}
	
	double popPerSecondPerPlayer = 1.0 / 15;

	@Override
	public void process(double time) {
		World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
		if (world.getWorldInfo().isThundering()) {
	        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();       

		    for (Object obj :  server.getConfigurationManager().playerEntityList){
		    	EntityPlayerMP player = (EntityPlayerMP) obj;
				if (Math.random() < time * popPerSecondPerPlayer && player.worldObj == world) {
	
					while (true) {
						int x, y, z;				
						Random rand = new Random();
						x = (int) (player.posX + Utils.rand(-100,100));
						z = (int) (player.posZ + Utils.rand(-100,100));
						y = 2;
						
						if(world.blockExists(x, y, z) == false) break;
						while (world.getBlock(x, y, z) != Blocks.air 
								|| Utils.getLight(world,EnumSkyBlock.Block,x,y,z) > 6) {
							y++;
						}
						ReplicatorEntity entityliving = new ReplicatorEntity(world);
						entityliving.setLocationAndAngles(x + 0.5, y, z + 0.5, 0f, 0f);
						entityliving.rotationYawHead = entityliving.rotationYaw;
						entityliving.renderYawOffset = entityliving.rotationYaw;
						world.spawnEntityInWorld(entityliving);
						entityliving.playLivingSound();
						entityliving.isSpawnedFromWeather = true;
						Utils.println("Spawn Replicator at " + x + " " + y + " " + z);					
	
						break;
					}
				}
			}
		}
	}

}
/*		World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
if (world.getWorldInfo().isThundering()) {

if (Math.random() < time * popPerSecondPerChunk * world.getChunkProvider().getLoadedChunkCount()) {

	while (true) {
		int x, y, z;
		try {
			Random rand = new Random();
			Class c = ChunkProviderServer.class;
			Field f = c.getDeclaredField("loadedChunks");
			f.setAccessible(true);
			ArrayList chunks = (ArrayList) f.get(world.getChunkProvider());
			if(chunks.size() == 0) return;
			Chunk chunk = (Chunk) chunks.get(rand.nextInt(chunks.size()));
			x = (chunk.xPosition*16 + rand.nextInt(16));
			z = (chunk.zPosition*16 + rand.nextInt(16));
			
			y = 2;
			
			if(world.blockExists(x, y, z) == false) break;
			while (world.getBlockId(x, y, z) != 0 
					|| world.getSkyBlockTypeBrightness(EnumSkyBlock.Block,x,y,z) > 6) {
				y++;
			}
			ReplicatorEntity entityliving = new ReplicatorEntity(world);
			entityliving.setLocationAndAngles(x + 0.5, y, z + 0.5, 0f, 0f);
			entityliving.rotationYawHead = entityliving.rotationYaw;
			entityliving.renderYawOffset = entityliving.rotationYaw;
			world.spawnEntityInWorld(entityliving);
			entityliving.playLivingSound();
			entityliving.isSpawnedFromWeather = true;
		//	Utils.println("Spawn Replicator at " + x + " " + y + " " + z);					
		
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		break;
	}
}
}
*/