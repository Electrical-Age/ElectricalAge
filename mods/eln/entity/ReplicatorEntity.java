package mods.eln.entity;

import net.minecraft.entity.ai.EntityAIEatGrass;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.world.World;

public class ReplicatorEntity extends EntitySheep {

	private EntityAIEatGrass aiEatGrass = new EntityAIEatGrass(this);
	private ReplicatoCableAI replicatorIa = new ReplicatoCableAI(this);
	private LookingForCableAi lookingForCableAi = new LookingForCableAi(this);
	public ReplicatorEntity(World par1World) {
		super(par1World);
		//this.tasks.addTask(18, replicatorIa);
		// this.tasks.addTask(0, this.aiEatGrass);
		
		this.tasks.addTask(-1, replicatorIa);
	}

	public void onUpdate(){
		 super.onUpdate();
	//	 System.out.println("");
	}
	
	
}
