package mods.eln.fluid

import mods.eln.init.Config.fuelHeatValueFactor
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry

object FuelRegistry {
    /**
     * Diesel is a refined, heavy fuel, can only be used by the fuel heat furnace for the moment.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     */
    private val dieselFuels = mapOf(
        "biodiesel" to 32560000.0,  // Immersive Engineering, density = 0.88 kg/l, heating value = 37 MJ/kg
        "heavyoil" to 39100000.0,   // Magneticraft, heating value = 39.1 MJ/l
        "diesel" to 39100000.0,     // Immersive Petrolium and Pneumaticraft, heating value ~ 38.6-39.6 MJ/L
        "creosote" to 750000.0      // Railcraft, heating value 7.5 MJ/l
    )
    val dieselList = dieselFuels.keys.toTypedArray()

    /**
     * Gasoline-equivalents: Light oils, the type which can reasonably be burned by internal combustion engines or gas turbines.
     * The ones on this list are all pretty close to each other in energy content.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     */
    private val gasolineFuels = mapOf(
        "fuel" to 31570000.0, // Buildcraft, density = 0.77 kg/l, heating value = 41 MJ/kg
        "rc ethanol" to 21172000.0, // RotaryCraft, density = 0,79 kg/l, heating value = 26.8 MJ/kg
        "biofuel" to 17826480.0, // Minefactory Reloaded, (Bioethanol) density = 0.786 kg/l, heating value = 22,68 MJ/l
        "bioethanol" to 17826480.0, // Forestry
        "gasoline" to 25820000.0, // PneumaticCraft, density = 0.755 kg/L, heat value = 34,2 MJ/l
        "kerosene" to 34800000.0, // PneumaticCraft, heat value = 34,8 MJ/l
        "lpg" to 24840000.0, // PneumaticCraft, density = 0.54 kg/l, heat value = 46 MJ/kg
        "fuelgc" to 31570000.0, // GalactiCraft, see "fuel"
        "lightoil" to 35358000.0    // Magneticraft, density = 0.83 kg/l, heating value = 42.6 MJ/kg
    )
    val gasolineList = gasolineFuels.keys.toTypedArray()

    /**
     * Burnable gases. Gas turbine is still happy, fuel generator is not.
     *
     * The values represent the heating value (energy) for 1L of the fuel IRL.
     */
    private val gasFuels = mapOf(
        "naturalgas" to 36000.0, // Magneticraft, heating value = 36 MJ/m3
        "syngas" to 20000.0         // Advanced Generators, heating value = 20 MJ/m3
    ).mapValues {
        // Multiplied by 1000 because Minecraft gases are -- heavily pressurized.
        it.value * 1000
    }
    val gasList = gasFuels.keys.toTypedArray()

    /**
     * Steam. The value represents the heating value (energy) for 1L of steam IRL.
     *
     * We assume a density of 1g/L to harmonize with other mods.
     */
    private val steam = mapOf(
        "steam" to 2.257,  // Heat of vaporization: 2.257 J/g
        "ic2steam" to 2.257  // Ditto, this is still steam. IC2 doesn't want to use other mods' steam.
    ).mapValues {
        // Unusually, the commonly accepted value (2.2) is pretty much correct. Undo the usual mapping.
        it.value / fuelHeatValueFactor
    }
    val steamList = steam.keys.toTypedArray()

    // All fuels together.
    private val allFuels = dieselFuels + gasolineFuels + gasFuels + steam

    fun fluidListToFluids(fluidNames: Array<String>) =
        fluidNames.map { FluidRegistry.getFluid(it) }.filterNotNull().toTypedArray()

    fun heatEnergyPerMilliBucket(fuelName: String): Double = fuelHeatValueFactor * (allFuels[fuelName] ?: 0.0)
    fun heatEnergyPerMilliBucket(fluid: Fluid?): Double = heatEnergyPerMilliBucket(fluid?.name ?: "")
}
