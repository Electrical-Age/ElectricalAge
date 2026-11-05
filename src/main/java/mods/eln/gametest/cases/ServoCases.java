package mods.eln.gametest.cases;

import mods.eln.api.MachineState;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.robotics.Robotics;
import mods.eln.robotics.servo.ElevatorCar;
import mods.eln.robotics.servo.LinearServoDriveDescriptor;
import mods.eln.robotics.servo.LinearServoDriveElement;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * The servo layer's elevator (mods.eln.robotics; Baughn: the servo layer first, 200 V only, players ride the carriage):
 * a Linear Servo Drive with a six-block Servo Rail mast, fed 200 V from the back, commanded directly (command) or over
 * its RS485 bus. The drive is placed looking south, so its front (the car's shaft) is north.
 */
public final class ServoCases {
    private ServoCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new OverTheBus());
        cases.add(new ByTheWire());
        cases.add(new CarPanel());
        cases.add(new FloorSensor());
        cases.add(new Sag());
        cases.add(new Obstruction());
        cases.add(new SaveMidMove());
        cases.add(new GhostFloor());
        cases.add(new CarriesAMob());
        cases.add(new CabinWalls());
        cases.add(new PanelId());
        cases.add(new Starved());
        cases.add(new EnergyBooks());
    }

    static final int TRAVEL = 6;

    /** A drive at p with its mast, 200 V through a Power Resistor of `coal` core items (0: none). */
    static final class Shaft {
        LinearServoDriveElement d;
        BlockPos head;
        Direction front;

        Shaft(TestContext ctx, BlockPos p, int coal) {
            d = (LinearServoDriveElement) ShaftArtCases.placeLooking(ctx, Robotics.DRIVE_ID, p, ShaftArtCases.SOUTH);
            if (d == null) throw new IllegalStateException("no drive at " + p);
            head = d.node.coordonate.pos();
            front = d.front;
            for (int i = 1; i <= TRAVEL; i++) ctx.level().setBlockAndUpdate(head.above(i), Robotics.SERVO_RAIL.get().defaultBlockState());
            var back = front.back().toEnumFacing();
            BlockPos at = head.relative(back);
            ctx.placeSix(Ids.six("Medium Voltage Cable"), at);
            if (coal > 0) {
                at = at.relative(back);
                ResistorElement r = (ResistorElement) ctx.placeSix(Wp9aCases.POWER_RESISTOR, at);
                Wp9aCases.orient(r, LRDU::right, front.back());
                r.getInventory().setItem(0, Wp9aCases.coal(coal));
                r.getInventory().setChanged();
            }
            ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(Wp11Cases.SOURCE, at.relative(back));
            s.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSourceElement.setVoltageId);
                out.writeFloat(200f);
            }));
        }

        /** The drive now (a reload swaps the element). */
        LinearServoDriveElement drive(TestContext ctx) {
            return LinearServoDriveElement.at(ctx.level(), head);
        }

        /** The volts on the in-position output (the left flank's floor, on its left) and the fault output (the right flank's). */
        double inPositionVolts(TestContext ctx) {
            return pinVolts(ctx, drive(ctx).front.left(), 4);
        }

        double faultVolts(TestContext ctx) {
            return pinVolts(ctx, drive(ctx).front.right(), 4);
        }

        /** The position output (the left flank's floor, on its right). */
        double positionVolts(TestContext ctx) {
            return pinVolts(ctx, drive(ctx).front.left(), -4);
        }

        /** The volts on the floor pin of side at leftward. */
        double pinVolts(TestContext ctx, mods.eln.misc.Direction side, int leftward) {
            for (var pin : drive(ctx).facePins(side))
                if (pin.lrdu() == LRDU.Down && pin.leftward() == leftward && pin.load() != null) return pin.load().getU();
            return Double.NaN;
        }

        /** The drive's machine (its energy books). */
        mods.eln.api.element.ElementMachine core(TestContext ctx) {
            return drive(ctx).machine;
        }

        /** Go to stop m (as its bus or wire would command it). */
        void call(TestContext ctx, int m) {
            drive(ctx).command(m);
        }

        ElevatorCar car(TestContext ctx) {
            List<ElevatorCar> cars = cars(ctx);
            return cars.size() == 1 ? cars.get(0) : null;
        }

        List<ElevatorCar> cars(TestContext ctx) {
            return ctx.level().getEntitiesOfClass(ElevatorCar.class, new AABB(head).inflate(2, TRAVEL + 3, 2));
        }

        /** The car's floor (its top) above the drive's foot (m). */
        double carFloor(TestContext ctx) {
            ElevatorCar c = car(ctx);
            return c == null ? Double.NaN : c.getBoundingBox().maxY - head.getY() - ElevatorCar.THICK;
        }
    }

    abstract static class Room implements TestCase {
        IProcess watch;
        int tick;

        public int width() {
            return 11;
        }

        public int height() {
            return 12;   // (the cabin's roof 3 m over its floor at the top station)
        }

        public int depth() {
            return 9;   // (the cabin in front of the drive: 3 blocks; power behind it)
        }

        public int floorDepth() {
            return 9;
        }

        void watch(IProcess p) {
            mods.eln.Eln.simulator.addSlowProcess(watch = p);
        }

        void unwatch() {
            if (watch != null) mods.eln.Eln.simulator.removeSlowProcess(watch);
        }
    }

    /**
     * Driven over its RS485 bus (Baughn 2026-11-05): a bus cable on the floor beside its right flank, ending at the bus
     * pin (the floor edge's right), and a port for a computer on it. Written its target (register 0) of 3 m, the car goes
     * there; read back, its state (register 3) is in position and its position (register 1) 3 m; the position wire
     * gives half its 50 V, in-position is high; a floor ghost holds the car.
     */
    static final class OverTheBus extends Room {
        Shaft s;
        mods.eln.bus.BusPort pc;
        final List<mods.eln.bus.Packet> replies = new java.util.ArrayList<>();
        boolean onBus;

        public String id() {
            return "servo_elevator_driven_over_the_bus";
        }

        public int ticks() {
            return 140;   // 3 m: ~3.5 s
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            var d = s.drive(ctx);
            d.networkUnserialize(ctx.stream(o -> {   // (its screen's packet)
                o.writeByte(LinearServoDriveElement.setAddressId);
                o.writeFloat(40);
            }));
            BlockPos beside = pinCable(ctx, s.head, d.front.right(), -4, mods.eln.node.voxel.CablePart.BUS);
            pc = new mods.eln.bus.BusPort();
            pc.setAddress(200);
            pc.listen(true);
            watch(dt -> {
                ++tick;
                if (tick == 5 || tick == 124) {   // (the car's floor ghosts beside the cable's block reconnect it as they come and go)
                    var v = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(beside, ctx.level()));
                    mods.eln.bus.BusNet.link(pc, v.busPorts().get(0));
                }
                if (tick == 10) pc.send(new mods.eln.bus.Packet(40, 200, mods.eln.bus.PacketType.WRITE, LinearServoDriveElement.REG_TARGET, 3));
                if (tick == 125) {
                    onBus = s.drive(ctx).port.bus() != null && s.drive(ctx).port.bus() == pc.bus();
                    pc.send(new mods.eln.bus.Packet(40, 200, mods.eln.bus.PacketType.READ, LinearServoDriveElement.REG_STATE, 0));
                    pc.send(new mods.eln.bus.Packet(40, 200, mods.eln.bus.PacketType.READ, LinearServoDriveElement.REG_POSITION, 0));
                    pc.send(new mods.eln.bus.Packet(40, 200, mods.eln.bus.PacketType.READ, 77, 0));   // (no such register: no reply)
                }
                if (tick > 125) for (mods.eln.bus.Packet k; (k = pc.poll()) != null; ) if (k.type() == mods.eln.bus.PacketType.REPLY) replies.add(k);
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            mods.eln.bus.BusNet.detach(List.of(pc));
            var d = s.drive(ctx);
            ctx.line("servo over the bus: " + d.getWaila() + " car floor " + s.carFloor(ctx) + " replies " + replies);
            ctx.check("servo bus: it reads the mast (6 blocks)", d.travel == TRAVEL, "" + d.travel);
            ctx.check("servo bus: its bus pin is on the computer's bus", onBus, "");
            ctx.check("servo bus: one car, standing at the target written", s.cars(ctx).size() == 1 && Math.abs(s.carFloor(ctx) - 3) < 1e-6,
                s.cars(ctx).size() + " cars, floor " + s.carFloor(ctx));
            ctx.check("servo bus: replies to the asker: in position, at 3 m, and none for no register", replies.size() == 2
                && replies.get(0).target() == 200 && replies.get(0).source() == 40
                && replies.get(0).index() == LinearServoDriveElement.REG_STATE && replies.get(0).value() == LinearServoDriveElement.IN_POSITION
                && replies.get(1).index() == LinearServoDriveElement.REG_POSITION && Math.abs(replies.get(1).value() - 3) < 1e-4, replies.toString());
            ctx.check("servo bus: in position (status and output)", d.status() == LinearServoDriveElement.IN_POSITION
                && s.inPositionVolts(ctx) > 40 && s.faultVolts(ctx) < 10, d.status() + " " + s.inPositionVolts(ctx) + " V");
            Wp9cCases.within(ctx, "servo bus: the position wire gives its share of the travel [V]", s.positionVolts(ctx), 25, 0.5);
            ctx.check("servo bus: a floor ghost under the stopped car", d.ghostAt(3), "");
            ctx.check("servo bus: Jade shows its address", "40".equals(d.getWaila().get("Address")), d.getWaila().toString());
            var ports = new java.util.TreeMap<String, mods.eln.introspect.Port.Kind>();
            for (var r : mods.eln.introspect.Describe.at(ctx.level(), d.node.coordonate.pos()))
                for (var p : r.ports()) ports.put(p.name(), p.kind());
            ctx.check("servo bus: introspect lists its ports by the spec's names", ports.keySet().containsAll(
                    List.of("power", "setpoint", "enable", "in_position", "fault", "position"))
                && ports.get("setpoint") == mods.eln.introspect.Port.Kind.SIGNAL && ports.keySet().stream().noneMatch(n -> n.startsWith("api_machine")), ports.toString());
        }
    }

    /**
     * The car's panel (Baughn 2026-11-05: real parts in it): an Addressable Button put in slot 0 by hand and a Light in
     * slot 5, the panel's first address 50 set in the drive's screen. A press on the button reaches a computer on the
     * drive's bus from address 50 (the travelling cable); power sent to 50 and 55 lights them, on the car too (synced);
     * sneaking with an empty hand takes the light back.
     */
    static final class CarPanel extends Room {
        Shaft s;
        mods.eln.bus.BusPort pc;
        final List<mods.eln.bus.Packet> heard = new java.util.ArrayList<>();
        boolean took, litButton, litLight, carShows, gaveBack;

        public String id() {
            return "servo_elevator_car_panel";
        }

        public int ticks() {
            return 50;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            var d = s.drive(ctx);
            d.networkUnserialize(ctx.stream(o -> {
                o.writeByte(LinearServoDriveElement.setPanelBaseId);
                o.writeFloat(50);
            }));
            BlockPos beside = pinCable(ctx, s.head, d.front.right(), -4, mods.eln.node.voxel.CablePart.BUS);
            pc = new mods.eln.bus.BusPort();
            pc.setAddress(200);
            pc.listen(true);
            watch(dt -> {
                ++tick;
                var p = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
                var dr = s.drive(ctx);
                if (tick == 5) {
                    var v = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(beside, ctx.level()));
                    mods.eln.bus.BusNet.link(pc, v.busPorts().get(0));
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mods.eln.Eln.findItemStack("Addressable Button", 1));
                    dr.panelClicked(p, 0);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mods.eln.Eln.findItemStack("Addressable Light", 1));
                    dr.panelClicked(p, 5);
                    took = dr.panel[0] == ElevatorCar.PANEL_BUTTON && dr.panel[5] == ElevatorCar.PANEL_LIGHT;
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                    dr.panelClicked(p, 0);   // (a press)
                }
                if (tick > 5 && tick < 20) for (mods.eln.bus.Packet k; (k = pc.poll()) != null; ) heard.add(k);
                if (tick == 20) {
                    pc.send(new mods.eln.bus.Packet(50, 200, mods.eln.bus.PacketType.POWER, 0, 1));
                    pc.send(new mods.eln.bus.Packet(55, 200, mods.eln.bus.PacketType.POWER, 0, 0.8f));
                }
                if (tick == 25) {
                    litButton = (dr.panelLit & 1) != 0;
                    litLight = (dr.panelLit >> 5 & 1) != 0;
                    ElevatorCar c = s.car(ctx);
                    carShows = c != null && c.panelKind(0) == ElevatorCar.PANEL_BUTTON && c.panelKind(5) == ElevatorCar.PANEL_LIGHT
                        && c.panelLit(0) && c.panelLit(5) && c.panelKind(1) == ElevatorCar.PANEL_NONE;
                    p.setShiftKeyDown(true);
                    p.getInventory().clearContent();
                    dr.panelClicked(p, 5);
                    p.setShiftKeyDown(false);
                    gaveBack = dr.panel[5] == ElevatorCar.PANEL_NONE && p.getInventory().countItem(mods.eln.Eln.findItemStack("Addressable Light", 1).getItem()) == 1;
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            mods.eln.bus.BusNet.detach(List.of(pc));
            ctx.check("servo panel: the slots take a button and a light", took, "");
            ctx.check("servo panel: a press reaches the computer from the slot's address", heard.stream().anyMatch(k ->
                k.type() == mods.eln.bus.PacketType.PRESS && k.source() == 50 && k.target() == mods.eln.bus.Packet.BROADCAST), heard.toString());
            ctx.check("servo panel: power sent to a slot's address lights it", litButton && litLight, litButton + " " + litLight);
            ctx.check("servo panel: the car shows the slots and lamps", carShows, "");
            ctx.check("servo panel: sneaking with an empty hand takes the light back", gaveBack, "");
            var w = s.drive(ctx).getWaila();
            ctx.check("servo panel: Jade on the drive sums the panel up", "1 button, 0 lights, addresses 50 to 61".equals(w.get("Car panel")), w.toString());
        }
    }

    /**
     * A Floor Sensor on the ground beside the cabin (Baughn 2026-11-05: floors found by sensors, not markers): level while
     * the car stands at stop 0, not while it's at 3, level again when it's back, sending `arrive` each time it comes.
     */
    static final class FloorSensor extends Room {
        Shaft s;
        mods.eln.sixnode.busdevice.FloorSensorElement sensor;
        mods.eln.bus.BusPort pc;
        boolean atStart, away = true;
        int heard;

        public String id() {
            return "servo_elevator_floor_sensor";
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            sensor = (mods.eln.sixnode.busdevice.FloorSensorElement) BusDeviceCases.device(ctx, BusDeviceCases.FLOOR_SENSOR, ctx.at(8, 1, 2), Direction.XP);
            sensor.setAddress(60);
            pc = new mods.eln.bus.BusPort();
            pc.listen(true);
            mods.eln.bus.BusNet.link(pc, sensor.port);
            watch(dt -> {
                ++tick;
                for (mods.eln.bus.Packet k; (k = pc.poll()) != null; ) if (k.type() == mods.eln.bus.PacketType.ARRIVE && k.source() == 60) heard++;
                if (tick == 10) atStart = sensor.level();
                if (tick == 12) s.call(ctx, 3);
                if (tick == 120) {
                    away = sensor.level();
                    s.call(ctx, 0);
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            mods.eln.bus.BusNet.detach(List.of(pc));
            ctx.check("servo floor sensor: level with the standing car", atStart, "");
            ctx.check("servo floor sensor: not while the car is at 3", !away, "");
            ctx.check("servo floor sensor: level again when it's back", sensor.level(), "");
            ctx.check("servo floor sensor: arrive sent each time it came (twice)", heard == 2 && sensor.arrivals == 2, heard + " " + sensor.arrivals);
        }
    }

    /**
     * Driven by its setpoint wire: a voxel signal cable beside its left flank, ending at the floor edge's middle pin,
     * fed 25 V (half the travel: 3 m) by a Signal Source beyond it. The car goes there.
     */
    static final class ByTheWire extends Room {
        Shaft s;

        public String id() {
            return "servo_elevator_follows_its_setpoint_wire";
        }

        public int ticks() {
            return 140;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            var d = s.drive(ctx);
            BlockPos beside = pinCable(ctx, s.head, d.front.left(), 0, mods.eln.node.voxel.CablePart.tierOf("Signal Cable"));
            var out = d.front.left();
            BusModuleCases.source(ctx, beside.relative(out.toEnumFacing()), out.getInverse(), 25);
        }

        public void measure(TestContext ctx) {
            var d = s.drive(ctx);
            ctx.check("servo wire: the setpoint pin is wired", d.machine.signalConnected(LinearServoDriveDescriptor.SETPOINT), "");
            Wp9cCases.within(ctx, "servo wire: it reads half its setpoint [0..1]", d.machine.signal(LinearServoDriveDescriptor.SETPOINT), 0.5, 0.01);
            ctx.check("servo wire: the car stands at half the travel", Math.abs(s.carFloor(ctx) - 3) < 1e-6 && d.status() == LinearServoDriveElement.IN_POSITION,
                s.carFloor(ctx) + " " + d.status());
        }
    }

    /**
     * A voxel cable of tier on the floor of the block beside the drive at head toward side, straight across it to the
     * drive's face and ending at the floor pin at leftward (voxels left of the middle, seen facing that face); its block.
     */
    static BlockPos pinCable(TestContext ctx, BlockPos head, mods.eln.misc.Direction side, int leftward, int tier) {
        var out = side.toEnumFacing();
        BlockPos at = head.relative(out);
        var fwd = out.getOpposite();   // (from the cable's block toward the drive)
        int lx = fwd.getStepZ(), lz = -fwd.getStepX();
        int lateral = 8 + leftward * (lx != 0 ? lx : lz);
        int[] pts = fwd.getAxis() == net.minecraft.core.Direction.Axis.X
            ? new int[]{0, 0, lateral, 16, 0, lateral} : new int[]{lateral, 0, 0, lateral, 0, 16};
        CableCases.voxel(ctx, at, List.of(new mods.eln.node.voxel.CablePart(tier, 0, pts)));
        return at;
    }

    /**
     * A weak line (a 33 ohm resistor in series) against a stiff one: going down empty, the car lifts its counterweight
     * (~1 kW at 2 m/s); the weak line sags as it speeds up, and the move is retargeted to the lower top speed mid-way.
     * The line gives at most ~300 W (at 100 V), so after a burst on the machine's buffer the car goes ~0.55 m/s (the
     * drive before the Machine API went ~1.3 m/s here, on energy it never drew) and takes ~12 s.
     */
    static final class Sag extends Room {
        Shaft stiff, weak;
        double maxStiff, maxWeak;
        int weakSegments;
        mods.eln.robotics.motion.Segment lastWeak;

        public String id() {
            return "servo_elevator_slower_on_a_sagging_line";
        }

        public int ticks() {
            return 440;
        }

        public void build(TestContext ctx) {
            stiff = new Shaft(ctx, ctx.at(2, 1, 4), 0);
            weak = new Shaft(ctx, ctx.at(8, 1, 4), 42);   // (42 core items: 33 ohm)
            watch(dt -> {
                tick++;
                if (tick == 10) {
                    stiff.call(ctx, 6);
                    weak.call(ctx, 6);
                }
                if (tick == 150) {
                    stiff.call(ctx, 0);
                    weak.call(ctx, 0);
                }
                if (tick > 150) {
                    var w = weak.drive(ctx);
                    maxStiff = Math.max(maxStiff, Math.abs(stiff.drive(ctx).speed));
                    maxWeak = Math.max(maxWeak, Math.abs(w.speed));
                    if (w.seg != lastWeak) {
                        lastWeak = w.seg;
                        weakSegments++;
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            ctx.line(String.format("servo sag: top speed down %.2f m/s stiff, %.2f m/s weak; %d weak segments; weak %s", maxStiff, maxWeak,
                weakSegments, weak.drive(ctx).getWaila()));
            ctx.check("servo: on a stiff 200 V line it goes 2 m/s", Math.abs(maxStiff - 2.0) < 0.05, "" + maxStiff);
            ctx.check("servo: on a sagging line it goes slower", maxWeak < 0.8 * maxStiff && maxWeak > 0.5, maxWeak + " vs " + maxStiff);
            ctx.check("servo: ... slowing mid-move (the sag retargets it)", weakSegments >= 2, weakSegments + " segments");
            ctx.check("servo: both arrive at the bottom", Math.abs(stiff.carFloor(ctx)) < 1e-6 && Math.abs(weak.carFloor(ctx)) < 1e-6,
                stiff.carFloor(ctx) + " " + weak.carFloor(ctx));
        }
    }

    /** A block on the car's way up stops it dead below the block, with the fault output on; a new call after clearing goes on. */
    static final class Obstruction extends Room {
        Shaft s;
        BlockPos stone;
        boolean faulted;
        double heldAt;

        public String id() {
            return "servo_elevator_stops_at_an_obstruction";
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            stone = s.head.above(7).relative(s.front.toEnumFacing());   // in the shaft, where the roof would be 2 m before the top
            ctx.level().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
            watch(dt -> {
                tick++;
                if (tick == 10) s.call(ctx, 6);
                if (tick == 120) {
                    var d = s.drive(ctx);
                    faulted = d.fault == LinearServoDriveElement.OBSTRUCTED && s.faultVolts(ctx) > 40 && d.status() == LinearServoDriveElement.FAULT;
                    heldAt = s.carFloor(ctx);
                    ctx.level().removeBlock(stone, false);
                    s.call(ctx, 6);
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            ctx.line("servo obstruction: held at " + heldAt + ", then " + s.drive(ctx).getWaila());
            ctx.check("servo: a block in the way faults it (fault output, status)", faulted, "");
            ctx.check("servo: ... and it stopped short of the block (the roof under it)", heldAt > 2 && heldAt + ElevatorCar.CABIN <= 7 + 1e-6, "" + heldAt);
            ctx.check("servo: cleared and called again, it goes on to the top", Math.abs(s.carFloor(ctx) - 6) < 1e-6
                && s.drive(ctx).fault == LinearServoDriveElement.NO_FAULT, "" + s.carFloor(ctx));
        }
    }

    /**
     * Saved and loaded mid-move, the drive takes up its motion where it was and adopts its car (no second one); its
     * machine comes back with its buffer and runs on.
     */
    static final class SaveMidMove extends Room {
        Shaft s;
        double before = Double.NaN, after = Double.NaN, bufBefore = Double.NaN, bufSaved = Double.NaN, inAfter = Double.NaN, minSpeed = 9;
        java.util.UUID carBefore;
        String err = "", states = "";

        public String id() {
            return "servo_elevator_reloads_mid_move";
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                tick++;
                if (tick == 10) s.call(ctx, 6);
                if (tick == 110) s.call(ctx, 0);   // (down empty: it lifts the counterweight, so it draws on its buffer)
                if (tick == 150) {
                    var d = s.drive(ctx);
                    before = d.seg.position(d.clock - d.seg.startTick());
                    carBefore = d.carId;
                    bufBefore = s.core(ctx).bufferJoules();
                    try {
                        bufSaved = find(Reload.node(ctx.level(), s.head).before, "api.machine.bufferJ");
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        err = e.toString();
                    }
                    var f = s.drive(ctx);
                    after = f.seg.position(f.clock - f.seg.startTick());
                }
                if (tick == 151) inAfter = s.core(ctx).joulesDelivered();   // (the restored buffer is booked as delivered)
                if (tick > 150 && tick <= 170) {
                    var d = s.drive(ctx);
                    states += d.machine.state().name().charAt(0);
                    if (tick > 151) minSpeed = Math.min(minSpeed, Math.abs(d.speed));   // (at 151 this watch may run before its first step)
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            var d = s.drive(ctx);
            ctx.line("servo reload: " + before + " -> " + after + "; now " + d.getWaila() + " " + err);
            ctx.check("servo: reloaded mid-move, the motion is where it was", before > 0.2 && before < 5.8 && Math.abs(after - before) < 1e-9,
                before + " -> " + after + " " + err);
            ctx.check("servo: ... it adopts its car and finishes the move", s.cars(ctx).size() == 1 && d.carId.equals(carBefore)
                && Math.abs(s.carFloor(ctx)) < 1e-6, s.cars(ctx).size() + " cars, floor " + s.carFloor(ctx));
            ctx.line("servo reload: machine buffer " + bufBefore + " J saved as " + bufSaved + ", booked in " + inAfter
                + "; states after " + states + ", slowest " + minSpeed + " m/s");
            ctx.check("servo: ... its machine's buffer is saved and loaded", bufBefore > 0 && bufSaved == bufBefore && inAfter >= bufBefore,
                bufBefore + " " + bufSaved + " " + inAfter);
            ctx.check("servo: ... and runs on at once (no power-up hold: the car doesn't stop)", states.matches("R{20}") && minSpeed > 0.5,
                states + ", slowest " + minSpeed + " m/s");
        }

        /** The double at key anywhere in tag (the element's tag is nested in the node's). */
        static double find(net.minecraft.nbt.CompoundTag tag, String key) {
            if (tag.contains(key)) return tag.getDoubleOr(key, Double.NaN);
            for (String k : tag.keySet())
                if (tag.get(k) instanceof net.minecraft.nbt.CompoundTag c && !Double.isNaN(find(c, key))) return find(c, key);
            return Double.NaN;
        }
    }

    /** The floor ghost: under the car while it stands, gone while it moves, back where it stops. */
    static final class GhostFloor extends Room {
        Shaft s;
        boolean atStart, whileMoving = true, airWhileMoving;

        public String id() {
            return "servo_elevator_floor_ghost";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                tick++;
                var d = s.drive(ctx);
                if (tick == 9) atStart = d.ghostAt(0);
                if (tick == 10) s.call(ctx, 6);
                if (tick == 40) {
                    whileMoving = d.ghostAt(0) || d.ghostAt(6);
                    airWhileMoving = ctx.level().getBlockState(s.head.relative(s.front.toEnumFacing(), 2)).isAir();
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            var d = s.drive(ctx);
            ctx.check("servo ghost: there under the standing car", atStart, "");
            ctx.check("servo ghost: gone while it moves", !whileMoving && airWhileMoving, whileMoving + " " + airWhileMoving);
            ctx.check("servo ghost: back where it stops", d.ghostAt(6) && !d.ghostAt(0), "");
        }
    }

    /**
     * The cabin's walls hold (Baughn 2026-11-05: a 3x3x3 cabin): a pig in the middle of the plate, pushed 3 m toward
     * each wall and the roof (its own move, as walking or jumping would), stays inside, against the wall; toward the
     * open front it walks out.
     */
    static final class CabinWalls extends Room {
        Shaft s;
        final List<String> results = new java.util.ArrayList<>();
        boolean held = true, outFront;

        public String id() {
            return "servo_elevator_cabin_walls_hold";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                if (++tick != 20) return;
                ElevatorCar c = s.car(ctx);
                if (c == null) {
                    held = false;
                    results.add("no car");
                    return;
                }
                AABB cabin = c.cabinBox(0);
                net.minecraft.core.Direction rail = c.railSide, across = rail.getClockWise();
                Vec3[] pushes = {Vec3.atLowerCornerOf(rail.getUnitVec3i()).scale(3), Vec3.atLowerCornerOf(across.getUnitVec3i()).scale(3),
                    Vec3.atLowerCornerOf(across.getOpposite().getUnitVec3i()).scale(3), new Vec3(0, 3, 0)};
                for (Vec3 push : pushes) {
                    Pig pig = pig(ctx, c);
                    pig.move(MoverType.SELF, push);
                    boolean in = cabin.contains(pig.getBoundingBox().getCenter()) && pig.getBoundingBox().maxY <= cabin.maxY;
                    results.add(push + " -> " + pig.position());
                    held &= in;
                    pig.discard();
                }
                Pig pig = pig(ctx, c);
                pig.move(MoverType.SELF, Vec3.atLowerCornerOf(rail.getOpposite().getUnitVec3i()).scale(3));
                outFront = !cabin.contains(pig.getBoundingBox().getCenter());
                results.add("front -> " + pig.position());
                pig.discard();
            });
        }

        static Pig pig(TestContext ctx, ElevatorCar c) {
            Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(ctx.level(), EntitySpawnReason.TRIGGERED);
            pig.setNoAi(true);
            pig.setNoGravity(true);
            Vec3 at = c.at(0);
            pig.snapTo(at.x, at.y + ElevatorCar.THICK + 0.01, at.z, 0, 0);
            ctx.level().addFreshEntity(pig);
            return pig;
        }

        public void measure(TestContext ctx) {
            unwatch();
            ctx.line("servo cabin walls: " + results);
            ctx.check("servo cabin: a pig pushed at the back, side walls and roof stays inside", held, results.toString());
            ctx.check("servo cabin: ... and walks out of the open front", outFront, results.toString());
        }
    }

    /** A pig standing on the car goes up with it, still standing on it, and weighs on the drive. */
    static final class CarriesAMob extends Room {
        Shaft s;
        Pig pig;
        double kg;

        public String id() {
            return "servo_elevator_carries_a_mob";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                tick++;
                if (tick == 5) {
                    pig = net.minecraft.world.entity.EntityTypes.PIG.create(ctx.level(), EntitySpawnReason.TRIGGERED);
                    BlockPos c = s.head.relative(s.front.toEnumFacing());
                    pig.setNoAi(true);
                    pig.snapTo(c.getX() + 0.5, c.getY() + ElevatorCar.THICK, c.getZ() + 0.5, 0, 0);
                    ctx.level().addFreshEntity(pig);
                }
                if (tick == 15) s.call(ctx, 6);
                if (tick == 40) kg = s.drive(ctx).loadKg;
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            double feet = pig.getY() - s.head.getY();
            ctx.line("servo mob: pig's feet at " + feet + ", car floor " + s.carFloor(ctx) + ", load " + kg + " kg");
            ctx.check("servo: a pig on the car rides it to the top", Math.abs(s.carFloor(ctx) - 6) < 1e-6
                && Math.abs(feet - (6 + ElevatorCar.THICK)) < 0.05, "feet " + feet + ", floor " + s.carFloor(ctx));
            ctx.check("servo: ... and weighs on it", kg > 10, kg + " kg");
            pig.discard();
        }
    }

    /**
     * The car's panel (a part entity, clicked through its own id) has an id no other entity has, before or after the
     * entities spawned next; the client learns it from the spawn packet, not as the car's id + 1, so a click on the
     * panel reaches the panel whatever id the server's counter gave it.
     */
    static final class PanelId extends Room {
        Shaft s;
        final List<Entity> spawned = new java.util.ArrayList<>();

        public String id() {
            return "servo_elevator_panel_id";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                if (++tick != 20 || s.car(ctx) == null) return;
                for (int i = 0; i < 3; i++) {   // (the entities made right after the car)
                    Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(ctx.level(), EntitySpawnReason.TRIGGERED);
                    pig.setNoAi(true);
                    pig.snapTo(s.head.getX() + 3.5, s.head.getY(), s.head.getZ() + 0.5 + i, 0, 0);
                    ctx.level().addFreshEntity(pig);
                    spawned.add(pig);
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            var level = ctx.level();
            ElevatorCar car = s.car(ctx);
            ctx.check("servo panel: a car, and the pigs spawned after it", car != null && spawned.size() == 3, car + " " + spawned);
            if (car == null) return;
            int id = car.panel.getId();
            var others = new java.util.ArrayList<String>();
            for (Entity e : level.getAllEntities()) if (e.getId() == id) others.add(e.toString());
            ctx.line("servo panel: car " + car.getId() + ", panel " + id + ", pigs " + spawned.stream().map(Entity::getId).toList());
            ctx.check("servo panel: no entity has the panel's id", others.isEmpty() && level.getEntity(id) == null
                && spawned.stream().noneMatch(e -> e.getId() == id), others.toString());
            ctx.check("servo panel: a click on its id reaches the panel", level.getEntityOrPart(id) == car.panel, String.valueOf(level.getEntityOrPart(id)));
            // the client's car, made from the spawn packet, numbers its panel as the server's: also when that isn't car + 1
            var packet = (net.minecraft.network.protocol.game.ClientboundAddEntityPacket) car.getAddEntityPacket(serverEntity(level, car));
            ElevatorCar client = new ElevatorCar(Robotics.ELEVATOR_CAR.get(), level);
            client.recreateFromPacket(packet);
            ElevatorCar gap = new ElevatorCar(Robotics.ELEVATOR_CAR.get(), level);
            gap.panel.setId(gap.getId() + 7);   // (as if another thread took the ids between the car and its panel)
            ElevatorCar gapClient = new ElevatorCar(Robotics.ELEVATOR_CAR.get(), level);
            gapClient.recreateFromPacket((net.minecraft.network.protocol.game.ClientboundAddEntityPacket) gap.getAddEntityPacket(serverEntity(level, gap)));
            ctx.check("servo panel: the spawn packet gives the client the panel's id", client.getId() == car.getId()
                && client.panel.getId() == id && gapClient.panel.getId() == gap.getId() + 7,
                client.getId() + "/" + client.panel.getId() + " for " + car.getId() + "/" + id + "; gap " + gapClient.panel.getId());
            for (Entity e : spawned) e.discard();
        }

        static net.minecraft.server.level.ServerEntity serverEntity(net.minecraft.server.level.ServerLevel level, Entity e) {
            return new net.minecraft.server.level.ServerEntity(level, e, net.minecraft.world.entity.UpdateInterval.NEVER, false,
                new net.minecraft.server.level.ServerEntity.Synchronizer() {
                    public void sendToTrackingPlayers(net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> p) {
                    }

                    public void sendToTrackingPlayersAndSelf(net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> p) {
                    }

                    public void sendToTrackingPlayersFiltered(net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> p,
                                                              java.util.function.Predicate<net.minecraft.server.level.ServerPlayer> f) {
                    }
                });
        }
    }

    /**
     * A starved supply (a 47 ohm line: at most ~210 W reach the drive) against a stiff one: going down empty, the car
     * lifts its counterweight, which needs ~1 kW at full speed. The drive gets what the line can give and goes only as
     * far as that pays for, so it is much slower, still arrives, and never did the work without paying for it.
     */
    static final class Starved extends Room {
        Shaft stiff, weak;
        double maxStiff, maxWeak, takenAtTop = Double.NaN, takenDown;
        int weakSegments, notRunning, stiffDown, weakDown;
        mods.eln.robotics.motion.Segment lastWeak;
        boolean called;

        public String id() {
            return "servo_elevator_starved_supply_slower_still_arrives";
        }

        public int ticks() {
            return 520;
        }

        public void build(TestContext ctx) {
            stiff = new Shaft(ctx, ctx.at(2, 1, 4), 0);
            weak = new Shaft(ctx, ctx.at(8, 1, 4), 44);   // (44 core items: 47 ohm)
            watch(dt -> {
                tick++;
                if (tick == 10) {
                    stiff.call(ctx, 6);
                    weak.call(ctx, 6);
                }
                if (tick == 130) {   // (both at the top: going up empty costs nothing, so the weak one is as fast)
                    takenAtTop = weak.core(ctx).joulesTaken();
                    stiff.call(ctx, 0);
                    weak.call(ctx, 0);
                    called = true;
                }
                if (called) {
                    var w = weak.drive(ctx);
                    maxStiff = Math.max(maxStiff, Math.abs(stiff.drive(ctx).speed));
                    maxWeak = Math.max(maxWeak, Math.abs(w.speed));
                    if (w.machine.state() != MachineState.RUNNING || w.stalled) notRunning++;
                    if (w.seg != lastWeak) {
                        lastWeak = w.seg;
                        weakSegments++;
                    }
                    takenDown = weak.core(ctx).joulesTaken() - takenAtTop;
                    if (stiffDown == 0 && stiff.drive(ctx).status() == LinearServoDriveElement.IN_POSITION) stiffDown = tick - 130;
                    if (weakDown == 0 && w.status() == LinearServoDriveElement.IN_POSITION) weakDown = tick - 130;
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            double down = TRAVEL - weak.carFloor(ctx);   // (how far it got: all the way, if it arrived)
            double work = mods.eln.robotics.motion.Drive.G * LinearServoDriveDescriptor.counterweightKg * down
                / LinearServoDriveDescriptor.drive.efficiency();
            double meanStiff = TRAVEL / (stiffDown / 20.0), meanWeak = TRAVEL / (weakDown / 20.0);
            ctx.line(String.format("servo starved: down in %d ticks stiff (%.2f m/s on average), %d starved (%.2f m/s);", stiffDown, meanStiff,
                weakDown, meanWeak) + String.format(" top speed down %.2f m/s stiff, %.2f m/s starved; %d segments, %d ticks not running;"
                + " taken %.0f J for %.0f J of work over the efficiency; starved %s", maxStiff, maxWeak, weakSegments, notRunning,
                takenDown, work, weak.drive(ctx).getWaila()));
            ctx.check("servo starved: on a stiff line it goes 2 m/s", Math.abs(maxStiff - 2.0) < 0.05, "" + maxStiff);
            // (its first second may be faster: the machine's buffer pays for it)
            ctx.check("servo starved: on a starved line it is much slower", weakDown > 0 && meanWeak < 0.5 * meanStiff && meanWeak > 0.1,
                meanWeak + " vs " + meanStiff + " m/s");
            ctx.check("servo starved: ... cut to what the energy pays for (new segments)", weakSegments >= 2, weakSegments + " segments");
            ctx.check("servo starved: ... it never did more work than it paid for", takenDown >= work * 0.99, takenDown + " J for " + work + " J");
            ctx.check("servo starved: ... it keeps running (no stop-and-go, never stalled)", notRunning == 0, notRunning + " ticks");
            ctx.check("servo starved: both arrive at the bottom", Math.abs(stiff.carFloor(ctx)) < 1e-6 && Math.abs(weak.carFloor(ctx)) < 1e-6,
                stiff.carFloor(ctx) + " " + weak.carFloor(ctx));
        }
    }

    /**
     * The energy books of a lift on a stiff line: going down empty, the car lifts its 40 kg counterweight 6 m; the drive
     * takes that work over its efficiency from the machine, and what the line delivered beyond that is its idle draw.
     */
    static final class EnergyBooks extends Room {
        Shaft s;
        double in0, out0, lost0, buf0, in1, out1, lost1, buf1;
        int t0, t1;

        public String id() {
            return "servo_elevator_energy_books";
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 4), 0);
            watch(dt -> {
                tick++;
                var c = s.core(ctx);
                if (tick == 10) s.call(ctx, 6);
                if (tick == 120) {
                    s.call(ctx, 0);
                    in0 = c.joulesDelivered();
                    out0 = c.joulesTaken();
                    lost0 = c.joulesLost();
                    buf0 = c.bufferJoules();
                    t0 = tick;
                }
                if (tick > 120 && t1 == 0 && s.drive(ctx).status() == LinearServoDriveElement.IN_POSITION) {
                    in1 = c.joulesDelivered();
                    out1 = c.joulesTaken();
                    lost1 = c.joulesLost();
                    buf1 = c.bufferJoules();
                    t1 = tick;
                }
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            double work = mods.eln.robotics.motion.Drive.G * LinearServoDriveDescriptor.counterweightKg * TRAVEL
                / LinearServoDriveDescriptor.drive.efficiency();
            double taken = out1 - out0, delivered = in1 - in0, lost = lost1 - lost0;
            double idle = LinearServoDriveDescriptor.drive.idleWatts() * (t1 - t0) * 0.05;
            ctx.line(String.format("servo books: %d ticks down; delivered %.0f J, taken %.0f J (work/eff %.0f J), lost %.0f J (idle %.0f J),"
                + " buffer %.0f -> %.0f J", t1 - t0, delivered, taken, work, lost, idle, buf0, buf1));
            ctx.check("servo books: it arrived", t1 > 0 && Math.abs(s.carFloor(ctx)) < 1e-6, t1 + " " + s.carFloor(ctx));
            ctx.check("servo books: taken = m g h / efficiency (+-2%)", Math.abs(taken - work) < 0.02 * work, taken + " vs " + work);
            ctx.check("servo books: delivered = taken + idle + what's left in the buffer (+-3% of the work)",
                Math.abs(delivered - taken - lost - (buf1 - buf0)) < 1e-6 * delivered + 1e-9 && Math.abs(lost - idle) < 0.03 * work,
                delivered + " = " + taken + " + " + lost + " + " + (buf1 - buf0) + "; idle " + idle);
        }
    }
}
