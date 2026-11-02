package mods.eln.robotics.servo;

import mods.eln.robotics.motion.Segment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * An elevator car: the floor plate a Linear Servo Drive moves along its rail. A view and a collider only; the drive
 * owns the truth (its Segment) and is never in doubt. Not saved: the drive spawns it and finds it again by UUID.
 * <p>
 * Its 1x1 plate (1/16 thick, the shape of a floor ghost) is solid to everything except players on the server: a
 * player's position there is a round trip late, and a rising box would wall them in. The drive's tick sets its place on
 * the server (so a car in a chunk whose entities don't tick never freezes) and carries mobs and items standing on it;
 * each client places it by its own clock from the last SegmentPayload and carries its own player (client.CarCarry).
 * Vanilla's position sync is off both ways: no update interval, and the client swallows any position packet.
 */
public class ElevatorCar extends Entity {
    /** The plate's thickness (m): a floor ghost's (GhostBlock.tFloor). */
    public static final double THICK = 1 / 16.0;
    /** Client: the cars in the client's level (the carry ticks them). */
    public static final Set<ElevatorCar> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    /** Where position 0 is (the plate's bottom centre), the axis it moves along, the side its rail is on. */
    public Vec3 origin;
    public Direction axis = Direction.UP, railSide = Direction.NORTH;
    /** The motion; null until known (a client before its first payload). */
    public @Nullable Segment segment;
    /** Server: the drive's clock (pairing data). Client: ticks into the segment, counted by the client. */
    long serverNow;
    public long clientT;
    /** Server: the drive head that owns it. */
    public @Nullable BlockPos head;
    int checkIn = 40;
    public final ElevatorPanel panel;

    public ElevatorCar(EntityType<? extends ElevatorCar> type, Level level) {
        super(type, level);
        panel = new ElevatorPanel(this);
        panel.setId(getId() + 1);   // (as the client numbers it: recreateFromPacket)
        setNoGravity(true);
        blocksBuilding = true;
    }

    // ---- where it is

    /** The plate's bottom centre at position s along the axis. */
    public Vec3 at(double s) {
        return origin.add(axis.getStepX() * s, axis.getStepY() * s, axis.getStepZ() * s);
    }

    /** The plate's box at position s. */
    public AABB boxAt(double s) {
        return getDimensions(getPose()).makeBoundingBox(at(s));
    }

    /** Puts the plate (and its panel) at position s, with no interpolation from where it was. */
    public void placeAt(double s) {
        Vec3 p = at(s);
        setPos(p);
        xo = xOld = p.x;
        yo = yOld = p.y;
        zo = zOld = p.z;
        panel.follow();
    }

    /** Client: its position at t ticks into the segment (NaN without one). */
    public double clientPos(double t) {
        return segment == null || origin == null ? Double.NaN : segment.position(t);
    }

    /** Client: take a new segment (SegmentPayload) and stand where it says now. */
    public void apply(SegmentPayload p) {
        origin = p.origin();
        axis = p.axis();
        railSide = p.railSide();
        segment = p.segment();
        clientT = p.elapsed();
        placeAt(segment.position(clientT));
    }

    // ---- the server side

    /** Server: the drive's segment and clock, every tick (sent to players who start tracking it). */
    void drive(Segment s, long now) {
        segment = s;
        serverNow = now;
    }

    /** Server: the payload for its current motion. */
    @Nullable SegmentPayload payload() {
        return segment == null || origin == null ? null : SegmentPayload.of(getId(), origin, axis, railSide, segment, serverNow);
    }

    /** Server: send its motion to everyone who sees it. */
    void broadcast() {
        SegmentPayload p = payload();
        if (p != null && level() instanceof ServerLevel) PacketDistributor.sendToPlayersTrackingEntity(this, p);
    }

    @Override
    public void sendPairingData(ServerPlayer player, Consumer<CustomPacketPayload> bundle) {
        super.sendPairingData(player, bundle);
        SegmentPayload p = payload();
        if (p != null) bundle.accept(p);   // (after the spawn packet, in the same bundle: joiners see the move)
    }

    /** The drive at `head` if it still owns this car. */
    @Nullable LinearServoDriveElement owner() {
        if (head == null || !(level() instanceof ServerLevel)) return null;
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(head, level()));
        if (n instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof LinearServoDriveElement e && getUUID().equals(e.carId))
            return e;
        return null;
    }

    @Override
    public void tick() {
        // no physics, no vanilla motion: the drive places it (server), the client's carry does (client)
        if (!level().isClientSide() && --checkIn <= 0) {
            checkIn = 40;
            if (owner() == null) discard();   // (its drive is gone, or has another car)
        }
    }

    /** Server: does a car carry this player (on it, or a move late)? The floating kick asks (robotics.mixin). */
    public static boolean carries(Entity e) {
        AABB b = e.getBoundingBox();
        for (ElevatorCar c : e.level().getEntitiesOfClass(ElevatorCar.class, b.inflate(0.5, Riders.PLAYER_LAG + 0.5, 0.5)))
            if (Riders.playerOnTop(b, c.getBoundingBox(), Riders.TOLERANCE + 0.1)) return true;
        return false;
    }

    /** Server: a rider pressed the panel (top half: up a station, bottom half: down). */
    void panelPressed(Player player, boolean up) {
        LinearServoDriveElement e = owner();
        if (e != null) e.panel(player, up);
    }

    // ---- vanilla plumbing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Solid to everything but players on the server (their positions there are late: Riders.PLAYER_LAG). */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return level().isClientSide() || !(other instanceof Player);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return new net.neoforged.neoforge.entity.PartEntity<?>[]{panel};
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        panel.setId(id + 1);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        panel.setId(packet.getId() + 1);
        panel.follow();
    }

    /** Position packets do nothing: the segment places it. */
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return new InterpolationHandler.NoOpInterpolationHandler() {
            @Override
            public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
                return true;
            }
        };
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level().isClientSide()) CLIENT.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        CLIENT.remove(this);
    }
}
