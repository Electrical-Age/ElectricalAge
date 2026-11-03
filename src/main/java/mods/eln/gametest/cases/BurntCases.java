package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.NodeMenus;
import mods.eln.sim.IProcess;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.Repair;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineElement;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * Burnt devices (Baughn: a damaged machine should tick, but should stop doing useful things; right-click only shows
 * the repair message):
 * - a burnt stone heat furnace, hot and fed, cools towards ambient and takes no fuel;
 * - a burnt, powered macerator processes nothing, has no GUI and its screen doesn't open on the server's use path;
 * - a macerator burnt, then repaired a second in, processes as usual.
 */
public final class BurntCases {
    private BurntCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FurnaceCase());
        cases.add(new MachineCase());
        cases.add(new RepairCase());
    }

    static final double HOT = 300;

    static final class FurnaceCase implements TestCase {
        HeatFurnaceElement burnt;

        public String name() {
            return "burnt heat furnace cools and burns nothing";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 200;
        }

        HeatFurnaceElement fed(TestContext ctx, int x) {
            HeatFurnaceElement f = (HeatFurnaceElement) ctx.placeTransparent(Wp10bCases.HEAT_FURNACE, ctx.at(x, 1, 1));
            f.getInventory().setItem(HeatFurnaceContainer.combustibleId, new ItemStack(Items.COAL, 16));
            f.setTakeFuel(true);
            f.furnaceProcess.setGain(1);
            f.thermalLoad.Tc = HOT;
            return f;
        }

        public void build(TestContext ctx) {
            burnt = fed(ctx, 1);
            burnt.burn(FailureCause.OVERHEAT, Failure.Kind.MACHINE);
        }

        public void measure(TestContext ctx) {
            int coalBurnt = burnt.getInventory().getItem(HeatFurnaceContainer.combustibleId).getCount();
            ctx.line(String.format("burnt furnace: %.1f -> %.1f K over ambient, coal %d", HOT, burnt.thermalLoad.Tc, coalBurnt));
            ctx.check("burnt heat furnace: cools towards ambient", burnt.thermalLoad.Tc < HOT * 0.75 && burnt.thermalLoad.Tc >= -1,
                String.format("T %.1f", burnt.thermalLoad.Tc));
            ctx.check("burnt heat furnace: takes no fuel", coalBurnt == 16, "coal " + coalBurnt);
            ctx.check("burnt heat furnace: no screen", !burnt.node.hasGui(Direction.XN), "");
        }
    }

    static ElectricalMachineElement fedMacerator(TestContext ctx, int width) {
        Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width);
        ElectricalMachineElement m = (ElectricalMachineElement) l.device(Wp10aCases.MACHINES[0], 2, 1);
        l.feed(ctx.at(2, 1, 1), m.front.left());
        m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT));
        m.getInventory().setChanged();
        return m;
    }

    static boolean processed(ElectricalMachineElement m) {
        ItemStack in = m.getInventory().getItem(m.inSlotId);
        ItemStack out = m.getInventory().getItem(m.outSlotId);
        return in.isEmpty() && out.getItem() == Eln.findItemStack("Copper Dust").getItem();
    }

    static final class MachineCase implements TestCase {
        ElectricalMachineElement macerator;

        public String name() {
            return "burnt macerator does nothing and has no screen";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 260;
        }

        public void build(TestContext ctx) {
            macerator = fedMacerator(ctx, width());
            macerator.burn(FailureCause.OVERVOLTAGE, Failure.Kind.MACHINE);
        }

        public void measure(TestContext ctx) {
            ItemStack in = macerator.getInventory().getItem(macerator.inSlotId);
            ctx.check("burnt macerator: the copper stays in", in.is(Items.COPPER_INGOT) && !processed(macerator),
                "in " + in + ", out " + macerator.getInventory().getItem(macerator.outSlotId));
            ctx.check("burnt macerator: no screen, no server menu", !macerator.node.hasGui(Direction.XN)
                && NodeMenus.menu(0, FakePlayerFactory.getMinecraft(ctx.level()), macerator.node, Direction.XN) == null, "");

            // the player's use path (MenusOpenCase opens a working one this way)
            FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Object[] opened = {null};
            NodeMenus.openedHook = (pl, m) -> opened[0] = m;
            try {
                BlockPos at = macerator.node.coordonate.pos();
                Vec3 hit = new Vec3(at.getX(), at.getY() + 0.5, at.getZ() + 0.5);
                fp.setPos(hit.x - 1.5, at.getY(), hit.z);
                var result = ctx.level().getBlockState(at).useWithoutItem(ctx.level(), fp,
                    new BlockHitResult(hit, net.minecraft.core.Direction.WEST, at, false));
                ctx.check("burnt macerator: a right-click opens no screen", opened[0] == null, "use gave " + result);
            } finally {
                NodeMenus.openedHook = null;
            }
        }
    }

    static final class RepairCase implements TestCase {
        ElectricalMachineElement macerator;
        /** Burnt for this many ticks, then repaired. */
        static final int BURNT_TICKS = 20;
        int tick;
        boolean idleWhileBurnt;
        final IProcess timer = dt -> {
            if (++tick != BURNT_TICKS) return;
            idleWhileBurnt = macerator.isBurnt() && macerator.getInventory().getItem(macerator.inSlotId).is(Items.COPPER_INGOT);
            macerator.repair();
        };

        public String name() {
            return "repaired macerator works again";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 280;
        }

        public void build(TestContext ctx) {
            macerator = fedMacerator(ctx, width());
            macerator.burn(FailureCause.OVERVOLTAGE, Failure.Kind.MACHINE);
            Eln.simulator.addSlowProcess(timer);
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(timer);
            ctx.check("macerator: idle while burnt", idleWhileBurnt, "tick " + tick);
            ctx.check("macerator: repaired, it processes the copper", !macerator.isBurnt() && processed(macerator),
                "in " + macerator.getInventory().getItem(macerator.inSlotId) + ", out " + macerator.getInventory().getItem(macerator.outSlotId));
            ctx.check("macerator: repaired, its screen is back", macerator.node.hasGui(Direction.XN), "");
        }
    }
}
