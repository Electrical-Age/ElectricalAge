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
 * on the RS485 bus (Baughn 2026-11-05: a plain actuator, an addressed device): 0 target (blocks along its path from the start), 1
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
    /** Where the car is to go (whole blocks along its path from the start). */
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
    /** Screen packets: its bus address, the car panel's first address (floats; anything but 17..255: none). */
    public static final byte setAddressId = 1, setPanelBaseId = 2;

    // the car's panel (ElevatorPanel): what each slot holds, the first slot's address, which lamps are lit (saved);
    // a bus port per filled slot, its address panelBase + slot, on the drive's bus (the travelling cable)
    public final int[] panel = new int[ElevatorPanel.SLOTS];
    public int panelBase, panelLit;
    final BusPort[] slotPorts = new BusPort[ElevatorPanel.SLOTS];

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

    /**
     * The rail's direction (Baughn 2026-11-26, straight paths in any direction): up for an elevator; along either flank
     * for a level run, its rail lying on the drive's top and on beside the car at that height (a guide rail on the
     * car's back wall), so the drive's faces stay free for its cables. Set by scan() from the rail on its top; saved
     * (NBT axis, absent: up), so a reload with the top rail gone doesn't put the car on another path.
     */
    net.minecraft.core.Direction axis() {
        return axis;
    }

    public net.minecraft.core.Direction axis = net.minecraft.core.Direction.UP;

    /** Can a rail run this way from the drive: up, or along either flank? */
    public boolean railAxis(net.minecraft.core.Direction d) {
        return d == net.minecraft.core.Direction.UP || d == front.left().toEnumFacing() || d == front.right().toEnumFacing();
    }

    /** The car's side of the drive (its front). */
    net.minecraft.core.Direction carSide() {
        return front.toEnumFacing();
    }

    /** The plate's bottom centre at position 0: the middle of the 3 x 3 blocks in front of the drive, level with its foot. */
    Vec3 origin() {
        BlockPos c = node.coordonate.pos().relative(carSide(), 2);
        return new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    // ---- a laid track (track B: TrackPlacer, the Track Kit); null: it runs on its rail as before

    /** Its track's points (blocks; the first is origin()) and which are stops; null without one. Saved ("track"). */
    @org.jspecify.annotations.Nullable List<Vec3> trackPoints;
    @org.jspecify.annotations.Nullable boolean[] trackStops;
    private mods.eln.node.voxel.TrackPlacer.@org.jspecify.annotations.Nullable Track trackCache;

    /** Is there a Servo Rail on its top (it is a mast or a level run: no track can be laid from it)? */
    public boolean hasRail() {
        return node.coordonate.world().getBlockState(node.coordonate.pos().above()).getBlock() instanceof ServoRailBlock;
    }

    /** Where a track laid from it starts: its car's place at position 0. */
    public Vec3 trackStart() {
        return origin();
    }

    /** The drive whose track starts at s (its car's start), or null. */
    public static @org.jspecify.annotations.Nullable LinearServoDriveElement atTrackStart(Level level, Vec3 s) {
        BlockPos c = BlockPos.containing(s.x, s.y + 0.01, s.z);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            LinearServoDriveElement e = at(level, c.relative(d, 2));
            if (e != null && e.origin().distanceToSqr(s) < 1e-6) return e;
        }
        return null;
    }

    /** Its laid track (points, stops, path), or null. */
    public mods.eln.node.voxel.TrackPlacer.@org.jspecify.annotations.Nullable Track track() {
        if (trackPoints == null || trackStops == null) return null;
        if (trackCache == null) {
            try {
                trackCache = mods.eln.node.voxel.TrackPlacer.track(trackPoints, trackStops);
            } catch (IllegalArgumentException ex) {   // (a saved track the rules no longer allow: dropped, said in the log)
                mods.eln.Eln.LOG.warn("Linear Servo Drive at {}: its track no longer fits ({}); dropped", node.coordonate.pos(), ex.getMessage());
                trackPoints = null;
                trackStops = null;
                return null;
            }
        }
        return trackCache;
    }

    /** Lay a track from it (TrackPlacer): replaces any track it had; the car goes back to the start. Null if laid, else why not. */
    public @org.jspecify.annotations.Nullable String setTrack(List<Vec3> points, boolean[] stops) {
        if (hasRail()) return "it runs on its rail";
        if (points.isEmpty() || points.get(0).distanceToSqr(origin()) > 1e-6) return "the track doesn't start at its car";
        trackPoints = List.copyOf(points);
        trackStops = stops.clone();
        trackCache = null;
        if (track() == null) return "that track doesn't fit";
        removeGhost();
        seg = Segment.rest(clock, 0);
        target = 0;
        travel = -1;   // (read again from the track)
        if (car != null) {   // (riders other than players stay where they were: the car is put back, not driven)
            setup(car);
            car.placeAt(0);
            car.drive(seg, clock);
            car.broadcastTrack();
            car.broadcast();
        }
        needPublish();
        return null;
    }

    /** How far the car can go (m): its track's length, or its rail's travel (blocks). */
    double reach() {
        var t = track();
        return t != null ? t.path().length() : travel;
    }

    /** Its track's stops (m along it), or null on a rail (every whole block a stop). */
    double @org.jspecify.annotations.Nullable [] stops() {
        var t = track();
        return t != null ? t.path().stops() : null;
    }

    /** Is p a stop (a whole block on a rail; one of the track's stops)? */
    boolean atStop(double p) {
        double[] st = stops();
        return st != null ? Stations.atStop(p, st) : Math.abs(p - Math.rint(p)) < 1e-6;
    }

    /** The car's path: its laid track, or from origin() along its rail, travel blocks long. */
    public mods.eln.robotics.motion.Path path() {
        var t = track();
        if (t != null) return t.path();
        return new mods.eln.robotics.motion.StraightPath(origin(), axis(), Math.max(0, travel));
    }

    /**
     * The rail from the drive: its direction (the rail on its top: up, or lying along a flank) and the car's travel, in
     * blocks: an elevator's rail blocks (the car's floor rises to the top one's height); a level run's rail blocks past
     * the one on the drive (the car goes as far as the rail beside it). A level run is read only as far as its chunks
     * are loaded (reading on would load them); cut short there, the travel stays as it was. The rail turned another
     * way: the car is put back at the start.
     */
    void scan(Level level) {
        var tr = track();
        if (tr != null) {   // (a laid track: no rail to read; the travel shown is its length in whole metres)
            travel = (int) Math.floor(tr.path().length() + 1e-9);
            return;
        }
        BlockPos top = node.coordonate.pos().above();
        var s0 = level.getBlockState(top);
        var ax = s0.getBlock() instanceof ServoRailBlock && railAxis(s0.getValue(ServoRailBlock.FACING)) ? s0.getValue(ServoRailBlock.FACING) : axis;
        int n = 0;
        boolean cut = false;
        while (n <= LinearServoDriveDescriptor.MAX_TRAVEL) {
            BlockPos b = top.relative(ax, n);
            if (ax != net.minecraft.core.Direction.UP && !level.isLoaded(b)) {   // (an elevator's mast stays in its own column)
                cut = true;
                break;
            }
            var s = level.getBlockState(b);
            if (!(s.getBlock() instanceof ServoRailBlock) || s.getValue(ServoRailBlock.FACING) != ax) break;
            n++;
        }
        int t = Math.min(LinearServoDriveDescriptor.MAX_TRAVEL, ax == net.minecraft.core.Direction.UP ? n : Math.max(0, n - 1));
        if (ax != axis) {
            axis = ax;
            removeGhost();
            seg = Segment.rest(clock, 0);
            target = 0;
            wired = Double.NaN;
            if (car != null) {   // (riders other than players stay where they were: the car is put back, not driven)
                setup(car);
                car.placeAt(0);
                car.drive(seg, clock);
                car.broadcast();
            }
            needPublish();
        } else if (cut) {   // (the run's far end is in an unloaded chunk: as it was; first read after a load: as far as it goes to)
            // (cut short, it can only undercount: the travel may grow, never shrink)
            int known = travel < 0 ? (int) Math.ceil(Math.max(target, posAt(clock)) - 1e-9) : travel;
            travel = Math.min(LinearServoDriveDescriptor.MAX_TRAVEL, Math.max(t, known));
            return;
        }
        travel = t;
    }

    /** Is the ground the car stands on and moves into next (its cabin's reach and 2 blocks more, along a level run) loaded? */
    boolean carGround(Level level) {
        double p = posAt(clock);
        var path = path();
        // (the cabin reaches 1.5 m either side of its centre: from 1 block behind the start to 1 past the end)
        for (double s : new double[]{p - 3, (travel < 0 ? p : Math.min(p, travel)) + 3}) {
            BlockPos b = BlockPos.containing(path.at(s));
            for (int out = -2; out <= 2; out += 4)   // (the rail line to past the car's far side)
                if (!level.isLoaded(b.relative(carSide(), out))) return false;
        }
        return true;
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
        double[] st = stops();
        target = st != null ? Stations.nearest(t, st) : Math.max(0, Math.min(Math.max(0, travel), Math.round(t)));
        if (fault == OBSTRUCTED) fault = NO_FAULT;
        needPublish();
    }

    // ---- the car's panel

    /**
     * A rider clicked slot i of the car's panel: with an Addressable Button or Light in hand, an empty slot takes it;
     * sneaking with an empty hand, a full one gives it back; an empty hand presses a button (`press` to everyone, from
     * the slot's address).
     */
    public void panelClicked(Player player, int i) {
        if (i < 0 || i >= panel.length) return;
        var hand = player.getMainHandItem();
        int kind = kindOf(hand);
        if (panel[i] == ElevatorCar.PANEL_NONE) {
            if (kind == ElevatorCar.PANEL_NONE) {
                say(player, I18N.tr("Needs an Addressable Button or Light"));
                return;
            }
            panel[i] = kind;
            if (!player.getAbilities().instabuild) hand.shrink(1);
            if (panelBase == 0) say(player, I18N.tr("Panel address unset"));
        } else if (player.isShiftKeyDown() && hand.isEmpty()) {
            player.getInventory().add(stackOf(panel[i]));
            panel[i] = ElevatorCar.PANEL_NONE;
            panelLit &= ~(1 << i);
        } else if (panel[i] == ElevatorCar.PANEL_BUTTON && hand.isEmpty()) {
            if (slotPorts[i] != null) slotPorts[i].send(new Packet(Packet.BROADCAST, slotPorts[i].address(), PacketType.PRESS, 0, 0));
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.2f);
            return;
        } else return;
        panelPorts();
        showPanel();
        needPublish();
    }

    /** What an item would be in the panel: PANEL_BUTTON, PANEL_LIGHT or PANEL_NONE. */
    static int kindOf(net.minecraft.world.item.ItemStack s) {
        if (!(s.getItem() instanceof mods.eln.node.six.SixNodeItem si)) return ElevatorCar.PANEL_NONE;
        if (si.descriptor instanceof mods.eln.sixnode.busdevice.AddressableButtonDescriptor) return ElevatorCar.PANEL_BUTTON;
        if (si.descriptor instanceof mods.eln.sixnode.busdevice.AddressableLightDescriptor) return ElevatorCar.PANEL_LIGHT;
        return ElevatorCar.PANEL_NONE;
    }

    /** The item a slot holds. */
    static net.minecraft.world.item.ItemStack stackOf(int kind) {
        String name = kind == ElevatorCar.PANEL_BUTTON ? "Addressable Button" : "Addressable Light";
        return Eln.findItemStack(name, 1);
    }

    /** Slot i's address: panelBase + i, or 0 (no panel address, or past 255). */
    public int slotAddress(int i) {
        int a = panelBase + i;
        return panelBase != 0 && Packet.deviceAddress(a) ? a : 0;
    }

    /** A port for each filled slot, at its address, on the drive's bus; none for an empty one. */
    void panelPorts() {
        for (int i = 0; i < panel.length; i++) {
            if (panel[i] == ElevatorCar.PANEL_NONE) {
                if (slotPorts[i] != null) mods.eln.bus.BusNet.detach(List.of(slotPorts[i]));
                slotPorts[i] = null;
                continue;
            }
            if (slotPorts[i] == null) {
                slotPorts[i] = new BusPort();
                slotPorts[i].listen(true);
            }
            slotPorts[i].setAddress(slotAddress(i));
            mods.eln.bus.BusNet.link(port, slotPorts[i]);
        }
    }

    /** The packets for the panel's slots: `power` sets a lamp (over half: lit). */
    void servePanel() {
        int lit = panelLit;
        for (int i = 0; i < panel.length; i++) {
            BusPort sp = slotPorts[i];
            if (sp == null) continue;
            for (Packet k; (k = sp.poll()) != null; )
                if (k.type() == PacketType.POWER && sp.address() != 0 && k.target() == sp.address())
                    lit = k.value() > 0.5f ? lit | 1 << i : lit & ~(1 << i);
        }
        if (lit != panelLit) {
            panelLit = lit;
            showPanel();
        }
    }

    void showPanel() {
        if (car != null) car.showPanel(panel, panelLit);
    }

    @Override
    public void connectJob() {
        super.connectJob();
        panelPorts();   // (the travelling cable: the slots on the drive's bus again)
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        List<BusPort> ps = new ArrayList<>();
        for (BusPort p : slotPorts) if (p != null) ps.add(p);
        mods.eln.bus.BusNet.detach(ps);
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
        if ((axis != net.minecraft.core.Direction.UP || track() != null) && !carGround(level)) return;   // (a level run reaching an unloaded chunk: the same)
        long t0 = clock;
        clock++;
        if (travel < 0 || clock % 20 == 0) scan(level);
        serveBus();
        servePanel();
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
        double reach = reach();
        boolean railOk = reach >= LinearServoDriveDescriptor.MIN_TRAVEL && p <= reach + 1e-9;
        if (!railOk && fault != OBSTRUCTED) fault = NO_RAIL;
        else if (railOk && fault == NO_RAIL) fault = NO_FAULT;

        // commands from the wires
        if (wiredUp() && travel >= 0) {
            double in = machine.signal(LinearServoDriveDescriptor.SETPOINT);
            double[] st = stops();
            double want = st != null ? Stations.nearest(Math.max(0, Math.min(1, in)) * reach, st) : Stations.proportional(in, travel);
            if (want != wired) {
                wired = want;
                command(want);
            }
        } else wired = Double.NaN;
        boolean en = enabled();
        if (en && !wasEnabled && fault == OBSTRUCTED) fault = NO_FAULT;   // (enable toggled: try again)
        wasEnabled = en;
        if (railOk) target = Math.max(0, Math.min(reach, target));
        // round a corner no faster than its lateral acceleration allows (track B: v <= sqrt(a r)); one limit per move
        if (track() != null) {
            double r = track().path().minRadius(p, target);
            if (Double.isFinite(r)) vlim = Math.min(vlim, Math.sqrt(LinearServoDriveDescriptor.LATERAL_ACCEL * r) / 20);
        }

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
                double[] st = stops();
                double stop = st != null ? Stations.stopAhead(p, v, a, st) : Stations.stopAhead(p, v, a, travel);
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
        // (the weight along the path where it is: all of it going straight up, none on the level)
        forceN = Drive.G * (loadKg - LinearServoDriveDescriptor.counterweightKg) * path().grade(p);
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
            if (c.cabinMode() == ElevatorCar.CABIN_CLAMP) clampMobs(level, c);
            c.drive(seg, clock);
            if (changed) c.broadcast();
            if (!movingAt(clock) && atStop(p1)) floorGhost(level, c, p1);
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
        machine.setSignal(LinearServoDriveDescriptor.POSITION, reach > 0 ? Math.max(0, Math.min(1, p1 / reach)) : 0);
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
        showPanel();
        return car;
    }

    void setup(ElevatorCar c) {
        c.setTrack(trackPoints, trackStops);
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

    /** Is anything in the way of the car's cabin (and its riders) going from p to p1: a block, or a creature not in it? */
    static boolean blocked(ServerLevel level, ElevatorCar c, List<Entity> riders, double p, double p1) {
        AABB b0 = c.cabinBox(p), b1 = c.cabinBox(p1);   // (the whole cabin sweeps the shaft)
        Vec3 d = c.at(p1).subtract(c.at(p));
        List<AABB> swept = new ArrayList<>();
        swept.add(b0.minmax(b1).deflate(1e-4));
        for (Entity e : riders) {   // (clipped to the car: a rider's overhang meets the landing, not the car)
            AABB s = Riders.standing(e.getBoundingBox(), c.boxAt(p)).deflate(1e-4);
            swept.add(s.minmax(s.move(d)));
        }
        for (AABB s : swept) if (!level.noBlockCollision(null, s)) return true;
        AABB all = swept.get(0);
        for (AABB s : swept) all = all.minmax(s);
        // (a creature in the cabin already, jumping or not on the plate, goes with it: only what the cabin moves into is in the way)
        for (Entity e : level.getEntities(c, all, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !riders.contains(e)
            && !b0.contains(e.getBoundingBox().getCenter())))
            for (AABB s : swept) if (e.getBoundingBox().intersects(s)) return true;
        return false;
    }

    /** Track B0 spike: the radial limit for creatures in a round cabin (players: their clients do it). */
    static void clampMobs(ServerLevel level, ElevatorCar c) {
        var out = c.railSide.getOpposite();
        for (Entity e : level.getEntities(c, c.getBoundingBox().setMaxY(c.getY() + ElevatorCar.CABIN).inflate(0.5, 0, 0.5),
            e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive())) {
            var k = Riders.clampInside(e.getBoundingBox(), c.getX(), c.getZ(), ElevatorCar.ROUND_IN, c.getY(), c.getY() + ElevatorCar.CABIN,
                out.getStepX(), out.getStepZ(), Math.toRadians(40), c.stopped());
            if (k != null) e.setPos(e.position().add(k));
        }
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

    /**
     * Stopped on a whole block: solid floor plate ghosts under the car's 3 x 3 plate (so nobody falls through before
     * it's back).
     */
    void floorGhost(ServerLevel level, ElevatorCar c, double s) {
        BlockPos mid = floorBlock(c, s);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos g = mid.offset(dx, 0, dz);
                Coordonate gc = new Coordonate(g, level);
                if (Eln.ghostManager.getGhost(gc) != null) continue;
                if (!Eln.ghostManager.canCreateGhostAt(level, g.getX(), g.getY(), g.getZ())) continue;
                Eln.ghostManager.createGhost(gc, node.coordonate, FLOOR_GHOST, Eln.ghostBlock, GhostBlock.tFloor);
                ghostUp = true;
            }
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
        for (int i = 0; i < panel.length; i++)   // (the panel's parts come back)
            if (panel[i] != ElevatorCar.PANEL_NONE && world() instanceof ServerLevel l) {
                BlockPos p = node.coordonate.pos();
                net.minecraft.world.Containers.dropItemStack(l, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, stackOf(panel[i]));
                panel[i] = ElevatorCar.PANEL_NONE;
            }
        panelPorts();
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
        if (id == setAddressId || id == setPanelBaseId) {
            try {
                int a = Math.round(stream.readFloat());
                if (id == setAddressId) setAddress(a);
                else {
                    panelBase = Packet.deviceAddress(a) ? a : 0;
                    panelPorts();
                    needPublish();
                }
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
        return Utils.plotCelsius("T: ", machine.celsius() - mods.eln.sim.PhysicalConstant.Tamb);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(status());
            stream.writeByte(port.address());
            stream.writeBoolean(port.bus() != null);
            stream.writeByte(panelBase);
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
        if (axis != net.minecraft.core.Direction.UP) nbt.putByte("axis", (byte) axis.get3DDataValue());   // (absent: up, an elevator)
        if (speedLimit > 0) nbt.putDouble("speedLimit", speedLimit);
        if (port.address() != 0) nbt.putInt("addr", port.address());
        if (panelBase != 0) nbt.putInt("panelBase", panelBase);
        nbt.putIntArray("panel", panel);
        if (panelLit != 0) nbt.putInt("panelLit", panelLit);
        if (trackPoints != null && trackStops != null) {   // (voxels: x, y, z, stop per point)
            int[] t = new int[trackPoints.size() * 4];
            for (int i = 0; i < trackPoints.size(); i++) {
                Vec3 q = trackPoints.get(i);
                t[4 * i] = (int) Math.round(q.x * 16);
                t[4 * i + 1] = (int) Math.round(q.y * 16);
                t[4 * i + 2] = (int) Math.round(q.z * 16);
                t[4 * i + 3] = trackStops[i] ? 1 : 0;
            }
            nbt.putIntArray("track", t);
        }
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
        var ax = net.minecraft.core.Direction.from3DDataValue(nbt.getByteOr("axis", (byte) net.minecraft.core.Direction.UP.get3DDataValue()));
        axis = ax.getAxis() == net.minecraft.core.Direction.Axis.Y ? net.minecraft.core.Direction.UP : ax;
        speedLimit = Math.max(0, nbt.getDoubleOr("speedLimit", 0));   // ("proportional" of before 2026-11-05 is ignored: always)
        port.setAddress(nbt.getIntOr("addr", 0));
        panelBase = nbt.getIntOr("panelBase", 0);
        int[] p = nbt.getIntArray("panel").orElse(new int[0]);
        for (int i = 0; i < panel.length; i++) panel[i] = i < p.length && p[i] >= 0 && p[i] <= 2 ? p[i] : ElevatorCar.PANEL_NONE;
        panelLit = nbt.getIntOr("panelLit", 0);
        panelPorts();
        int[] t = nbt.getIntArray("track").orElse(new int[0]);
        if (t.length >= 8 && t.length % 4 == 0) {
            List<Vec3> pts = new ArrayList<>();
            boolean[] st = new boolean[t.length / 4];
            for (int i = 0; i < st.length; i++) {
                pts.add(new Vec3(t[4 * i] / 16.0, t[4 * i + 1] / 16.0, t[4 * i + 2] / 16.0));
                st[i] = t[4 * i + 3] != 0;
            }
            trackPoints = pts;
            trackStops = st;
        } else {
            trackPoints = null;
            trackStops = null;
        }
        trackCache = null;
        carId = nbt.contains("carMost") ? new UUID(nbt.getLongOr("carMost", 0), nbt.getLongOr("carLeast", 0)) : null;
    }

    @Override
    /** State/faults first, then position/target and power; secondary telemetry in details. */
    public Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        double p = posAt(clock);
        info.headline(I18N.tr("State"), stateText());
        info.headline(I18N.tr("Position"), String.format("%.1f / %.0f m", p, target));
        info.headline(I18N.tr("Power"), Utils.plotPower("", machine.watts()));
        info.detail(I18N.tr("Speed"), String.format("%.2f m/s", Math.abs(speed)));
        info.detail(I18N.tr("Voltage"), Utils.plotVolt("", machine.volts()));
        info.detail(I18N.tr("Address"), port.address() == 0 ? I18N.tr("None") : String.valueOf(port.address()));
        int buttons = 0, lights = 0;
        for (int k : panel) if (k == ElevatorCar.PANEL_BUTTON) buttons++; else if (k == ElevatorCar.PANEL_LIGHT) lights++;
        if (buttons + lights > 0)
            info.detail(I18N.tr("Car panel"), (buttons == 1 ? I18N.tr("1 button") : I18N.tr("%1$ buttons", buttons)) + ", "
                + (lights == 1 ? I18N.tr("1 light") : I18N.tr("%1$ lights", lights)) + ", " + (panelBase == 0 ? I18N.tr("no address")
                : I18N.tr("addresses %1$–%2$", panelBase, Math.min(255, panelBase + ElevatorPanel.SLOTS - 1))));
        return info;
    }

    /** The state in words (Jade). */
    public String stateText() {
        return switch (fault) {
            case OBSTRUCTED -> I18N.tr("Fault: obstructed");
            case OVERHEAT -> I18N.tr("Fault: overheated");
            case OVERVOLTAGE -> I18N.tr("Fault: overvoltage");
            case NO_RAIL -> travel < LinearServoDriveDescriptor.MIN_TRAVEL
                ? I18N.tr("Fault: needs %1$–%2$ blocks of rail", LinearServoDriveDescriptor.MIN_TRAVEL, LinearServoDriveDescriptor.MAX_TRAVEL)
                : I18N.tr("Fault: rail broken");
            default -> stalled && machine.state() == MachineState.RUNNING ? I18N.tr("Line too weak")
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
        st.choice("axis", axis);
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
