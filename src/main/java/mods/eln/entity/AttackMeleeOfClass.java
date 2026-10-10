package mods.eln.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * 1.7.10 EntityAIAttackOnCollide(creature, targetClass, speed, longMemory): melee attack that only runs while the
 * attack target is an instance of targetClass (1.12 / 26.x melee goals have no class filter).
 */
public class AttackMeleeOfClass extends MeleeAttackGoal {
    private final Class<?> targetClass;
    private final PathfinderMob owner;

    public AttackMeleeOfClass(PathfinderMob creature, Class<?> targetClass, double speed, boolean longMemory) {
        super(creature, speed, longMemory);
        this.owner = creature;
        this.targetClass = targetClass;
    }

    private boolean targetMatches() {
        LivingEntity target = owner.getTarget();
        return target != null && targetClass.isAssignableFrom(target.getClass());
    }

    @Override
    public boolean canUse() {
        return targetMatches() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return targetMatches() && super.canContinueToUse();
    }
}
