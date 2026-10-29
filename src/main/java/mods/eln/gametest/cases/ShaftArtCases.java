package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** The shaft redo's art pass (Baughn 2026-10-29): hitboxes that follow each model and turn with the block's front. */
public final class ShaftArtCases {
    private ShaftArtCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new HitboxPerFacing());
        cases.add(new PortsMeetWhenPlaced());
    }

    static final int HP = 9 + (4 << 6), GEN4 = 10 + (4 << 6), GAS = 11 + (4 << 6), JOINT = 12 + (4 << 6), HUB = 13 + (4 << 6),
        FLYWHEEL = 14 + (4 << 6), TACHO = 15 + (4 << 6), GEN8 = 16 + (4 << 6), MOTOR = 17 + (4 << 6), LP = 18 + (4 << 6),
        PLANT_CONDENSER = 52 + (82 << 6);

    /**
     * Every shaft part's hitbox, for each of the four fronts: the model's (not the full cell), with its shaft at both
     * shaft faces (left and right of its front); the turbines' inlet on top and exhaust at the back; the plant
     * condenser's exhaust inlets on top, at its back and on its right.
     */
    static final class HitboxPerFacing implements TestCase {
        final List<String> bad = new ArrayList<>();
        int checked;

        public String id() {
            return "shaft_hitbox_per_facing";
        }

        public int ticks() {
            return 2;
        }

        static boolean has(VoxelShape s, double x, double y, double z) {
            for (var b : s.toAabbs()) if (b.contains(new Vec3(x, y, z))) return true;
            return false;
        }

        /** The point 0.47 of a block from the centre toward D, at height Y. */
        static boolean toward(VoxelShape s, Direction d, double y) {
            var f = d.toEnumFacing();
            return has(s, 0.5 + 0.47 * f.getStepX(), y, 0.5 + 0.47 * f.getStepZ());
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 1);
            int[] ids = {HP, LP, GAS, GEN4, GEN8, MOTOR, JOINT, HUB, FLYWHEEL, TACHO, PLANT_CONDENSER};
            String[] names = {"HP turbine", "LP turbine", "gas turbine", "4 kW generator", "8 kW generator", "shaft motor", "joint", "joint hub",
                "flywheel", "tachometer", "plant condenser"};
            for (int i = 0; i < ids.length; i++)
                for (Direction f : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                    var e = ctx.placeTransparent(ids[i], p);
                    if (ids[i] != HUB) {   // (the hub has no front: it faces XP)
                        e.front = f;
                        e.reconnect();
                    }
                    Direction front = e.front;
                    VoxelShape s = ctx.level().getBlockState(p).getShape(ctx.level(), p);
                    String n = names[i] + " facing " + front;
                    var b = s.isEmpty() ? null : s.bounds();
                    if (b == null || (b.minX == 0 && b.minY == 0 && b.minZ == 0 && b.maxX == 1 && b.maxY == 1 && b.maxZ == 1 && s.toAabbs().size() == 1))
                        bad.add(n + ": not the model's hitbox");
                    else if (ids[i] == PLANT_CONDENSER) {
                        if (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5) || !toward(s, front.right(), 0.5))
                            bad.add(n + ": exhaust inlets (top, back, right)");
                    } else {
                        if (!toward(s, front.left(), 0.5) || !toward(s, front.right(), 0.5)) bad.add(n + ": shaft at its left and right");
                        if (ids[i] == HUB && (!toward(s, front, 0.5) || !toward(s, front.back(), 0.5))) bad.add(n + ": shaft on all four sides");
                        if ((ids[i] == HP || ids[i] == LP || ids[i] == GAS) && (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5)))
                            bad.add(n + ": inlet on top, exhaust at its back");
                    }
                    checked++;
                    ctx.level().removeBlock(p, false);
                }
        }

        public void measure(TestContext ctx) {
            ctx.check("shaft hitboxes: each model's, turned with its front (" + checked + " placements)", bad.isEmpty() && checked == 44, String.join("; ", bad));
        }
    }

    /** Place by the item path as a player looking toward YAW (degrees: 0 south, 90 west, 180 north, 270 east). */
    static mods.eln.node.transparent.TransparentNodeElement placeLooking(TestContext ctx, int damage, BlockPos p, float yaw) {
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(damage);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
        player.setYRot(yaw);
        player.setXRot(0);
        var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, ctx.level(), p, Direction.YN);
        return node == null ? null : node.element;
    }

    static boolean piped(mods.eln.sim.fluid.FluidNode n) {
        return n != null && !mods.eln.fluid.FluidWorld.edgesOf(n).isEmpty();
    }

    static final float EAST = 270, WEST = 90, NORTH = 180, SOUTH = 0;

    /**
     * AdventurAgent's client check (2026-10-29): an LP turbine and a plant condenser placed side by side by a player never
     * coupled. Placed by the item path with the player's view (no front set by the test): a shaft part's shaft runs
     * along the view; a turbine, gas turbine, tank, heat exchanger or plant condenser turns to meet the ports already
     * beside it. LP then condenser (the condenser in each of 4 facings) and condenser then LP; an HP's and a gas
     * turbine's exhaust and a large pipe, both orders; a tank and a pipe on its low spot; a heat exchanger and a pipe
     * at its end; a plant condenser between two cooling-water pipes.
     */
    static final class PortsMeetWhenPlaced extends PlumbingCases.Big {
        final List<String> bad = new ArrayList<>();
        final List<Runnable> checks = new ArrayList<>();

        public String id() {
            return "fluid_ports_meet_when_placed_by_a_player";
        }

        public int ticks() {
            return 10;
        }

        void expect(String what, java.util.function.BooleanSupplier ok) {
            checks.add(() -> {
                if (!ok.getAsBoolean()) bad.add(what);
            });
        }

        public void build(TestContext ctx) {
            // LP looking east (shaft east-west, exhaust south), then a condenser south of it, in each facing
            for (int k = 0; k < 4; k++) {
                BlockPos a = ctx.at(1 + 2 * k, 1, 0);
                var lp = (mods.eln.mechanical.ShaftSteamTurbineElement) placeLooking(ctx, LP, a, EAST);
                boolean along = lp.front == Direction.ZN || lp.front == Direction.ZP;
                expect("LP looking east: its shaft runs east-west (front " + lp.front + ")", () -> along);
                float yaw = 90 * k;
                placeLooking(ctx, PLANT_CONDENSER, a.south(), yaw);
                expect("LP then condenser (placed looking " + yaw + "): coupled", () -> piped(lp.exhaust));
            }
            // a condenser first, then the LP north of it looking west (its back would face north: it turns)
            for (int k = 0; k < 4; k++) {
                BlockPos a = ctx.at(1 + 2 * k, 1, 3);
                placeLooking(ctx, PLANT_CONDENSER, a.south(), 90 * k);
                var lp = (mods.eln.mechanical.ShaftSteamTurbineElement) placeLooking(ctx, LP, a, WEST);
                int kk = k;
                expect("condenser (placed looking " + 90 * kk + ") then LP: coupled", () -> piped(lp.exhaust));
            }
            // an HP and a gas turbine, then a large pipe at the back; a large pipe, then the turbine (it turns)
            int[] turbines = {HP, GAS};
            for (int t = 0; t < 2; t++) {
                BlockPos a = ctx.at(1 + 4 * t, 1, 6), b = ctx.at(3 + 4 * t, 1, 6);
                var e1 = placeLooking(ctx, turbines[t], a, EAST);
                PipeCases.lay(ctx, a.relative(e1.front.back().toEnumFacing()), 2, 8, 8, 0, 8, 8, 16);
                PipeCases.lay(ctx, b.south(), 2, 8, 8, 0, 8, 8, 16);
                var e2 = placeLooking(ctx, turbines[t], b, WEST);
                String n = t == 0 ? "HP" : "gas turbine";
                mods.eln.sim.fluid.FluidNode x1 = e1 instanceof mods.eln.mechanical.ShaftSteamTurbineElement s1 ? s1.exhaust : ((mods.eln.mechanical.GasTurbineElement) e1).exhaust;
                mods.eln.sim.fluid.FluidNode x2 = e2 instanceof mods.eln.mechanical.ShaftSteamTurbineElement s2 ? s2.exhaust : ((mods.eln.mechanical.GasTurbineElement) e2).exhaust;
                expect(n + " then a large pipe at its back: coupled", () -> piped(x1));
                expect("a large pipe, then the " + n + " (turned to it): coupled", () -> piped(x2));
            }
            // a tank looking at a pipe on its east (its front, the sight glass, would face it: it turns)
            PipeCases.lay(ctx, ctx.at(1, 1, 9), 1, 0, 4, 8, 16, 4, 8);
            var tank = (mods.eln.oil.TankElement) placeLooking(ctx, PlumbingCases.ALLOY_TANK, ctx.at(0, 1, 9), WEST);
            expect("a pipe, then a tank looking at it: coupled", () -> piped(tank.vessel));
            // a heat exchanger looking east (its ends east and west), a pipe at its east end's centre
            var hx = (mods.eln.oil.HeatExchangerElement) placeLooking(ctx, GasPlantCases.HX, ctx.at(4, 1, 9), EAST);
            PipeCases.lay(ctx, ctx.at(5, 1, 9), 2, 0, 8, 8, 16, 8, 8);
            expect("a heat exchanger, then a large pipe at its end: coupled", () -> piped(hx.a));
            // a plant condenser between two pipes on its low spots (north and south): its cooling ports take both
            PipeCases.lay(ctx, ctx.at(8, 1, 7), 1, 8, 4, 0, 8, 4, 16);
            PipeCases.lay(ctx, ctx.at(8, 1, 9), 1, 8, 4, 0, 8, 4, 16);
            var c = (mods.eln.oil.PlantCondenserElement) placeLooking(ctx, PLANT_CONDENSER, ctx.at(8, 1, 8), NORTH);
            expect("two pipes, then a plant condenser between them: both on its cooling-water ports",
                () -> mods.eln.fluid.FluidWorld.edgesOf(c.tubes).size() >= 2);
        }

        public void measure(TestContext ctx) {
            checks.forEach(Runnable::run);
            ctx.check("fluid ports meet when placed by a player (" + checks.size() + " checks)", bad.isEmpty(), String.join("; ", bad));
        }
    }
}
