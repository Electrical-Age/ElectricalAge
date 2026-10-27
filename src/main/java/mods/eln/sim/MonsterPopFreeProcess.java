package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Removes monsters that appear within range while {@code active} holds (spawn suppression; mobs already around when
 * it turns on are left alone). Baughn 2026-10-27: a lamp keeps monsters away only while it is lit.
 */
public class MonsterPopFreeProcess implements IProcess {

    private Coordonate coordonate;
    private int range;
    private final BooleanSupplier active;

    double timerCounter = 0;
    final double timerPeriod = 0.212;

    List oldList = null;

    public MonsterPopFreeProcess(Coordonate coordonate, int range, BooleanSupplier active) {
        this.coordonate = coordonate;
        this.range = range;
        this.active = active;
    }

    @Override
    public void process(double time) {
        //Monster killing must be active before continuing :
        if (!Eln.killMonstersAroundLamps)
            return;

        timerCounter += time;
        if (timerCounter > timerPeriod) {
            timerCounter -= Utils.rand(1, 1.5) * timerPeriod;
            List list = coordonate.world().getEntitiesOfClass(Monster.class, coordonate.getAxisAlignedBB(range + 8));

            boolean on = active.getAsBoolean();
            for (Object o : list) {
                if (!on) break;
                //Utils.println("MonsterPopFreeProcess : In range");
                Monster mob = (Monster) o;
                if (oldList == null || !oldList.contains(o)) {
                    if (coordonate.distanceTo(mob) < range) {
                        //Utils.println("MonsterPopFreeProcess : Must die");
                        // 1.12 also spared the replicator (Wp12, not ported yet: it is no Monster subclass check here)
                        if (!(o instanceof WitherBoss) && !(o instanceof Enderman)) {
                            mob.discard();
                            Utils.println("MonsterPopFreeProcess : Dead");
                        }
                    }
                }
            }
            oldList = list;
        }
    }

}
