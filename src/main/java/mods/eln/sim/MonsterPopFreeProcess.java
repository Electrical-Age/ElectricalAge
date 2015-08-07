package mods.eln.sim;

import java.util.List;

import mods.eln.entity.ReplicatorEntity;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;

public class MonsterPopFreeProcess implements IProcess {

	private Coordinate coordinate;
	private int range;

	double timerCounter = 0;
	final double timerPeriod = 0.212;

	List oldList = null;

	public MonsterPopFreeProcess(Coordinate coordinate, int range) {
		this.coordinate = coordinate;
		this.range = range;
	}

	@Override
	public void process(double time) {
		timerCounter += time;
		if (timerCounter > timerPeriod) {
			timerCounter -= Utils.rand(1, 1.5) * timerPeriod;
			List list = coordinate.world().getEntitiesWithinAABB(EntityMob.class, coordinate.getAxisAlignedBB(range + 8));
			
			for(Object o : list) {
				//Utils.println("MonsterPopFreeProcess : in RANGE");
				EntityMob mob = (EntityMob) o;
				if (oldList == null || !oldList.contains(o)) {
					if (coordinate.distanceTo(mob) < range) {
						//Utils.println("MonsterPopFreeProcess : Must die");
						if (!(o instanceof ReplicatorEntity) && !(o instanceof EntityWither) && !(o instanceof EntityEnderman)) {
							mob.setDead();
							Utils.println("MonsterPopFreeProcess : dead");
						}
					}
				}
			}
			oldList = list;
		}
	}

	/*class MobData{
		EntityMob mob;
	}*/
}
