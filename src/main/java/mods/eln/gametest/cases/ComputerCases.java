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
    }

    /** A computer at p with 50 V behind it (a low voltage cable, then a source). */
    static ComputerElement powered(TestContext ctx, BlockPos p) {
        var c = (ComputerElement) ShaftArtCases.placeLooking(ctx, Robotics.COMPUTER_ID, p, ShaftArtCases.SOUTH);
        if (c == null) throw new IllegalStateException("no computer at " + p);
        var back = c.front.back().toEnumFacing();
        ctx.placeSix(Wp10aCases.LV_CABLE, p.relative(back));
        ElectricalSourceElement s = (ElectricalSourceElement) ctx.placeSix(Wp11Cases.SOURCE, p.relative(back, 2));
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(50f);
        }));
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
    static final class BootsJanet implements TestCase {
        BlockPos p;
        String afterBoot = "", afterTyping = "";
        byte asleep = -1;
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
            return 80;
        }

        public void build(TestContext ctx) {
            p = ctx.at(4, 1, 3);
            ComputerElement c = powered(ctx, p);
            c.getInventory().setItem(0, new ItemStack(Robotics.JANET_ROM.get()));
            c.getInventory().setItem(1, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(2, new ItemStack(Robotics.RAM_CHIP.get()));
            c.getInventory().setItem(ComputerElement.DRIVE_A, new ItemStack(Robotics.FLOPPY_DISK.get()));
            c.getInventory().setChanged();
            mods.eln.Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                ComputerElement e = at(ctx, p);
                if (e == null) return;
                if (tick == 40) {
                    afterBoot = terminal(e);
                    asleep = e.status();
                    e.networkUnserialize(ctx.stream(o -> {
                        o.writeByte(ComputerElement.TYPE_ID);
                        o.writeUTF("(* 6 7)\n");
                    }), null);
                }
                if (tick == 70) afterTyping = terminal(e);
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(watch);
            ComputerElement c = at(ctx, p);
            ctx.line("computer: " + c.getWaila() + " terminal " + afterTyping.replace("\n", "|"));
            ctx.check("computer: boots to Janet's REPL", afterBoot.contains("Janet 1.41.2 on Electrical Age") && afterBoot.contains("repl:1:>"), afterBoot);
            ctx.check("computer: asleep at the prompt", asleep == ComputerElement.SLEEPING, "" + asleep);
            ctx.check("computer: answers what is typed", afterTyping.contains("42") && afterTyping.contains("repl:2:>"), afterTyping);
            ctx.check("computer: a blank floppy got a disk id", FloppyItem.disk(c.getInventory().getItem(ComputerElement.DRIVE_A)) != null, "");
        }
    }
}
