package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;

import java.util.List;

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
        //Monster killing must be active before continuing :
        if (!Eln.instance.killMonstersAroundLamps)
            return;

        timerCounter += time;
        if (timerCounter > timerPeriod) {
            timerCounter -= Utils.rand(1, 1.5) * timerPeriod;
            List list = coordinate.world().getEntitiesWithinAABB(EntityMob.class, coordinate.getAxisAlignedBB(range + 8));

            for (Object o : list) {
                //Utils.println("MonsterPopFreeProcess : In range");
                EntityMob mob = (EntityMob) o;
                if (oldList == null || !oldList.contains(o)) {
                    if (coordinate.distanceTo(mob) < range) {
                        //Utils.println("MonsterPopFreeProcess : Must die");
                        if (!(o instanceof ReplicatorEntity) && !(o instanceof EntityWither) && !(o instanceof EntityEnderman)) {
                            mob.setDead();
                            Utils.println("MonsterPopFreeProcess : Dead");
                        }
                    }
                }
            }
            oldList = list;
        }
    }

}
