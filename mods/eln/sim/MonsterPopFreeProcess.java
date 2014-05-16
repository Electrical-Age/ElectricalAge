package mods.eln.sim;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.misc.Coordonate;

public class MonsterPopFreeProcess implements IProcess{
	private Coordonate coordonate;
	private int range;
	public MonsterPopFreeProcess(Coordonate coordonate,int range) {
		this.coordonate = coordonate;
		this.range = range;

	}
	
	double timerCounter = 0;
	final double timerPeriod = 0.212;
	
	@Override
	public void process(double time) {
		timerCounter += time;
		if(timerCounter > timerPeriod)
		{
			timerCounter -= timerPeriod;
			List list = coordonate.world().getEntitiesWithinAABB(EntityMob.class, coordonate.getAxisAlignedBB( range + 8));
			
			for(Object o : list)
			{
				EntityMob mob = (EntityMob) o;
				if(oldList == null || oldList.contains(o) == false)
				{
					if(coordonate.distanceTo(mob) < range && o instanceof ReplicatorEntity == false){
						mob.setDead();
						System.out.println("MonsterPopFreeProcess : dead");
					}
				}
			}
			
			oldList = list;
		}
		
	}
	List oldList = null;
	/*class MobData{
		EntityMob mob;
		
	}*/

}
