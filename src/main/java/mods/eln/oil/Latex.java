package mods.eln.oil;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Latex (Baughn 2026-11-07, the rubber chain): rubber trees' milky sap, tapped by the Industrial Resin Collector and
 * pressed from dandelions in the 230V compressor, which also presses it to Raw Rubber. Species eln:latex (its own group,
 * so it doesn't mix with water; it never boils). Registered with the oil fluids' registers but not as a column cut
 * (the column indexes Oil.Cut).
 */
public final class Latex {
    private Latex() {
    }

    /** ARGB, the fluid's tint (bucket, pipes). */
    public static final int COLOR = 0xFFF2EEDC;
    /** A resin's worth of latex (mB): one Raw Rubber. */
    public static final int MB_PER_RUBBER = 250;

    public static DeferredHolder<FluidType, FluidType> TYPE;
    public static DeferredHolder<Fluid, FlowingFluid> SOURCE, FLOWING;
    public static DeferredItem<Item> BUCKET;

    static BaseFlowingFluid.Properties props() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).bucket(BUCKET);
    }

    static void register() {
        TYPE = Oil.TYPES.register("latex", () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.eln.latex")
            .density(980).viscosity(10000)));
        SOURCE = Oil.FLUIDS.register("latex", () -> new BaseFlowingFluid.Source(props()));
        FLOWING = Oil.FLUIDS.register("flowing_latex", () -> new BaseFlowingFluid.Flowing(props()));
        BUCKET = Oil.ITEMS.registerItem("latex_bucket", p -> new BucketItem(SOURCE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(BUCKET);
    }
}
