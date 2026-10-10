package mods.eln.entity;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** Hungry replicators hunt other replicators (1.12 ReplicatorHungryAttack; target chance 0 = every tick). */
public class ReplicatorHungryAttack extends NearestAttackableTargetGoal<ReplicatorEntity> {

    final ReplicatorEntity replicator;

    public ReplicatorHungryAttack(ReplicatorEntity replicator) {
        super(replicator, ReplicatorEntity.class, 0, false, false, null);
        this.replicator = replicator;
    }

    @Override
    public boolean canUse() {
        if (replicator.hunger < replicator.hungerToCanibal) return false;
        return super.canUse();
    }
}
