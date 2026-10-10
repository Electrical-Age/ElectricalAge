package mods.eln.entity;

import mods.eln.misc.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Random;

/**
 * The replicator (1.12 ReplicatorEntity, an EntityMob): a small cable-eating mob. It looks for live (>= 30 V,
 * non-signal) cables within 15 blocks, sits on them drawing power through a resistor (eating it lowers its hunger),
 * duplicates when well fed, starves when hungry, and attacks players, villagers and (when hungry) other replicators.
 * Thunderstorms pop them near players (ReplicatorPopProcess). Silverfish sounds; render (silverfish model) in P3.
 */
public class ReplicatorEntity extends Monster {

    boolean isSpawnedFromWeather = false;
    double hungerTime = 10 * 60;
    double hungerToEnergy = 10.0 * hungerTime;
    double energyToDuplicate = 10000;
    double hungerToDuplicate = -energyToDuplicate / hungerToEnergy;
    double hungerToCanibal = 0.6;

    public static final ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();

    double hunger = (Math.random() - 0.5) * 0.3;

    public ReplicatorEntity(EntityType<? extends ReplicatorEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired(); // 1.12 enablePersistence
    }

    /** 1.12 applyEntityAttributes. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.FOLLOW_RANGE, 8.0D)
            .add(Attributes.MAX_HEALTH, 8.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.23000000417232513D)
            .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        int p = 0;
        this.goalSelector.addGoal(p++, new FloatGoal(this));
        this.goalSelector.addGoal(p++, new AttackMeleeOfClass(this, Player.class, 1.0D, false));
        this.goalSelector.addGoal(p++, new AttackMeleeOfClass(this, AbstractVillager.class, 1.0D, true));
        this.goalSelector.addGoal(p++, new AttackMeleeOfClass(this, ReplicatorEntity.class, 1.0D, true));
        this.goalSelector.addGoal(p++, new ReplicatoCableAI(this));
        this.goalSelector.addGoal(p++, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(p++, new MoveThroughVillageGoal(this, 1.0D, false, 4, () -> false));
        this.goalSelector.addGoal(p++, new ConfigurableAiWander(this, 1.0D, 20));
        this.goalSelector.addGoal(p, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(p++, new RandomLookAroundGoal(this));
        p = 1;
        this.targetSelector.addGoal(p++, new HurtByTargetGoal(this));
        // 1.12 target chance 0: every tick
        this.targetSelector.addGoal(p, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        this.targetSelector.addGoal(p, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 0, false, false, null));
        this.targetSelector.addGoal(p++, new ReplicatorHungryAttack(this));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity e) {
        if (e instanceof ReplicatorEntity r) {
            this.hunger -= 0.4;
            r.hunger += 0.4;
        }
        return super.doHurtTarget(level, e);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) { // 1.12 updateAITasks
        super.customServerAiStep(level);
        hunger += 0.05 / hungerTime;

        if (hunger > 1 && Math.random() < 0.05 / 5) {
            hurtServer(level, level.damageSources().starve(), 1);
        }
        if (hunger < 0.5 && Math.random() * 10 < 0.05) {
            heal(1f);
        }
        if (hunger < hungerToDuplicate) {
            ReplicatorEntity child = mods.eln.registry.ElnRegistry.REPLICATOR.get().create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
            if (child != null) {
                child.snapTo(getX(), getY(), getZ(), 0f, 0f);
                child.yHeadRot = child.getYRot();
                child.yBodyRot = child.getYRot();
                level.addFreshEntity(child);
                child.playAmbientSound();
            }
            hunger = 0;
        }
    }

    void eatElectricity(double e) {
        hunger = hunger - Math.min(0.001, e / hungerToEnergy);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT; // 1.7.10 "mob.silverfish.say"
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!dropList.isEmpty()) this.spawnAtLocation(level, dropList.get(new Random().nextInt(dropList.size())).copy(), 0.5f);
        if (isSpawnedFromWeather && Math.random() < 0.33) {
            this.spawnAtLocation(level, new ItemStack(mods.eln.registry.ElnRegistry.REPLICATOR_SPAWN_EGG.get()), 0.5f);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putDouble("ElnHunger", hunger);
        out.putBoolean("isSpawnedFromWeather", isSpawnedFromWeather);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        hunger = in.getDoubleOr("ElnHunger", 0.0);
        isSpawnedFromWeather = in.getBooleanOr("isSpawnedFromWeather", false);
        Utils.println("[Replicator] " + getX() + " " + getY() + " " + getZ() + " ");
    }
}
