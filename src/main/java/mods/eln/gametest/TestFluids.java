package mods.eln.gametest;

import mods.eln.Eln;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Test-only fuel: eln:gasoline (source, flowing, bucket), registered only in GameTest runs (system property
 * eln.testfluids). The 1.12 lab server had fuel mods; the 26.x test server has none, and FuelRegistry matches fluids by
 * registry path, so this stands in for Buildcraft's "fuel" & co. in the fuel generator / gas turbine / fuel furnace cases.
 */
public final class TestFluids {
    private TestFluids() {
    }

    static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Eln.MODID);
    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Eln.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eln.MODID);

    static final DeferredHolder<FluidType, FluidType> GASOLINE_TYPE = TYPES.register("gasoline",
        () -> new FluidType(FluidType.Properties.create()));
    static DeferredHolder<Fluid, FlowingFluid> GASOLINE;
    static DeferredHolder<Fluid, FlowingFluid> GASOLINE_FLOWING;
    static DeferredItem<Item> GASOLINE_BUCKET;

    private static BaseFlowingFluid.Properties props() {
        return new BaseFlowingFluid.Properties(GASOLINE_TYPE, GASOLINE, GASOLINE_FLOWING).bucket(GASOLINE_BUCKET);
    }

    public static boolean enabled() {
        return Boolean.getBoolean("eln.testfluids");
    }

    public static void register(IEventBus modBus) {
        GASOLINE = FLUIDS.register("gasoline", () -> new BaseFlowingFluid.Source(props()));
        GASOLINE_FLOWING = FLUIDS.register("flowing_gasoline", () -> new BaseFlowingFluid.Flowing(props()));
        GASOLINE_BUCKET = ITEMS.registerItem("gasoline_bucket",
            p -> new BucketItem(GASOLINE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
        TYPES.register(modBus);
        FLUIDS.register(modBus);
        ITEMS.register(modBus);
    }
}
