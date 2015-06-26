package mods.eln.sim;

import mods.eln.entity.ReplicatorEntity;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;

import java.util.List;

public class MonsterPopFreeProcess implements IProcess {

    private Coordonate coordonate;
    private int range;

    double timerCounter = 0;
    final double timerPeriod = 0.212;

    List oldList = null;

    public MonsterPopFreeProcess(Coordonate coordonate, int range) {
        this.coordonate = coordonate;
        this.range = range;
    }

    @Override
    public void process(double time) {
        timerCounter += time;
        if (timerCounter > timerPeriod) {
            timerCounter -= Utils.rand(1, 1.5) * timerPeriod;
            List list = coordonate.world().getEntitiesWithinAABB(EntityMob.class, coordonate.getAxisAlignedBB(range + 8));

            for (Object o : list) {
                //Utils.println("MonsterPopFreeProcess : in RANGE");
                EntityMob mob = (EntityMob) o;
                if (oldList == null || !oldList.contains(o)) {
                    if (coordonate.distanceTo(mob) < range) {
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
