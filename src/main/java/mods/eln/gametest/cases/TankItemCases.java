package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeItem;
import mods.eln.oil.TankContents;
import mods.eln.oil.TankElement;
import mods.eln.registry.ElnRegistry;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.PlumbingCases.ALLOY_TANK;
import static mods.eln.gametest.cases.PlumbingCases.IRON_TANK;

/**
 * Baughn 2026-11-25: a broken tank drops as an item that keeps its contents (any mix, a partial fill, a gas), frozen
 * while carried, and places back as the same tank; a block of a box drops its share (by layer) and the rest keeps the
 * rest; a tank of nothing but air drops a plain, stackable tank. No fluid is made or lost on the way.
 */
public final class TankItemCases {
    private TankItemCases() {
    }

    public static void addTo(List<TestCase> cases) {
        for (RoundTrip.Row r : RoundTrip.Row.values()) cases.add(new RoundTrip(r));
        for (BoxBreak.Row r : BoxBreak.Row.values()) cases.add(new BoxBreak(r));
        cases.add(new JoinsBox());
        cases.add(new NoDupeLoop());
        cases.add(new PendingBucket());
        cases.add(new PickIsPlain());
        cases.add(new UnknownSpecies());
        cases.add(new BurstBox());
        cases.add(new BuildLimit());
        cases.add(new NotAnIngredientFilled());
    }

    /** Water, air, energy, then the other species, of n. */
    static double[] snap(FluidNode n) {
        int k = n.extra == null ? 0 : n.extra.length;
        double[] s = new double[3 + k];
        s[0] = n.mWater;
        s[1] = n.mAir;
        s[2] = n.energy;
        for (int i = 0; i < k; i++) s[3 + i] = n.extra[i];
        return s;
    }

    static double[] plus(double[] a, double[] b) {
        double[] s = new double[Math.max(a.length, b.length)];
        for (int i = 0; i < s.length; i++) s[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
        return s;
    }

    /** "" if a and b agree to rel of the larger (of each entry; masses also to an absolute 1e-15 kg), else what differs. */
    static String differs(double[] a, double[] b, double rel) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            double x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (Math.abs(x - y) > rel * Math.max(Math.abs(x), Math.abs(y)) + (i == 2 ? 0 : 1e-15))
                out.append(String.format("[%d] %.12g vs %.12g; ", i, x, y));
        }
        return out.toString();
    }

    /** The item stacks dropped around p (taken: their entities go). */
    static List<ItemStack> drops(TestContext ctx, BlockPos p) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity e : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(1.5))) {
            out.add(e.getItem().copy());
            e.discard();
        }
        return out;
    }

    /** Break the tank at p (the block removed, as anything that removes it does) and take what it drops. */
    static List<ItemStack> breakAt(TestContext ctx, BlockPos p) {
        ctx.level().removeBlock(p, false);
        return drops(ctx, p);
    }

    /** Place the tank item s at p as a player would, or null. */
    static TankElement place(TestContext ctx, ItemStack s, BlockPos p) {
        var node = ((TransparentNodeItem) s.getItem()).placeAt(s, ctx.player(), ctx.level(), p, Direction.YN);
        return node == null ? null : (TankElement) node.element;
    }

    static TankElement at(TestContext ctx, BlockPos p) {
        return ShaftArtCases.TankBoxReloads.at(ctx, p);
    }

    static TankContents contents(ItemStack s) {
        return s.get(ElnRegistry.TANK_CONTENTS.get());
    }

    /** s through the save codec and the network codec, as an inventory saves it and a client gets it. */
    static ItemStack throughCodecs(TestContext ctx, ItemStack s, StringBuilder bad) {
        var ops = ctx.level().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var tag = ItemStack.CODEC.encodeStart(ops, s).getOrThrow();
        ItemStack saved = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        if (!ItemStack.isSameItemSameComponents(saved, s)) bad.append("the saved stack differs: ").append(tag).append("; ");
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), ctx.level().registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, saved);
        ItemStack sent = ItemStack.STREAM_CODEC.decode(buf);
        if (!ItemStack.isSameItemSameComponents(sent, s)) bad.append("the synced stack differs; ");
        return sent;
    }

    /**
     * A lone tank, filled as the row says, broken: its item carries the contents, stacks only if empty, keeps its tier,
     * says what it holds; through the save and network codecs and placed back it is the same tank (masses and energy
     * exactly, so the same temperature and pressure), and a reload of the placed tank keeps them.
     */
    static final class RoundTrip implements TestCase {
        enum Row {
            WATER(IRON_TANK, false), WATER_EXPLODED(IRON_TANK, true), OIL_AND_WATER(ALLOY_TANK, false), HOT_STEAM(ALLOY_TANK, false),
            AIR_ONLY(IRON_TANK, false), AIR_AND_TRACE_OF_OIL(IRON_TANK, false), AIR_AND_TRACE_OF_GAS(IRON_TANK, false);
            final int tank;
            /** Broken by an explosion over it, not removed. */
            final boolean exploded;

            Row(int tank, boolean exploded) {
                this.tank = tank;
                this.exploded = exploded;
            }
        }

        final Row row;
        final StringBuilder bad = new StringBuilder();
        String line = "";

        RoundTrip(Row row) {
            this.row = row;
        }

        public String id() {
            return "tank_item_round_trip_" + row.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            TankElement t = (TankElement) ctx.placeTransparent(row.tank, p);
            FluidNode n = t.own;
            switch (row) {
                case WATER, WATER_EXPLODED -> PlumbingCases.fill(n, 0.4 * n.volume * Water.RHO_L, Water.P_ATM, 330);
                case OIL_AND_WATER -> {
                    PlumbingCases.fill(n, 0.3 * n.volume * Water.RHO_L, Water.P_ATM, 300);
                    mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:crude_oil"), 0.2 * n.volume * 850, 300, false);
                    n.update();
                }
                case HOT_STEAM -> {
                    n.mWater = 0;
                    n.mAir = 0;
                    n.extra = null;
                    n.mWater = 8e5 / (Water.R_V * 520) * n.volume;   // (steam at about 8 bar, 247 C: superheated)
                    n.energy = n.energyAt(520);
                    n.update();
                }
                case AIR_ONLY -> PlumbingCases.fill(n, 0, Water.P_ATM, 293.15);
                case AIR_AND_TRACE_OF_OIL -> {   // (what a tank drained of oil keeps: Jade lists none)
                    PlumbingCases.fill(n, 0, Water.P_ATM, 293.15);
                    mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:heavy_oil"), 1e-9, 293.15, false);
                    n.update();
                }
                case AIR_AND_TRACE_OF_GAS -> {   // (a gas species: natural gas, a trace)
                    PlumbingCases.fill(n, 0, Water.P_ATM, 293.15);
                    mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:natural_gas"), 1e-9, 293.15, true);
                    n.update();
                }
            }
            double[] before = snap(n);
            double T0 = n.T(), P0 = n.P();
            // (a piston neither pushes nor breaks it: a block with a block entity is immovable)
            if (net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(ctx.level().getBlockState(p), ctx.level(), p,
                net.minecraft.core.Direction.EAST, true, net.minecraft.core.Direction.EAST)) bad.append("a piston would move it; ");
            List<ItemStack> got;
            if (row.exploded) {
                ctx.level().explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1.5f, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                got = drops(ctx, p).stream().filter(x -> x.getItem() instanceof TransparentNodeItem).toList();
                if (!ctx.level().getBlockState(p).isAir()) bad.append("the explosion left the tank; ");
            } else got = breakAt(ctx, p);
            if (got.size() != 1) {
                bad.append(got.size()).append(" stacks dropped; ");
                return;
            }
            ItemStack s = got.get(0);
            var tier = mods.eln.Eln.transparentNodeItem.getDescriptor(row.tank);
            if (s.getItem() != tier.newItemStack().getItem()) bad.append("dropped ").append(s.getItem()).append(", not its own tier; ");
            TankContents c = contents(s);
            if (row == Row.AIR_ONLY || row == Row.AIR_AND_TRACE_OF_OIL || row == Row.AIR_AND_TRACE_OF_GAS) {
                if (c != null || !ItemStack.isSameItemSameComponents(s, tier.newItemStack()) || s.getMaxStackSize() != 64)
                    bad.append("an air-only tank dropped ").append(s.getComponentsPatch()).append(", not a plain stackable tank; ");
                line = "dropped a plain tank, stacks to " + s.getMaxStackSize();
                return;
            }
            if (c == null) {
                bad.append("no contents on the item; ");
                return;
            }
            if (s.getMaxStackSize() != 1) bad.append("a filled tank stacks to ").append(s.getMaxStackSize()).append("; ");
            List<String> tip = TooltipCases.TooltipCase.lines(s);
            // (Baughn 2026-11-25: a filled tank's tooltip is its contents and pressure only)
            if (tip.size() != 2 || !tip.get(0).startsWith("Contents: ") || !tip.get(1).startsWith("Pressure: ") || !tip.get(1).contains(" bar of ")
                || tip.get(1).contains("°C"))
                bad.append("tooltip not contents and pressure only: ").append(tip).append("; ");
            List<String> plainTip = TooltipCases.TooltipCase.lines(tier.newItemStack());
            if (plainTip.stream().noneMatch(l -> l.startsWith("Holds "))) bad.append("an empty tank lost its usual tooltip: ").append(plainTip).append("; ");
            ItemStack back = throughCodecs(ctx, s, bad);
            TankElement t2 = place(ctx, back, p);
            if (t2 == null) {
                bad.append("could not place it back; ");
                return;
            }
            if (t2.transparentNodeDescriptor != tier) bad.append("placed back as another tier; ");
            FluidNode m = t2.own;
            m.update();
            bad.append(differs(before, snap(m), 1e-12));
            if (Math.abs(m.T() - T0) > 1e-6 || Math.abs(m.P() - P0) > 1e-6 * P0)
                bad.append(String.format("placed back at %.4f K, %.2f Pa, was %.4f K, %.2f Pa; ", m.T(), m.P(), T0, P0));
            line = String.format("%.2f C, %.3f bar: %s; tooltip %s", T0 - Water.T0, P0 / 1e5, c.composition(), tip);
            // (the placed tank through a save and reload: floats, 1e-6)
            try {
                ShaftArtCases.reloadAll(ctx, List.of(p));
            } catch (ReflectiveOperationException e) {
                bad.append(e).append("; ");
                return;
            }
            FluidNode r = at(ctx, p).own;
            r.update();
            bad.append(differs(before, snap(r), 1e-6));
        }

        public void measure(TestContext ctx) {
            ctx.line("tank item " + row + ": " + line);
            ctx.check("tank item " + row + ": broken and placed back, the same contents", bad.isEmpty(), bad.toString());
        }
    }

    /**
     * A 2 x 2 x 2 iron box 30 % full of water and crude: one block broken (by the row's means) drops its share by layer, a
     * bottom block a quarter of the liquid, a top block none; the item and the seven tanks left hold exactly what the box
     * did, each species and the energy.
     */
    static final class BoxBreak extends PlumbingCases.Big {
        enum Row {
            BOTTOM_REMOVED(1, 0, true), BOTTOM_DESTROYED(1, 1, true), TOP_DESTROYED(2, 1, true), TOP_OF_WATER_DESTROYED(2, 1, false);
            final int y, how;
            /** Crude over the water (else water only: the top block's share is air and vapour at 1 atm, a plain tank). */
            final boolean oil;

            Row(int y, int how, boolean oil) {
                this.y = y;
                this.how = how;
                this.oil = oil;
            }
        }

        final Row row;
        final StringBuilder bad = new StringBuilder();
        String line = "";

        BoxBreak(Row row) {
            this.row = row;
        }

        public String id() {
            return "tank_item_box_break_" + row.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            List<TankElement> ts = new ArrayList<>();
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 3; x++)
                    for (int z = 2; z <= 3; z++) ts.add((TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(x, y, z)));
            var g = ts.get(0).group;
            if (g == null || g.members.size() != 8) {
                bad.append("not a box; ");
                return;
            }
            FluidNode n = g.node;
            PlumbingCases.fill(n, 0.2 * n.volume * Water.RHO_L, Water.P_ATM, 300);
            if (row.oil) mods.eln.sim.fluid.EaUnits.add(n, SpeciesRegistry.get().id("eln:crude_oil"), 0.1 * n.volume * 850, 300, false);
            n.update();
            double[] before = snap(n);
            double liquid0 = n.liquidVolume();
            BlockPos p = ctx.at(3, row.y, 3);
            if (row.how == 0) ctx.level().removeBlock(p, false);
            else ctx.level().destroyBlock(p, true);   // (as other mods' block breakers do)
            List<ItemStack> got = drops(ctx, p);
            List<ItemStack> tanks = got.stream().filter(s -> s.getItem() instanceof TransparentNodeItem).toList();
            if (!ctx.level().getBlockState(p).isAir()) bad.append("the block stands; ");
            if (tanks.size() != 1) {
                bad.append(tanks.size()).append(" tanks dropped of ").append(got).append("; ");
                return;
            }
            TankContents c = contents(tanks.get(0));
            double[] item = new double[3];
            double itemLiquid = 0;
            if (c != null) {
                FluidNode i = new FluidNode(c.volume(), 0, 1, false);
                mods.eln.fluid.FluidSaves.restore(i, c.saved());
                i.update();
                item = snap(i);
                itemLiquid = i.liquidVolume();
            }
            // the rest: every tank but the broken one, each vessel once
            double[] rest = new double[0];
            java.util.Set<FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            int left = 0;
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 3; x++)
                    for (int z = 2; z <= 3; z++) {
                        BlockPos q = ctx.at(x, y, z);
                        if (q.equals(p)) continue;
                        TankElement t = at(ctx, q);
                        if (t == null) continue;
                        left++;
                        if (t.group != null) bad.append("a box remains; ");
                        if (seen.add(t.vessel)) rest = plus(rest, snap(t.vessel));
                    }
            if (left != 7) bad.append(left).append(" tanks left; ");
            if (!row.oil && (c != null || tanks.get(0).getMaxStackSize() != 64)) bad.append("a share of air and vapour dropped a filled tank: ").append(c).append("; ");
            if (c != null) bad.append(differs(before, plus(item, rest), 1e-12));
            else {
                // (a share of nothing but air and a trace of vapour at 1 atm drops a plain tank: that air and vapour go)
                double[] r = plus(item, rest), keep = before.clone(), got2 = r.clone();
                keep[1] = keep[2] = got2[1] = got2[2] = 0;
                keep[0] = got2[0] = 0;
                bad.append(differs(keep, got2, 1e-12));
                if (Math.abs(before[0] - r[0]) > 1e-4 * before[0]) bad.append(String.format("water %.9f -> %.9f kg; ", before[0], r[0]));
                line += String.format("(plain: %.3g kg air and %.3g kg water vapour went) ", before[1] - r[1], before[0] - r[0]);
            }
            double share = itemLiquid / liquid0;
            line += String.format("item %.4f of the liquid (%s), the rest %.4f", share, c == null ? "plain" : c.composition(), 1 - share);
            if (row.y == 1 && Math.abs(share - 0.25) > 0.01) bad.append(String.format("a bottom block took %.4f of the liquid, not a quarter; ", share));
            if (row.y == 2 && share > 1e-6) bad.append(String.format("a top block took %.4f of the liquid; ", share));
        }

        public void measure(TestContext ctx) {
            ctx.line("tank item box " + row + ": " + line);
            ctx.check("tank item box " + row + ": the block takes its share, the item and the rest hold what the box did", bad.isEmpty(), bad.toString());
        }
    }

    /**
     * A filled tank item placed in line with a 1 x 1 x 2 box of the same tier makes a 1 x 1 x 3 box whose contents are the
     * box's and the item's summed (each species and the energy, to rounding).
     */
    static final class JoinsBox extends PlumbingCases.Big {
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_item_joins_box";
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            TankElement a = (TankElement) ctx.placeTransparent(IRON_TANK, ctx.at(2, 1, 2));
            ctx.placeTransparent(IRON_TANK, ctx.at(2, 1, 3));
            if (a.group == null) {
                bad.append("no box; ");
                return;
            }
            PlumbingCases.fill(a.group.node, 0.5 * a.group.node.volume * Water.RHO_L, Water.P_ATM, 300);
            double[] box = snap(a.group.node);
            // a lone tank with hot oil and water, broken
            BlockPos lone = ctx.at(6, 1, 6);
            TankElement l = (TankElement) ctx.placeTransparent(IRON_TANK, lone);
            PlumbingCases.fill(l.own, 0.3 * l.own.volume * Water.RHO_L, 2e5, 360);
            mods.eln.sim.fluid.EaUnits.add(l.own, SpeciesRegistry.get().id("eln:crude_oil"), 0.1 * l.own.volume * 850, 360, false);
            l.own.update();
            double[] item = snap(l.own);
            List<ItemStack> got = breakAt(ctx, lone);
            if (got.size() != 1 || contents(got.get(0)) == null) {
                bad.append("dropped ").append(got).append("; ");
                return;
            }
            TankElement t = place(ctx, got.get(0), ctx.at(2, 1, 4));
            if (t == null || t.group == null || t.group.members.size() != 3) {
                bad.append("did not join the box: ").append(t == null ? "not placed" : t.group == null ? "lone" : t.group.members.size() + " blocks").append("; ");
                return;
            }
            bad.append(differs(plus(box, item), snap(t.group.node), 1e-12));
            line = String.format("the box of 3: %s, %.2f C", mods.eln.sim.fluid.EaUnits.composition(t.group.node), t.group.node.T() - Water.T0);
        }

        public void measure(TestContext ctx) {
            ctx.line("tank item joins a box: " + line);
            ctx.check("tank item: placed in line with a box it joins, the contents summed", bad.isEmpty(), bad.toString());
        }
    }

    /**
     * Break and place a tank and a box block 20 times over (the box splitting and re-forming each time): no fluid made or
     * lost, each species and the energy.
     */
    static final class NoDupeLoop extends PlumbingCases.Big {
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_item_no_dupe_on_break_place_loops";
        }

        public int ticks() {
            return 4;
        }

        double[] total(TestContext ctx, List<BlockPos> at) {
            double[] s = new double[0];
            java.util.Set<FluidNode> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (BlockPos q : at) {
                TankElement t = at(ctx, q);
                if (t != null && seen.add(t.vessel)) s = plus(s, snap(t.vessel));
            }
            return s;
        }

        public void build(TestContext ctx) {
            List<BlockPos> at = new ArrayList<>();
            for (int x = 2; x <= 3; x++)
                for (int z = 2; z <= 3; z++) {
                    ctx.placeTransparent(ALLOY_TANK, ctx.at(x, 1, z));
                    at.add(ctx.at(x, 1, z));
                }
            var g = at(ctx, at.get(0)).group;
            if (g == null) {
                bad.append("no box; ");
                return;
            }
            PlumbingCases.fill(g.node, 0.4 * g.node.volume * Water.RHO_L, 5e5, Water.tsat(5e5));
            mods.eln.sim.fluid.EaUnits.add(g.node, SpeciesRegistry.get().id("eln:crude_oil"), 0.05 * g.node.volume * 850, Water.tsat(5e5), false);
            g.node.update();
            double[] before = total(ctx, at);
            for (int i = 0; i < 20; i++) {
                BlockPos p = at.get(i % at.size());
                List<ItemStack> got = breakAt(ctx, p);
                if (got.size() != 1) {
                    bad.append("loop ").append(i).append(": ").append(got.size()).append(" stacks; ");
                    return;
                }
                if (place(ctx, got.get(0), p) == null) {
                    bad.append("loop ").append(i).append(": not placed; ");
                    return;
                }
            }
            double[] after = total(ctx, at);
            var g2 = at(ctx, at.get(0)).group;
            if (g2 == null || g2.members.size() != 4) bad.append("the box did not re-form; ");
            bad.append(differs(before, after, 1e-10));
            line = String.format("water %.9f -> %.9f kg, energy %.3f -> %.3f J", before[0], after[0], before[2], after[2]);
        }

        public void measure(TestContext ctx) {
            ctx.line("tank item loops: " + line);
            ctx.check("tank item: 20 breaks and placings make and lose no fluid", bad.isEmpty(), bad.toString());
        }
    }

    /** Small cases: one tank at (1, 1, 1) in a 3-block space, the result checked in measure. */
    abstract static class Small implements TestCase {
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public int width() {
            return 3;
        }

        public int ticks() {
            return 4;
        }

        abstract String what();

        public void measure(TestContext ctx) {
            ctx.line(id() + ": " + line);
            ctx.check(what(), bad.isEmpty(), bad.toString());
        }
    }

    /** A bucket's worth put in through the fluid capability and the tank broken before the next fluid step: the item has it. */
    static final class PendingBucket extends Small {
        public String id() {
            return "tank_item_takes_pending_bucket";
        }

        String what() {
            return "tank item: water put in since the last step goes with it";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            TankElement t = (TankElement) ctx.placeTransparent(IRON_TANK, p);
            PlumbingCases.fill(t.own, 0.2 * t.own.volume * Water.RHO_L, Water.P_ATM, 293.15);
            t.showWater();
            double w0 = t.own.mWater;
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, p, net.minecraft.core.Direction.UP);
            int put;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                put = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 1000, tx);
                tx.commit();
            }
            List<ItemStack> got = breakAt(ctx, p);
            TankContents c = got.size() == 1 ? contents(got.get(0)) : null;
            if (put != 1000 || c == null) {
                bad.append("put ").append(put).append(" mB, dropped ").append(got).append("; ");
                return;
            }
            double want = w0 + 1000 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
            if (Math.abs(c.water() - want) > 1e-9 * want) bad.append(String.format("item %.9f kg water, want %.9f; ", c.water(), want));
            line = String.format("%.6f kg before, %.6f kg on the item", w0, c.water());
        }
    }

    /** Middle-click on a filled tank gives a plain tank (a copy of the contents is no pick), and leaves the tank as it was. */
    static final class PickIsPlain extends Small {
        public String id() {
            return "tank_item_pick_is_plain";
        }

        String what() {
            return "tank item: pick-block gives a plain tank";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            TankElement t = (TankElement) ctx.placeTransparent(IRON_TANK, p);
            PlumbingCases.fill(t.own, 0.5 * t.own.volume * Water.RHO_L, Water.P_ATM, 300);
            double[] before = snap(t.own);
            ItemStack pick = ((mods.eln.node.transparent.TransparentNode) t.node).pickItem(null, ctx.level());
            ItemStack plain = mods.eln.Eln.transparentNodeItem.getDescriptor(IRON_TANK).newItemStack();
            if (!ItemStack.isSameItemSameComponents(pick, plain)) bad.append("picked ").append(pick.getComponentsPatch()).append("; ");
            bad.append(differs(before, snap(t.own), 0));
            line = String.valueOf(pick);
        }
    }

    /** An item holding a species no datapack defines places with it kept (a placeholder) and breaks again with it. */
    static final class UnknownSpecies extends Small {
        public String id() {
            return "tank_item_keeps_unknown_species";
        }

        String what() {
            return "tank item: a species no datapack defines survives placing and breaking";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            ItemStack s = mods.eln.Eln.transparentNodeItem.getDescriptor(IRON_TANK).newItemStack();
            FluidNode n = new FluidNode(mods.eln.oil.TankDescriptor.bucketsVolume(12), 0, 1, false);
            PlumbingCases.fill(n, 0.3 * n.volume * Water.RHO_L, Water.P_ATM, 300);
            TankContents c = TankContents.of(n);
            var sp = new java.util.HashMap<>(c.species());
            sp.put("eln_test:unobtainium", 0.25);
            c = new TankContents(c.water(), c.air(), c.energy(), sp, c.format(), c.volume(), c.kelvin(), c.pascal(), c.contents(), c.composition());
            s.set(ElnRegistry.TANK_CONTENTS.get(), c);
            s.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
            TankElement t = place(ctx, s, p);
            if (t == null) {
                bad.append("not placed; ");
                return;
            }
            List<ItemStack> got = breakAt(ctx, p);
            TankContents back = got.size() == 1 ? contents(got.get(0)) : null;
            Double kg = back == null ? null : back.species().get("eln_test:unobtainium");
            if (kg == null || Math.abs(kg - 0.25) > 1e-12) bad.append("broken again it holds ").append(back == null ? got : back.species()).append("; ");
            if (back != null && Math.abs(back.water() - c.water()) > 1e-12 * c.water()) bad.append("its water changed; ");
            line = back == null ? "" : back.species().toString();
        }
    }

    /**
     * A 2 x 2 x 1 iron box over its burst pressure (air at 50 bar; it bursts over 40): a block broken then drops a plain
     * tank (it vents: its share would burst again where placed); the rest left to burst drops nothing over its burst.
     */
    static final class BurstBox extends PlumbingCases.Big {
        final StringBuilder bad = new StringBuilder();
        String line = "";

        public String id() {
            return "tank_item_burst_box_drops_nothing_over_burst";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext ctx) {
            for (int x = 2; x <= 3; x++)
                for (int z = 2; z <= 3; z++) ctx.placeTransparent(IRON_TANK, ctx.at(x, 1, z));
            var g = at(ctx, ctx.at(2, 1, 2)).group;
            if (g == null) {
                bad.append("no box; ");
                return;
            }
            PlumbingCases.fill(g.node, 0, 50e5, 293.15);
            List<ItemStack> got = breakAt(ctx, ctx.at(3, 1, 3));
            if (got.size() != 1 || contents(got.get(0)) != null || got.get(0).getMaxStackSize() != 64)
                bad.append("a block broken over its burst dropped ").append(got).append("; ");
            line = String.format("box at %.1f bar; broken block dropped %s", g.node.P() / 1e5, got);
        }

        public void measure(TestContext ctx) {
            BlockPos lo = ctx.at(0, 0, 0), hi = ctx.at(9, 4, 9);
            var burst = 40e5;
            int n = 0;
            for (ItemEntity e : ctx.level().getEntitiesOfClass(ItemEntity.class, new AABB(net.minecraft.world.phys.Vec3.atCenterOf(lo), net.minecraft.world.phys.Vec3.atCenterOf(hi)).inflate(4))) {
                TankContents c = contents(e.getItem());
                if (e.getItem().getItem() instanceof TransparentNodeItem) n++;
                if (c != null && c.pascal() > burst) bad.append(String.format("a tank dropped at %.1f bar; ", c.pascal() / 1e5));
                e.discard();
            }
            line += "; " + n + " tank items lay about after the bursts";
            ctx.line("tank item burst box: " + line);
            ctx.check("tank item: no tank item is over its burst pressure", bad.isEmpty(), bad.toString());
        }
    }

    /** A filled tank placed past the build height is refused: the item kept as it was, no node there. */
    static final class BuildLimit extends Small {
        public String id() {
            return "tank_item_refused_past_build_height";
        }

        String what() {
            return "tank item: placing past the build height keeps the item and makes no node";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            TankElement t = (TankElement) ctx.placeTransparent(IRON_TANK, p);
            PlumbingCases.fill(t.own, 0.5 * t.own.volume * Water.RHO_L, Water.P_ATM, 300);
            List<ItemStack> got = breakAt(ctx, p);
            if (got.size() != 1 || contents(got.get(0)) == null) {
                bad.append("dropped ").append(got).append("; ");
                return;
            }
            ItemStack s = got.get(0), copy = s.copy();
            BlockPos high = new BlockPos(p.getX(), ctx.level().getMaxY() + 1, p.getZ());
            var item = (TransparentNodeItem) s.getItem();
            var node = ctx.holding(s, pl -> item.useOn(new net.minecraft.world.item.context.UseOnContext(pl, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(high), net.minecraft.core.Direction.UP, high.below(), false))));
            if (node.consumesAction()) bad.append("the use was taken: ").append(node).append("; ");
            if (!ItemStack.matches(s, copy)) bad.append("the item changed: ").append(s).append("; ");
            if (ctx.nodeAt(high) != null) bad.append("a node stands past the build height; ");
            line = "use: " + node + ", item " + s.getCount() + " with contents " + (contents(s) != null);
        }
    }

    /**
     * A filled tank is no recipe ingredient (crafted into an Alloy Tank it lost its contents): the Alloy Tank's recipe takes
     * a plain Iron Tank, and the same with contents not.
     */
    static final class NotAnIngredientFilled extends Small {
        public String id() {
            return "tank_item_filled_is_no_ingredient";
        }

        String what() {
            return "tank item: a recipe takes a plain tank, not a filled one";
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(1, 1, 1);
            TankElement t = (TankElement) ctx.placeTransparent(IRON_TANK, p);
            PlumbingCases.fill(t.own, 0.5 * t.own.volume * Water.RHO_L, Water.P_ATM, 300);
            List<ItemStack> got = breakAt(ctx, p);
            if (got.size() != 1 || contents(got.get(0)) == null) {
                bad.append("dropped ").append(got).append("; ");
                return;
            }
            var rm = ctx.level().getServer().getRecipeManager();
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "alloy_tank"));
            var r = rm.byKey(key).map(h -> h.value()).orElse(null);
            if (!(r instanceof net.minecraft.world.item.crafting.CraftingRecipe cr)) {
                bad.append("no alloy tank recipe: ").append(r).append("; ");
                return;
            }
            ItemStack plain = mods.eln.Eln.transparentNodeItem.getDescriptor(IRON_TANK).newItemStack();
            boolean withPlain = cr.matches(grid(plain), ctx.level()), withFilled = cr.matches(grid(got.get(0)), ctx.level());
            if (!withPlain || withFilled) bad.append("plain ").append(withPlain).append(", filled ").append(withFilled).append("; ");
            line = "plain " + withPlain + ", filled " + withFilled;
        }

        /** The Alloy Tank's grid (alloy ingots, glass panes) around tank. */
        static net.minecraft.world.item.crafting.CraftingInput grid(ItemStack tank) {
            ItemStack a = mods.eln.Eln.findItemStack("Alloy Ingot", 1), pane = new ItemStack(net.minecraft.world.item.Items.GLASS_PANE);
            return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(a.copy(), pane.copy(), a.copy(), a.copy(), tank.copy(), a.copy(), a.copy(), pane.copy(), a.copy()));
        }
    }
}
