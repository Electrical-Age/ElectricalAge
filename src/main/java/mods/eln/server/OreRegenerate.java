package mods.eln.server;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.ore.OreDescriptor;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import java.util.HashSet;
import java.util.LinkedList;

public class OreRegenerate {

    LinkedList<ChunkRef> jobs = new LinkedList<ChunkRef>();
    HashSet<ChunkRef> alreadyLoadedChunks = new HashSet<ChunkRef>();

    public OreRegenerate() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    static class ChunkRef {
        public int x, z;
        public int worldId;

        public ChunkRef(int x, int z, int worldId) {
            this.x = x;
            this.z = z;
            this.worldId = worldId;
        }

        @Override
        public int hashCode() {
            return x * z + (worldId << 20);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChunkRef)) return false;
            ChunkRef other = (ChunkRef) o;
            return other.x == x && other.z == z && other.worldId == worldId;
        }
    }

    public void clear() {
        jobs.clear();
        alreadyLoadedChunks.clear();
    }

    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase != Phase.START) return;
        for (int idx = 0; idx < 1; idx++) {
            if (!jobs.isEmpty()) {
                ChunkRef j = jobs.pollLast();
                if (!Eln.saveConfig.reGenOre && !Eln.instance.forceOreRegen) return;

                WorldServer server = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(j.worldId);
                Chunk chunk = server.getChunkFromChunkCoords(j.x, j.z);

                for (int y = 0; y < 60; y += 2) {
                    for (int z = y & 1; z < 16; z += 2) {
                        for (int x = y & 1; x < 16; x += 2) {
                            if (chunk.getBlockState(x, y, z).getBlock() == Eln.oreBlock) {
                                //	Utils.println("NO Regenrate ore ! left " + jobs.size());
                                return;
                            }
                        }
                    }
                }

                Utils.println("Regenerated! " + jobs.size());
                for (OreDescriptor d : Eln.oreItem.descriptors) {
                    d.generate(server.rand, chunk.x, chunk.z, server, null, null);
                }
                //Utils.println("Regenrate ore! left " + jobs.size());
            }
        }
    }

    @SubscribeEvent
    public void chunkLoad(ChunkEvent.Load e) {
        //	if (e.world.isRemote == false) Utils.println("Chunk loaded!");
        if (e.getWorld().isRemote || (Eln.saveConfig != null && !Eln.saveConfig.reGenOre)) return;
        Chunk c = e.getChunk();
        ChunkRef ref = new ChunkRef(c.x, c.z, c.getWorld().provider.getDimension());
        if (alreadyLoadedChunks.contains(ref)) {
            Utils.println("Already regenerated!");
            return;
        }
        alreadyLoadedChunks.add(ref);
        jobs.addFirst(ref);
    }
}
