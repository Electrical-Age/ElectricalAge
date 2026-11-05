package mods.eln.robotics.servo;

import mods.eln.Eln;
import mods.eln.api.element.ElementMachine;
import mods.eln.api.MachineState;
import mods.eln.api.element.Machines;
import mods.eln.bus.BusPort;
import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.ghost.GhostBlock;
import mods.eln.i18n.I18N;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.robotics.Robotics;
import mods.eln.robotics.motion.Drive;
import mods.eln.robotics.motion.Segment;
import mods.eln.robotics.motion.Stations;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Linear Servo Drive (LinearServoDriveDescriptor): owns an elevator car's motion and is never in doubt about it.
 * <p>
 * Each tick (its slow process, on the server tick) it reads its rail and stations, its inputs and its supply, decides
 * the car's Segment (a new one only when something changes: a call, a sag, a stop), sweeps the car and its riders to
 * the next position, and then places the car and carries the mobs and items standing on it. A block or a creature in
 * the way stops it dead (fault, a zero-velocity segment where it stood). Every planned stop is on a whole block, and a
 * stopped car has a floor ghost under its plate, so a player logging in on it, or a chunk loading, can't fall through
 * before the car entity is back. Motion counts in the drive's own clock (saved), which stops while its chunks are
 * unloaded, so a move resumes where it was.
 * <p>
 * Electrics: a machine on the Machine API (LinearServoDriveDescriptor.spec: a 200 V consumer, idle losses as its idle
 * draw, a heat model). Each tick it asks for the lifting work of its step over the motor's efficiency and moves only as
 * far as what it got pays for (Drive.afterPaying), so a starved supply makes a slower car; its top speed also follows
 * the line voltage, and a move under way is retargeted when the line sags or recovers. Below the stall share, too hot
 * or overvolted it holds (the last two are faults; it never burns).
 * <p>
 * Commands: its setpoint wire (a share of the travel) while connected (Baughn: wires override), else its registers
 * on the RS485 bus (Baughn 2026-11-05: a plain actuator, an addressed device): 0 target (blocks above its foot), 1
 * position, 2 speed limit (m/s; 0: none), 3 state (STATUS_NAMES' order), 4 load (kg). It takes `write` (0 and 2),
 * answers `read` with `reply` to the asker, and sends `fault` (its code, FAULT_NAMES' order) to everyone when a fault
 * starts.
 */
public class LinearServoDriveElement extends TransparentNodeElement implements mods.eln.introspect.Describable {
    /** What it shows (publish, lamp, Jade). */
    public static final byte IDLE = 0, IN_POSITION = 1, MOVING = 2, FAULT = 3;
    /** Why it stopped. OBSTRUCTED holds until the next command; the others clear themselves. */
    public static final byte NO_FAULT = 0, OBSTRUCTED = 1, OVERHEAT = 2, NO_RAIL = 3, OVERVOLTAGE = 4;
    /** Its floor ghost's id (the descriptor has no ghost group: getGhostGroupUuid is -1). */
    static final int FLOOR_GHOST = 0x5E7A;

    final LinearServoDriveDescriptor desc;
    /** Its power, heat and signals; their parts are in this element's lists. */
    public final ElementMachine machine;

    // saved
    /** The drive's clock (ticks it has run with its chunks loaded); segments start on it. */
    public long clock;
    public Segment seg = Segment.rest(0, 0);
    /** Where the car is to go (whole blocks from the bottom). */
    public double target;
    public byte fault = NO_FAULT;
    /** The speed limit written to register 2 (m/s; 0: none). */
    public double speedLimit;
    /** The car entity's UUID (it isn't saved with the world: found again within a session, else spawned anew). */
    public @Nullable UUID carId;

    // live
    /** The rail's length (blocks; -1 not scanned yet). */
    public int travel = -1;
    @Nullable ElevatorCar car;
    /** The setpoint wire's last command (NaN: not wired), whether enable was high last tick. */
    double wired = Double.NaN;
    boolean wasEnabled = true;
    int sinceRetarget;
    /** Stalled at the stall share (Drive.STALL_SHARE), and the ticks the line has been back over the resume share. */
    public boolean stalled;
    int resumeTicks;
    /** A floor ghost of ours may stand (true after a load: unknown). */
    boolean ghostUp = true;
    /** Load on the car (kg), the force it holds along the axis (N), speed (m/s, + along the axis) this tick. */
    public double loadKg, forceN, speed;
    byte status = IDLE;
    /** The fault last sent on the bus. */
    byte faultSent = NO_FAULT;
    /** Its RS485 port (the right flank's floor, on its right). */
    public final BusPort port;

    /** Its registers on the bus. */
    public static final int REG_TARGET = 0, REG_POSITION = 1, REG_SPEED_LIMIT = 2, REG_STATE = 3, REG_LOAD = 4;
    /** Screen packet: its bus address (a float; anything but 17..255: none). */
    public static final byte setAddressId = 1;

    public LinearServoDriveElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (LinearServoDriveDescriptor) d;
        machine = Machines.forElement(this, LinearServoDriveDescriptor.spec());
        port = machine.busPort();
        port.listen(true);
        slowProcessList.add(this::step);
    }

    /** The drive at pos, or null. */
    public static @Nullable LinearServoDriveElement at(Level level, BlockPos pos) {
        var n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, level));
        return n instanceof TransparentNode t && t.element instanceof LinearServoDriveElement e ? e : null;
    }

    // ---- geometry

    /** The rail's direction from the drive (up: an elevator). */
    net.minecraft.core.Direction axis() {
        return net.minecraft.core.Direction.UP;
    }

    /** The car's side of the drive (its front). */
    net.minecraft.core.Direction carSide() {
        return front.toEnumFacing();
    }

    /** The plate's bottom centre at position 0: in front of the drive, level with its foot. */
    Vec3 origin() {
        BlockPos c = node.coordonate.pos().relative(carSide());
        return new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    /** Blocks of rail from the drive along its axis (0..MAX_TRAVEL). */
    void scan(Level level) {
        var ax = axis();
        BlockPos p = node.coordonate.pos();
        int n = 0;
        while (n < LinearServoDriveDescriptor.MAX_TRAVEL) {
            var s = level.getBlockState(p.relative(ax, n + 1));
            if (!(s.getBlock() instanceof ServoRailBlock) || s.getValue(ServoRailBlock.FACING) != ax) break;
            n++;
        }
        travel = n;
    }

    /** Position (blocks along the axis) and velocity (m/tick) at clock tick t. */
    double posAt(long t) {
        return seg.position(t - seg.startTick());
    }

    boolean movingAt(long t) {
        return !seg.arrived(t - seg.startTick());
    }

    // ---- commands

    /** The setpoint wire rules while connected (Baughn: wires override the buttons). */
    boolean wiredUp() {
        return machine.signalConnected(LinearServoDriveDescriptor.SETPOINT);
    }

    /** Go to stop t (a new command also clears an obstruction). */
    public void command(double t) {
        target = Math.max(0, Math.min(Math.max(0, travel), Math.round(t)));
        if (fault == OBSTRUCTED) fault = NO_FAULT;
        needPublish();
    }

    /** A rider pressed the car's panel (it has no buttons of its own: Baughn 2026-11-05, no magic stepping). */
    public void panel(Player player, boolean up) {
        say(player, wiredUp() ? I18N.tr("This elevator follows its setpoint wire") : I18N.tr("This elevator is driven over its RS485 bus"));
    }

    // ---- the bus

    /** The packets for it: register writes and reads (see the class comment). */
    void serveBus() {
        for (Packet k; (k = port.poll()) != null; ) {
            if (k.target() != port.address() || port.address() == 0) continue;   // (broadcasts: nothing for it yet)
            switch (k.type()) {
                case WRITE -> {
                    if (k.index() == REG_TARGET && !wiredUp() && Double.isFinite(k.value())) command(k.value());
                    else if (k.index() == REG_SPEED_LIMIT) speedLimit = Math.max(0, k.value());
                }
                case READ -> {
                    double v = register(k.index());
                    if (!Double.isNaN(v)) port.send(new Packet(k.source(), port.address(), PacketType.REPLY, k.index(), (float) v));
                }
                default -> {
                }
            }
        }
    }

    /** Register r's value, or NaN: no such register. */
    public double register(int r) {
        return switch (r) {
            case REG_TARGET -> target;
            case REG_POSITION -> posAt(clock);
            case REG_SPEED_LIMIT -> speedLimit;
            case REG_STATE -> status();
            case REG_LOAD -> loadKg;
            default -> Double.NaN;
        };
    }

    public void setAddress(int a) {
        port.setAddress(a);
        needPublish();
    }

    static void say(Player p, String text) {
        p.sendOverlayMessage(Component.literal(text));
    }

    /** True when its enable input is high (over 0.6, a gate's high), or not connected (it defaults to 1). */
    boolean enabled() {
        return machine.signal(LinearServoDriveDescriptor.ENABLE) > 0.6;
    }

    // ---- the tick

    void step(double dt) {
        if (!(world() instanceof ServerLevel level)) return;
        BlockPos hp = node.coordonate.pos();
        BlockPos col = hp.relative(carSide());
        if (!level.isLoaded(hp) || !level.isLoaded(col)) return;   // (unloaded: the clock stops, the car with it; it asks for nothing)
        long t0 = clock;
        clock++;
        if (travel < 0 || clock % 20 == 0) scan(level);
        serveBus();
        Drive drive = LinearServoDriveDescriptor.drive;
        double ratio = machine.voltageRatio();
        if (machine.state() == MachineState.RUNNING) {
            if (ratio < Drive.STALL_SHARE) {
                stalled = true;
                resumeTicks = 0;
            } else if (stalled) {
                resumeTicks = ratio >= Drive.RESUME_SHARE ? resumeTicks + 1 : 0;
                if (resumeTicks >= Drive.RESUME_TICKS) stalled = false;
            }
        }
        double vlim = stalled ? 0 : drive.vmaxAt(ratio) / 20, a = LinearServoDriveDescriptor.accelPerTick();
        if (speedLimit > 0) vlim = Math.min(vlim, speedLimit / 20);
        double p = posAt(t0), v = seg.velocity(t0 - seg.startTick());
        boolean moving = movingAt(t0);

        // faults that come and go by themselves: the machine's trips, the rail
        MachineState ms = machine.state();
        byte trip = ms == MachineState.OVERHEAT_TRIPPED ? OVERHEAT : ms == MachineState.OVERVOLTAGE_TRIPPED ? OVERVOLTAGE : NO_FAULT;
        if ((fault == OVERHEAT || fault == OVERVOLTAGE) && fault != trip) fault = NO_FAULT;
        if (trip != NO_FAULT && fault != OBSTRUCTED) fault = trip;
        boolean railOk = travel >= LinearServoDriveDescriptor.MIN_TRAVEL && p <= travel + 1e-9;
        if (!railOk && fault != OBSTRUCTED) fault = NO_RAIL;
        else if (railOk && fault == NO_RAIL) fault = NO_FAULT;

        // commands from the wires
        if (wiredUp() && travel >= 0) {
            double in = machine.signal(LinearServoDriveDescriptor.SETPOINT);
            double want = Stations.proportional(in, travel);
            if (want != wired) {
                wired = want;
                command(want);
            }
        } else wired = Double.NaN;
        boolean en = enabled();
        if (en && !wasEnabled && fault == OBSTRUCTED) fault = NO_FAULT;   // (enable toggled: try again)
        wasEnabled = en;
        if (railOk) target = Math.max(0, Math.min(travel, target));

        // the motion: the voltage's speed limit (the energy's comes with the step below)
        Segment next = seg;
        if (fault != NO_FAULT) {
            if (moving) next = Segment.rest(t0, p);   // (stop dead)
        } else if (vlim <= 0) {
            if (moving) {   // stalled: brake to rest where it can
                double stop = p + v * Math.abs(v) / (2 * a);
                if (Math.abs(seg.to() - stop) > 1e-3) next = seg.retarget(t0, stop, Math.max(Math.abs(v), 1e-4), a);
            }
        } else if (!en) {
            if (moving) {   // disabled: stop at the next whole block it can
                int stop = Stations.stopAhead(p, v, a, travel);
                if (seg.to() != stop) next = seg.retarget(t0, stop, Math.max(seg.vmax(), Math.abs(v)), a);
            }
        } else if (seg.to() != target) {
            next = seg.retarget(t0, target, vlim, a);
        } else if (moving && sinceRetarget >= LinearServoDriveDescriptor.RETARGET_TICKS) {
            double share = LinearServoDriveDescriptor.RETARGET_SHARE;
            // up only when it costs nothing (moving with the net weight) or energy is piling up: else the energy's limit holds it back
            boolean free = forceN * (target - p) <= 0;
            boolean spare = machine.bufferJoules() >= LinearServoDriveDescriptor.SPARE_SHARE * machine.bufferCapacityJoules();
            if (vlim < (1 - share) * seg.vmax() || vlim > (1 + share) * seg.vmax() && (free || spare))
                next = seg.retarget(t0, target, vlim, a);   // (the line sagged or recovered: the new top speed)
        }
        boolean changed = next != seg;
        seg = next;
        double p1 = posAt(clock);

        // who rides, and what the step costs: it goes only as far as the energy it got pays for
        ElevatorCar c = travel >= LinearServoDriveDescriptor.MIN_TRAVEL || car != null ? car(level) : null;
        List<Entity> riders = List.of();
        loadKg = 0;
        if (c != null) {
            riders = riders(level, c, c.boxAt(p), Riders.tolerance(p1 - p));
            for (Entity e : riders) loadKg += massOf(e);
        }
        forceN = Drive.G * (loadKg - LinearServoDriveDescriptor.counterweightKg) * axis().getStepY();
        double need = drive.joules(p1 - p, forceN);
        double got = need > 0 ? machine.takeJoules(Math.min(need, machine.ratedJoulesPerTick())) : 0;
        double sustain = Math.max(0, machine.watts() - drive.idleWatts()) / 20;   // (J a tick)
        Segment paid = drive.afterPaying(seg, t0, forceN, got, sustain);
        if (paid != seg) {
            seg = paid;
            changed = true;
            p1 = posAt(clock);
        }

        // the car: sweep, place, carry
        if (c != null) {
            if (p1 != p) {
                removeGhost();
                if (blocked(level, c, riders, p, p1)) {
                    fault = OBSTRUCTED;
                    seg = Segment.rest(clock, p);
                    p1 = p;
                    changed = true;
                } else {
                    c.placeAt(p1);
                    Vec3 delta = c.at(p1).subtract(c.at(p));
                    for (Entity e : riders) {
                        if (e instanceof Player) continue;   // (its client carries it)
                        e.move(MoverType.SHULKER, delta);
                        Vec3 dm = e.getDeltaMovement();
                        if (dm.y < 0) e.setDeltaMovement(dm.x, 0, dm.z);
                        e.resetFallDistance();
                    }
                }
            } else if (c.position().distanceToSqr(c.at(p1)) > 1e-8) c.placeAt(p1);   // (snapped back where it belongs)
            c.drive(seg, clock);
            if (changed) c.broadcast();
            if (!movingAt(clock) && Math.abs(p1 - Math.rint(p1)) < 1e-6) floorGhost(level, c, p1);
        }
        sinceRetarget = changed ? 0 : sinceRetarget + 1;
        speed = (p1 - p) * 20;
        // the motor's losses heat it: what it got less the work done (all of it, when the step was blocked)
        if (got > 0) machine.heatJoules(Math.max(0, got - Math.max(0, forceN * (p1 - p))));

        // outputs and the lamp
        boolean stopped = !movingAt(clock);
        boolean there = stopped && fault == NO_FAULT && Math.abs(p1 - target) < 1e-6 && railOk;
        machine.setSignal(LinearServoDriveDescriptor.IN_POSITION, there ? 1 : 0);
        machine.setSignal(LinearServoDriveDescriptor.FAULT, fault != NO_FAULT ? 1 : 0);
        machine.setSignal(LinearServoDriveDescriptor.POSITION, travel > 0 ? Math.max(0, Math.min(1, p1 / travel)) : 0);
        if (fault != faultSent) {   // (a fault starting: everyone hears it)
            if (fault != NO_FAULT) port.send(new Packet(Packet.BROADCAST, port.address(), PacketType.FAULT, fault, 0));
            faultSent = fault;
        }
        byte st = status();
        if (st != status) {
            status = st;
            needPublish();
        }
    }

    /** What the lamp shows, from the saved state alone. */
    public byte status() {
        if (fault != NO_FAULT) return FAULT;
        if (movingAt(clock)) return MOVING;
        return Math.abs(posAt(clock) - target) < 1e-6 ? IN_POSITION : IDLE;
    }

    /** The car, found again by UUID or spawned at its place; null if its chunk isn't there. */
    @Nullable ElevatorCar car(ServerLevel level) {
        if (car != null && !car.isRemoved()) return car;
        car = null;
        if (carId != null && level.getEntity(carId) instanceof ElevatorCar c && !c.isRemoved()) car = c;
        if (car == null) {
            ElevatorCar c = Robotics.ELEVATOR_CAR.get().create(level, EntitySpawnReason.TRIGGERED);
            if (c == null) return null;
            setup(c);
            c.placeAt(posAt(clock - 1));
            c.drive(seg, clock - 1);
            if (!level.addFreshEntity(c)) return null;
            carId = c.getUUID();
            car = c;
        }
        setup(car);
        return car;
    }

    void setup(ElevatorCar c) {
        c.origin = origin();
        c.axis = axis();
        c.railSide = carSide().getOpposite();
        c.head = node.coordonate.pos();
    }

    /** Entities standing on the car (players by the server's late view of them: Riders.playerOnTop). */
    static List<Entity> riders(ServerLevel level, ElevatorCar c, AABB box0, double tol) {
        AABB look = box0.inflate(0.5, Riders.PLAYER_LAG + tol, 0.5).expandTowards(0, 2, 0);
        return level.getEntities(c, look, e -> e.isAlive() && !e.isSpectator() && !(e instanceof ElevatorCar) && !(e instanceof ElevatorPanel)
            && !e.isPassenger() && (e instanceof Player ? Riders.playerOnTop(e.getBoundingBox(), box0, tol) : Riders.onTop(e.getBoundingBox(), box0, tol)));
    }

    /** Is anything in the way of the car (and its riders) going from p to p1: a block, or a creature not riding it? */
    static boolean blocked(ServerLevel level, ElevatorCar c, List<Entity> riders, double p, double p1) {
        AABB b0 = c.boxAt(p), b1 = c.boxAt(p1);
        Vec3 d = c.at(p1).subtract(c.at(p));
        List<AABB> swept = new ArrayList<>();
        swept.add(b0.minmax(b1).deflate(1e-4));
        for (Entity e : riders) {   // (clipped to the car: a rider's overhang meets the landing, not the car)
            AABB s = Riders.standing(e.getBoundingBox(), b0).deflate(1e-4);
            swept.add(s.minmax(s.move(d)));
        }
        for (AABB s : swept) if (!level.noBlockCollision(null, s)) return true;
        AABB all = swept.get(0);
        for (AABB s : swept) all = all.minmax(s);
        for (Entity e : level.getEntities(c, all, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !riders.contains(e)))
            for (AABB s : swept) if (e.getBoundingBox().intersects(s)) return true;
        return false;
    }

    /** A rider's weight on the car (kg): players a set weight, other creatures by their size, anything else a little. */
    static double massOf(Entity e) {
        if (e instanceof Player) return LinearServoDriveDescriptor.playerKg;
        if (e instanceof LivingEntity) {
            var b = e.getBoundingBox();
            double vol = (b.maxX - b.minX) * (b.maxY - b.minY) * (b.maxZ - b.minZ);
            return Math.max(1, Math.min(1000, LinearServoDriveDescriptor.playerKg * vol / (0.6 * 0.6 * 1.8)));
        }
        return LinearServoDriveDescriptor.itemKg;
    }

    // ---- the floor ghost

    /** The car's floor block at position s. */
    static BlockPos floorBlock(ElevatorCar c, double s) {
        return BlockPos.containing(c.at(s).add(0, 0.01, 0));
    }

    /** Stopped on a whole block: a solid floor plate ghost under the car's (so nobody falls through before it's back). */
    void floorGhost(ServerLevel level, ElevatorCar c, double s) {
        BlockPos g = floorBlock(c, s);
        Coordonate gc = new Coordonate(g, level);
        if (Eln.ghostManager.getGhost(gc) != null) return;
        if (!Eln.ghostManager.canCreateGhostAt(level, g.getX(), g.getY(), g.getZ())) return;
        Eln.ghostManager.createGhost(gc, node.coordonate, FLOOR_GHOST, Eln.ghostBlock, GhostBlock.tFloor);
        ghostUp = true;
    }

    void removeGhost() {
        if (!ghostUp) return;
        Eln.ghostManager.removeGhostAndBlockWithObserver(node.coordonate, FLOOR_GHOST);
        ghostUp = false;
    }

    /** Is our floor ghost at the car's floor block at position s? (GameTests) */
    public boolean ghostAt(double s) {
        if (car == null) return false;
        var g = Eln.ghostManager.getGhost(new Coordonate(floorBlock(car, s), world()));
        return g != null && g.getObservatorCoordonate().equals(node.coordonate) && g.getUUID() == FLOOR_GHOST;
    }

    @Override
    public void ghostDestroyed(int uuid) {
        if (uuid == FLOOR_GHOST) ghostUp = false;   // (mined: it comes back while the car stands there)
    }

    @Override
    public boolean ghostBlockActivated(int uuid, Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    // ---- the device

    @Override
    public void initialize() {
        Eln.ghostManager.addObserver(this);
        status = status();
        connect();
    }

    @Override
    public void onBreakElement() {
        if (car != null) car.discard();
        else if (carId != null && world() instanceof ServerLevel l && l.getEntity(carId) instanceof ElevatorCar c) c.discard();
        Eln.ghostManager.removeObserver(node.coordonate);
        Eln.ghostManager.removeGhostAndBlockWithObserver(node.coordonate);
        super.onBreakElement();
    }

    /** An empty hand opens its screen. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    /** Its screen: the bus address. */
    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public BusPort busPortOn(Direction side) {
        return machine.busPortOn(side);
    }

    @Override
    public List<mods.eln.node.FacePins.FacePin> facePins(Direction side) {
        return machine.facePins(side);
    }

    @Override
    public byte networkUnserialize(java.io.DataInputStream stream) {
        byte id = super.networkUnserialize(stream);
        if (id == setAddressId) {
            try {
                setAddress(Math.round(stream.readFloat()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return unserializeNulldId;
        }
        return id;
    }

    /** Power on the back; the signals on their flanks' floor edges (LinearServoDriveDescriptor.spec). */
    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.electricalLoad(side, lrdu);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return machine.connectionMask(side, lrdu);
    }

    /** A flank: its input (the floor cable); elsewhere the supply. */
    @Override
    public String multiMeterString(Direction side) {
        if (side == front.left() || side == front.right()) {
            ElectricalLoad in = machine.electricalLoad(side, LRDU.Down);
            return Utils.plotSignal(in.getU(), in.getCurrent());
        }
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", machine.celsius());
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(status());
            stream.writeByte(port.address());
            stream.writeBoolean(port.bus() != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("clock", clock);
        nbt.putLong("segStart", seg.startTick());
        nbt.putDouble("segFrom", seg.from());
        nbt.putDouble("segV0", seg.v0());
        nbt.putDouble("segTo", seg.to());
        nbt.putDouble("segVmax", seg.vmax());
        nbt.putDouble("segAccel", seg.accel());
        nbt.putDouble("target", target);
        nbt.putByte("fault", fault);
        if (speedLimit > 0) nbt.putDouble("speedLimit", speedLimit);
        if (port.address() != 0) nbt.putInt("addr", port.address());
        if (carId != null) {
            nbt.putLong("carMost", carId.getMostSignificantBits());
            nbt.putLong("carLeast", carId.getLeastSignificantBits());
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        clock = nbt.getLongOr("clock", 0);
        double vmax = nbt.getDoubleOr("segVmax", 1), accel = nbt.getDoubleOr("segAccel", 1);
        seg = new Segment(nbt.getLongOr("segStart", 0), nbt.getDoubleOr("segFrom", 0), nbt.getDoubleOr("segV0", 0),
            nbt.getDoubleOr("segTo", 0), vmax > 0 ? vmax : 1, accel > 0 ? accel : 1);
        target = nbt.getDoubleOr("target", 0);
        fault = nbt.getByteOr("fault", NO_FAULT);
        speedLimit = Math.max(0, nbt.getDoubleOr("speedLimit", 0));   // ("proportional" of before 2026-11-05 is ignored: always)
        port.setAddress(nbt.getIntOr("addr", 0));
        carId = nbt.contains("carMost") ? new UUID(nbt.getLongOr("carMost", 0), nbt.getLongOr("carLeast", 0)) : null;
    }

    @Override
    /** The machine's lines (volts, power drawn, its state when not running), then the drive's. */
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>(machine.wailaLines());
        double p = posAt(clock);
        info.put(I18N.tr("Position"), String.format("%.1f m", p));
        info.put(I18N.tr("Target"), String.format("%.0f m", target));
        info.put(I18N.tr("Speed"), String.format("%.2f m/s", Math.abs(speed)));
        info.put(I18N.tr("Address"), port.address() == 0 ? I18N.tr("none (set it in its screen)") : String.valueOf(port.address()));
        info.put(I18N.tr("State"), stateText());
        return info;
    }

    /** The state in words (Jade). */
    public String stateText() {
        return switch (fault) {
            case OBSTRUCTED -> I18N.tr("Fault: something is in the way");
            case OVERHEAT -> I18N.tr("Fault: the motor is too hot");
            case OVERVOLTAGE -> I18N.tr("Fault: the line voltage is too high");
            case NO_RAIL -> travel < LinearServoDriveDescriptor.MIN_TRAVEL ? I18N.tr("Fault: needs a rail of %1$ to %2$ blocks on top",
                LinearServoDriveDescriptor.MIN_TRAVEL, LinearServoDriveDescriptor.MAX_TRAVEL) : I18N.tr("Fault: the rail is broken");
            default -> stalled && machine.state() == MachineState.RUNNING ? I18N.tr("The line is too weak")
                : machine.state() != MachineState.RUNNING ? I18N.tr("No power")
                : !enabled() ? I18N.tr("Disabled") : movingAt(clock) ? I18N.tr("Moving") : I18N.tr("In position");
        };
    }

    /** Statuses and faults by name, in their byte order (introspect). */
    static final java.util.List<String> STATUS_NAMES = java.util.List.of("idle", "in_position", "moving", "fault"),
        FAULT_NAMES = java.util.List.of("none", "obstructed", "overheat", "no_rail", "overvoltage");

    @Override
    public void describe(mods.eln.introspect.Report r) {
        var st = r.state();
        st.choice(mods.eln.introspect.Keys.STATUS, STATUS_NAMES.get(status()), STATUS_NAMES);
        st.num(mods.eln.introspect.Keys.POSITION, posAt(clock), mods.eln.introspect.Unit.M).range(0, Math.max(0, travel));
        st.num(mods.eln.introspect.Keys.TARGET, target, mods.eln.introspect.Unit.M);
        st.num(mods.eln.introspect.Keys.SPEED, speed, mods.eln.introspect.Unit.M_PER_S);
        st.num(mods.eln.introspect.Keys.POWER, machine.watts(), mods.eln.introspect.Unit.W);
        st.num(mods.eln.introspect.Keys.VOLTAGE, machine.volts(), mods.eln.introspect.Unit.V);
        st.num(mods.eln.introspect.Keys.BUFFER_ENERGY, machine.bufferJoules(), mods.eln.introspect.Unit.J);
        st.num("energy_delivered", machine.joulesDelivered(), mods.eln.introspect.Unit.J);
        st.num("energy_taken", machine.joulesTaken(), mods.eln.introspect.Unit.J);
        st.num("energy_lost", machine.joulesLost(), mods.eln.introspect.Unit.J);
        st.num(mods.eln.introspect.Keys.TEMPERATURE, machine.celsius(), mods.eln.introspect.Unit.C);
        st.choice("machine_state", machine.state());
        st.num(mods.eln.introspect.Keys.LOAD, loadKg, mods.eln.introspect.Unit.KG);
        st.integer("travel", travel);
        st.integer("address", port.address());
        st.num("speed_limit", speedLimit, mods.eln.introspect.Unit.M_PER_S);
        st.bool(mods.eln.introspect.Keys.ENABLED, enabled());
        st.num("clock", clock, mods.eln.introspect.Unit.TICK);
        if (carId != null) st.ref("car", carId.toString());
        // the render shows the status lamp from the published byte: both halves must agree on it
        r.sync().choice(mods.eln.introspect.Keys.STATUS, STATUS_NAMES.get(status()), STATUS_NAMES);
        if (fault != NO_FAULT) r.fault(FAULT_NAMES.get(fault), stateText());
    }
}
