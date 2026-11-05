package mods.eln.robotics.servo;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * The car's call panel, on the rail side at chest height: a part of the car (as a dragon's head is of the dragon) that
 * can be clicked but not stood on. Its top half sends the car up a station, its bottom half down one.
 */
public class ElevatorPanel extends PartEntity<ElevatorCar> {
    /** Width and height (m), and where it sits: toward the rail and up from the plate's bottom centre. */
    static final float W = 0.4f, H = 0.6f;
    static final double OUT = ElevatorCar.HALF - ElevatorCar.WALL - 0.2, UP = 0.85;   // (inside the back wall)
    final EntityDimensions size = EntityDimensions.fixed(W, H);

    public ElevatorPanel(ElevatorCar car) {
        super(car);
        refreshDimensions();
    }

    /** Stands where the car's panel is. */
    void follow() {
        ElevatorCar c = getParent();
        Vec3 p = c.position().add(c.railSide.getStepX() * OUT, UP, c.railSide.getStepZ() * OUT);
        setPos(p);
        xo = xOld = p.x;
        yo = yOld = p.y;
        zo = zOld = p.z;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide()) getParent().panelPressed(player, location.y > H / 2);
        return InteractionResult.SUCCESS;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean isPickable() {
        return !getParent().isRemoved();
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }
}
