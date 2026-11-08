package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** The rubber chain (Baughn 2026-11-07): vanilla sulfur macerates to Sulfur Dust (twice an ore's dust), soul sand to
 *  Soul Dust; resin smelts to Raw Rubber, which smelts to 1 Rubber; four Raw Rubber, a Sulfur Dust and a Soul Dust make
 *  a Rubber Compound that vulcanises in the Electrical Furnace to 8 Rubber. */
public final class RubberCases {
    private RubberCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ChainCase());
        cases.add(new TreeCase());
        cases.add(new CarbonCase());
        cases.add(new DesulfuriserCase());
        cases.add(new SteamCrackerCase());
        cases.add(new ButadienePipeCase());
        cases.add(new PolymeriserCase(false));
        cases.add(new PolymeriserCase(true));
    }

    /** Rubber trees (Baughn 2026-11-07): the sapling grows a rubber fig (Ficus elastica); a collector gets 1.5x the old rate on its trunk and
     *  half on other trees; the compressor takes two logs a resin (two rubber logs: three); the grove is in the worldgen
     *  registries; a sapling is crafted from jungle, oak, birch and acacia saplings. */
    static final class TreeCase implements TestCase {
        String grown = "";

        public String id() {
            return "rubber_tree";
        }

        public int width() {
            return 21;
        }

        public int depth() {
            return 21;
        }

        public int height() {
            return 20;   // (a rubber fig: a trunk up to 13, its crown, and 8 blocks of reach each way)
        }

        public void build(TestContext c) {
            var level = c.level();
            var ground = c.at(10, 1, 10);
            for (var g : net.minecraft.core.BlockPos.betweenClosed(c.at(1, 1, 1), c.at(19, 1, 19)))
                level.setBlock(g, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            var pos = ground.above();
            var sap = mods.eln.rubber.RubberTrees.SAPLING.get().defaultBlockState();
            level.setBlock(pos, sap, 3);
            boolean ok = mods.eln.rubber.RubberTrees.GROWER.growTree(level, level.getChunkSource().getGenerator(), pos, sap, level.getRandom());
            int logs = 0;
            for (int y = 0; y < 16; y++) if (level.getBlockState(pos.above(y)).is(mods.eln.rubber.RubberTrees.LOG.get())) logs++;
            int leaves = 0, roots = 0, wide = 0, decaying = 0;
            for (var p : net.minecraft.core.BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 17, 8))) {
                var st = level.getBlockState(p);
                if (st.is(mods.eln.rubber.RubberTrees.LEAVES.get())) {
                    leaves++;
                    if (Math.max(Math.abs(p.getX() - pos.getX()), Math.abs(p.getZ() - pos.getZ())) >= 5) wide++;
                    if (st.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) >= 7) decaying++;
                }
                if (st.is(mods.eln.rubber.RubberTrees.AERIAL_ROOT.get())) roots++;
                if (st.is(mods.eln.rubber.RubberTrees.LOG.get()) && p.getY() == pos.getY() && !p.equals(pos)) wide += 0;
            }
            if (!ok) c.line("rubber fig refused: " + mods.eln.rubber.RubberFigFeature.lastRefusal);
            grown = ok + " " + logs + " logs " + leaves + " leaves " + wide + " wide " + roots + " roots " + decaying + " decaying";
        }

        public void measure(TestContext c) {
            String[] g = grown.split(" ");
            c.check("a rubber sapling grows a rubber fig: a trunk of 10+ logs, a broad crown (leaves 5+ out), none decaying",
                g[0].equals("true") && Integer.parseInt(g[1]) >= 10 && Integer.parseInt(g[3]) > 50 && Integer.parseInt(g[5]) > 5
                    && g[9].equals("0"), grown);
            // the Industrial Resin Collector reads the trunk behind it: a rubber fig's three times an oak's of the same height
            var lvl = c.level();
            var trunk = c.at(10, 2, 10);
            double fig = mods.eln.transparentnode.resincollector.IndustrialResinCollectorElement.handRate(lvl, trunk.west(), mods.eln.misc.Direction.XP);
            c.check("the industrial collector sees the rubber fig behind it, counting at most 6 logs (Baughn)",
                Math.abs(fig - 3.0 / 5 / 1440 * 6 * 1.5) < 1e-9, "" + fig);
            c.check("rubber logs are logs, its leaves leaves (the collector's checks)",
                mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor.isWood(mods.eln.rubber.RubberTrees.LOG.get())
                    && mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor.isLeaf(mods.eln.rubber.RubberTrees.LEAVES.get()), "");
            double oak = mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement.treeFactor(Blocks.OAK_LOG),
                rub = mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement.treeFactor(mods.eln.rubber.RubberTrees.LOG.get());
            c.check("a collector gets half the old rate on an oak, 1.5x on a rubber tree", oak == 0.5 && rub == 1.5, oak + " " + rub);

            var oneOak = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(Items.OAK_LOG, 1));
            var twoOak = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(Items.SPRUCE_LOG, 2));
            var twoRubber = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(mods.eln.rubber.RubberTrees.LOG_ITEM.get(), 2));
            c.check("compressor: one log isn't enough, two logs make a resin", oneOak == null && twoOak != null
                && twoOak.getOutputCopy()[0].getCount() == 1, oneOak + " / " + (twoOak == null ? "none" : twoOak.getOutputCopy()[0].toString()));
            c.check("compressor: two rubber logs make three resin", twoRubber != null && twoRubber.getOutputCopy()[0].getCount() == 3,
                twoRubber == null ? "none" : twoRubber.getOutputCopy()[0].toString());

            var reg = c.level().registryAccess();
            boolean feature = reg.lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE).get(mods.eln.rubber.RubberTrees.TREE).isPresent();
            boolean grove = reg.lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).get(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.PLACED_FEATURE, net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "rubber_grove"))).isPresent();
            c.check("worldgen: the rubber tree and its grove are registered", feature && grove, feature + " " + grove);

            List<ItemStack> grid = new ArrayList<>(List.of(new ItemStack(Items.JUNGLE_SAPLING), new ItemStack(Items.OAK_SAPLING),
                new ItemStack(Items.BIRCH_SAPLING), new ItemStack(Items.ACACIA_SAPLING)));
            var input = CraftingInput.of(2, 2, grid);
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, c.level());
            c.check("a rubber sapling from jungle, oak, birch and acacia saplings",
                r.isPresent() && r.get().value().assemble(input).is(mods.eln.rubber.RubberTrees.SAPLING_ITEM.get()), r.map(rh -> rh.value().assemble(input).toString()).orElse("none"));
        }
    }

    static final class SlotSpecName {
        static String of(mods.eln.gui.SlotSpec sl) {
            return sl.kind() + "#" + sl.index();
        }
    }

    /** Carbon black (the oil route, 2026-11-07): the burner makes one from 500 mB of residue (20 mB/s), a filled compound
     *  vulcanises to 11 Rubber, carbon black is a black dye. */
    static final class CarbonCase implements TestCase {
        mods.eln.transparentnode.carbonblack.CarbonBlackBurnerElement burner;

        public String id() {
            return "carbon_black";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext c) {
            burner = (mods.eln.transparentnode.carbonblack.CarbonBlackBurnerElement) c.placeTransparent(0 + (93 << 6), c.at(2, 1, 1));
            ((mods.eln.fluid.ElementFluidHandler) burner.getFluidHandler()).fill(
                new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.Cut.RESIDUE.source.get(), 2000), true);
        }

        public void measure(TestContext c) {
            c.check("the burner made a Carbon Black from 500 mB of residue (30 s at 20 mB/s)", burner.carbon() >= 1,
                String.format("%d, %.0f mB burnt", burner.carbon(), burner.burnt()));
            var h = mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.treatmentFor(new ItemStack(ElnRegistry.CARBON_BLACK_COMPOUND.get()));
            c.check("a carbon black compound vulcanises to 11 Rubber (Baughn: a filler)", h != null && h.out().get().getCount() == 11, String.valueOf(h));
            c.check("carbon black is a black dye (c:dyes/black)", new ItemStack(ElnRegistry.CARBON_BLACK.get()).is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/black"))), "");
        }
    }

    /** The Desulfuriser (the oil route, 2026-11-07), powered at 230 V with nothing on its outlet (LabAgent: test it against
     *  a closed outlet): it treats the cut into its out tank until that's full, and loses nothing. */
    static final class DesulfuriserCase implements TestCase {
        mods.eln.transparentnode.desulfuriser.DesulfuriserElement d;

        public String id() {
            return "desulfuriser_closed_outlet";
        }

        public int ticks() {
            return 500;
        }

        public void build(TestContext c) {
            var l = new Wp10aCases.Layout(c, width());
            d = (mods.eln.transparentnode.desulfuriser.DesulfuriserElement) l.device(0 + (94 << 6), 3, 1);
            l.feed(c.at(3, 1, 1), d.front.left(), 8 + (32 << 6), mods.eln.Eln.MVU);   // (a 230 V cable on its side)
            d.fillIn(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.Cut.DIESEL.source.get(), 1000));
        }

        public void measure(TestContext c) {
            String st = String.format("treated %.0f, in %d, out %d, sulfur %d", d.treated(), d.inMb(), d.outMb(), d.sulfur());
            c.check("the desulfuriser treats the diesel into its out tank (~50 mB/s at 230 V)", d.treated() > 500, st);
            c.check("... with its outlet closed, nothing is lost (in + out = 1000 mB)", d.inMb() + d.outMb() == 1000, st);
        }
    }

    /**
     * The Steam Cracker (the oil route), on 400 V with 1000 mB of gasoline and 4000 mB of steam in its tanks: it cracks 10 mB/s
     * of gasoline until the steam (12.5 mB a mB) runs out at 320 mB, into 40 % butadiene and 30 % natural gas, held in its
     * OUT tanks (no pipes), and then says it has no steam.
     */
    static final class SteamCrackerCase implements TestCase {
        mods.eln.transparentnode.steamcracker.SteamCrackerElement d;

        public String id() {
            return "steam_cracker";
        }

        public int ticks() {
            return 900;
        }

        public void build(TestContext c) {
            var l = new Wp10aCases.Layout(c, width());
            d = (mods.eln.transparentnode.steamcracker.SteamCrackerElement) l.device(0 + (95 << 6), 3, 1);
            l.feed(c.at(3, 1, 1), d.front.left(), 12 + (32 << 6), mods.eln.Eln.HVU);   // (a 400 V cable on its side)
            d.fillIn(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.Cut.GASOLINE.source.get(), 1000));
            d.fillIn(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.STEAM.get(), 4000));
        }

        public void measure(TestContext c) {
            String st = String.format("cracked %.0f; gasoline %d, steam %d, butadiene %d, gas %d; %s", d.cracked(), d.gasolineMb(), d.steamMb(),
                d.butadieneMb(), d.gasMb(), d.status());
            c.check("the steam cracker cracks gasoline until its steam runs out (320 mB at 12.5 mB of steam each)", Math.abs(d.cracked() - 320) <= 1, st);
            c.check("... the gasoline it cracked left its tank", d.gasolineMb() == 1000 - (int) Math.round(d.cracked()), st);
            c.check("... into 40 % butadiene and 30 % natural gas, held in its OUT tanks", Math.abs(d.butadieneMb() - 128) <= 1 && Math.abs(d.gasMb() - 96) <= 1, st);
            c.check("... and then it waits for steam", d.status().equals("no steam") && !d.running(), st);
        }
    }

    /**
     * The Polymeriser (the oil route), on 230 V with 100 mB of butadiene (as liquid, or the same mass as vapour) and a Soul
     * Dust: it makes 5 Raw Rubber (20 mB each), the dust initiating 3 more, and then waits for butadiene.
     */
    static final class PolymeriserCase implements TestCase {
        final boolean vapour;
        mods.eln.transparentnode.polymeriser.PolymeriserElement d;

        PolymeriserCase(boolean vapour) {
            this.vapour = vapour;
        }

        public String id() {
            return vapour ? "polymeriser_vapour" : "polymeriser";
        }

        public int ticks() {
            return 700;
        }

        public void build(TestContext c) {
            var l = new Wp10aCases.Layout(c, width());
            d = (mods.eln.transparentnode.polymeriser.PolymeriserElement) l.device(0 + (96 << 6), 3, 1);
            l.feed(c.at(3, 1, 1), d.front.left(), 8 + (32 << 6), mods.eln.Eln.MVU);
            int id = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:butadiene");
            int gasMb = 1 + (int) Math.ceil(100 * mods.eln.fluid.FluidSpecies.kgPerMb(id, false) / mods.eln.fluid.FluidSpecies.kgPerMb(id, true));
            d.fillIn(vapour ? new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Butadiene.GAS.get(), gasMb)
                : new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Butadiene.SOURCE.get(), 100));
            d.getInventory().setItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.SOUL_SLOT, new ItemStack(ElnRegistry.SOUL_DUST.get()));
        }

        public void measure(TestContext c) {
            var inv = d.getInventory();
            String st = String.format("made %d, butadiene %.1f mB, soul %d, raw rubber %d; %s; %s", d.made(), d.butadieneMb(),
                inv.getItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.SOUL_SLOT).getCount(),
                inv.getItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.RUBBER_SLOT).getCount(), d.status(), d.getWaila());
            c.check("the polymeriser makes 100 mB of butadiene" + (vapour ? " (as vapour)" : "") + " into 5 Raw Rubber", d.made() == 5
                && inv.getItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.RUBBER_SLOT).is(ElnRegistry.RAW_RUBBER.get()), st);
            c.check("... using one Soul Dust (good for 8)", inv.getItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.SOUL_SLOT).isEmpty()
                && st.contains("+3 rubber left"), st);
            c.check("... and then waits for butadiene", d.status().equals("no butadiene") && d.butadieneMb() < 20, st);
        }
    }

    /**
     * Butadiene in a closed pipe at room temperature (LabAgent: a real condensable, boiling at -4.4 C): it boils until about
     * 2.3 bar is over it and stays liquid under that, without NaNs.
     */
    static final class ButadienePipeCase implements TestCase {
        BlockPos a;

        public String id() {
            return "butadiene_in_a_closed_pipe";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext c) {
            a = c.at(1, 1, 1);
            for (BlockPos p : new BlockPos[]{a, c.at(2, 1, 1)}) PipeCases.lay(c, p, 0, 0, 8, 8, 16, 8, 8);
            var f = PipeCases.node(c, a).parts.get(0) instanceof mods.eln.node.voxel.PipePart pp ? pp.fluid : null;
            int id = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:butadiene");
            f.fill(0, 1, 293.15);
            mods.eln.sim.fluid.EaUnits.add(f, id, 0.3 * f.volume * 615, 293.15, false);   // (a third full of liquid, no air)
            f.update();
            mods.eln.fluid.FluidWorld.changed(PipeCases.node(c, a));
        }

        public void measure(TestContext c) {
            var f = ((mods.eln.node.voxel.PipePart) PipeCases.node(c, a).parts.get(0)).fluid;
            int id = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:butadiene");
            double p = f.P(), liq = f.liquid(id), vap = f.vapour(id);
            String st = String.format("P %.3g Pa, T %.1f K, liquid %.3g kg, vapour %.3g kg", p, f.T(), liq, vap);
            c.check("butadiene in a closed pipe at room temperature boils to ~2.3 bar over its liquid", p > 1.6e5 && p < 3.2e5 && Double.isFinite(p), st);
            c.check("... and most of it stays liquid", liq > vap && liq > 0, st);
        }
    }

    static final class ChainCase implements TestCase {
        public String id() {
            return "rubber_chain";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext c) {
        }

        static String smelt(TestContext c, Item in) {
            var input = new SingleRecipeInput(new ItemStack(in));
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.SMELTING, input, c.level());
            if (r.isEmpty()) return "none";
            ItemStack out = r.get().value().assemble(input);
            return out.getCount() + " " + BuiltInRegistries.ITEM.getKey(out.getItem());
        }

        static String macerate(Item in) {
            var r = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(in, 64));
            if (r == null) return "none";
            ItemStack[] max = r.maxOutput();
            return r.getOutputCopy()[0].getCount() + ".." + max[0].getCount() + " " + BuiltInRegistries.ITEM.getKey(max[0].getItem());
        }

        public void measure(TestContext c) {
            c.check("sulfur macerates to 4-5 sulfur dust", macerate(Items.SULFUR).equals("4..5 eln:sulfur_dust"), macerate(Items.SULFUR));
            c.check("sulfur bricks macerate like sulfur", macerate(Items.SULFUR_BRICKS).equals("4..5 eln:sulfur_dust"), macerate(Items.SULFUR_BRICKS));
            c.check("a sulfur spike macerates to 1 dust", macerate(Items.SULFUR_SPIKE).equals("1..1 eln:sulfur_dust"), macerate(Items.SULFUR_SPIKE));
            c.check("potent sulfur macerates to 36 dust", macerate(Items.POTENT_SULFUR).equals("36..36 eln:sulfur_dust"), macerate(Items.POTENT_SULFUR));
            c.check("sulfur slabs don't macerate (no dupes)", macerate(Items.SULFUR_SLAB).equals("none"), macerate(Items.SULFUR_SLAB));
            c.check("soul sand macerates to 2 soul dust", macerate(Items.SOUL_SAND).equals("2..2 eln:soul_dust"), macerate(Items.SOUL_SAND));
            c.check("soul soil macerates to 2 soul dust", macerate(Items.SOUL_SOIL).equals("2..2 eln:soul_dust"), macerate(Items.SOUL_SOIL));

            int latex = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:latex");
            c.check("latex is a fluid species, named by the latex fluid", latex >= 0
                && mods.eln.fluid.FluidSpecies.of(mods.eln.oil.Latex.SOURCE.get()) != null, "id " + latex);
            Item resin = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "tree_resin"));
            c.check("resin smelts to raw rubber", smelt(c, resin).equals("1 eln:raw_rubber"), smelt(c, resin));
            c.check("raw rubber smelts to 1 rubber", smelt(c, ElnRegistry.RAW_RUBBER.get()).equals("1 eln:rubber"), smelt(c, ElnRegistry.RAW_RUBBER.get()));
            c.check("rubber compound doesn't smelt in a plain furnace", smelt(c, ElnRegistry.RUBBER_COMPOUND.get()).equals("none"), smelt(c, ElnRegistry.RUBBER_COMPOUND.get()));
            var h = mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.treatmentFor(new ItemStack(ElnRegistry.RUBBER_COMPOUND.get()));
            var furnace = mods.eln.Eln.transparentNodeItem.getDescriptor(0 + (32 << 6));
            var inSlot = mods.eln.gui.MenuSpecs.of(furnace).slots().stream()
                .filter(sl -> sl.index() == mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.inSlotId).findFirst();
            c.check("the Electrical Furnace's input slot takes Rubber Compound (AdventurAgent: it refused it)",
                inSlot.isPresent() && inSlot.get().filter().test(new ItemStack(ElnRegistry.RUBBER_COMPOUND.get())), String.valueOf(inSlot.map(SlotSpecName::of)));
            c.check("rubber compound vulcanises to 8 rubber in the Electrical Furnace", h != null && h.out().get().getCount() == 8
                && h.out().get().is(mods.eln.Eln.findItemStack("Rubber").getItem()), String.valueOf(h));

            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 4; i++) grid.add(new ItemStack(ElnRegistry.RAW_RUBBER.get()));
            grid.add(new ItemStack(ElnRegistry.SULFUR_DUST.get()));
            grid.add(new ItemStack(ElnRegistry.SOUL_DUST.get()));
            for (int i = 0; i < 3; i++) grid.add(ItemStack.EMPTY);
            var input = CraftingInput.of(3, 3, grid);
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, c.level());
            String got = r.map(rh -> rh.value().assemble(input).toString()).orElse("none");
            c.check("4 raw rubber + sulfur dust + soul dust make a rubber compound",
                r.isPresent() && r.get().value().assemble(input).is(ElnRegistry.RUBBER_COMPOUND.get()), got);
            c.check("the macerator gives off souls for soul sand, not for sulfur",
                mods.eln.transparentnode.electricalmachine.MaceratorDescriptor.isSoulBlock(new ItemStack(Blocks.SOUL_SAND))
                    && !mods.eln.transparentnode.electricalmachine.MaceratorDescriptor.isSoulBlock(new ItemStack(Blocks.SULFUR)), "");
        }
    }
}
