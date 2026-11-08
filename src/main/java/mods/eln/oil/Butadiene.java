package mods.eln.oil;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Butadiene (the rubber chain's oil route, 2026-11-07): the Steam Cracker's product, which the Polymeriser makes into
 * Raw Rubber. Species eln:butadiene (LabAgent: a real condensable, boiling at -4.4 °C with ~2.4 bar over it at 20 °C, so
 * it runs liquid in pipes at their 6 bar), as two NeoForge fluids: eln:butadiene the liquid and eln:butadiene_gas the
 * vapour. No bucket (it boils off at 1 atm) and no fuel: a feedstock.
 */
public final class Butadiene {
    private Butadiene() {
    }

    /** ARGB, the fluids' tint (pipes, tanks). */
    public static final int COLOR = 0xFFD8D0E8;

    public static DeferredHolder<FluidType, FluidType> TYPE, GAS_TYPE;
    public static DeferredHolder<Fluid, FlowingFluid> SOURCE, FLOWING, GAS, GAS_FLOWING;

    static void register() {
        TYPE = Oil.TYPES.register("butadiene", () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.eln.butadiene")
            .density(615).viscosity(200)));
        GAS_TYPE = Oil.TYPES.register("butadiene_gas", () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.eln.butadiene_gas")
            .density(-300).viscosity(10)));
        SOURCE = Oil.FLUIDS.register("butadiene", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING)));
        FLOWING = Oil.FLUIDS.register("flowing_butadiene", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING)));
        GAS = Oil.FLUIDS.register("butadiene_gas", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(GAS_TYPE, GAS, GAS_FLOWING)));
        GAS_FLOWING = Oil.FLUIDS.register("flowing_butadiene_gas", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(GAS_TYPE, GAS, GAS_FLOWING)));
    }
}
