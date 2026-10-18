package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Pipes phase 3 (notes/pipes/DESIGN.md): voxel nodes holding pipe parts, coupled into fluid networks. */
public final class PipeCases {
    private PipeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new BoilerPipeTurbine());
        cases.add(new TwoPipesOneBlock());
        cases.add(new RoutedOverAWall());
        cases.add(new JoinMidRun());
        cases.add(new KitCrafting());
        cases.add(new Burst());
        cases.add(new Valve());
        cases.add(new Adapter());
    }

    static VoxelNode node(TestContext ctx, BlockPos p) {
        return ctx.level().getBlockEntity(p) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
    }

    static void lay(TestContext ctx, BlockPos p, int tier, int... pts) {
        String err = PipeKitItem.place(ctx.level(), p, new PipePart(tier, pts), null);
        if (err != null) throw new IllegalStateException("pipe at " + p + ": " + err);
    }

    /**
     * Boiler and turbine side by side, an iron pipe from the boiler's top port up, across three blocks and down into the
     * turbine's inlet: the turbine runs off the boiler's steam (as the stacked plant, a little behind for the pipe).
     */
    static final class BoilerPipeTurbine implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        mods.eln.oil.ElectricHeaterElement load;
        mods.eln.sim.IProcess fire;
        BlockPos first;

        public String id() {
            return "pipe_boiler_to_turbine";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            load.front = mods.eln.misc.Direction.ZN;
            load.reconnect();
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            first = ctx.at(1, 2, 1);
            lay(ctx, first, 0, 8, 0, 8, 8, 8, 8, 16, 8, 8);           // up out of the boiler, then east
            lay(ctx, ctx.at(2, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(3, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(4, 2, 1), 0, 0, 8, 8, 8, 8, 8, 8, 0, 8);   // east in, then down into the turbine
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(2500 * t));
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.source.getEffectiveP();
            VoxelNode n = node(ctx, first);
            var net = mods.eln.fluid.FluidWorld.networkOf(boiler.vessel);
            ctx.line("pipe: boiler " + boiler.getWaila() + ", turbine " + turbine.getWaila() + ", network " + (net == null ? 0 : net.nodes.size()) + " nodes");
            // (the 4 pipe blocks are one run: one lumped node; with the boiler, the turbine and the valve's air: 4 nodes)
            ctx.check("pipe: boiler, the pipe run and turbine are one network", net != null && n != null && net == mods.eln.fluid.FluidWorld.networkOf(turbine.inlet)
                && net == mods.eln.fluid.FluidWorld.networkOf(((PipePart) n.parts.get(0)).fluid) && net.nodes.size() == 4, net == null ? "none" : net.nodes.size() + " nodes");
            ctx.check("pipe: the turbine runs off the piped steam", p > 500 && p <= 1050, String.format("%.0f W", p));
            ctx.check("pipe: steam is in the pipe", n != null && ((PipePart) n.parts.get(0)).fluid.mVapour() > 0, "");
        }
    }

    /**
     * The router: boiler and turbine with a stone wall between them; a route from the boiler's top port to the
     * turbine's inlet finds its way over the wall and is laid; the turbine runs.
     */
    static final class RoutedOverAWall implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        mods.eln.sim.IProcess fire;
        String err;
        int length;

        public String id() {
            return "pipe_routed_over_a_wall";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 1600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 4, 1);
            var load = (mods.eln.oil.ElectricHeaterElement) l.device(0 + (82 << 6), 5, 1);
            turbine.front = mods.eln.misc.Direction.XN;
            turbine.reconnect();
            load.front = mods.eln.misc.Direction.ZN;
            load.reconnect();
            for (int y = 1; y <= 2; y++) for (int z = 0; z <= 2; z++) ctx.level().setBlock(ctx.at(2, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            boiler.thermal.Tc = 99 - (mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius);
            boiler.fromTanks(8000, 0);
            boiler.showTanks();
            var level = ctx.level();
            var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            var b = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(4, 1, 1), net.minecraft.core.Direction.UP);
            if (a == null || b == null) {
                err = "ports not found: " + a + " " + b;
                return;
            }
            var r = mods.eln.node.voxel.PipeRouter.route(mods.eln.node.voxel.PipePlacer.space(level), 4, a, List.of(), b);
            if (r.error != null) {
                err = r.error;
                return;
            }
            length = r.length();
            err = mods.eln.node.voxel.PipePlacer.lay(level, 0, r.points, null);
            mods.eln.Eln.simulator.addSlowProcess(fire = t -> boiler.thermal.moveEnergyTo(2500 * t));
        }

        public void measure(TestContext ctx) {
            if (fire != null) mods.eln.Eln.simulator.removeSlowProcess(fire);
            double p = turbine.source.getEffectiveP();
            ctx.line("pipe: routed " + length / 16.0 + " blocks; boiler " + boiler.getWaila() + ", turbine " + turbine.getWaila());
            ctx.check("pipe: a route over the wall is found and laid", err == null, "" + err);
            ctx.check("pipe: it goes over the wall (longer than straight across)", length > 3 * 16, length + " voxels");
            ctx.check("pipe: the turbine runs off the routed pipe", p > 400 && p <= 1050, String.format("%.0f W", p));
        }
    }

    /**
     * Joining a run mid-way (Baughn): a straight run lies across the turbine's top (into its inlet from above, through
     * a pipe that then turns along x); a route from the boiler's top port joins that run in the middle of a block. The
     * run is cut there into two pieces, the three meet in a junction, and boiler, run and turbine are one network.
     */
    static final class JoinMidRun implements TestCase {
        mods.eln.oil.BoilerElement boiler;
        mods.eln.oil.SteamGeneratorElement turbine;
        String err;
        BlockPos joinBlock;
        int partsAtJoin;

        public String id() {
            return "pipe_join_mid_run";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            boiler = (mods.eln.oil.BoilerElement) l.device(8 + (82 << 6), 1, 1);
            turbine = (mods.eln.oil.SteamGeneratorElement) l.device(12 + (82 << 6), 5, 1);
            // the run: down into the turbine at x 5, coming along x from x 3 (open end inside block 3)
            lay(ctx, ctx.at(5, 2, 1), 0, 0, 8, 8, 8, 8, 8, 8, 0, 8);
            lay(ctx, ctx.at(4, 2, 1), 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, ctx.at(3, 2, 1), 0, 4, 8, 8, 16, 8, 8);
            joinBlock = ctx.at(4, 2, 1);
            var level = ctx.level();
            var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            long[] join = {joinBlock.getX() * 16L + 8, joinBlock.getY() * 16L + 8, joinBlock.getZ() * 16L + 8};
            err = mods.eln.node.voxel.PipePlacer.routeAndJoinForTests(level, 0, a, join);
            VoxelNode n = node(ctx, joinBlock);
            partsAtJoin = n == null ? 0 : n.parts.size();
        }

        public void measure(TestContext ctx) {
            var net = mods.eln.fluid.FluidWorld.networkOf(boiler.vessel);
            ctx.check("pipe: the route joins the run", err == null, "" + err);
            ctx.check("pipe: the run is cut in two at the junction, plus the new pipe: 3 pieces", partsAtJoin == 3, partsAtJoin + " pieces");
            ctx.check("pipe: boiler, run and turbine are one network", net != null && net == mods.eln.fluid.FluidWorld.networkOf(turbine.inlet), "");
        }
    }

    /** A pipe adapter: water put in through the fluid capability on its face goes into the run behind it, and comes
     *  back out the same way. */
    static final class Adapter implements TestCase {
        BlockPos at;
        int put, took;
        double inRunAfterPut = -1;

        public String id() {
            return "pipe_adapter_capability";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            at = ctx.at(2, 1, 1);
            ctx.level().setBlock(ctx.at(1, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            var item = new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get());
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ctx.at(1, 1, 1)),
                net.minecraft.core.Direction.EAST, ctx.at(1, 1, 1), false);
            item.useOn(new net.minecraft.world.item.context.UseOnContext(ctx.level(), null, net.minecraft.world.InteractionHand.MAIN_HAND, item, hit));
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.WEST);
            if (cap == null) return;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                put = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 500, tx);
                tx.commit();
            }
        }

        public void measure(TestContext ctx) {
            VoxelNode n = node(ctx, at);
            ctx.check("pipe: an adapter goes on the stone's face, with its stub", n != null && n.adapterOn(net.minecraft.core.Direction.WEST.get3DDataValue()) != null, "");
            if (n == null) return;
            PipePart stub = null;
            for (VoxelPart p : n.parts) if (p instanceof PipePart pp) stub = pp;
            double run = stub == null ? 0 : mods.eln.fluid.FluidWorld.nodeFor(stub.fluid).mWater / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
            ctx.check("pipe: 500 mB of water went in through the capability (the stub holds ~950)", put == 500, put + " mB");
            ctx.check("pipe: it is in the run (some evaporates into its air)", run > 495 && run < 505, run + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.WEST);
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                took = cap.extract(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 200, tx);
                tx.commit();
            }
            ctx.check("pipe: water comes back out through it", took == 200, took + " mB");
            var other = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, net.minecraft.core.Direction.UP);
            ctx.check("pipe: only on the adapter's face", other == null, "" + other);
        }
    }

    /** A valve between a pressurised run and an empty one: shut, it holds; open, the pressures even out. */
    static final class Valve implements TestCase {
        BlockPos a, b, c;
        String err;
        double heldHigh, heldLow;

        public String id() {
            return "pipe_valve";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 300;
        }

        public void build(TestContext ctx) {
            a = ctx.at(1, 1, 1);
            b = ctx.at(2, 1, 1);
            c = ctx.at(3, 1, 1);
            for (BlockPos p : new BlockPos[]{a, b, c}) lay(ctx, p, 0, 0, 8, 8, 16, 8, 8);
            err = node(ctx, b).addValve();
            if (err != null) return;
            for (VoxelPart p : node(ctx, b).parts) if (p instanceof mods.eln.node.voxel.ValvePart v) v.open = false;
            ((PipePart) node(ctx, a).parts.get(0)).fluid.fill(0, 5e5, 293.15);
            mods.eln.fluid.FluidWorld.changed(node(ctx, a));
        }

        public void measure(TestContext ctx) {
            ctx.check("pipe: a valve fits in a straight pipe", err == null, "" + err);
            if (err != null) return;
            double pa = ((PipePart) node(ctx, a).parts.get(0)).fluid.P(), pc = ((PipePart) node(ctx, c).parts.get(0)).fluid.P();
            ctx.check("pipe: shut, it holds (5 bar on one side, 1 on the other)", pa > 3e5 && pc < 1.5e5, String.format("%.2f / %.2f bar", pa / 1e5, pc / 1e5));
            var parts = node(ctx, b).parts;
            ctx.check("pipe: the pipe was cut in two around the valve", parts.size() == 3, parts.size() + " parts");
        }
    }

    /** An iron run pressed past its burst rating (40 bar) bursts: the weak piece goes, scorched, and the run vents to
     *  the air (Baughn: a burst vents; a player's break seals). Breaking the burst piece drops nothing. */
    static final class Burst implements TestCase {
        BlockPos a, b;

        public String id() {
            return "pipe_burst_vents";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            a = ctx.at(1, 1, 1);
            b = ctx.at(2, 1, 1);
            lay(ctx, a, 0, 0, 8, 8, 16, 8, 8);
            lay(ctx, b, 0, 0, 8, 8, 16, 8, 8);
            for (BlockPos p : new BlockPos[]{a, b}) {
                VoxelNode n = node(ctx, p);
                ((PipePart) n.parts.get(0)).fluid.fill(0, 50e5, 293.15);
                mods.eln.fluid.FluidWorld.changed(n);
            }
        }

        public void measure(TestContext ctx) {
            PipePart pa = (PipePart) node(ctx, a).parts.get(0), pb = (PipePart) node(ctx, b).parts.get(0);
            ctx.check("pipe: one piece of the run burst", pa.burst ^ pb.burst, pa.burst + " " + pb.burst);
            ctx.check("pipe: the run vented to the air", pa.fluid.P() < 2e5, String.format("%.2f bar", pa.fluid.P() / 1e5));
            VoxelNode n = node(ctx, pa.burst ? a : b);
            n.removePart(0, true);
            var items = ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(a).inflate(3));
            ctx.check("pipe: a burst piece drops nothing", items.isEmpty(), items.size() + " items");
        }
    }

    /** Pipe kits in the crafting grid: plates make a block of pipe; kits merge; a kit splits in halves. */
    static final class KitCrafting implements TestCase {
        public String id() {
            return "pipe_kit_crafting";
        }

        public int width() {
            return 2;
        }

        public int ticks() {
            return 1;
        }

        static ItemStack craft(TestContext ctx, int w, int h, ItemStack... items) {
            var in = net.minecraft.world.item.crafting.CraftingInput.of(w, h, java.util.Arrays.asList(items));
            var level = ctx.level();
            var r = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, level);
            return r.map(x -> x.value().assemble(in)).orElse(ItemStack.EMPTY);
        }

        static java.util.List<ItemStack> remaining(TestContext ctx, int w, int h, ItemStack... items) {
            var in = net.minecraft.world.item.crafting.CraftingInput.of(w, h, java.util.Arrays.asList(items));
            var level = ctx.level();
            var r = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, level);
            return r.map(x -> (java.util.List<ItemStack>) x.value().getRemainingItems(in)).orElse(java.util.List.of());
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ItemStack plate = mods.eln.Eln.findItemStack("Iron Plate");
            ItemStack k = craft(ctx, 2, 1, plate.copy(), plate.copy());
            ctx.check("pipe: 2 iron plates make an iron kit of one block (16 voxels)", k.getItem() == mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()
                && PipeKitItem.length(k) == 16, k + " " + PipeKitItem.length(k));
            ItemStack m = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(0, 40));
            ctx.check("pipe: two iron kits merge (16 + 40)", PipeKitItem.length(m) == 56, "" + PipeKitItem.length(m));
            ItemStack mix = craft(ctx, 2, 1, PipeKitItem.kit(0, 16), PipeKitItem.kit(1, 16));
            ctx.check("pipe: iron and alloy kits don't merge", mix.isEmpty() || !(mix.getItem() instanceof PipeKitItem), "" + mix);
            ItemStack half = craft(ctx, 1, 1, PipeKitItem.kit(1, 33));
            var rest = remaining(ctx, 1, 1, PipeKitItem.kit(1, 33));
            ItemStack half16 = craft(ctx, 3, 3, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, PipeKitItem.kit(0, 16),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            ctx.check("pipe: a one-block kit splits too (in a 3x3 grid: 8)", PipeKitItem.length(half16) == 8, "" + PipeKitItem.length(half16));
            ctx.check("pipe: a kit splits in halves (33: 16 out, 17 stays)", PipeKitItem.length(half) == 16 && rest.size() == 1 && PipeKitItem.length(rest.get(0)) == 17,
                PipeKitItem.length(half) + " / " + rest);
        }
    }

    /** Two pipes crossing one block at different heights; a third that would cut through one is refused; taking one out
     *  leaves the other and drops its length as a kit. */
    static final class TwoPipesOneBlock implements TestCase {
        BlockPos at;
        String refused;
        boolean emptyAfterFirst, emptyAfterSecond;
        int partsLeft, dropped;

        public String id() {
            return "pipe_two_in_one_block";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext ctx) {
            at = ctx.at(1, 1, 1);
            lay(ctx, at, 0, 0, 4, 8, 16, 4, 8);                       // along x, low
            lay(ctx, at, 1, 8, 12, 0, 8, 12, 16);                     // along z, high (alloy)
            refused = PipeKitItem.place(ctx.level(), at, new PipePart(0, new int[]{8, 4, 0, 8, 4, 16}), null);
            VoxelNode n = node(ctx, at);
            int low = n.partAt(0.5, 4 / 16.0, 0.5);
            emptyAfterFirst = n.removePart(low, true);
            partsLeft = n.parts.size();
        }

        public void measure(TestContext ctx) {
            var items = ctx.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2));
            for (var it : items) dropped += PipeKitItem.length(it.getItem());
            ctx.check("pipe: a crossing pipe at another height fits; one through the first is refused", refused != null, "" + refused);
            ctx.check("pipe: taking the low pipe out leaves the high one", !emptyAfterFirst && partsLeft == 1, partsLeft + " left");
            ctx.check("pipe: it drops as a kit of its length (16 voxels)", dropped == 16, dropped + " voxels dropped");
            for (var it : items) it.discard();
        }
    }
}
