package mods.eln.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Random wander with a configurable chance (1.12 ConfigurableAiWander: 1 in randLimit per check). */
public class ConfigurableAiWander extends Goal {

    private final PathfinderMob entity;
    private double xPosition;
    private double yPosition;
    private double zPosition;
    private final double speed;

    private final int randLimit;

    public ConfigurableAiWander(PathfinderMob entity, double speed, int randLimit) {
        this.entity = entity;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE)); // 1.12 mutex bits 1
        this.randLimit = randLimit;
    }

    @Override
    public boolean canUse() {
        if (this.entity.getNoActionTime() >= 100) {
            return false;
        } else if (this.entity.getRandom().nextInt(randLimit) != 0) {
            return false;
        } else {
            Vec3 vec3 = DefaultRandomPos.getPos(this.entity, 10, 7);
            if (vec3 == null) {
                return false;
            } else {
                this.xPosition = vec3.x;
                this.yPosition = vec3.y;
                this.zPosition = vec3.z;
                return true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.entity.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.entity.getNavigation().moveTo(this.xPosition, this.yPosition, this.zPosition, this.speed);
    }
}
