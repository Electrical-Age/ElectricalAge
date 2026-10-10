package mods.eln.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackMelee;

/**
 * 1.7.10 EntityAIAttackOnCollide(creature, targetClass, speed, longMemory): melee attack that only runs while the
 * attack target is an instance of targetClass. 1.12's EntityAIAttackMelee has no class filter; this adds it back.
 */
public class AttackMeleeOfClass extends EntityAIAttackMelee {
    private final Class<?> targetClass;
    private final EntityCreature owner;

    public AttackMeleeOfClass(EntityCreature creature, Class<?> targetClass, double speed, boolean longMemory) {
        super(creature, speed, longMemory);
        this.owner = creature;
        this.targetClass = targetClass;
    }

    private boolean targetMatches() {
        EntityLivingBase target = owner.getAttackTarget();
        return target != null && targetClass.isAssignableFrom(target.getClass());
    }

    @Override
    public boolean shouldExecute() {
        return targetMatches() && super.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetMatches() && super.shouldContinueExecuting();
    }
}
