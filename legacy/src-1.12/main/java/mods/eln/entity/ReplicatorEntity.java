package mods.eln.entity;

import mods.eln.misc.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;

public class ReplicatorEntity extends EntityMob {

    boolean isSpawnedFromWeather = false;
    double hungerTime = 10 * 60;
    double hungerToEnergy = 10.0 * hungerTime;
    double energyToDuplicate = 10000;
    double hungerToDuplicate = -energyToDuplicate / hungerToEnergy;
    double hungerToCanibal = 0.6;

    public static final ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();

    public ReplicatorEntity(World par1World) {
        super(par1World);

        enablePersistence(); //persistenceRequired

        this.setSize(0.3F, 0.7F);

        ReplicatoCableAI replicatorIa = new ReplicatoCableAI(this);
        int p = 0;

        this.tasks.addTask(p++, new EntityAISwimming(this));
        // this.tasks.addTask(p++, new EntityAIBreakDoor(this));
        this.tasks.addTask(p++, new AttackMeleeOfClass(this, EntityPlayer.class, 1.0D, false));
        this.tasks.addTask(p++, new AttackMeleeOfClass(this, EntityVillager.class, 1.0D, true));
        this.tasks.addTask(p++, new AttackMeleeOfClass(this, ReplicatorEntity.class, 1.0D, true));
        this.tasks.addTask(p++, replicatorIa);
        this.tasks.addTask(p++, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(p++, new EntityAIMoveThroughVillage(this, 1.0D, false));
        this.tasks.addTask(p++, new ConfigurableAiWander(this, 1.0D, 20));
        this.tasks.addTask(p, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(p++, new EntityAILookIdle(this));
        p = 1;
        this.targetTasks.addTask(p++, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(p, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 0, true, false, null));
        this.targetTasks.addTask(p, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, 0, false, false, null));
        this.targetTasks.addTask(p++, new ReplicatorHungryAttack(this, ReplicatorEntity.class, 0, false));
        // this.targetTasks.addTask(p++, new EntityAINearestAttackableTarget(this, ReplicatorEntity.class, 0, false));
        // this.targetTasks.addTask(p++, replicatorIa);
    }

    @Override
    public boolean attackEntityAsMob(Entity e) {
        if (e instanceof ReplicatorEntity) {
            this.hunger -= 0.4;
            ((ReplicatorEntity) e).hunger += 0.4;
        }
        return super.attackEntityAsMob(e);
    }

    @Override
    protected void updateAITasks() { // 1.7.10 updateAITick (called from updateAITasks there)
        super.updateAITasks();
        //setDead();
        hunger += 0.05 / hungerTime;

        if (hunger > 1 && Math.random() < 0.05 / 5) {
            attackEntityFrom(DamageSource.STARVE, 1);
        }
        if (hunger < 0.5 && Math.random() * 10 < 0.05) {
            heal(1f);
        }
        if (hunger < hungerToDuplicate) {
            ReplicatorEntity entityliving = new ReplicatorEntity(this.world);
            entityliving.setLocationAndAngles(this.posX, this.posY, this.posZ, 0f, 0f);
            entityliving.rotationYawHead = entityliving.rotationYaw;
            entityliving.renderYawOffset = entityliving.rotationYaw;
            world.spawnEntity(entityliving);
            entityliving.playLivingSound();
            hunger = 0;
        }
    }

    void eatElectricity(double e) {
        hunger = hunger - Math.min(0.001, e / hungerToEnergy);
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23000000417232513D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        // this.getAttributeMap().func_111150_b(field_110186_bp).setAttribute(this.rand.nextDouble() * ForgeDummyContainer.zombieSummonBaseChance);
    }

    protected boolean isAIEnabled() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SILVERFISH_AMBIENT; // 1.7.10 "mob.silverfish.say"
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SILVERFISH_HURT; // "mob.silverfish.hit"
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SILVERFISH_DEATH; // "mob.silverfish.kill"
    }

    /*protected void playStepSound(int par1, int par2, int par3, int par4) {
        this.playSound("mob.silverfish.step", 0.15F, 1.0F);
    }

    protected int getDropItemId() {
        return Item.getIdFromItem(Items.ROTTEN_FLESH);
    }*/

    @Override
    protected void dropFewItems(boolean par1, int par2) {
        this.entityDropItem(dropList.get(new Random().nextInt(dropList.size())).copy(), 0.5f);

        if (isSpawnedFromWeather) {
            if (Math.random() < 0.33) {
                // 1.7.10: spawn egg with the replicator's global entity id as damage; 1.12: egg tagged with the entity id
                ResourceLocation id = EntityList.getKey(ReplicatorEntity.class);
                if (id != null) {
                    ItemStack egg = new ItemStack(Items.SPAWN_EGG);
                    ItemMonsterPlacer.applyEntityIdToItemStack(egg, id);
                    this.entityDropItem(egg, 0.5f);
                }
            }
        }
    }

    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEFINED;
    }

    double hunger = (Math.random() - 0.5) * 0.3;

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setDouble("ElnHunger", hunger);
        nbt.setBoolean("isSpawnedFromWeather", isSpawnedFromWeather);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);

        hunger = nbt.getDouble("ElnHunger");
        isSpawnedFromWeather = nbt.getBoolean("isSpawnedFromWeather");

        Utils.println("[Replicator] " + posX + " " + posY + " " + posZ + " ");
    }
}
