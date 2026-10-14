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

    public static final DeferredHolder<FluidType, FluidType> CRUDE_TYPE = TYPES.register("crude_oil", () -> new CrudeType(
        FluidType.Properties.create().descriptionId("block.eln.crude_oil").density(850).viscosity(6000).temperature(290)
            .canExtinguish(false).canConvertToSource(false).supportsBoating(true).canSwim(true).canDrown(true)
            .motionScale(0.003).fallDistanceModifier(0f)));

    /**
     * Crude's movement for entities: NeoForge leaves custom fluids to say how one moves in them (without this, a
     * player in a lake couldn't move at all: AdventurAgent). Like a thick water: slow, sinking slowly, with a jump
     * out at the shore.
     */
    static final class CrudeType extends FluidType {
        CrudeType(FluidType.Properties p) {
            super(p);
        }

        @Override
        public boolean move(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.phys.Vec3 input, double gravity) {
            double oldY = entity.getY();
            entity.moveRelative(0.015f, input);
            entity.move(net.minecraft.world.entity.MoverType.SELF, entity.getDeltaMovement());
            var m = entity.getDeltaMovement().multiply(0.6, 0.7, 0.6);
            if (gravity != 0) m = m.add(0, -gravity / 6, 0);
            entity.setDeltaMovement(m);
            // climb out at the shore, as in water
            var d = entity.getDeltaMovement();
            if (entity.horizontalCollision && entity.isFree(d.x, d.y + 0.6 - entity.getY() + oldY, d.z))
                entity.setDeltaMovement(d.x, 0.3, d.z);
            return true;
        }
    }

    public static DeferredHolder<Fluid, FlowingFluid> CRUDE;
    public static DeferredHolder<Fluid, FlowingFluid> CRUDE_FLOWING;
    public static DeferredBlock<LiquidBlock> CRUDE_BLOCK;
    public static DeferredItem<Item> CRUDE_BUCKET;
    public static DeferredHolder<Fluid, ReservoirOilFluid> RESERVOIR_OIL;
    /** Steam (FuelRegistry: "steam", 2.257 J/mB), for EA's steam turbine; made by the boiler. No block, no bucket. */
    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = TYPES.register("steam", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid.eln.steam").density(-500).viscosity(200).temperature(373)));
    public static DeferredHolder<Fluid, FlowingFluid> STEAM;
    public static DeferredHolder<Fluid, FlowingFluid> STEAM_FLOWING;
    public static DeferredBlock<ReservoirOilBlock> RESERVOIR_BLOCK;

    /**
     * The fractionating column's cuts (FuelRegistry names, so EA's generators, gas turbine and fuel furnace burn them;
     * residue is no fuel). Ordinary NeoForge fluids without a world block; all but the gas come in buckets.
     */
    public enum Cut {
        NATURALGAS("naturalgas", "Natural Gas", -300, 10, false, 0xFFD8E4F0),
        GASOLINE("gasoline", "Gasoline", 740, 600, true, 0xFFF4E27A),
        DIESEL("diesel", "Diesel", 840, 3000, true, 0xFFC8862A),
        HEAVYOIL("heavyoil", "Heavy Oil", 950, 20000, true, 0xFF6B3A1C),
        RESIDUE("residue", "Residue", 1050, 100000, true, 0xFF2A2826);

        public final String id, display;
        final int density, viscosity;
        public final boolean bucket;
        /** ARGB, for the fluid's texture tint (bucket, pipes). */
        public final int color;
        public DeferredHolder<FluidType, FluidType> type;
        public DeferredHolder<Fluid, FlowingFluid> source, flowing;
        public DeferredItem<Item> bucketItem;

        Cut(String id, String display, int density, int viscosity, boolean bucket, int color) {
            this.id = id;
            this.display = display;
            this.density = density;
            this.viscosity = viscosity;
            this.bucket = bucket;
            this.color = color;
        }

        BaseFlowingFluid.Properties props() {
            var p = new BaseFlowingFluid.Properties(type, source, flowing);
            return bucket ? p.bucket(bucketItem) : p;
        }

        void register() {
            type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.eln." + id)
                .density(density).viscosity(viscosity)));
            source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(props()));
            flowing = FLUIDS.register("flowing_" + id, () -> new BaseFlowingFluid.Flowing(props()));
            if (bucket) bucketItem = ITEMS.registerItem(id + "_bucket", p -> new BucketItem(source.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
        }
    }

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
        STEAM = FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, STEAM_FLOWING)));
        STEAM_FLOWING = FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, STEAM_FLOWING)));
        for (Cut c : Cut.values()) c.register();
        RESERVOIR_BLOCK = BLOCKS.registerBlock("reservoir_oil", ReservoirOilBlock::new, p -> liquidProps(p));
        OilWorldgen.register(modBus);
        TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        Reservoirs.register();
    }
}
