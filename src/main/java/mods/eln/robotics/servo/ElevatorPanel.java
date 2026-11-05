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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * The car's panel, inside the cabin's back wall at chest height: a part of the car (as a dragon's head is of the dragon)
 * that can be clicked but not stood on. It has SLOTS places, COLS across and ROWS down (slot 0 at the top left as a
 * rider facing it sees it), each empty or holding an Addressable Button or Light put in by hand (Baughn 2026-11-05:
 * real parts in the car's panel); the drive keeps them (LinearServoDriveElement.panelClicked).
 */
public class ElevatorPanel extends PartEntity<ElevatorCar> {
    /** Its slots: COLS across, ROWS down, each CELL square (m). */
    public static final int COLS = 3, ROWS = 4, SLOTS = COLS * ROWS;
    public static final double CELL = 0.25;
    /** Width, height and depth (m), and where it sits: toward the rail (on the back wall's inside) and up from the plate. */
    public static final float W = (float) (COLS * CELL), H = (float) (ROWS * CELL);
    public static final double D = 2 / 16.0, OUT = ElevatorCar.HALF - ElevatorCar.WALL - D / 2;
    public static final double UP = 0.75;
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

    /** A thin board on the wall, W across and D deep (not the square of its dimensions). */
    @Override
    protected AABB makeBoundingBox(Vec3 at) {
        ElevatorCar c = getParent();
        if (c == null || c.railSide == null) return super.makeBoundingBox(at);
        var right = c.railSide.getClockWise();
        double hx = Math.abs(right.getStepX()) * W / 2 + Math.abs(c.railSide.getStepX()) * D / 2;
        double hz = Math.abs(right.getStepZ()) * W / 2 + Math.abs(c.railSide.getStepZ()) * D / 2;
        return new AABB(at.x - hx, at.y, at.z - hz, at.x + hx, at.y + H, at.z + hz);
    }

    /** The slot at a hit (relative to its position: the board's bottom centre), or -1. */
    int slotAt(Vec3 location) {
        var right = getParent().railSide.getClockWise();
        double across = location.x * right.getStepX() + location.z * right.getStepZ();
        int col = (int) Math.floor((across + W / 2) / CELL), row = (int) Math.floor((H - location.y) / CELL);
        return col < 0 || col >= COLS || row < 0 || row >= ROWS ? -1 : row * COLS + col;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        int slot = slotAt(location);
        if (slot < 0) return InteractionResult.PASS;
        if (!level().isClientSide()) getParent().panelClicked(player, slot);
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
