package mods.eln.entity;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

public class ReplicatorHungryAttack extends EntityAINearestAttackableTarget {

    ReplicatorEntity replicator;

    public ReplicatorHungryAttack(ReplicatorEntity replicator, Class par2Class, int par3, boolean par4) {
        super(replicator, par2Class, par3, par4, false, null); // 1.7.10 (creature, class, chance, checkSight)
        this.replicator = replicator;
    }

    @Override
    public boolean shouldExecute() {
        if (replicator.hunger < replicator.hungerToCanibal) return false;
        return super.shouldExecute();
    }
}
