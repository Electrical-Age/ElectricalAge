package mods.eln.fluid

import net.minecraftforge.fluids.FluidRegistry

class FuelRegistry {
    companion object {

    /**
     * Diesel is a refined, heavy fuel, and not usable anywhere yet.
     */
    val dieselList = arrayOf(
            "biodiesel", // Immersive Engineering
            "heavyoil"  // Magneticraft
    )

    /**
     * Gasoline-equivalents: Light oils, the type which can reasonably be burned by internal combustion engines or gas turbines.
     * The ones on this list are all pretty close to each other in energy content.
     */
    val gasolineList = arrayOf(
            // Various gasoline equivalents, and light oils a small turbine can reasonably burn.
            "syngas", // Advanced Generators
            "fuel", // Buildcraft
            "rc ethanol", // RotaryCraft
            "biofuel", // Minefactory Reloaded
            "bioethanol", // Forestry
            "kerosene", // PneumaticCraft
            "lpg", // PneumaticCraft
            "fuelgc", // GalactiCraft
            "lightoil"  // Magneticraft
    )

    /**
     * Burnable gases. Gas turbine is still happy, fuel generator is not.
     */
    val gasList = arrayOf(
            "naturalgas"  // Magneticraft
    )

    fun fluidListToFluids(fluidNames: Array<String>) =
            fluidNames.map { FluidRegistry.getFluid(it) }.filterNotNull().toTypedArray()
    }
}
