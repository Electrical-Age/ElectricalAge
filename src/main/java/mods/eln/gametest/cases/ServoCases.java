package mods.eln.gametest.cases;

import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.robotics.Robotics;
import mods.eln.robotics.servo.ElevatorCar;
import mods.eln.robotics.servo.LinearServoDriveElement;
import mods.eln.robotics.servo.StationMarkerBlock;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * The servo layer's elevator (mods.eln.robotics; Baughn: the servo layer first, 200 V only, players ride the carriage):
 * a Linear Servo Drive with a six-block Servo Rail mast, stations at 0, 3 and 6, fed 200 V from the back. The drive is
 * placed looking south, so its front (the car's shaft) is north.
 */
public final class ServoCases {
    private ServoCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ToStation());
        cases.add(new Sag());
        cases.add(new Obstruction());
        cases.add(new SaveMidMove());
        cases.add(new GhostFloor());
        cases.add(new CarriesAMob());
    }

    static final int TRAVEL = 6;

    /** A drive at p with its mast, markers at the given stops, 200 V through a Power Resistor of `coal` core items (0: none). */
    static final class Shaft {
        LinearServoDriveElement d;
        BlockPos head;
        Direction front;

        Shaft(TestContext ctx, BlockPos p, int[] marks, int coal) {
            d = (LinearServoDriveElement) ShaftArtCases.placeLooking(ctx, Robotics.DRIVE_ID, p, ShaftArtCases.SOUTH);
            if (d == null) throw new IllegalStateException("no drive at " + p);
            head = d.node.coordonate.pos();
            front = d.front;
            for (int i = 1; i <= TRAVEL; i++) ctx.level().setBlockAndUpdate(head.above(i), Robotics.SERVO_RAIL.get().defaultBlockState());
            var left = front.left().toEnumFacing();
            for (int m : marks)
                ctx.level().setBlockAndUpdate(head.above(m).relative(left),
                    Robotics.STATION_MARKER.get().defaultBlockState().setValue(StationMarkerBlock.FACING, left));
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

        BlockPos marker(int m) {
            return head.above(m).relative(front.left().toEnumFacing());
        }

        /** A click on the marker of stop m. */
        void call(TestContext ctx, int m) {
            var level = ctx.level();
            var state = level.getBlockState(marker(m));
            Player p = FakePlayerFactory.getMinecraft(level);
            LinearServoDriveElement e = StationMarkerBlock.driveOf(level, marker(m), state);
            if (e == null) throw new IllegalStateException("the marker at " + m + " finds no drive");
            e.callTo(p, marker(m));
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
            return 10;
        }

        public int depth() {
            return 5;
        }

        public int floorDepth() {
            return 5;
        }

        void watch(IProcess p) {
            mods.eln.Eln.simulator.addSlowProcess(watch = p);
        }

        void unwatch() {
            if (watch != null) mods.eln.Eln.simulator.removeSlowProcess(watch);
        }
    }

    /** Called to its middle station, the car goes there and the drive says it's in position; a floor ghost holds it. */
    static final class ToStation extends Room {
        Shaft s;

        public String id() {
            return "servo_elevator_goes_to_a_station";
        }

        public int ticks() {
            return 140;   // 3 m: ~3.5 s
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 1), new int[]{0, 3, 6}, 0);
            watch(dt -> {
                if (++tick == 10) s.call(ctx, 3);
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            var d = s.drive(ctx);
            ctx.line("servo to a station: " + d.getWaila() + " stations " + java.util.Arrays.toString(d.stations) + " car floor " + s.carFloor(ctx));
            ctx.check("servo: it reads the mast (6 blocks) and the markers' stations", d.travel == TRAVEL
                && java.util.Arrays.equals(d.stations, new int[]{0, 3, 6}), d.travel + " " + java.util.Arrays.toString(d.stations));
            ctx.check("servo: one car, standing at the called station", s.cars(ctx).size() == 1 && Math.abs(s.carFloor(ctx) - 3) < 1e-6,
                s.cars(ctx).size() + " cars, floor " + s.carFloor(ctx));
            ctx.check("servo: in position (status and output)", d.status() == LinearServoDriveElement.IN_POSITION
                && d.inPosition.getU() > 40 && d.faultOut.getU() < 10, d.status() + " " + d.inPosition.getU() + " V");
            ctx.check("servo: a floor ghost under the stopped car", d.ghostAt(3), "");
        }
    }

    /**
     * A weak line (a 33 ohm resistor in series) against a stiff one: going down empty, the car lifts its counterweight
     * (~1 kW at 2 m/s); the weak line sags as it speeds up, and the move is retargeted to the lower top speed (~1.3 m/s)
     * mid-way.
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
            return 320;
        }

        public void build(TestContext ctx) {
            stiff = new Shaft(ctx, ctx.at(2, 1, 1), new int[]{0, 6}, 0);
            weak = new Shaft(ctx, ctx.at(8, 1, 1), new int[]{0, 6}, 42);   // (42 core items: 33 ohm)
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
            s = new Shaft(ctx, ctx.at(5, 1, 1), new int[]{0, 3, 6}, 0);
            stone = s.head.above(4).relative(s.front.toEnumFacing());   // in the shaft, a block below the top station
            ctx.level().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
            watch(dt -> {
                tick++;
                if (tick == 10) s.call(ctx, 6);
                if (tick == 120) {
                    var d = s.drive(ctx);
                    faulted = d.fault == LinearServoDriveElement.OBSTRUCTED && d.faultOut.getU() > 40 && d.status() == LinearServoDriveElement.FAULT;
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
            ctx.check("servo: ... and it stopped short of the block", heldAt > 2 && heldAt + ElevatorCar.THICK <= 4 + 1e-6, "" + heldAt);
            ctx.check("servo: cleared and called again, it goes on to the top", Math.abs(s.carFloor(ctx) - 6) < 1e-6
                && s.drive(ctx).fault == LinearServoDriveElement.NO_FAULT, "" + s.carFloor(ctx));
        }
    }

    /** Saved and loaded mid-move, the drive takes up its motion where it was and adopts its car (no second one). */
    static final class SaveMidMove extends Room {
        Shaft s;
        double before = Double.NaN, after = Double.NaN;
        java.util.UUID carBefore;
        String err = "";

        public String id() {
            return "servo_elevator_reloads_mid_move";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            s = new Shaft(ctx, ctx.at(5, 1, 1), new int[]{0, 6}, 0);
            watch(dt -> {
                tick++;
                if (tick == 10) s.call(ctx, 6);
                if (tick == 50) {
                    var d = s.drive(ctx);
                    before = d.seg.position(d.clock - d.seg.startTick());
                    carBefore = d.carId;
                    try {
                        Reload.node(ctx.level(), s.head);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        err = e.toString();
                    }
                    var f = s.drive(ctx);
                    after = f.seg.position(f.clock - f.seg.startTick());
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
                && Math.abs(s.carFloor(ctx) - 6) < 1e-6, s.cars(ctx).size() + " cars, floor " + s.carFloor(ctx));
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
            s = new Shaft(ctx, ctx.at(5, 1, 1), new int[]{0, 6}, 0);
            watch(dt -> {
                tick++;
                var d = s.drive(ctx);
                if (tick == 9) atStart = d.ghostAt(0);
                if (tick == 10) s.call(ctx, 6);
                if (tick == 40) {
                    whileMoving = d.ghostAt(0) || d.ghostAt(6);
                    airWhileMoving = ctx.level().getBlockState(s.head.relative(s.front.toEnumFacing())).isAir();
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
            s = new Shaft(ctx, ctx.at(5, 1, 1), new int[]{0, 6}, 0);
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
}
