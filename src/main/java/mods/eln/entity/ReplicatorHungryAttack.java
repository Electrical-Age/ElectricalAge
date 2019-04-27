package mods.eln.entity;

import com.google.common.base.Predicate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

public class ReplicatorHungryAttack extends EntityAINearestAttackableTarget {

    ReplicatorEntity replicator;

    public ReplicatorHungryAttack(ReplicatorEntity replicator, Class classTarget, int chance, boolean checkSight, boolean onlyNearby, Predicate targetSelector) {
        super(replicator, classTarget, chance, checkSight, onlyNearby, targetSelector);
        this.replicator = replicator;
    }

    @Override
    public boolean shouldExecute() {
        if (replicator.hunger < replicator.hungerToCanibal) return false;
        return super.shouldExecute();
    }
}
