package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorPopProcess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static mods.eln.Eln.*;

/**
 * Device batch Wp12e (26.x port, split from 1.12 Wp12Content): the replicator mob (entity type and spawn egg in
 * ElnRegistry, attributes in Eln) and its thunderstorm spawning.
 */
public class Wp12eContent implements ElnContent {
    @Override
    public void serverAboutToStart() {
        // 1.12 registerReplicator: what a replicator drops (stacks can only exist once the server starts)
        ReplicatorEntity.dropList.clear();
        ReplicatorEntity.dropList.add(findItemStack("Iron Dust"));
        ReplicatorEntity.dropList.add(findItemStack("Copper Dust"));
        ReplicatorEntity.dropList.add(findItemStack("Gold Dust"));
        ReplicatorEntity.dropList.add(new ItemStack(Items.REDSTONE));
        ReplicatorEntity.dropList.add(new ItemStack(Items.GLOWSTONE_DUST));
        ReplicatorPopProcess.popPerSecondPerPlayer = replicatorPopPerSecondPerPlayer;
        if (replicatorPop) simulator.addSlowProcess(new ReplicatorPopProcess());
    }
}
