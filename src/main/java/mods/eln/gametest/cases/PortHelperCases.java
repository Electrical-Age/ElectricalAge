package mods.eln.gametest.cases;

import mods.eln.compat.BlockMeta;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Checks of the 26.x replacements for 1.12 world APIs that no device case exercises directly (26.x port, no 1.12
 * counterpart): fuel burn times from the COOKING_FUEL component, smelting through the recipe manager, and the eln:light
 * block (META level, timeouts, self-removal, cave air).
 */
public final class PortHelperCases {
    private PortHelperCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FuelSmeltCase());
        cases.add(new LightCase());
        cases.add(new FluidCase());
        cases.add(new WaterloggedCableCase());
    }

    /** 26.x (Baughn): a six-node device placed in a water source is waterlogged; removing it leaves the water. */
    static final class WaterloggedCableCase implements TestCase {
        public String id() {
            return "port26_waterlogged_cable";
        }

        public int width() {
            return 3;
        }

        mods.eln.node.six.SixNodeElement wet, dry;

        public void build(TestContext ctx) {
            BlockPos w = ctx.at(0, 1, 0), d = ctx.at(2, 1, 0);
            ctx.world().setBlockAndUpdate(w, Blocks.WATER.defaultBlockState());
            wet = ctx.placeSix(4 + (32 << 6), w);   // Low Voltage Cable
            dry = ctx.placeSix(4 + (32 << 6), d);
        }

        public void measure(TestContext ctx) {
            BlockPos w = ctx.at(0, 1, 0), d = ctx.at(2, 1, 0);
            var ws = ctx.world().getBlockState(w);
            var dryState = ctx.world().getBlockState(d);
            var prop = mods.eln.node.six.SixNodeBlock.WATERLOGGED;
            ctx.check("cable in water: waterlogged, water fluid", ws.hasProperty(prop) && ws.getValue(prop)
                && ctx.world().getFluidState(w).is(net.minecraft.world.level.material.Fluids.WATER), String.valueOf(ws));
            ctx.check("cable on dry ground: not waterlogged", dryState.hasProperty(prop) && !dryState.getValue(prop), String.valueOf(dryState));
            ctx.world().destroyBlock(w, false);
            ctx.check("removed: the water stays", ctx.world().getBlockState(w).is(Blocks.WATER), String.valueOf(ctx.world().getBlockState(w)));
        }
    }

    /** 1.12 burn times: coal 1600 ticks (80 kJ), lava bucket 20000; cobblestone smelts to stone, dirt to nothing. */
    static final class FuelSmeltCase implements TestCase {
        public String name() {
            return "port26 fuel and smelting";
        }

        public int width() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            int coal = Utils.burnTime(new ItemStack(Items.COAL));
            int lava = Utils.burnTime(new ItemStack(Items.LAVA_BUCKET));
            int dirt = Utils.burnTime(new ItemStack(Items.DIRT));
            ctx.check("burn time coal = 1600, lava bucket = 20000, dirt = 0", coal == 1600 && lava == 20000 && dirt == 0,
                "coal " + coal + ", lava " + lava + ", dirt " + dirt);
            ctx.checkValue("fuel energy of coal [J] (80 kJ)", Utils.getItemEnergie(new ItemStack(Items.COAL)), 80000.0);
            ItemStack stone = Utils.smeltResult(ctx.world(), new ItemStack(Items.COBBLESTONE));
            ItemStack none = Utils.smeltResult(ctx.world(), new ItemStack(Items.DIRT));
            ctx.check("smelting: cobblestone -> stone, dirt -> nothing", stone.getItem() == Items.STONE && none.isEmpty(),
                "cobblestone -> " + stone + ", dirt -> " + none);
        }
    }

    /**
     * Three lights at build: level 10 for 1000 ticks in air, level 7 for 5 ticks in air, level 12 for 1000 ticks in
     * cave air. After the run: the first is an eln:light with META 10 and block light 10, the second removed itself,
     * the third lit the cave air (1.12 only knew Blocks.AIR).
     */
    static final class LightCase implements TestCase {
        BlockPos lasting, shortLived, cave;

        public String name() {
            return "port26 light block";
        }

        public int width() {
            return 5;
        }

        public void build(TestContext ctx) {
            lasting = ctx.at(0, 2, 1);
            shortLived = ctx.at(2, 2, 1);
            cave = ctx.at(4, 2, 1);
            ctx.world().setBlock(cave, Blocks.CAVE_AIR.defaultBlockState(), 3);
            LightBlockEntity.addLight(new Coordonate(lasting, ctx.world()), 10, 1000);
            LightBlockEntity.addLight(new Coordonate(shortLived, ctx.world()), 7, 5);
            LightBlockEntity.addLight(new Coordonate(cave, ctx.world()), 12, 1000);
        }

        public void measure(TestContext ctx) {
            BlockState a = ctx.world().getBlockState(lasting);
            int meta = a.hasProperty(BlockMeta.META) ? a.getValue(BlockMeta.META) : -1;
            int light = ctx.world().getBrightness(LightLayer.BLOCK, lasting);
            ctx.check("light block: eln:light, META 10, block light 10", a.getBlock() == ElnDeviceRegistry.lightBlock && meta == 10 && light == 10,
                a + ", block light " + light);
            BlockState b = ctx.world().getBlockState(shortLived);
            ctx.check("light block: removed itself after its timeout", b.isAir(), String.valueOf(b));
            BlockState c = ctx.world().getBlockState(cave);
            ctx.check("light block: placed in cave air too", c.getBlock() == ElnDeviceRegistry.lightBlock, String.valueOf(c));
            // leave the box clean
            ctx.world().removeBlock(lasting, false);
            ctx.world().removeBlock(cave, false);
        }
    }

    /**
     * ElementFluidHandler on the transfer API: simulate vs commit, whitelist, drain, NBT round trip; and the fuel heat
     * furnace's tank reachable as the block's fluid capability (what pipes use).
     */
    static final class FluidCase implements TestCase {
        mods.eln.node.transparent.TransparentNodeElement furnace;

        public String name() {
            return "port26 fluid handler";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            furnace = ctx.placeTransparent(Ids.transparent("Fuel Heat Furnace"), ctx.at(1, 1, 1));
        }

        public void measure(TestContext ctx) {
            mods.eln.fluid.ElementFluidHandler h = new mods.eln.fluid.ElementFluidHandler(1000);
            net.neoforged.neoforge.fluids.FluidStack water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 600);
            int sim = h.fill(water, false);
            int simAmount = h.getFluidAmount();
            int put = h.fill(water, true);
            int put2 = h.fill(water, true);
            ctx.check("fluid tank: simulate 600 (tank stays 0), fill 600, then 400 more (capacity 1000)",
                sim == 600 && simAmount == 0 && put == 600 && put2 == 400 && h.getFluidAmount() == 1000,
                "sim " + sim + "/" + simAmount + ", fill " + put + " + " + put2 + " -> " + h.getFluidAmount());
            net.neoforged.neoforge.fluids.FluidStack out = h.drain(250, true);
            ctx.check("fluid tank: drain 250 water", out.getAmount() == 250 && out.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                && h.getFluidAmount() == 750, out + ", left " + h.getFluidAmount());
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            h.writeToNBT(nbt, "t");
            mods.eln.fluid.ElementFluidHandler h2 = new mods.eln.fluid.ElementFluidHandler(1000);
            h2.readFromNBT(nbt, "t");
            ctx.check("fluid tank: NBT round trip", h2.getFluidAmount() == 750 && h2.getFluid().getFluid() == net.minecraft.world.level.material.Fluids.WATER,
                String.valueOf(h2.getFluid()));
            mods.eln.fluid.ElementFluidHandler lavaOnly = new mods.eln.fluid.ElementFluidHandler(1000);
            lavaOnly.setFilter(new net.minecraft.world.level.material.Fluid[]{net.minecraft.world.level.material.Fluids.LAVA});
            ctx.check("fluid tank: a lava-only filter refuses water", lavaOnly.fill(water, true) == 0 && lavaOnly.getFluidAmount() == 0, "");
            Object cap = ctx.world().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, ctx.at(1, 1, 1), net.minecraft.core.Direction.UP);
            ctx.check("fuel heat furnace exposes its tank as the fluid capability", cap != null && cap == furnace.getFluidHandler(), String.valueOf(cap));
        }
    }
}
