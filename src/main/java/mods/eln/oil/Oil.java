package mods.eln.oil;

import mods.eln.Eln;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Oil (26.x, design: notes/oil/DESIGN-v2.md, Baughn-approved). Crude oil as an ordinary NeoForge fluid (bucket, source,
 * flowing), and reservoir oil: the still, level 1-8 fluid underground lakes are made of, a view of a Reservoir's pooled
 * volume (ReservoirData) so the whole surface moves together.
 */
public final class Oil {
    private Oil() {
    }

    static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Eln.MODID);
    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Eln.MODID);
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eln.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eln.MODID);

    public static final DeferredHolder<FluidType, FluidType> CRUDE_TYPE = TYPES.register("crude_oil", () -> new FluidType(
        FluidType.Properties.create().descriptionId("block.eln.crude_oil").density(850).viscosity(6000).temperature(290)
            .canExtinguish(false).canConvertToSource(false).supportsBoating(true).canSwim(true).canDrown(true)
            .motionScale(0.003).fallDistanceModifier(0f)));

    public static DeferredHolder<Fluid, FlowingFluid> CRUDE;
    public static DeferredHolder<Fluid, FlowingFluid> CRUDE_FLOWING;
    public static DeferredBlock<LiquidBlock> CRUDE_BLOCK;
    public static DeferredItem<Item> CRUDE_BUCKET;
    public static DeferredHolder<Fluid, ReservoirOilFluid> RESERVOIR_OIL;
    public static DeferredBlock<ReservoirOilBlock> RESERVOIR_BLOCK;

    private static BaseFlowingFluid.Properties crudeProps() {
        return new BaseFlowingFluid.Properties(CRUDE_TYPE, CRUDE, CRUDE_FLOWING).bucket(CRUDE_BUCKET).block(CRUDE_BLOCK)
            .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30);
    }

    static BlockBehaviour.Properties liquidProps(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.COLOR_BLACK).replaceable().noCollision().strength(100f).pushReaction(PushReaction.POPPED)
            .noLootTable().liquid().sound(SoundType.EMPTY);
    }

    public static void register(IEventBus modBus) {
        CRUDE = FLUIDS.register("crude_oil", () -> new BaseFlowingFluid.Source(crudeProps()));
        CRUDE_FLOWING = FLUIDS.register("flowing_crude_oil", () -> new BaseFlowingFluid.Flowing(crudeProps()));
        CRUDE_BLOCK = BLOCKS.registerBlock("crude_oil", p -> new LiquidBlock(CRUDE.get(), p), p -> liquidProps(p));
        CRUDE_BUCKET = ITEMS.registerItem("crude_oil_bucket", p -> new BucketItem(CRUDE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
        RESERVOIR_OIL = FLUIDS.register("reservoir_oil", ReservoirOilFluid::new);
        RESERVOIR_BLOCK = BLOCKS.registerBlock("reservoir_oil", ReservoirOilBlock::new, p -> liquidProps(p));
        TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        Reservoirs.register();
    }
}
