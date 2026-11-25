package mods.eln.gametest.cases;

import mods.eln.fluid.FluidWorld;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.PipePart;
import mods.eln.oil.TankCart;
import mods.eln.oil.TankContents;
import mods.eln.oil.TankElement;
import mods.eln.oil.TankStationEntity;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.PlumbingCases.ALLOY_TANK;
import static mods.eln.gametest.cases.TankItemCases.differs;
import static mods.eln.gametest.cases.TankItemCases.plus;
import static mods.eln.gametest.cases.TankItemCases.snap;

/**
 * The Tank Minecart and the Tank Loading Station (Baughn 2026-11-25, todo #611): crafted only from an empty tank; a
 * docked cart filled and drained through the station's flange by pressure; it leaves with its contents exactly and docks
 * again; held until the station is powered; a docked and an undocked cart through a save and an unload; broken, it drops
 * a minecart and the tank with its contents; it warms in a hot ambient; over its burst pressure it bursts; a station
 * with no cart passes nothing. Fluid is never made or lost.
 */
public final class TankCartCases {
    private TankCartCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Craft());
        for (boolean alloy : new boolean[]{false, true}) cases.add(new Fill(alloy));
        cases.add(new Drain());
        cases.add(new LeavesAndRedocks());
        cases.add(new Rebase());
        cases.add(new HeldAndReleased());
        for (RoundTrip.Row r : RoundTrip.Row.values()) cases.add(new RoundTrip(r));
        for (boolean filled : new boolean[]{true, false}) cases.add(new Breaks(filled));
        cases.add(new HotAmbient());
        for (boolean explosions : new boolean[]{true, false}) cases.add(new Bursts(explosions));
        cases.add(new Jostled());
        cases.add(new Killed());
        cases.add(new TwoCarts());
        cases.add(new Inert());
    }

    /** A Tank Minecart standing on the rail at p. */
    static TankCart cart(TestContext ctx, BlockPos p, boolean alloy) {
        var type = alloy ? ElnRegistry.ALLOY_TANK_MINECART_ENTITY.get() : ElnRegistry.IRON_TANK_MINECART_ENTITY.get();
        TankCart c = type.create(ctx.level(), EntitySpawnReason.COMMAND);
        c.setInitialPos(p.getX() + 0.5, p.getY() + 0.0625, p.getZ() + 0.5);
        ctx.level().addFreshEntity(c);
        return c;
    }

    /** A Tank Loading Station at p along shape (its flanges across it). */
    static TankStationEntity station(TestContext ctx, BlockPos p, RailShape shape) {
        ctx.level().setBlock(p, ElnRegistry.TANK_STATION.get().defaultBlockState().setValue(PoweredRailBlock.SHAPE, shape), 3);
        return (TankStationEntity) ctx.level().getBlockEntity(p);
    }

    /**
     * The row every plumbing case here uses, along x at y 1, z 1: an alloy feed tank at x 1 (its east side's low port), a
     * pipe across x 2 at the low ports' height, a north-south station at x 3 (its west flange).
     */
    static final class Line {
        TankElement feed;
        PipePart pipe;
        TankStationEntity station;

        Line(TestContext ctx) {
            feed = PlumbingCases.tank(ctx, ALLOY_TANK, ctx.at(1, 1, 1), Direction.ZN);
            PipeCases.lay(ctx, ctx.at(2, 1, 1), 1, 0, TankElement.LOW, 8, 16, TankElement.LOW, 8);
            for (var part : ctx.voxelByEntity(ctx.at(2, 1, 1)).parts) if (part instanceof PipePart p) pipe = p;
            station = station(ctx, ctx.at(3, 1, 1), RailShape.NORTH_SOUTH);
        }

        /** The feed's and the pipe's contents (the pipe run's lump this step). */
        double[] rest() {
            return plus(snap(feed.vessel), snap(FluidWorld.nodeFor(pipe.fluid)));
        }
    }

    /** An item's tooltip lines under its name, as the game builds them. */
    static List<String> tooltip(TestContext ctx, ItemStack s) {
        var lines = s.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(ctx.level()), null, net.minecraft.world.item.TooltipFlag.NORMAL);
        List<String> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) out.add(lines.get(i).getString());
        return out;
    }

    /** Water and air (kg) of a snapshot, for a line. */
    static String masses(double[] s) {
        return String.format("%.4f kg water, %.4f kg air", s[0], s[1]);
    }

    /** The minecart recipes take a plain tank, not a filled one; the cart item's tooltips (plain, and with contents). */
    static final class Craft extends TankItemCases.Small {
        public String id() {
            return "tank_cart_crafted_only_from_an_empty_tank";
        }

        String what() {
            return "tank cart: crafted from a minecart and an empty tank only";
        }

        public void build(TestContext ctx) {
            var rm = ctx.level().getServer().getRecipeManager();
            for (boolean alloy : new boolean[]{false, true}) {
                String name = alloy ? "alloy_tank_minecart" : "iron_tank_minecart";
                var r = rm.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", name))).map(h -> h.value()).orElse(null);
                if (!(r instanceof net.minecraft.world.item.crafting.CraftingRecipe cr)) {
                    bad.append("no recipe ").append(name).append("; ");
                    continue;
                }
                var tier = (mods.eln.oil.TankDescriptor) mods.eln.Eln.transparentNodeItem.getDescriptor(alloy ? mods.eln.oil.TankDescriptor.ALLOY_ID
                    : mods.eln.oil.TankDescriptor.IRON_ID);
                ItemStack plain = tier.newItemStack();
                FluidNode n = new FluidNode(1, 0, 1, false);
                PlumbingCases.fill(n, 300, Water.P_ATM, 300);
                ItemStack filled = tier.dropStack(n);
                if (TankItemCases.contents(filled) == null) bad.append("the filled tank has no contents; ");
                var in = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.MINECART), plain));
                var inFilled = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.MINECART), filled));
                boolean withPlain = cr.matches(in, ctx.level()), withFilled = cr.matches(inFilled, ctx.level());
                ItemStack out = withPlain ? cr.assemble(in) : ItemStack.EMPTY;
                if (!withPlain || withFilled) bad.append(name).append(": plain ").append(withPlain).append(", filled ").append(withFilled).append("; ");
                if (!out.is(alloy ? ElnRegistry.ALLOY_TANK_MINECART.get() : ElnRegistry.IRON_TANK_MINECART.get())) bad.append(name).append(" makes ").append(out).append("; ");
                line += name + ": plain " + withPlain + ", filled " + withFilled + "; ";
                // (its tooltip: the tank's rating and where it fills; with contents only those and the pressure, as the tank item's)
                List<String> tip = tooltip(ctx, out);
                if (tip.stream().noneMatch(l -> l.startsWith("Holds "))) bad.append(name).append(" tooltip ").append(tip).append("; ");
                ItemStack withContents = out.copy();
                withContents.set(ElnRegistry.TANK_CONTENTS.get(), TankItemCases.contents(filled));
                List<String> tip2 = tooltip(ctx, withContents);
                if (tip2.size() != 2 || !tip2.get(0).startsWith("Contents: ") || !tip2.get(1).startsWith("Pressure: "))
                    bad.append(name).append(" tooltip with contents ").append(tip2).append("; ");
            }
        }
    }

    /**
     * A docked cart filled through the station: a feed tank 70 % full of water at 8 bar, piped to the station's flange,
     * fills an empty cart (iron or alloy) standing on it. The cart docks, takes water, the comparator reads its fill, and
     * the feed, the pipe and the cart hold the water and air they held together.
     */
    static final class Fill implements TestCase {
        final boolean alloy;
        Line l;
        TankCart c;
        double[] before;

        Fill(boolean alloy) {
            this.alloy = alloy;
        }

        public String id() {
            return "tank_cart_fills_at_station_" + (alloy ? "alloy" : "iron");
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            l = new Line(ctx);
            PlumbingCases.fill(l.feed.vessel, 0.7 * l.feed.vessel.volume * Water.RHO_L, 8e5, 293.15);
            c = cart(ctx, ctx.at(3, 1, 1), alloy);
            before = plus(l.rest(), snap(c.node()));
        }

        public void measure(TestContext ctx) {
            FluidNode n = c.node();
            n.update();
            double[] after = plus(l.rest(), snap(n));
            int sig = l.station.signal();
            double fill = n.liquidVolume() / n.volume;
            ctx.line(String.format("tank cart fill (%s): docked %s, cart %.1f kg water (%.0f %%), %.2f bar; comparator %d; total %s, was %s",
                alloy ? "alloy" : "iron", l.station.docked() == c, n.mWater, 100 * fill, n.P() / 1e5, sig, masses(after), masses(before)));
            ctx.check("tank cart: docked at the station", l.station.docked() == c && FluidWorld.devices().contains(l.station), "");
            ctx.check("tank cart: filled through the flange", fill > 0.2, String.format("%.2f kg, %.0f %%", n.mLiquid(), 100 * fill));
            ctx.check("tank cart: no fluid made or lost", differs(new double[]{before[0], before[1]}, new double[]{after[0], after[1]}, 1e-9).isEmpty(),
                masses(after) + " vs " + masses(before));
            var sw = l.station.waila();
            var cw = c.waila();
            ctx.check("tank cart: Jade on the station and the cart shows its tank's lines", sw.containsKey("Pressure") && sw.containsKey("Contents")
                && cw.containsKey("Pressure") && cw.containsKey("Contents") && cw.containsKey("Station"), sw + " / " + cw);
            int want = fill >= 0.9 ? 15 : 2 + (int) Math.min(12, Math.floor(fill / 0.9 * 13));
            ctx.check("tank cart: the comparator reads its fill", sig == want, sig + " vs " + want);
            c.discard();
        }
    }

    /** A docked cart 60 % full of water at 5 bar drains through the station into an empty feed tank, none lost. */
    static final class Drain implements TestCase {
        Line l;
        TankCart c;
        double[] before;
        double cart0;

        public String id() {
            return "tank_cart_drains_at_station";
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            l = new Line(ctx);
            c = cart(ctx, ctx.at(3, 1, 1), false);
            PlumbingCases.fill(c.node(), 0.6 * c.node().volume * Water.RHO_L, 5e5, 293.15);
            cart0 = c.node().mWater;
            before = plus(l.rest(), snap(c.node()));
        }

        public void measure(TestContext ctx) {
            FluidNode n = c.node();
            double[] after = plus(l.rest(), snap(n));
            ctx.line(String.format("tank cart drain: cart %.1f of %.1f kg water left, feed %.1f kg; total %s, was %s", n.mWater, cart0,
                l.feed.vessel.mWater, masses(after), masses(before)));
            ctx.check("tank cart: drained through the flange", l.feed.vessel.mLiquid() > 0.3 * cart0 && n.mWater < 0.5 * cart0,
                String.format("feed %.2f kg", l.feed.vessel.mLiquid()));
            ctx.check("tank cart: no fluid made or lost draining", differs(new double[]{before[0], before[1]}, new double[]{after[0], after[1]}, 1e-9).isEmpty(),
                masses(after) + " vs " + masses(before));
            c.discard();
        }
    }

    /**
     * A cart filling at the station is taken off onto the rail beside it mid-fill: the station leaves the fluid world, the
     * cart keeps exactly what it held when it left (masses; energy but for its heat to the ambient), and the feed and
     * pipe keep the rest; put back, it docks again and fills on. Nothing made or lost over the whole run.
     */
    static final class LeavesAndRedocks implements TestCase {
        Line l;
        TankCart c;
        double[] before, atLeave;
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_cart_leaves_with_its_contents_and_redocks";
        }

        public int ticks() {
            return 160;
        }

        public void build(TestContext ctx) {
            l = new Line(ctx);
            ctx.level().setBlock(ctx.at(3, 1, 2), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH), 3);
            PlumbingCases.fill(l.feed.vessel, 0.7 * l.feed.vessel.volume * Water.RHO_L, 8e5, 293.15);
            c = cart(ctx, ctx.at(3, 1, 1), false);
            before = plus(l.rest(), snap(c.node()));
            ctx.after(40, () -> {
                if (l.station.docked() != c) bad.append("not docked first; ");
                c.setPos(c.getX(), c.getY(), c.getZ() + 1);   // (onto the rail beside it)
            });
            // (what it left with: once it has ticked off the station; the fluid step before that still had it docked)
            ctx.after(45, () -> atLeave = snap(c.node()));
            ctx.after(80, () -> {
                double[] now = snap(c.node());
                if (l.station.docked() != null || FluidWorld.devices().contains(l.station)) bad.append("the station kept it; ");
                if (now[0] != atLeave[0] || now[1] != atLeave[1]) bad.append("off the station its contents changed: ").append(masses(now))
                    .append(" vs ").append(masses(atLeave)).append("; ");
                double[] all = plus(l.rest(), now);
                String d = differs(new double[]{before[0], before[1]}, new double[]{all[0], all[1]}, 1e-9);
                if (!d.isEmpty()) bad.append("undocked, the total changed: ").append(d);
                line = String.format("left with %.2f kg water; ", atLeave[0]);
                // (the feed topped up to 8 bar again while it was away, so there is more to take when it is back: a new total)
                PlumbingCases.fill(l.feed.vessel, 0.7 * l.feed.vessel.volume * Water.RHO_L, 8e5, 293.15);
                before = plus(l.rest(), now);
                c.setPos(c.getX(), c.getY(), c.getZ() - 1);   // (back)
            });
        }

        public void measure(TestContext ctx) {
            double[] now = snap(c.node());
            if (l.station.docked() != c) bad.append("not docked again; ");
            if (atLeave != null && !(now[0] > atLeave[0] + 0.02)) bad.append(String.format("no more water after docking again (%.2f kg); ", now[0]));
            double[] all = plus(l.rest(), now);
            String d = differs(new double[]{before[0], before[1]}, new double[]{all[0], all[1]}, 1e-9);
            if (!d.isEmpty()) bad.append("the total changed: ").append(d);
            line += String.format("docked again, now %.2f kg", now[0]);
            ctx.line("tank cart leaves and redocks: " + line);
            ctx.check("tank cart: leaves with its contents exactly and docks again", bad.isEmpty(), bad.toString());
            c.discard();
        }
    }

    /**
     * A cart whose node was made a block higher (as one that rolled down from there) docks at the station: its node is
     * made again at the station's height with the same contents (water and crude), exactly.
     */
    static final class Rebase implements TestCase {
        TankStationEntity s;
        TankCart c;
        double[] was;
        double z0;

        public String id() {
            return "tank_cart_docks_at_another_height";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            s = station(ctx, ctx.at(3, 1, 1), RailShape.NORTH_SOUTH);
            c = cart(ctx, ctx.at(3, 2, 1), false);
            FluidNode n = c.node();
            PlumbingCases.fill(n, 0.3 * n.volume * Water.RHO_L, 2e5, 293.15);
            mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:crude_oil"), 0.1 * n.volume * 850, 293.15, false);
            n.update();
            z0 = n.z;
            was = snap(n);
            c.setPos(c.getX(), c.getY() - 1, c.getZ());   // (onto the station)
        }

        public void measure(TestContext ctx) {
            FluidNode n = c.node();
            double[] now = snap(n);
            // (masses exactly; the energy too but for its heat to the ambient, which is where it was filled)
            now[2] = was[2] = 0;
            ctx.line(String.format("tank cart rebase: z %.4f -> %.4f, docked %s", z0, n.z, s.docked() == c));
            ctx.check("tank cart: docked at another height, its node at the station's", s.docked() == c && n.z == ctx.at(3, 1, 1).getY() + TankCart.TANK_Z && z0 != n.z,
                z0 + " -> " + n.z);
            ctx.check("tank cart: the same contents at the new height", differs(was, now, 0).isEmpty(), differs(was, now, 0));
            c.discard();
        }
    }

    /**
     * A cart rolling east onto an unpowered station along a line of rails stops there and docks (held); a redstone block
     * beside the station powers it, and the cart goes on east, the way it came in, undocked.
     */
    static final class HeldAndReleased implements TestCase {
        TankStationEntity s;
        TankCart c;
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_cart_held_until_station_powered";
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext ctx) {
            for (int x = 0; x <= 7; x++)
                if (x != 3) ctx.level().setBlock(ctx.at(x, 1, 1), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST), 3);
            s = station(ctx, ctx.at(3, 1, 1), RailShape.EAST_WEST);
            c = cart(ctx, ctx.at(1, 1, 1), false);
            c.setDeltaMovement(0.3, 0, 0);
            ctx.after(50, () -> {
                line = String.format("at x %.2f, docked %s; ", c.getX() - ctx.at(0, 0, 0).getX(), s.docked() == c);
                if (s.docked() != c || c.blockPosition().getX() != ctx.at(3, 1, 1).getX()) bad.append("not held at the station: ").append(line);
                ctx.level().setBlock(ctx.at(3, 1, 2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            });
        }

        public void measure(TestContext ctx) {
            double x = c.getX() - ctx.at(0, 0, 0).getX();
            line += String.format("released: at x %.2f, docked %s", x, s.docked() == c);
            if (!s.released()) bad.append("the station is not powered; ");
            if (s.docked() != null || x < 4) bad.append("not let go east; ");
            ctx.line("tank cart hold: " + line);
            ctx.check("tank cart: held on an unpowered station, let go when it is powered", bad.isEmpty(), bad.toString());
            c.discard();
        }
    }

    /**
     * A cart of water and crude through a save and an unload: docked (its chunk unloads: it leaves the station, which
     * leaves the fluid world; loaded again it docks again), undocked, and docked with the station broken under it (the
     * cart keeps everything). Masses and energy exactly.
     */
    static final class RoundTrip implements TestCase {
        enum Row {DOCKED, UNDOCKED, STATION_BROKEN}

        final Row row;
        final StringBuilder bad = new StringBuilder();
        String line = "";
        TankStationEntity s;
        TankCart copy;

        RoundTrip(Row row) {
            this.row = row;
        }

        public String id() {
            return "tank_cart_round_trip_" + row.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int ticks() {
            return 30;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(3, 1, 1);
            if (row == Row.UNDOCKED) ctx.level().setBlock(p, Blocks.RAIL.defaultBlockState(), 3);
            else s = station(ctx, p, RailShape.NORTH_SOUTH);
            TankCart c = cart(ctx, p, true);
            FluidNode n = c.node();
            PlumbingCases.fill(n, 0.3 * n.volume * Water.RHO_L, 3e5, 320);
            mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:crude_oil"), 0.2 * n.volume * 850, 320, false);
            n.update();
            ctx.after(15, () -> {
                double[] was = snap(c.node());
                if (row != Row.UNDOCKED && s.docked() != c) bad.append("not docked; ");
                if (row == Row.STATION_BROKEN) {
                    ctx.level().removeBlock(p, false);
                    if (FluidWorld.devices().contains(s)) bad.append("the broken station is still in the fluid world; ");
                    String d = differs(was, snap(c.node()), 0);
                    if (!d.isEmpty()) bad.append("the cart lost contents with the station: ").append(d);
                    copy = c;
                    line = String.format("station broken under it: %.2f kg water kept, %s, station in the fluid world %s", c.node().mWater,
                        c.isRemoved() ? "removed" : "standing", FluidWorld.devices().contains(s));
                    return;
                }
                var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, ctx.level().registryAccess());
                c.saveWithoutId(out);
                var tag = out.buildResult();
                c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                if (s != null && (s.docked() != null || FluidWorld.devices().contains(s))) bad.append("unloaded, the station kept it; ");
                copy = (TankCart) c.getType().create(ctx.level(), EntitySpawnReason.LOAD);
                copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, ctx.level().registryAccess(), tag));
                ctx.level().addFreshEntity(copy);
                String d = differs(was, snap(copy.node()), 0);
                if (!d.isEmpty()) bad.append("loaded, it differs: ").append(d);
                if (copy.alloy != c.alloy) bad.append("loaded as another tier; ");
                line = String.format("%.2f kg water, %.2f bar, %s", copy.node().mWater, copy.node().P() / 1e5, mods.eln.sim.fluid.EaUnits.composition(copy.node()));
            });
        }

        public void measure(TestContext ctx) {
            if (row == Row.DOCKED && (s.docked() != copy || !FluidWorld.devices().contains(s))) bad.append("loaded again, it did not dock again; ");
            if (row == Row.STATION_BROKEN && copy.node() == null) bad.append("no node; ");
            ctx.line("tank cart round trip " + row + ": " + line);
            ctx.check("tank cart: " + row + " through a save and an unload, the same contents", bad.isEmpty(), bad.toString());
            if (copy != null) copy.discard();
        }
    }

    /** A cart broken (destroyed by hits) drops a minecart and its tank: with its contents exactly, or plain and stackable if empty. */
    static final class Breaks extends TankItemCases.Small {
        final boolean filled;

        Breaks(boolean filled) {
            this.filled = filled;
        }

        public String id() {
            return "tank_cart_breaks_into_minecart_and_tank_" + (filled ? "filled" : "empty");
        }

        String what() {
            return "tank cart: broken, a minecart and the tank item with its contents";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            ctx.level().setBlock(p, Blocks.RAIL.defaultBlockState(), 3);
            TankCart c = cart(ctx, p, false);
            FluidNode n = c.node();
            if (filled) PlumbingCases.fill(n, 0.5 * n.volume * Water.RHO_L, 2e5, 330);
            double[] was = snap(n);
            c.hurtServer(ctx.level(), ctx.level().damageSources().generic(), 100);
            if (!c.isRemoved()) bad.append("not broken; ");
            List<ItemStack> got = new ArrayList<>();
            for (ItemEntity e : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2))) {
                got.add(e.getItem().copy());
                e.discard();
            }
            ItemStack cartItem = got.stream().filter(s -> s.is(Items.MINECART)).findFirst().orElse(ItemStack.EMPTY);
            ItemStack tank = got.stream().filter(s -> s.getItem() instanceof mods.eln.node.transparent.TransparentNodeItem).findFirst().orElse(ItemStack.EMPTY);
            if (cartItem.isEmpty() || tank.isEmpty() || got.size() != 2) bad.append("dropped ").append(got).append("; ");
            if (tank.isEmpty()) return;
            TankContents tc = TankItemCases.contents(tank);
            if (!filled) {
                if (tc != null || tank.getMaxStackSize() != 64) bad.append("an empty cart's tank isn't plain: ").append(tank.getComponentsPatch()).append("; ");
                line = "dropped " + got;
                return;
            }
            if (tc == null) {
                bad.append("no contents on the tank; ");
                return;
            }
            FluidNode i = new FluidNode(tc.volume(), 0, 1, false);
            mods.eln.fluid.FluidSaves.restore(i, tc.saved());
            bad.append(differs(was, snap(i), 0));
            line = "dropped " + got + ": " + tc.composition();
        }
    }

    /**
     * A cart half full of water at 20 C standing in a desert (50 C) warms towards it through the tank's loss coefficient (1 W/K):
     * after 20 s by about 1 - exp(-t / tau) of the 30 K, tau its heat capacity over 1 W/K.
     */
    static final class HotAmbient implements TestCase {
        TankCart c;
        double t0, tau, ambient;
        long tick0;

        public String id() {
            return "tank_cart_warms_in_hot_ambient";
        }

        public int width() {
            return 12;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(5, 1, 1);
            AmbientCases.DesertAndSnow.fillBiome(ctx, p.offset(-2, -2, -2), p.offset(2, 2, 2), net.minecraft.world.level.biome.Biomes.DESERT);
            ctx.level().setBlock(p, Blocks.RAIL.defaultBlockState(), 3);
            c = cart(ctx, p, false);
            mods.eln.sim.Ambient.fixedC = null;
            try {
                ambient = c.ambientK();   // (read where it stands; it reads it again only once it moves)
            } finally {
                mods.eln.sim.Ambient.fixedC = 20.0;
            }
            FluidNode n = c.node();
            PlumbingCases.fill(n, 0.5 * n.volume * Water.RHO_L, Water.P_ATM, 293.15);
            t0 = n.T();
            tau = (n.energyAt(t0 + 0.5) - n.energyAt(t0 - 0.5)) / 1.0;
            tick0 = ctx.world().getGameTime();
        }

        public void measure(TestContext ctx) {
            FluidNode n = c.node();
            n.update();
            double t = (ctx.world().getGameTime() - tick0) / 20.0;
            double want = (ambient - t0) * (1 - Math.exp(-t / tau)), got = n.T() - t0;
            ctx.line(String.format("tank cart ambient: %.2f C, contents %.4f -> %.4f C over %.1f s (tau %.0f s): +%.4f K, expected +%.4f K",
                ambient - Water.T0, t0 - Water.T0, n.T() - Water.T0, t, tau, got, want));
            ctx.check("tank cart: the desert's ambient read where it stands (50 C)", Math.abs(ambient - Water.T0 - 50) < 1e-6, ambient - Water.T0 + " C");
            ctx.check("tank cart: warms towards the ambient by the tank's loss coefficient", got > 0 && Math.abs(got - want) < 0.25 * want,
                String.format("+%.4f K vs +%.4f K", got, want));
            c.discard();
        }
    }

    /**
     * An iron cart of air at 50 bar (it bursts over 40) on a rail bursts as a tank does: the cart goes, no tank item drops.
     * With explosions off (Eln.explosionEnable, only while its failure runs, here in build) it burns out: burnt by
     * overpressure, removed, the minecart dropped.
     */
    static final class Bursts extends PlumbingCases.Big {
        final boolean explosions;
        TankCart c;
        BlockPos p;

        Bursts(boolean explosions) {
            this.explosions = explosions;
        }

        public String id() {
            return explosions ? "tank_cart_bursts_over_burst_pressure" : "tank_cart_bursts_over_burst_pressure_explosions_off";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            p = ctx.at(5, 1, 5);
            ctx.level().setBlock(p, Blocks.RAIL.defaultBlockState(), 3);
            c = cart(ctx, p, false);
            PlumbingCases.fill(c.node(), 0, 50e5, 293.15);
            if (explosions) return;
            // (the failure run at once with explosions off: the flag is global, so not across a tick other tests share)
            mods.eln.Eln.explosionEnable = false;
            try {
                c.queueBurst().destructImpl();
            } finally {
                mods.eln.Eln.explosionEnable = true;
            }
        }

        public void measure(TestContext ctx) {
            List<ItemStack> got = new ArrayList<>();
            for (ItemEntity e : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(6))) {
                got.add(e.getItem().copy());
                e.discard();
            }
            boolean tank = got.stream().anyMatch(s -> s.getItem() instanceof mods.eln.node.transparent.TransparentNodeItem);
            ctx.line("tank cart burst: removed " + c.isRemoved() + ", burnt " + c.getBurnt() + ", dropped " + got);
            ctx.check("tank cart: bursts over its burst pressure, the cart destroyed", c.isRemoved(), "");
            ctx.check("tank cart: a burst cart drops no tank", !tank, got.toString());
            if (!explosions) {
                ctx.check("tank cart: with explosions off it burns out by overpressure", c.getBurnt() == mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE,
                    String.valueOf(c.getBurnt()));
                ctx.check("tank cart: with explosions off the minecart drops", got.stream().anyMatch(s -> s.is(Items.MINECART)), got.toString());
            }
            if (!c.isRemoved()) c.discard();
        }
    }

    /**
     * A cart docked on a station, pushed back and forth on it every tick (faster than it counts as standing), stays
     * docked: one docking, no flicker (each dock and undock would rebuild the fluid world).
     */
    static final class Jostled implements TestCase {
        TankStationEntity s;
        TankCart c;
        int docks0 = -1, away;

        public String id() {
            return "tank_cart_jostled_on_station_stays_docked";
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext ctx) {
            s = station(ctx, ctx.at(3, 1, 1), RailShape.NORTH_SOUTH);
            c = cart(ctx, ctx.at(3, 1, 1), false);
            for (int t = 15; t < 55; t++) {
                int k = t;
                ctx.after(t, () -> {
                    if (docks0 < 0) docks0 = s.dockings;
                    if (s.docked() != c) away++;
                    c.setDeltaMovement(0, 0, k % 2 == 0 ? 0.2 : -0.2);
                });
            }
        }

        public void measure(TestContext ctx) {
            ctx.line(String.format("tank cart jostled: dockings %d (first %d), ticks not docked %d, at z %.2f", s.dockings, docks0, away,
                c.getZ() - ctx.at(0, 0, 0).getZ()));
            ctx.check("tank cart: jostled on the station, docked once and never left", s.dockings == 1 && away == 0 && s.docked() == c,
                s.dockings + " dockings, " + away + " ticks away");
            c.discard();
        }
    }

    /** A docked cart of water killed by a command drops its tank with its contents; the station leaves the fluid world. */
    static final class Killed implements TestCase {
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_cart_killed_drops_its_tank";
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(3, 1, 1);
            TankStationEntity s = station(ctx, p, RailShape.NORTH_SOUTH);
            TankCart c = cart(ctx, p, false);
            PlumbingCases.fill(c.node(), 0.5 * c.node().volume * Water.RHO_L, 2e5, 300);
            ctx.after(12, () -> {
                if (s.docked() != c) bad.append("not docked; ");
                double[] was = snap(c.node());
                c.kill(ctx.level());   // (as /kill does)
                if (FluidWorld.devices().contains(s) || s.docked() != null) bad.append("the station kept it; ");
                List<ItemStack> got = new ArrayList<>();
                for (ItemEntity e : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2))) {
                    got.add(e.getItem().copy());
                    e.discard();
                }
                ItemStack tank = got.stream().filter(x -> x.getItem() instanceof mods.eln.node.transparent.TransparentNodeItem).findFirst().orElse(ItemStack.EMPTY);
                TankContents tc = tank.isEmpty() ? null : TankItemCases.contents(tank);
                if (tc == null) {
                    bad.append("no tank with contents in ").append(got).append("; ");
                    return;
                }
                FluidNode i = new FluidNode(tc.volume(), 0, 1, false);
                mods.eln.fluid.FluidSaves.restore(i, tc.saved());
                bad.append(differs(was, snap(i), 0));
                line = "dropped " + got + ": " + tc.composition();
            });
        }

        public void measure(TestContext ctx) {
            ctx.line("tank cart killed: " + line);
            ctx.check("tank cart: killed, its tank drops with its contents and the station lets go", bad.isEmpty(), bad.toString());
        }
    }

    /** Two carts on one station: the first docked stays docked, the second is never docked. */
    static final class TwoCarts implements TestCase {
        TankStationEntity s;
        TankCart a, b;

        public String id() {
            return "tank_cart_one_cart_per_station";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            s = station(ctx, ctx.at(3, 1, 1), RailShape.NORTH_SOUTH);
            a = cart(ctx, ctx.at(3, 1, 1), false);
            ctx.after(10, () -> b = cart(ctx, ctx.at(3, 1, 1), true));
        }

        public void measure(TestContext ctx) {
            ctx.line(String.format("tank cart two on a station: docked %s, dockings %d", s.docked() == a ? "the first" : s.docked() == b ? "the second" : "none", s.dockings));
            ctx.check("tank cart: one cart per station, the first", s.docked() == a && s.dockings == 1, s.dockings + " dockings");
            a.discard();
            if (b != null) b.discard();
        }
    }

    /**
     * A station with no cart passes nothing: a feed at 8 bar piped to its flange keeps its contents, the station is out
     * of the fluid world, its flanges are there for the pipe kit, the comparator reads 0.
     */
    static final class Inert implements TestCase {
        Line l;
        double[] before;

        public String id() {
            return "tank_cart_station_without_cart_is_inert";
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext ctx) {
            l = new Line(ctx);
            PlumbingCases.fill(l.feed.vessel, 0.7 * l.feed.vessel.volume * Water.RHO_L, 8e5, 293.15);
            before = l.rest();
        }

        public void measure(TestContext ctx) {
            double[] after = l.rest();
            int ports = mods.eln.fluid.FluidPlacement.portsAt(ctx.level(), ctx.at(3, 1, 1)).size();
            ctx.line("tank cart inert station: feed and pipe " + masses(after) + ", was " + masses(before) + "; flanges for the kit " + ports);
            ctx.check("tank cart: a station with no cart passes nothing", differs(new double[]{before[0], before[1]}, new double[]{after[0], after[1]}, 1e-12).isEmpty(),
                masses(after) + " vs " + masses(before));
            ctx.check("tank cart: a station with no cart is out of the fluid world", !FluidWorld.devices().contains(l.station) && l.station.signal() == 0, "");
            ctx.check("tank cart: its two flanges are there for the pipe kit", ports == 2, ports + "");
            ctx.check("tank cart: Jade on a station with no cart says so", "none docked".equals(l.station.waila().get("Cart")), l.station.waila().toString());
        }
    }
}
