package mods.eln.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import mods.eln.misc.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.ai.EntityAIEatGrass;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

public class ReplicatorEntity extends EntityMob {

	private ReplicatoCableAI replicatorIa = new ReplicatoCableAI(this);
	public ReplicatorEntity(World par1World) {
		super(par1World);
		

		func_110163_bv(); //persistenceRequired
		/*try {
			Class c = EntityLiving.class;
			Field f;
			for(Field ff : c.getDeclaredFields()){
				Utils.println(ff.getName());
			}
			
			f = c.getDeclaredField("persistenceRequired");
			f.setAccessible(true);		
			f.set(this, true);
		} catch (NoSuchFieldException e) {
			
			e.printStackTrace();
		} catch (SecurityException e) {
			
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			
			e.printStackTrace();
		}*/

		
	//	Utils.println("new replicator");
        this.setSize(0.3F, 0.7F);
		
		int p = 0;
		
        this.tasks.addTask(p++, new EntityAISwimming(this));
       // this.tasks.addTask(p++, new EntityAIBreakDoor(this));
        this.tasks.addTask(p++, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
        this.tasks.addTask(p++, new EntityAIAttackOnCollide(this, EntityVillager.class, 1.0D, true));
        this.tasks.addTask(p++, new EntityAIAttackOnCollide(this, ReplicatorEntity.class, 1.0D, true));
        this.tasks.addTask(p++, replicatorIa);
        this.tasks.addTask(p++, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(p++, new EntityAIMoveThroughVillage(this, 1.0D, false));
        this.tasks.addTask(p++, new ConfigurableAiWander(this, 1.0D,20));
        this.tasks.addTask(p, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(p++, new EntityAILookIdle(this));
        p = 1;
        this.targetTasks.addTask(p++, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(p, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        this.targetTasks.addTask(p, new EntityAINearestAttackableTarget(this, EntityVillager.class, 0, false));
        this.targetTasks.addTask(p++, new ReplicatorHungryAttack(this, ReplicatorEntity.class, 0, false));
      //  this.targetTasks.addTask(p++, new EntityAINearestAttackableTarget(this, ReplicatorEntity.class, 0, false));
       // this.targetTasks.addTask(p++, replicatorIa);

	}
	
	@Override
	protected void despawnEntity() {
		
		//Utils.println("Replicator despawn");
		super.despawnEntity();
	}
	
	@Override
	public boolean attackEntityAsMob(Entity e) {
		
		if(e instanceof ReplicatorEntity){
			this.hunger -= 0.4;
			((ReplicatorEntity)e).hunger += 0.4;	
		//	Utils.print("ATTAQUE");
		}
		return super.attackEntityAsMob(e);
	}
	
	boolean isSpawnedFromWeather = false;
	double hungerTime = 10*60;
	//double hungerTime = 20;
	double hungerToEnergy = 10.0*hungerTime;
	double energyToDuplicate = 10000;
	double hungerToDuplicate = - energyToDuplicate / hungerToEnergy;
	double hungerToCanibal = 0.6;
	
	@Override
	protected void updateAITick() {
		
		super.updateAITick();
		//setDead();
//		Utils.print(hunger + " ");
		hunger += 0.05/hungerTime;
		if(hunger > 1){
			if(Math.random() < 0.05/5)
				attackEntityFrom(DamageSource.starve, 1);
		} 
		if(hunger < 0.5){
			if(Math.random() * 10 < 0.05){
				heal(1f);
			}
		}
		if(hunger < hungerToDuplicate){
			ReplicatorEntity entityliving = new ReplicatorEntity(this.worldObj);
			entityliving.setLocationAndAngles(this.posX,this.posY,this.posZ,0f,0f);
            entityliving.rotationYawHead = entityliving.rotationYaw;
            entityliving.renderYawOffset = entityliving.rotationYaw;
            worldObj.spawnEntityInWorld(entityliving);
            entityliving.playLivingSound();
            hunger = 0;
		}
	}
	
	void eatElectricity(double e){
		hunger = hunger -  Math.min(0.001,e/hungerToEnergy);
	}
	
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.23000000417232513D);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(3.0D);
       // this.getAttributeMap().func_111150_b(field_110186_bp).setAttribute(this.rand.nextDouble() * ForgeDummyContainer.zombieSummonBaseChance);
    }

    
    protected boolean isAIEnabled()
    {
        return true;
    }

    /**
     * Returns the sound this mob makes while it's alive.
     */
    protected String getLivingSound()
    {
        return "mob.silverfish.say";
    }

    /**
     * Returns the sound this mob makes when it is hurt.
     */
    protected String getHurtSound()
    {
        return "mob.silverfish.hurt";
    }

    /**
     * Returns the sound this mob makes on death.
     */
    protected String getDeathSound()
    {
        return "mob.silverfish.death";
    }

    /**
     * Plays step sound at given x, y, z for the entity
     */
    protected void playStepSound(int par1, int par2, int par3, int par4)
    {
        this.playSound("mob.silverfish.step", 0.15F, 1.0F);
    } 
    
    protected int getDropItemId()
    {
        return Item.getIdFromItem(Items.rotten_flesh);
    }
    
    public static ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();
    @Override
    protected void dropFewItems(boolean par1, int par2) {
    	
    	this.entityDropItem(dropList.get(new Random().nextInt(dropList.size())).copy(), 0.5f);
    	if(isSpawnedFromWeather == true){
    		if(Math.random() < 0.33){
    			for(Object s : EntityList.IDtoClassMapping.entrySet()){
    				Entry e = (Entry)s;
    				if(e.getValue() == ReplicatorEntity.class){
    					this.entityDropItem(new ItemStack((Item)Item.itemRegistry.getObject("spawn_egg"),1,(Integer)e.getKey()), 0.5f);
    					break;
    				}
    			}
    		}
    	}
    }
    
    
    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.UNDEFINED;
    }
    
    double hunger = (Math.random()-0.5)*0.3;
    
    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
    	super.writeEntityToNBT(nbt);
    	nbt.setDouble("ElnHunger",hunger);
    	nbt.setBoolean("isSpawnedFromWeather", isSpawnedFromWeather);
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
    	super.readEntityFromNBT(nbt);
    	
    	hunger = nbt.getDouble("ElnHunger");
    	isSpawnedFromWeather = nbt.getBoolean("isSpawnedFromWeather");

    	
    	Utils.println("[Replicator] " + posX + " " + posY + " " + posZ + " " );
    }
    
	
}
