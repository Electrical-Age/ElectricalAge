package mods.eln.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

public class ReplicatorPopProcess implements IProcess {

    public ReplicatorPopProcess() {
    }

    public static double popPerSecondPerPlayer = 1.0 / 60;

    @Override
    public void process(double time) {
        World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];

        int replicatorCount = 0;

        for (Object o : world.loadedEntityList) {
            if (o instanceof ReplicatorEntity) {
                replicatorCount++;
                if (replicatorCount > 100) {
                    ReplicatorEntity r = (ReplicatorEntity) o;
                    r.setDead();
                }
            }
        }

        if (world.getDifficulty() == EnumDifficulty.PEACEFUL) return;

        if (world.getWorldInfo().isThundering()) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            for (Object obj : world.playerEntities) {
                EntityPlayerMP player = (EntityPlayerMP) obj;
                if (Math.random() * (world.playerEntities.size()) < time * popPerSecondPerPlayer && player.worldObj == world) {
                    int x, y, z;
                    x = (int) (player.posX + Utils.rand(-100, 100));
                    z = (int) (player.posZ + Utils.rand(-100, 100));
                    y = 2;
                    BlockPos pos = new BlockPos(x,y,z)
                    Utils.println("POP");

                    if (world.blockExists(pos) == false) break;

                    while (world.isAirBlock(pos) || Utils.getLight(world, EnumSkyBlock.BLOCK, pos) > 6) {
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
			
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			
			e.printStackTrace();
		} catch (SecurityException e) {
			
			e.printStackTrace();
		}

		break;
	}
}
}
*/
