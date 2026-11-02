package mods.eln.robotics.servo;

import mods.eln.Eln;
import mods.eln.ghost.GhostBlock;
import mods.eln.i18n.I18N;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.robotics.Robotics;
import mods.eln.robotics.motion.Drive;
import mods.eln.robotics.motion.Segment;
import mods.eln.robotics.motion.Stations;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
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
 * Electrics: a resistor on the 200 V line drawing idle losses plus the lifting power over the motor's efficiency
 * (Drive), the losses heating the motor; too hot, it faults and holds until it cools (it doesn't burn). Its top speed
 * follows the line voltage, so a weak supply makes a slower car, and a move under way slows when the line sags.
 */
public class LinearServoDriveElement extends TransparentNodeElement {
    /** What it shows (publish, lamp, Jade). */
    public static final byte IDLE = 0, IN_POSITION = 1, MOVING = 2, FAULT = 3;
    /** Why it stopped. OBSTRUCTED holds until the next command; the others clear themselves. */
    public static final byte NO_FAULT = 0, OBSTRUCTED = 1, OVERHEAT = 2, NO_RAIL = 3;
    /** Its floor ghost's id (the descriptor has no ghost group: getGhostGroupUuid is -1). */
    static final int FLOOR_GHOST = 0x5E7A;

    final LinearServoDriveDescriptor desc;
    public final NbtElectricalLoad power = new NbtElectricalLoad("power");
    final Resistor motor = new Resistor(power, null);
    public final NbtElectricalGateInput setpoint = new NbtElectricalGateInput("setpoint"), enable = new NbtElectricalGateInput("enable");
    public final NbtElectricalGateOutput inPosition = new NbtElectricalGateOutput("inPosition"), faultOut = new NbtElectricalGateOutput("faultOut");
    final NbtElectricalGateOutputProcess inPositionProcess = new NbtElectricalGateOutputProcess("inPositionProcess", inPosition),
        faultProcess = new NbtElectricalGateOutputProcess("faultProcess", faultOut);
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");

    // saved
    /** The drive's clock (ticks it has run with its chunks loaded); segments start on it. */
    public long clock;
    public Segment seg = Segment.rest(0, 0);
    /** Where the car is to go (whole blocks from the bottom). */
    public double target;
    public byte fault = NO_FAULT;
    /** The setpoint spans the travel (else its eight bands pick stations). */
    public boolean proportional;
    /** The car entity's UUID (it isn't saved with the world: found again within a session, else spawned anew). */
    public @Nullable UUID carId;

    // live
    /** The rail's length (blocks; -1 not scanned yet) and the stations on it. */
    public int travel = -1;
    public int[] stations = new int[0];
    @Nullable ElevatorCar car;
    /** The setpoint wire's last command (NaN: not wired), whether enable was high last tick. */
    double wired = Double.NaN;
    boolean wasEnabled = true;
    int sinceRetarget;
    /** A floor ghost of ours may stand (true after a load: unknown). */
    boolean ghostUp = true;
    /** Load on the car (kg), power drawn (W), speed (m/s, + along the axis) this tick. */
    public double loadKg, drawW, speed;
    byte status = IDLE;

    public LinearServoDriveElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (LinearServoDriveDescriptor) d;
        electricalLoadList.add(power);
        electricalComponentList.add(motor);
        electricalLoadList.add(setpoint);
        electricalLoadList.add(enable);
        electricalLoadList.add(inPosition);
        electricalLoadList.add(faultOut);
        electricalComponentList.add(inPositionProcess);
        electricalComponentList.add(faultProcess);
        thermalLoadList.add(thermal);
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

    /** Blocks of rail from the drive along its axis (0..MAX_TRAVEL), and the stations marked beside it. */
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
        List<Integer> marks = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            BlockPos r = p.relative(ax, i);
            for (var d : net.minecraft.core.Direction.values()) {
                if (d.getAxis() == ax.getAxis() || d == carSide()) continue;
                BlockPos m = r.relative(d);
                if (!level.isLoaded(m)) return;   // (a marker's chunk not there: keep the stations as they were)
                var s = level.getBlockState(m);
                if (s.getBlock() instanceof StationMarkerBlock && s.getValue(StationMarkerBlock.FACING) == d) marks.add(i);
            }
        }
        stations = Stations.of(marks.stream().mapToInt(Integer::intValue).toArray(), n);
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
        return !setpoint.getConnectedComponents().isEmpty();
    }

    /** Go to stop t (a new command also clears an obstruction). */
    public void command(double t) {
        target = Math.max(0, Math.min(Math.max(0, travel), Math.round(t)));
        if (fault == OBSTRUCTED) fault = NO_FAULT;
        needPublish();
    }

    /** A Station Marker at pos was clicked. */
    public void callTo(Player player, BlockPos pos) {
        BlockPos d = pos.subtract(node.coordonate.pos());
        var ax = axis();
        int i = d.getX() * ax.getStepX() + d.getY() * ax.getStepY() + d.getZ() * ax.getStepZ();
        if (wiredUp()) say(player, I18N.tr("This elevator follows its setpoint wire"));
        else if (i < 0 || i > travel) say(player, I18N.tr("This station is off the rail"));
        else {
            command(i);
            say(player, I18N.tr("Elevator called"));
        }
    }

    /** A rider pressed the car's panel: up or down a station. */
    public void panel(Player player, boolean up) {
        if (wiredUp()) {
            say(player, I18N.tr("This elevator follows its setpoint wire"));
            return;
        }
        double p = posAt(clock);
        int s = up ? Stations.above(p, stations) : Stations.below(p, stations);
        if (s < 0) say(player, up ? I18N.tr("Top station") : I18N.tr("Bottom station"));
        else command(s);
    }

    static void say(Player p, String text) {
        p.sendOverlayMessage(Component.literal(text));
    }

    /** True when its enable input is high, or not connected. */
    boolean enabled() {
        return enable.getConnectedComponents().isEmpty() || enable.stateHigh();
    }

    // ---- the tick

    void step(double dt) {
        if (!(world() instanceof ServerLevel level)) return;
        BlockPos hp = node.coordonate.pos();
        BlockPos col = hp.relative(carSide());
        if (!level.isLoaded(hp) || !level.isLoaded(col)) {
            setDraw(0, 0);   // (unloaded: the clock stops, the car with it)
            return;
        }
        long t0 = clock;
        clock++;
        if (travel < 0 || clock % 20 == 0) scan(level);
        double u = power.getU(), vlim = LinearServoDriveDescriptor.vmaxPerTick(u), a = LinearServoDriveDescriptor.accelPerTick();
        double p = posAt(t0), v = seg.velocity(t0 - seg.startTick());
        boolean moving = movingAt(t0);

        // faults that come and go by themselves
        double heat = thermal.Tc - thermal.ambient;
        if (fault == OVERHEAT && heat < LinearServoDriveDescriptor.RESUME_K) fault = NO_FAULT;
        if (heat > LinearServoDriveDescriptor.WARM_K && fault != OBSTRUCTED) fault = OVERHEAT;
        boolean railOk = travel >= LinearServoDriveDescriptor.MIN_TRAVEL && p <= travel + 1e-9;
        if (!railOk && fault != OBSTRUCTED) fault = NO_RAIL;
        else if (railOk && fault == NO_RAIL) fault = NO_FAULT;

        // commands from the wires
        if (wiredUp() && travel >= 0) {
            double want = proportional ? Stations.proportional(setpoint.getNormalized(), travel) : Stations.band(setpoint.getNormalized(), stations);
            if (want != wired) {
                wired = want;
                command(want);
            }
        } else wired = Double.NaN;
        boolean en = enabled();
        if (en && !wasEnabled && fault == OBSTRUCTED) fault = NO_FAULT;   // (enable toggled: try again)
        wasEnabled = en;
        if (railOk) target = Math.max(0, Math.min(travel, target));

        // the motion
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
        } else if (moving && Math.abs(vlim - seg.vmax()) > LinearServoDriveDescriptor.RETARGET_SHARE * seg.vmax()
            && sinceRetarget >= LinearServoDriveDescriptor.RETARGET_TICKS) {
            next = seg.retarget(t0, target, vlim, a);   // (the line sagged or recovered: the new top speed)
        }
        boolean changed = next != seg;
        seg = next;
        sinceRetarget = changed ? 0 : sinceRetarget + 1;
        double p1 = posAt(clock);

        // the car: sweep, place, carry
        ElevatorCar c = travel >= LinearServoDriveDescriptor.MIN_TRAVEL || car != null ? car(level) : null;
        loadKg = 0;
        if (c != null) {
            AABB box0 = c.boxAt(p);
            double tol = Riders.tolerance(p1 - p);
            List<Entity> riders = riders(level, c, box0, tol);
            for (Entity e : riders) loadKg += massOf(e);
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

        // power and heat
        double vm = (p1 - p) * 20;
        speed = vm;
        var ax = axis();
        double force = LinearServoDriveDescriptor.G * (loadKg - LinearServoDriveDescriptor.counterweightKg) * ax.getStepY();
        setDraw(vm, force);
        double pin = u * u / motor.getR();
        double mech = Math.max(0, force * vm);
        thermal.moveEnergyTo(Math.max(0, pin - Math.min(mech, pin * LinearServoDriveDescriptor.drive.efficiency())) * dt);

        // outputs and the lamp
        boolean stopped = !movingAt(clock);
        boolean there = stopped && fault == NO_FAULT && Math.abs(p1 - target) < 1e-6 && railOk;
        inPositionProcess.state(there);
        faultProcess.state(fault != NO_FAULT);
        byte s = status();
        if (s != status) {
            status = s;
            needPublish();
        }
    }

    /** What the lamp shows, from the saved state alone. */
    public byte status() {
        if (fault != NO_FAULT) return FAULT;
        if (movingAt(clock)) return MOVING;
        return Math.abs(posAt(clock) - target) < 1e-6 ? IN_POSITION : IDLE;
    }

    /** Draw at speed vm (m/s) against force f (N): the resistor's value. */
    void setDraw(double vm, double f) {
        Drive d = LinearServoDriveDescriptor.drive;
        drawW = d.watts(vm, f);
        motor.setR(Drive.resistance(d.nominalVolts(), drawW));
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
        ElnDeviceRegistry.meduimVoltageCableDescriptor.applyTo(power);
        thermal.set(1, LinearServoDriveDescriptor.HEAT_RP, LinearServoDriveDescriptor.HEAT_C);
        setDraw(0, 0);
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

    /** An empty hand switches what the setpoint means: stations, or a share of the travel. */
    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        if (!player.getMainHandItem().isEmpty()) return false;
        if (player.level().isClientSide()) return true;
        proportional = !proportional;
        wired = Double.NaN;
        say(player, proportional ? I18N.tr("The setpoint spans the travel") : I18N.tr("The setpoint selects a station"));
        needPublish();
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (side == front.back()) return lrdu == LRDU.Down ? power : null;
        if (side == front.left()) return lrdu == LRDU.Down ? setpoint : lrdu == LRDU.Up ? inPosition : null;
        if (side == front.right()) return lrdu == LRDU.Down ? enable : lrdu == LRDU.Up ? faultOut : null;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (side == front.back()) return lrdu == LRDU.Down ? NodeBase.maskElectricalPower : 0;
        if (side == front.left() || side == front.right())
            return lrdu == LRDU.Down ? NodeBase.maskElectricalInputGate : lrdu == LRDU.Up ? NodeBase.maskElectricalOutputGate : 0;
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        if (side == front.left()) return Utils.plotSignal(setpoint.getU(), setpoint.getCurrent());
        if (side == front.right()) return Utils.plotSignal(enable.getU(), enable.getCurrent());
        return Utils.plotUIP(power.getU(), power.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(status());
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
        nbt.putBoolean("proportional", proportional);
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
        proportional = nbt.getBooleanOr("proportional", false);
        carId = nbt.contains("carMost") ? new UUID(nbt.getLongOr("carMost", 0), nbt.getLongOr("carLeast", 0)) : null;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();
        double p = posAt(clock);
        info.put(I18N.tr("Position"), String.format("%.1f m", p));
        info.put(I18N.tr("Target"), String.format("%.0f m", target));
        info.put(I18N.tr("Speed"), String.format("%.2f m/s", Math.abs(speed)));
        info.put(I18N.tr("Power"), Utils.plotPower("", drawW));
        info.put(I18N.tr("Setpoint"), proportional ? I18N.tr("share of the travel") : I18N.tr("station"));
        info.put(I18N.tr("State"), stateText());
        return info;
    }

    /** The state in words (Jade). */
    public String stateText() {
        return switch (fault) {
            case OBSTRUCTED -> I18N.tr("Fault: something is in the way");
            case OVERHEAT -> I18N.tr("Fault: the motor is too hot");
            case NO_RAIL -> travel < LinearServoDriveDescriptor.MIN_TRAVEL ? I18N.tr("Fault: needs a rail of %1$ to %2$ blocks on top",
                LinearServoDriveDescriptor.MIN_TRAVEL, LinearServoDriveDescriptor.MAX_TRAVEL) : I18N.tr("Fault: the rail is broken");
            default -> LinearServoDriveDescriptor.vmaxPerTick(power.getU()) <= 0 ? I18N.tr("No power")
                : !enabled() ? I18N.tr("Disabled") : movingAt(clock) ? I18N.tr("Moving") : I18N.tr("In position");
        };
    }
}
