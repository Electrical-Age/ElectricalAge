package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Pipes phase 3 (notes/pipes/DESIGN.md): voxel nodes holding pipe parts, coupled into fluid networks. */
public final class PipeCases {
    private PipeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new BoilerPipeTurbine());
        cases.add(new TwoPipesOneBlock());
        cases.add(new RoutedOverAWall());
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
