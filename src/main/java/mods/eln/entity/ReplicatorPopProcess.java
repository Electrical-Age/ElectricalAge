package mods.eln.entity;

import mods.eln.Eln;
import mods.eln.compat.WorldCompat;
import mods.eln.misc.Utils;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.IProcess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LightLayer;

/**
 * Thunderstorms pop replicators near players (1.12 ReplicatorPopProcess, overworld only): each player gets one per
 * 1 / popPerSecondPerPlayer s on average, within 100 blocks, at the first dark (block light <= 6) air above y 2.
 * Also caps the overworld at 100 replicators.
 */
public class ReplicatorPopProcess implements IProcess {

    public static double popPerSecondPerPlayer = 1.0 / 60;

    @Override
    public void process(double time) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel world = server.overworld();

        int replicatorCount = 0;
        for (ReplicatorEntity r : world.getEntities(ElnRegistry.REPLICATOR.get(), e -> true)) {
            replicatorCount++;
            if (replicatorCount > 100) r.discard();
        }

        if (world.getDifficulty() == Difficulty.PEACEFUL) return;

        if (world.isThundering()) {
            for (ServerPlayer player : world.players()) {
                if (Math.random() * (world.players().size()) < time * popPerSecondPerPlayer) {
                    int x, y, z;
                    x = (int) (player.getX() + Utils.rand(-100, 100));
                    z = (int) (player.getZ() + Utils.rand(-100, 100));
                    y = 2;
                    Utils.println("POP");

                    if (!WorldCompat.blockExists(world, x, y, z)) break;

                    while (!Utils.isAirBlock(WorldCompat.getBlock(world, x, y, z)) || Utils.getLight(world, LightLayer.BLOCK, x, y, z) > 6) {
                        y++;
                        if (y >= world.getMaxY()) break;
                    }

                    ReplicatorEntity entityliving = ElnRegistry.REPLICATOR.get().create(world, EntitySpawnReason.EVENT);
                    if (entityliving == null) continue;
                    entityliving.snapTo(x + 0.5, y, z + 0.5, 0f, 0f);
                    entityliving.yHeadRot = entityliving.getYRot();
                    entityliving.yBodyRot = entityliving.getYRot();
                    entityliving.isSpawnedFromWeather = true;
                    world.addFreshEntity(entityliving);
                    entityliving.playAmbientSound();
                    Utils.println("Spawn Replicator at " + x + " " + y + " " + z);
                }
            }
        }
    }
}
