package mods.eln.robotics.servo;

import mods.eln.robotics.motion.Segment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
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
import java.util.List;
import java.util.Set;
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
public class ElevatorCar extends Entity implements mods.eln.introspect.Describable, mods.eln.introspect.ClientDescribable {
    /** The plate's thickness (m): a floor ghost's (GhostBlock.tFloor). */
    public static final double THICK = 1 / 16.0;
    /** The cabin outside (m): a 3 x 3 plate, walls and roof 3 high (Baughn 2026-11-05: 3x3x3, about 2.5 inside); a
     *  wall's and the roof's thickness; half the plate's width. */
    public static final double CABIN = 3, WALL = 1 / 16.0, HALF = CABIN / 2;
    /** Client: the ids of the cabin's walls (negative: the server's are positive). */
    static final java.util.concurrent.atomic.AtomicInteger CLIENT_PART_IDS = new java.util.concurrent.atomic.AtomicInteger();
    /** Client: the cars in the client's level (the carry ticks them). */
    /**
     * The cars a client has (CarCarry steps them). By identity: Entity's equals and hashCode are its id, and a car left
     * here from before a relog (a disconnect removes no entities) had the same id as the car the new session spawned, so
     * the new one was never added and froze on that client (AdventurAgent 2026-11-26). CarCarry drops cars of a level
     * gone.
     */
    public static final Set<ElevatorCar> CLIENT = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

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
    /** The cabin's back wall (on the rail side), its left and right walls, and its roof; the front is open. Then a ring
     *  of thin posts on the plate's rim for a round cabin (round()): which are solid depends on the cabin. */
    public final CabinWall[] walls;

    /**
     * A round cabin's posts (track B, Baughn 2026-11-27: round cabins on curved tracks; AdventurAgent's B0 spike): how many,
     * on what radius (m, to their centres: the plate's rim), half their width; the door: posts within this angle of
     * "out" (away from the rail) open while the car stands still. Inside, riders meet the radial limit (ROUND_IN), not
     * the posts (whose inner edge is there): the posts keep others out and stop reaching through the wall.
     */
    public static final int POSTS = 24;
    public static final double POST_R = HALF - 0.03, POST_HALF = 0.03, DOOR_HALF_ANGLE = Math.toRadians(40);
    /** A round cabin's inside radius (m) for the radial limit: the plate's half width less a wall. */
    public static final double ROUND_IN = HALF - WALL;

    public ElevatorCar(EntityType<? extends ElevatorCar> type, Level level) {
        super(type, level);
        // (on the server the panel, an Entity, takes its own id from the level's entity counter, so no other entity
        // ever has it; a client learns it from the spawn packet: recreateFromPacket)
        panel = new ElevatorPanel(this);
        walls = new CabinWall[]{
            new CabinWall(this, -HALF, HALF, THICK, CABIN - WALL, -HALF, -HALF + WALL),
            new CabinWall(this, -HALF, -HALF + WALL, THICK, CABIN - WALL, -HALF + WALL, HALF),
            new CabinWall(this, HALF - WALL, HALF, THICK, CABIN - WALL, -HALF + WALL, HALF),
            new CabinWall(this, -HALF, HALF, CABIN - WALL, CABIN, -HALF, HALF),
            post(0), post(1), post(2), post(3), post(4), post(5), post(6), post(7), post(8), post(9), post(10), post(11),
            post(12), post(13), post(14), post(15), post(16), post(17), post(18), post(19), post(20), post(21), post(22), post(23)};
        // (a client level gives no entity ids, and its part list compares parts by id: without one, the car leaving
        // the client threw and disconnected it, AdventurAgent 2026-11-05. Nothing is sent to a wall, so a client-only
        // id, below every id a server gives, will do.)
        if (level.isClientSide()) for (CabinWall w : walls) w.setId(CLIENT_PART_IDS.decrementAndGet());
        setNoGravity(true);
        blocksBuilding = true;
    }

    /** Ring post k, at world angle k/POSTS of a turn from +x (a cylinder looks the same at any heading: the posts never turn). */
    private CabinWall post(int k) {
        double th = 2 * Math.PI * k / POSTS, x = POST_R * Math.cos(th), z = POST_R * Math.sin(th);
        return CabinWall.post(this, x, z, th);
    }

    /** A round cabin: on a curved track (Baughn: walled cabins keep to straight, axis-aligned paths). */
    public boolean round() {
        var p = path();
        return p != null && !p.axisAligned();
    }

    /** Where it is along its path now (by the drive's clock on the server, its own on a client); NaN without a segment. */
    public double posNow() {
        if (segment == null) return Double.NaN;
        return segment.position(level().isClientSide() ? clientT : serverNow - segment.startTick());
    }

    /** The horizontal unit to the left of travel at s (railSide's side when the path runs straight up or down). */
    Vec3 left(double s) {
        var p = path();
        Vec3 t = p == null ? Vec3.ZERO : p.tangent(s);
        double h = Math.sqrt(t.x * t.x + t.z * t.z);
        if (h < 1e-6) return new Vec3(railSide.getStepX(), 0, railSide.getStepZ());
        return new Vec3(t.z / h, 0, -t.x / h);
    }

    /** +1 if the rail is to the left of travel, -1 if to the right: the side of the drive at the start. */
    double railSign() {
        Vec3 l = left(0);
        return l.x * railSide.getStepX() + l.z * railSide.getStepZ() < -1e-6 ? -1 : 1;
    }

    /** The horizontal unit from the car's centre towards its rail at s. */
    public Vec3 towardRail(double s) {
        return left(s).scale(railSign());
    }

    /** Is the round cabin's door open (it stands still)? */
    public boolean doorOpen() {
        return stopped();
    }

    // ---- where it is

    /** Its drive's laid track (track B: TrackPayload on clients), or null: it runs on the rail's straight path. */
    private mods.eln.robotics.motion.@Nullable TrackPath track;
    private @Nullable List<Vec3> trackPoints;
    private boolean @Nullable [] trackStops;

    /** Run on this track (points in blocks, stops), or on the rail again (null). */
    public void setTrack(@Nullable List<Vec3> points, boolean @Nullable [] stops) {
        trackPoints = points == null ? null : List.copyOf(points);
        trackStops = stops == null ? null : stops.clone();
        track = null;
        if (points != null && stops != null) {
            try {
                track = mods.eln.node.voxel.TrackPlacer.track(points, stops).path();
            } catch (IllegalArgumentException ex) {
                trackPoints = null;
                trackStops = null;
            }
        }
    }

    /** Its path (its track, else a straight one from origin along axis, as its payload gives), or null before it has one. */
    public mods.eln.robotics.motion.@Nullable Path path() {
        if (track != null) return track;
        if (origin == null) return null;
        if (path == null || !path.origin().equals(origin) || path.axis() != axis)
            path = new mods.eln.robotics.motion.StraightPath(origin, axis, Double.POSITIVE_INFINITY);
        return path;
    }

    private mods.eln.robotics.motion.StraightPath path;

    /** The plate's bottom centre at position s along its path. */
    public Vec3 at(double s) {
        return path().at(s);
    }

    /** The plate's box at position s. */
    public AABB boxAt(double s) {
        return getDimensions(getPose()).makeBoundingBox(at(s));
    }

    /** The whole cabin's box at position s: the plate's, CABIN high. */
    public AABB cabinBox(double s) {
        AABB b = boxAt(s);
        return b.setMaxY(b.minY + CABIN);
    }

    /** Puts the plate (and its panel) at position s, with no interpolation from where it was. */
    public void placeAt(double s) {
        Vec3 p = at(s);
        setPos(p);
        xo = xOld = p.x;
        yo = yOld = p.y;
        zo = zOld = p.z;
        panel.follow();
        for (CabinWall w : walls) w.follow();
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

    /** Server: send its track to everyone who sees it (it changed). */
    void broadcastTrack() {
        if (level() instanceof ServerLevel) PacketDistributor.sendToPlayersTrackingEntity(this, TrackPayload.of(getId(), trackPoints, trackStops));
    }

    /** Server: send its motion to everyone who sees it. */
    void broadcast() {
        SegmentPayload p = payload();
        if (p != null && level() instanceof ServerLevel) PacketDistributor.sendToPlayersTrackingEntity(this, p);
    }

    @Override
    public void sendPairingData(ServerPlayer player, Consumer<CustomPacketPayload> bundle) {
        super.sendPairingData(player, bundle);
        if (trackPoints != null) bundle.accept(TrackPayload.of(getId(), trackPoints, trackStops));   // (the track before the motion on it)
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
        for (CabinWall w : walls) w.follow();   // (a round cabin's door posts open and close as it stops and starts)
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

    /** Standing still (its segment arrived; on the server by the drive's clock, on a client by its own). */
    public boolean stopped() {
        if (segment == null) return true;
        long t = level().isClientSide() ? clientT : serverNow - segment.startTick();
        return segment.arrived(t);
    }

    /** Server: a rider clicked a slot of the panel. */
    void panelClicked(Player player, int slot) {
        LinearServoDriveElement e = owner();
        if (e != null) e.panelClicked(player, slot);
    }

    /** The panel's slots, 2 bits each (PANEL_NONE, PANEL_BUTTON, PANEL_LIGHT), and which are lit (a bit each): synced. */
    static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PANEL_KINDS =
        SynchedEntityData.defineId(ElevatorCar.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PANEL_LIT =
        SynchedEntityData.defineId(ElevatorCar.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public static final int PANEL_NONE = 0, PANEL_BUTTON = 1, PANEL_LIGHT = 2;

    /** Server: show the panel's slots (kinds) and lamps (a lit bit each). */
    void showPanel(int[] kinds, int lit) {
        int k = 0;
        for (int i = 0; i < kinds.length && i < 16; i++) k |= (kinds[i] & 3) << 2 * i;
        if (entityData.get(PANEL_KINDS) != k) entityData.set(PANEL_KINDS, k);
        if (entityData.get(PANEL_LIT) != lit) entityData.set(PANEL_LIT, lit);
    }

    /** What slot i holds (PANEL_NONE, PANEL_BUTTON, PANEL_LIGHT). */
    public int panelKind(int i) {
        return entityData.get(PANEL_KINDS) >> 2 * i & 3;
    }

    public boolean panelLit(int i) {
        return (entityData.get(PANEL_LIT) >> i & 1) != 0;
    }

    // ---- vanilla plumbing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(PANEL_KINDS, 0);
        data.define(PANEL_LIT, 0);

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
        var parts = new net.neoforged.neoforge.entity.PartEntity<?>[walls.length + 1];
        parts[0] = panel;
        System.arraycopy(walls, 0, parts, 1, walls.length);
        return parts;
    }

    /**
     * The spawn packet carries the panel's id in its data: the server's counter gave it, and it needn't be the car's
     * id + 1 (another thread may have taken that one first); assumed, a click on the panel could reach that entity.
     */
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity, panel.getId());
    }

    /** Client: the car's id, and the panel's from the packet's data (getAddEntityPacket). */
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        panel.setId(packet.getData());
        panel.follow();
        for (CabinWall w : walls) w.follow();
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

    // ---- introspect: the server's segment and clock against each client's

    /** How many ticks a client may be behind the server when the two halves are read (a round trip and a tick). */
    static final int SYNC_LAG_TICKS = 6;

    @Override
    public void describe(mods.eln.introspect.Report r) {
        if (head != null) r.state().ref("head", head.getX() + "," + head.getY() + "," + head.getZ());
        // (the drive's clock: a client gets only the ticks into the segment, so this isn't a sync key)
        if (segment != null) r.state().num(mods.eln.introspect.Keys.SEGMENT_START, segment.startTick(), mods.eln.introspect.Unit.TICK);
        motion(r, serverNow - (segment == null ? 0 : segment.startTick()));
    }

    @Override
    public void describeClient(mods.eln.introspect.Report r) {
        motion(r, clientT);
        if (segment != null && origin != null) {
            // where the entity really stands against where its segment says (the carry and interpolation must not drift)
            double s0 = segment.position(clientT);
            r.client().num("entity_offset", path().along(position(), s0) - s0, mods.eln.introspect.Unit.M);
        }
    }

    /**
     * Sync keys of both halves, from a segment and the ticks into it: the segment's shape (exact: both come from one
     * payload), the ticks into it, and position and velocity, which may differ by SYNC_LAG_TICKS of motion.
     */
    private void motion(mods.eln.introspect.Report r, long dt) {
        var sync = r.sync();
        sync.bool("has_segment", segment != null && origin != null);
        if (segment == null || origin == null) return;
        double accel = segment.accel(), vmax = Math.abs(segment.vmax());
        sync.choice("axis", axis);
        var seg = sync.section("segment");
        seg.num("from", segment.from(), mods.eln.introspect.Unit.M).tol(1e-9);
        seg.num("to", segment.to(), mods.eln.introspect.Unit.M).tol(1e-9);
        seg.num("vmax", segment.vmax() * 20, mods.eln.introspect.Unit.M_PER_S).tol(1e-9);
        sync.num(mods.eln.introspect.Keys.ELAPSED, dt, mods.eln.introspect.Unit.TICK).tol(SYNC_LAG_TICKS);
        sync.num(mods.eln.introspect.Keys.POSITION, segment.position(dt), mods.eln.introspect.Unit.M).tol(0.05 + vmax * SYNC_LAG_TICKS);
        sync.num(mods.eln.introspect.Keys.VELOCITY, segment.velocity(dt) * 20, mods.eln.introspect.Unit.M_PER_S).tol(0.05 + accel * 20 * SYNC_LAG_TICKS);
    }
}
