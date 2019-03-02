package mods.eln.init

import com.teamwizardry.librarianlib.features.config.ConfigDoubleRange
import com.teamwizardry.librarianlib.features.config.ConfigNeedsFullRestart
import com.teamwizardry.librarianlib.features.config.ConfigNeedsWorldRestart
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import mods.eln.misc.Utils
import java.util.*

// TODO(1.12): Go through these and mark them as not needing a restart, if possible.
object Config {
    /* Sound */
    @ConfigProperty("sound", configId="eln/client")
    @ConfigNeedsFullRestart
    var maxSoundDistance = 16.0

    /* Balancing */
    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var heatTurbinePowerFactor = 1.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var solarPanelPowerFactor = 1.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var windTurbinePowerFactor = 1.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var waterTurbinePowerFactor = 1.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var fuelGeneratorPowerFactor = 1.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var fuelGeneratorTankCapacityInSeconds = 20.0 * 60.0

    @ConfigProperty("balancing", configId="eln/common")
    @ConfigNeedsFullRestart
    var fuelHeatFurnacePowerFactor = 1.0

    @ConfigProperty("balancing", "Correction factor for the energy content of liquid fuels.", configId="eln/common")
    @ConfigNeedsFullRestart
    var fuelEnergyContentFactor = 1.0
    val fuelHeatValueFactor get() = fuelEnergyContentFactor * 0.0000675

    @ConfigProperty("balancing", "Maximum radius of the autominer. It mines at a 45 degree angle at most.", configId="eln/common")
    var autominerRange = 24

    @ConfigProperty("balancing", configId="eln/common")
    var plateConversionRatio = 1

    @ConfigProperty("balancing", "How many days it takes for a battery to decay half-way.", configId="eln/common")
    var batteryHalfLifeDays = 2.0
    val stdBatteryHalfLife get() = batteryHalfLifeDays * Utils.minecraftDay

    @ConfigProperty("balancing", configId="eln/common")
    var batteryCapacityFactor = 1.0


    /* Signals */
    @ConfigProperty("signals", configId="eln/common")
    var wirelessTxRange = 32


    /* Compatibility */
    @ConfigProperty("compatibility", configId="eln/common")
    var elnToIc2ConversionRatio = 1.0 / 3.0

    @ConfigProperty("compatibility", configId="eln/common")
    var elnToOcConversionRatio = elnToIc2ConversionRatio / 2.5

    @ConfigProperty("compatibility", configId="eln/common")
    var elnToTeConversionRatio = elnToIc2ConversionRatio * 4.0

    @ConfigProperty("compatibility", configId="eln/common")
    @ConfigNeedsFullRestart
    var computerProbeEnable = true

    @ConfigProperty("compatibility", configId="eln/common")
    @ConfigNeedsFullRestart
    var elnToOtherEnergyConverterEnable = true

    @ConfigProperty("compatibility", configId="eln/common")
    @ConfigNeedsFullRestart
    var oredictChips = true
    val dictCheapChip get() = if (oredictChips) "circuitBasic" else "circuitElnBasic"
    val dictAdvancedChip get() = if (oredictChips) "circuitAdvanced" else "circuitElnAdvanced"


    /* X-ray scanner */
    @ConfigProperty("xray", configId="eln/common")
    var scannerRange = 10.0

    @ConfigProperty("xray", "If false, render only vanilla and Eln ores.", configId="eln/common")
    @ConfigNeedsFullRestart
    var addOtherModOreToXRay = true


    /* Difficulty */
    @ConfigProperty("difficulty", "If true, overloaded blocks explode instead of dropping as items.", configId="eln/common")
    var explosionEnable = false

    @ConfigProperty("difficulty", "Spawn replicators on lightning strikes?", configId="eln/common")
    var replicatorSpawn = true

    @ConfigProperty("difficulty", configId="eln/common")
    var replicatorSpawnPerSecondPerPlayer = 1.0 / 120.0

    @ConfigProperty("difficulty", configId="eln/common")
    var wailaEasyMode = false

    @ConfigProperty("difficulty", "Multiplication factor for cable capacity. We recommend 1.5 to 2.0 for larger packs, but no higher.", configId="eln/common")
    @ConfigDoubleRange(0.1, 4.0)
    var cablePowerFactor = 1.0


    /* Lamps */
    @ConfigProperty("lamps", configId="eln/common")
    var incandescentLampLifeInHours = 16.0

    @ConfigProperty("lamps", configId="eln/common")
    var economicLampLifeInHours = 64.0

    @ConfigProperty("lamps", configId="eln/common")
    var carbonLampLifeInHours = 6.0

    @ConfigProperty("lamps", configId="eln/common")
    var ledLampLifeInHours = 512.0

    @ConfigProperty("lamps", configId="eln/common")
    var ledLampInfiniteLife = false


    /* Map generation */
    @ConfigProperty("mapgen", configId="eln/common")
    @ConfigNeedsFullRestart
    var forceOreRegen = false

    @ConfigProperty("mapgen", configId="eln/common")
    @ConfigNeedsFullRestart
    var generateCopper = true

    @ConfigProperty("mapgen", configId="eln/common")
    @ConfigNeedsFullRestart
    var generateLead = true

    @ConfigProperty("mapgen", configId="eln/common")
    @ConfigNeedsFullRestart
    var generateTungsten = true


    /* Modbus */
    @ConfigProperty("modbus", configId="eln/common")
    @ConfigNeedsWorldRestart
    var modbusEnable = false

    @ConfigProperty("modbus", configId="eln/common")
    @ConfigNeedsWorldRestart
    var modbusPort = 1502


    /* Simulation */
    @ConfigProperty("simulation", "Do not change these settings unless you know what you're doing.", configId="eln/common")
    var electricalFrequency = 20.0

    @ConfigProperty("simulation", "Do not change these settings unless you know what you're doing.", configId="eln/common")
    var electricalInterSystemOverSampling = 50

    @ConfigProperty("simulation", "Do not change these settings unless you know what you're doing.", configId="eln/common")
    var thermalFrequency = 400.0


    /* Debugging */
    @ConfigProperty("debugging", configId="eln/common")
    var debugEnable = false


    /* Analytics */
    @ConfigProperty("analytics", configId="eln/client")
    @ConfigNeedsFullRestart
    var analyticsEnabled = true

    @ConfigProperty("analytics", configId="eln/client")
    var playerUUID = UUID.randomUUID().toString()
}
