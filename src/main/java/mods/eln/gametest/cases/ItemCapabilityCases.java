package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.LabCommands;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.transparentnode.autominer.AutoMinerSlowProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

/**
 * Item capability of EA machines (hoppers, pipes; never worked in any EA version): each machine's own sided rules,
 * input slots only take what the machine can use, devices whose slots are upgrades expose nothing, burnt devices
 * expose nothing, an aborted transfer changes nothing, and the autominer drops into a capability-only inventory.
 */
public final class ItemCapabilityCases {
    private ItemCapabilityCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new MachineCase());
        cases.add(new MinerDropCase());
    }

    static ResourceHandler<ItemResource> cap(TestContext ctx, BlockPos p, Direction side) {
        return ctx.level().getCapability(Capabilities.Item.BLOCK, p, side);
    }

    static int insert(ResourceHandler<ItemResource> h, ItemStack s, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            int n = 0;
            for (int i = 0; i < h.size() && n < s.getCount(); i++) n += h.insert(i, ItemResource.of(s), s.getCount() - n, tx);
            if (commit) tx.commit();
            return n;
        }
    }

    static int extract(ResourceHandler<ItemResource> h, ItemStack s) {
        try (Transaction tx = Transaction.openRoot()) {
            int n = 0;
            for (int i = 0; i < h.size() && n < s.getCount(); i++) n += h.extract(i, ItemResource.of(s), s.getCount() - n, tx);
            tx.commit();
            return n;
        }
    }

    static final class MachineCase implements TestCase {
        public String id() {
            return "item_capability";
        }

        public int width() {
            return 15;
        }

        public int ticks() {
            return 60;   // hopper: one item per 8 ticks
        }

        TransparentNodeElement macerator, incubator;
        BlockPos hopper;
        int in;

        public void build(TestContext ctx) {
            macerator = ctx.placeTransparent(LabCommands.transparentId("50V Macerator"), ctx.at(1, 1, 1));
            BlockPos m = macerator.node.coordonate.pos();
            in = ((mods.eln.transparentnode.electricalmachine.ElectricalMachineElement) macerator).inSlotId;
            var inv = macerator.getInventory();

            ResourceHandler<ItemResource> top = cap(ctx, m, Direction.UP), north = cap(ctx, m, Direction.NORTH);
            ctx.check("macerator: top exposes the input slot", top != null && top.size() == 1, top == null ? "null" : "size " + top.size());
            ctx.check("macerator: a side exposes the output slots", north != null && north.size() > 0, north == null ? "null" : "size " + north.size());
            ctx.check("macerator: no handler for the null side", cap(ctx, m, null) == null, "");
            if (top == null || north == null) return;

            ctx.check("macerator: a stick (no recipe) is refused at the top", insert(top, new ItemStack(Items.STICK), true) == 0
                && inv.getItem(in).isEmpty(), "input " + inv.getItem(in));
            ctx.check("macerator: nothing goes in through a side", insert(north, new ItemStack(Items.IRON_ORE), true) == 0, "");

            // aborted: inserted inside the transaction, gone after it closes without commit
            int during;
            try (Transaction tx = Transaction.openRoot()) {
                during = top.insert(0, ItemResource.of(Items.IRON_ORE), 3, tx);
            }
            ctx.check("macerator: an aborted insert leaves the input empty", during == 3 && inv.getItem(in).isEmpty(),
                "inserted " + during + " in the transaction, input now " + inv.getItem(in));

            // output: taken out through a side and the bottom, not the top
            ItemStack dust = Eln.findItemStack("Iron Dust", 2);
            inv.setItem(0, dust.copy());
            inv.setChanged();
            ctx.check("macerator: the output can't be taken from the top", extract(top, dust) == 0, "");
            int got = extract(north, dust.copyWithCount(1)) + extract(cap(ctx, m, Direction.DOWN), dust.copyWithCount(1));
            ctx.check("macerator: the output comes out of a side and the bottom", got == 2 && inv.getItem(0).isEmpty(),
                "extracted " + got + ", output now " + inv.getItem(0));

            // a real hopper on top: the stick stays, the ore goes in
            hopper = m.above();
            ctx.level().setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
            if (ctx.level().getBlockEntity(hopper) instanceof HopperBlockEntity h) {
                h.setItem(0, new ItemStack(Items.STICK));
                h.setItem(1, new ItemStack(Items.IRON_ORE, 3));
            }

            // devices whose slots are upgrades (cores, dielectric, casing) stay closed
            String[] closed = {"Power capacitor", "Power inductor", "DC-DC Converter"};
            for (int i = 0; i < closed.length; i++) {
                String name = closed[i];
                TransparentNodeElement e = ctx.placeTransparent(LabCommands.transparentId(name), ctx.at(4 + 3 * i, 1, 1));
                BlockPos p = e.node.coordonate.pos();
                StringBuilder open = new StringBuilder();
                for (Direction d : Direction.values()) if (cap(ctx, p, d) != null) open.append(d).append(' ');
                ctx.check(name + ": no item handler on any face", open.length() == 0, "open: " + open);
            }

            // egg incubator: eggs only
            incubator = ctx.placeTransparent(LabCommands.transparentId("50V Egg Incubator"), ctx.at(13, 1, 1));
            ResourceHandler<ItemResource> eggs = cap(ctx, incubator.node.coordonate.pos(), Direction.EAST);
            ctx.check("egg incubator: takes eggs, not sticks", eggs != null && insert(eggs, new ItemStack(Items.STICK), true) == 0
                && insert(eggs, new ItemStack(Items.EGG, 2), true) == 2, "");
            ctx.check("egg incubator: eggs can't be pulled back out", eggs != null && extract(eggs, new ItemStack(Items.EGG)) == 0, "");
        }

        public void measure(TestContext ctx) {
            var inv = macerator.getInventory();
            ctx.check("hopper: iron ore went into the macerator input", inv.getItem(in).is(Items.IRON_ORE) && inv.getItem(in).getCount() == 3,
                "input " + inv.getItem(in));
            ctx.check("hopper: the stick stayed in the hopper", ctx.level().getBlockEntity(hopper) instanceof HopperBlockEntity h
                && h.getItem(0).is(Items.STICK), "");

            // burnt: a cache held by a neighbour sees the handler go away, and come back on repair
            BlockPos m = macerator.node.coordonate.pos();
            BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> cache =
                BlockCapabilityCache.create(Capabilities.Item.BLOCK, ctx.level(), m, Direction.NORTH);
            boolean before = cache.getCapability() != null;
            macerator.burn(FailureCause.OVERVOLTAGE, Failure.Kind.MACHINE);
            StringBuilder open = new StringBuilder();
            for (Direction d : Direction.values()) if (cap(ctx, m, d) != null) open.append(d).append(' ');
            boolean cachedBurnt = cache.getCapability() == null;
            macerator.repair();
            ctx.check("burnt macerator: no item handler on any face", before && open.length() == 0 && cachedBurnt,
                "before " + before + ", open: " + open + ", cache cleared " + cachedBurnt);
            ctx.check("repaired macerator: the handler is back", cache.getCapability() != null, "");
        }
    }

    /** The autominer's drop target is a macerator: no vanilla Container, only the capability. */
    static final class MinerDropCase implements TestCase {
        public String id() {
            return "autominer_drop_capability";
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
            return 7;
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            TransparentNodeElement miner = ctx.placeTransparent(LabCommands.transparentId("Auto Miner"), ctx.at(4, 3, 3));
            Coordonate c = new Coordonate(1, -1, 0, ctx.level());
            c.applyTransformation(miner.front, miner.node.coordonate);
            BlockPos target = c.pos();
            if (ctx.level().getBlockState(target.below()).isAir()) ctx.floor(target.below());
            if (!ctx.level().getBlockState(target).isAir()) ctx.level().removeBlock(target, false);
            TransparentNodeElement m = ctx.placeTransparent(LabCommands.transparentId("50V Macerator"), target);
            int in = ((mods.eln.transparentnode.electricalmachine.ElectricalMachineElement) m).inSlotId;
            AutoMinerSlowProcess p = Batch1Cases.field(miner, "slowProcess");
            boolean stick = p.drop(new ItemStack(Items.STICK));
            boolean ore = p.drop(new ItemStack(Items.IRON_ORE, 5));
            ctx.check("autominer: drops ore into the macerator below (capability only)", ore
                && m.getInventory().getItem(in).is(Items.IRON_ORE) && m.getInventory().getItem(in).getCount() == 5,
                "macerator at " + m.node.coordonate.pos() + " (target " + target + "), input " + m.getInventory().getItem(in));
            ctx.check("autominer: a stick the macerator refuses stays queued", !stick, "");
        }
    }
}
