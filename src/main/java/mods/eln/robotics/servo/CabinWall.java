package mods.eln.robotics.servo;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * A wall or the roof of the car's cabin (Baughn 2026-11-05: a 3x3x3 cabin outside, about 2.5 inside): a part of the
 * car, a thin box in the car's frame that creatures collide with, so riders can't walk out through it. Its box is set
 * from the car's position and rail side (makeBoundingBox), not from square entity dimensions. Solid as the car's plate
 * is: on the client to its player (which the client moves), on the server to everything but players.
 */
public class CabinWall extends PartEntity<ElevatorCar> {
    /** Its box in the car's frame (m): across (toward railSide's clockwise), up from the plate's bottom, out from the rail. */
    final double a0, a1, y0, y1, o0, o1;
    /** What it is: a side of the walled cabin, the roof (both cabins), or a round cabin's post (track B). */
    enum Kind { WALL, ROOF, POST }

    final Kind kind;
    /** A post's world angle (rad from +x towards +z): the door posts are those near "out" as the car moves on. */
    double angle;

    /** A round cabin's post at (x, z) from the car's centre (world frame: a cylinder needn't turn), at angle th. */
    static CabinWall post(ElevatorCar car, double x, double z, double th) {
        double h = ElevatorCar.POST_HALF;
        CabinWall w = new CabinWall(car, x - h, x + h, ElevatorCar.THICK, ElevatorCar.CABIN - ElevatorCar.WALL, z - h, z + h, Kind.POST);
        w.angle = th;
        return w;
    }

    public CabinWall(ElevatorCar car, double a0, double a1, double y0, double y1, double o0, double o1) {
        this(car, a0, a1, y0, y1, o0, o1, y0 >= ElevatorCar.CABIN - ElevatorCar.WALL - 1e-9 ? Kind.ROOF : Kind.WALL);
    }

    public CabinWall(ElevatorCar car, double a0, double a1, double y0, double y1, double o0, double o1, Kind kind) {
        super(car);
        this.kind = kind;
        this.a0 = a0;
        this.a1 = a1;
        this.y0 = y0;
        this.y1 = y1;
        this.o0 = o0;
        this.o1 = o1;
        noPhysics = true;
    }

    /** Stands where the car is (its box follows). */
    void follow() {
        Vec3 p = getParent().position();
        setPos(p);
        xo = xOld = p.x;
        yo = yOld = p.y;
        zo = zOld = p.z;
    }

    /** Its box with the car's plate at `at` (bottom centre). */
    AABB boxAt(Vec3 at) {
        if (kind == Kind.POST) return new AABB(at.x + a0, at.y + y0, at.z + o0, at.x + a1, at.y + y1, at.z + o1);   // (world frame)
        Direction rail = getParent().railSide, out = rail.getOpposite(), across = rail.getClockWise();
        double ax0 = across.getStepX() * a0 + out.getStepX() * o0, ax1 = across.getStepX() * a1 + out.getStepX() * o1;
        double az0 = across.getStepZ() * a0 + out.getStepZ() * o0, az1 = across.getStepZ() * a1 + out.getStepZ() * o1;
        return new AABB(at.x + ax0, at.y + y0, at.z + az0, at.x + ax1, at.y + y1, at.z + az1);
    }

    /** GameTests: is it there now (round cabin posts, walled sides)? A post (else a side or the roof)? */
    public boolean solidForTest() {
        return active();
    }

    public boolean postForTest() {
        return kind == Kind.POST;
    }

    /** A post within the door's angle of "out" (away from the rail, where the car is now). */
    boolean inDoor(ElevatorCar c) {
        double s = c.posNow();
        Vec3 out = c.towardRail(Double.isNaN(s) ? 0 : s).scale(-1);
        double d = Math.cos(angle) * out.x + Math.sin(angle) * out.z;
        return d > Math.cos(ElevatorCar.DOOR_HALF_ANGLE);
    }

    /** Is it there in the car's cabin (a walled side, or a round cabin's post, not in its open door)? */
    boolean active() {
        ElevatorCar c = getParent();
        if (c == null) return false;
        return switch (kind) {
            case ROOF -> true;
            case WALL -> !c.round();
            case POST -> c.round() && !(c.doorOpen() && inDoor(c));
        };
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return getParent() == null || getParent().railSide == null || !active() ? new AABB(position, position) : boxAt(position);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return active() && getParent().canBeCollidedWith(other);
    }

    /** Picked (a click or the crosshair stops at it), so nobody inside reaches blocks behind the walls. */
    @Override
    public boolean isPickable() {
        return !getParent().isRemoved() && active();
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
