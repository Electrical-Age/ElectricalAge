package mods.eln.sim;

import java.util.List;

import mods.eln.entity.ReplicatorEntity;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;

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
			timerCounter -= Utils.rand(1,1.5)*timerPeriod;
			List list = coordonate.world().getEntitiesWithinAABB(EntityMob.class, coordonate.getAxisAlignedBB( range + 8));
			
			for(Object o : list)
			{
				//Utils.println("MonsterPopFreeProcess : in RANGE");
				EntityMob mob = (EntityMob) o;
				if(oldList == null || oldList.contains(o) == false)
				{
					if(coordonate.distanceTo(mob) < range){
						//Utils.println("MonsterPopFreeProcess : Must die");
						if(o instanceof ReplicatorEntity == false && o instanceof EntityWither == false && o instanceof EntityEnderman == false){
							mob.setDead();
							Utils.println("MonsterPopFreeProcess : dead");
						}
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
