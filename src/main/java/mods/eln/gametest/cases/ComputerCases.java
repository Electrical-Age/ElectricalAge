package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.robotics.Robotics;
import mods.eln.robotics.computer.ComputerElement;
import mods.eln.robotics.computer.FloppyItem;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** The robotics computer in the world (robotics phase 3, Baughn 2026-11-05). */
public final class ComputerCases {
    private ComputerCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new BootsJanet());
        cases.add(new RunsTheElevator());
        cases.add(new PausesWithoutPower());
    }

    /** The source behind the last computer powered() placed. */
    static ElectricalSourceElement lastSource;

    static void volts(TestContext ctx, ElectricalSourceElement s, float v) {
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(v);
        }));
    }

    /** A computer at p with 50 V behind it (a low voltage cable, then a source). */
    static ComputerElement powered(TestContext ctx, BlockPos p) {
        var c = (ComputerElement) ShaftArtCases.placeLooking(ctx, Robotics.COMPUTER_ID, p, ShaftArtCases.SOUTH);
        if (c == null) throw new IllegalStateException("no computer at " + p);
        var back = c.front.back().toEnumFacing();
        ctx.placeSix(Wp10aCases.LV_CABLE, p.relative(back));
        ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(Wp11Cases.SOURCE, p.relative(back, 2));
        volts(ctx, s, 50f);
        lastSource = s;
        return c;
    }

    static ComputerElement at(TestContext ctx, BlockPos p) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, ctx.level()));
        return n instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof ComputerElement c ? c : null;
    }

    static String terminal(ComputerElement c) {
        return new String(c.scrollbackBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Janet's ROM in slot 1 and RAM in slots 2 and 3, powered: it boots to its REPL, answers what is typed, and draws
     * more while it works than asleep. A blank floppy in drive A is given a disk id.
     */
    static class BootsJanet implements TestCase {
        BlockPos p;
        String afterBoot = "", afterTyping = "";
        byte asleep = -1;
        boolean onBus;
        mods.eln.sim.IProcess watch;
        int tick;

        public String id() {
            return "computer_boots_janet_and_answers";
        }

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        public int floorDepth() {
            return 7;   // (floor under every row: the computer needs one)
        }

        public int ticks() {
            return 100;   // (Janet boots in about 26M instructions: 26 slices, longer while the JIT warms up)
        }

        public void build(TestContext ctx) {
            p = ctx.at(4, 1, 3);
            ComputerElement c = powered(ctx, p);
            c.getInventory().setItem(0, new ItemStack(Robotics.JANET_ROM.get()));
            c.getInventory().setItem(1, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(2, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(ComputerElement.DRIVE_A, new ItemStack(Robotics.FLOPPY_DISK.get()));
            c.getInventory().setChanged();
            // a bus kit from its bus pin (right flank, floor edge, right end), as a player lays one (AdventurAgent:
            // "No connector there" when the flank had nothing but the bus)
            ServoCases.kitCable(ctx, p, c.front.right(), -4, mods.eln.node.voxel.CablePart.BUS);
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                ComputerElement e = at(ctx, p);
                if (e == null) return;
                if (tick == 20) onBus = e.port.bus() != null && e.port.bus().portCount() > 1;
                if (tick == 60) {
                    afterBoot = terminal(e);
                    asleep = e.status();
                    e.networkUnserialize(ctx.stream(o -> {
                        o.writeByte(ComputerElement.TYPE_ID);
                        o.writeUTF("(* 6 7)\n");
                    }), null);
                }
                if (tick == 90) afterTyping = terminal(e);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            ComputerElement c = at(ctx, p);
            ctx.line("computer: " + c.getWaila() + " terminal " + afterTyping.replace("\n", "|"));
            ctx.check("computer: boots to Janet's REPL", afterBoot.contains("Janet 1.41.2 on Electrical Age") && afterBoot.endsWith("\n> "), afterBoot);
            ctx.check("computer: asleep at the prompt", asleep == ComputerElement.SLEEPING, "" + asleep);
            ctx.check("computer: answers what is typed", afterTyping.endsWith("> (* 6 7)\n42\n> "), afterTyping);
            ctx.check("computer: a bus kit ends at its bus pin and links it", onBus, "");
            ctx.check("computer: a blank floppy got a disk id", FloppyItem.disk(c.getInventory().getItem(ComputerElement.DRIVE_A)) != null, "");
        }
    }

    /**
     * The canned elevator floppy end to end: a computer with Janet, RAM and the Elevator disk, on the bus of a Linear
     * Servo Drive at address 40 (its settings: floors at 0, 4 and 8 m, car buttons at 50, 51, 52). The button for the
     * second floor (51, standing in for the car panel's) is pressed: its lamp lights, the car goes to 4 m, and the lamp
     * goes out when it arrives.
     */
    static final class RunsTheElevator extends ServoCases.Room {
        ServoCases.Shaft s;
        BlockPos cp;
        mods.eln.bus.BusPort button;
        final java.util.List<Float> lamp = new java.util.ArrayList<>();
        String terminal = "";
        double floorAtPress = -1;

        public String id() {
            return "computer_runs_the_elevator";
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            s = new ServoCases.Shaft(ctx, ctx.at(5, 1, 4), 0);
            s.drive(ctx).networkUnserialize(ctx.stream(o -> {
                o.writeByte(mods.eln.robotics.servo.LinearServoDriveElement.setAddressId);
                o.writeFloat(40);
            }));
            cp = ctx.at(1, 1, 4);
            ComputerElement c = powered(ctx, cp);
            c.getInventory().setItem(0, new ItemStack(Robotics.JANET_ROM.get()));
            c.getInventory().setItem(1, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(2, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(ComputerElement.DRIVE_A, new ItemStack(Robotics.ELEVATOR_DISK.get()));
            c.getInventory().setChanged();
            button = new mods.eln.bus.BusPort();
            button.setAddress(51);
            button.listen(true);
            watch(dt -> {
                ++tick;
                ComputerElement e = at(ctx, cp);
                var d = s.drive(ctx);
                if (e == null || d == null) return;
                // (one bus: the computer, the drive and the button, linked as a cable would)
                if (e.port.bus() == null || e.port.bus() != d.port.bus()) mods.eln.bus.BusNet.link(e.port, d.port);
                if (button.bus() == null || button.bus() != d.port.bus()) mods.eln.bus.BusNet.link(button, d.port);
                if (tick == 100) {
                    floorAtPress = s.carFloor(ctx);
                    button.send(new mods.eln.bus.Packet(mods.eln.bus.Packet.BROADCAST, 51, mods.eln.bus.PacketType.PRESS, 0, 0));
                }
                for (mods.eln.bus.Packet k; (k = button.poll()) != null; )
                    if (k.type() == mods.eln.bus.PacketType.POWER) lamp.add(k.value());
                terminal = terminal(e);
            });
        }

        public void measure(TestContext ctx) {
            unwatch();
            mods.eln.bus.BusNet.detach(java.util.List.of(button));
            ctx.line("elevator: terminal " + terminal.replace("\n", "|") + " lamp " + lamp + " car " + s.carFloor(ctx));
            ctx.check("elevator disk: it ran at boot", terminal.contains("Running A:/main.janet") && terminal.contains("Elevator: drive 40, 3 floors"), terminal);
            ctx.check("elevator disk: the car stood at the bottom before the press", Math.abs(floorAtPress) < 1e-6, "" + floorAtPress);
            ctx.check("elevator disk: the press brought the car to the second floor (4 m)", Math.abs(s.carFloor(ctx) - 4) < 1e-6, "" + s.carFloor(ctx));
            ctx.check("elevator disk: the button's lamp lit, then went out on arrival", lamp.size() >= 2 && lamp.contains(1f)
                && lamp.get(lamp.size() - 1) == 0f, lamp.toString());
            ctx.check("elevator disk: no alarms", !terminal.contains("ALARM"), terminal);
        }
    }

    /** Power cut and back (Brownout.STOP): the computer pauses with its RAM kept, and goes on where it was. */
    static final class PausesWithoutPower extends BootsJanet {
        ElectricalSourceElement source;
        byte cut = -1;
        String after = "";

        @Override
        public String id() {
            return "computer_pauses_without_power";
        }

        @Override
        public int ticks() {
            return 120;
        }

        @Override
        public void build(TestContext ctx) {
            p = ctx.at(4, 1, 3);
            ComputerElement c = powered(ctx, p);
            source = lastSource;
            c.getInventory().setItem(0, new ItemStack(Robotics.JANET_ROM.get()));
            c.getInventory().setItem(1, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(2, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setChanged();
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                ComputerElement e = at(ctx, p);
                if (e == null) return;
                if (tick == 40) e.networkUnserialize(ctx.stream(o -> {
                    o.writeByte(ComputerElement.TYPE_ID);
                    o.writeUTF("(def x 99)\n");
                }), null);
                if (tick == 50) volts(ctx, source, 0);
                if (tick == 65) cut = e.status();
                if (tick == 66) volts(ctx, source, 50);
                if (tick == 85) e.networkUnserialize(ctx.stream(o -> {
                    o.writeByte(ComputerElement.TYPE_ID);
                    o.writeUTF("(+ x 1)\n");
                }), null);
                if (tick == 110) after = terminal(e);
            });
        }

        @Override
        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            ctx.check("computer power: off while the line is dead", cut == ComputerElement.OFF, "" + cut);
            ctx.check("computer power: goes on where it was (x still 99)", after.endsWith("> (+ x 1)\n100\n> "), after);
        }
    }
}
