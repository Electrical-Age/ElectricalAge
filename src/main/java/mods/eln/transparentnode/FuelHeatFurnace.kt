package mods.eln.transparentnode

import mods.eln.network.ClientValues
import mods.eln.sound.LoopedSound

import mods.eln.gl.GL11

import mods.eln.Eln
import mods.eln.fluid.PreciseElementFluidHandler
import mods.eln.i18n.I18N
import mods.eln.item.FuelBurnerDescriptor
import mods.eln.item.regulator.IRegulatorDescriptor
import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.RegulatorProcess
import mods.eln.sim.ThermalLoadInitializerByPowerDrop
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.Failure
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

class FuelHeatFurnaceDescriptor(name: String, model: Obj3D, val thermal: ThermalLoadInitializerByPowerDrop) :
    TransparentNodeDescriptor(name, FuelHeatFurnaceElement::class.java, null) {
    private val obj = model
    private val main = model.getPart("main")
    /** The burner guns by FuelBurnerDescriptor.type: 0 big, 1 medium, 2 small. */
    private val burners = arrayOf(model.getPart("burnerbig"), model.getPart("burnermedium"), model.getPart("burnersmall"))
    private val powerLED = model.getPart("powerled")
    private val heatLED = model.getPart("heatled")
    private val glow = model.getPart("glow")
    private val glass = model.getPart("glass")

    init {
        thermal.setMaximalPower(2000.0)
        voltageLevelColor = VoltageLevelColor.Thermal
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addThermal(newItemStack())
    }

    /** True if the model has these parts (mods.eln.cad.models.FuelHeater; GameTest wp10b fuel heat furnace model). */
    fun hasParts(vararg names: String) = names.all { obj.getPart(it) != null }

    /**
     * The oil heater (mods.eln.cad.models.FuelHeater): the housing, the installed burner gun, the power and heat lamps,
     * then the flame behind the sight port, as high as the fire's level (0..1) and from dull orange at no power to bright
     * yellow-white at full (power 0..1, against the big burner), unlit, and the port's pane over it, see-through.
     */
    fun draw(installedBurner: Int? = null, on: Boolean = false, heating: Boolean = false, fire: Float = 0f, power: Float = 0f) {
        main?.draw()
        if (installedBurner != null) {
            burners.getOrNull(installedBurner)?.draw()
        }

        // (off: dark glass; it looked lit at half green, AdventurAgent)
        mods.eln.anim.Led.draw(powerLED, on, 0f, 1f, 0f, 0.05f, 0.18f, 0.05f)
        mods.eln.anim.Led.draw(heatLED, heating, 1f, 0f, 0f, 0.2f, 0.04f, 0.04f)

        if (glow != null && heating && fire > 0.01f) {   // (only a burning flame: not while it kindles, nor when burnt out)
            mods.eln.anim.Glow.draw({ mods.eln.anim.Fill.drawAbout(glow, WINDOW_Y0 / 16f - 0.5f, Math.min(1f, fire)) },
                power.coerceIn(0f, 1f), FLAME)
        }
        if (glass != null) {
            UtilsClient.enableBlend()
            GL11.glColor4f(1f, 1f, 1f, 0.3f)
            glass.draw()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            UtilsClient.disableBlend()
        }
    }


    override fun hasItemDraw() = true

    /** The item: a medium burner, off. */
    override fun drawItem(entity: Boolean) = draw(1)

    companion object {
        /** The foot of the sight port (px; mods.eln.cad.models.FuelHeater WIN_Y0): the flame is scaled about it. */
        const val WINDOW_Y0 = 6.5f
        /** The flame's colour by power (0..1), always self-lit. */
        val FLAME: mods.eln.anim.Glow.Ramp = mods.eln.anim.Glow.ramp({ k -> floatArrayOf(0.85f + 0.15f * k, 0.38f + 0.54f * k, 0.08f + 0.67f * k * k) }, -1f)
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Generates heat when supplied with fuel."))
        list.add(I18N.tr("Burns gasoline, diesel, heavy oil, residue, crude oil and piped fuel gas."))
        list.add(I18N.tr("Crude, residue and heavy oil coke its burner; clean fuel clears it. Wool cleans it."))
        list.add(Utils.plotCelsius(I18N.tr("  Max. temperature: "), thermal.warmLimit))
    }
}

class FuelHeatFurnaceElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(transparentNode, descriptor), mods.eln.sim.fluid.FluidDevice {
    companion object {
        val ExternalControlledToggleEvent: Byte = 0
        val MainSwitchToggleEvent: Byte = 1
        val SetManualControlValueEvent: Byte = 2
        val SetTemperatureEvent: Byte = 3
        /** The most a burner cokes: it still fires a tenth. */
        const val COKE_MAX = 0.9
        /**
         * The coking index (each species' `coking` by its share of what burns: residue 1, heavy oil 0.15, diesel 0.01,
         * light cuts 0) at which a burner neither cokes nor clears: a plausible refinery's fuel (Baughn 2026-11-11: on
         * better fuel "the ash gets pulled out with the smoke eventually"). Over it the burner cokes, under it it clears.
         */
        const val COKE_BALANCE = 0.05
        /** Coking per unit of index over the balance: dead crude (index 0.216) cokes at rate 1, as before. */
        const val COKE_PER_INDEX = 1 / (0.2159 - COKE_BALANCE)
        /**
         * Its fuel tank (mB, two buckets: Baughn 2026-11-03, it fills by bucket) and its volume (m^3): two buckets under
         * the view's 95 % fill, with head space over them (AdventurAgent 2026-11-11: a second bucket didn't fit).
         */
        const val TANK_MB = 2000.0
        const val TANK_FILL = 0.95
        val TANK_M3 = TANK_MB / TANK_FILL * 1.01 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / mods.eln.sim.fluid.Water.RHO_L
        /** The tank's height in the block (m) and its fuel flange, 4 px up (mods.eln.cad.models.FuelHeater). */
        const val TANK_H = 0.5
        const val FLANGE_Y = 4 / 16.0
        /** Its flange's conductance (as the oil devices' ports) and its breather's on top (small: a vent, not an opening). */
        const val PORT_K = 6e-11
        const val BREATHER_K = 1e-11
        /** The pressure difference (Pa) over which each breather vent starts to open, fully open at twice it. */
        const val BREATHER_DEADBAND_PA = 300.0
        /**
         * Pressure-fed burner (Baughn 2026-11-11): over the deadband, the tank's gauge pressure feeds the burner more fuel
         * and the air with it, firing 1 + gauge / OVERFIRE_PA times its setting (1 bar doubles it). Past its rating it
         * overheats and burns out, as before.
         */
        const val OVERFIRE_PA = 1e5
        /** Its relief valve on top (the breather's gas side): opens at RELIEF_PA gauge, fully at 1.1 x; lets anything out. */
        const val RELIEF_PA = 3e5
        // (as its flange: a gas line on the flange holds it near 3 bar up to ~7 bar in the line; past ~13 bar it bursts)
        const val RELIEF_K = 6e-11
        /** The liquid share of the tank at which the gas vent's float has shut (it closes over the 2 % under it). */
        const val FLOAT_SHUT = 0.97
        /** It isn't built for pressure: past BURST_PA gauge the tank bursts. */
        const val BURST_PA = 6e5
        /**
         * Fuel gas escaping its vents past what the flame took, over this (kg/s, averaged over GAS_FIRE_S) with the fire
         * lit, sets fire around it: a puff doesn't, a stream does.
         */
        const val GAS_FIRE_KG_S = 5e-5
        const val GAS_FIRE_S = 2.0
        /** Heat to boil off a kg of water that reached the burner (J/kg: warming and evaporating). */
        const val WATER_BOIL_J_PER_KG = 2.6e6
        /** The burner's thermal mass per W of its rating (J/K/W) and how far it runs over the furnace at its rating (K). */
        const val BURNER_C_PER_W = 0.3
        const val BURNER_DT = 100.0
        /** The fuel's heat capacity (J/(kg K)), for its share of the burner's inertia. */
        const val FUEL_CP = 2000.0
        /** Drowned, the hot burner flashes this many seconds of its fire's heat of the water to steam out of its stack. */
        const val DROWN_FLASH_S = 5.0
    }

    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val controlLoad = NbtElectricalGateInput("commandLoad")

    /**
     * Its fuel tank, a fluid node (two buckets: Baughn 2026-11-03, it fills by bucket): its flange low on its front
     * (EA pipes, 4 px up) and buckets put any liquid in; a breather on top lets air in as it burns and live crude's gas
     * out. What it holds burns species by species (heating_value): a mix burns as the mix it is (Baughn 2026-11-10:
     * "mixed on the machines").
     */
    var vessel: mods.eln.sim.fluid.FluidNode? = null
        private set
    /**
     * Its breather: a small float vent letting gas out (never liquid: a full tank shuts it) and a vacuum breaker letting
     * air in, each opening only past a small pressure difference, so a standing tank doesn't breathe its fuel away. Gas
     * fed in faster than the small vent passes (air or gas on its flange) raises the tank's pressure: the pressure-fed
     * burner. Over RELIEF_PA gauge its relief valve lets anything out (Baughn 2026-11-11: it isn't built for pressure).
     */
    private var breather: mods.eln.sim.fluid.FluidDevice.Vent? = null
    private var airIn: mods.eln.sim.fluid.FluidDevice.Vent? = null
    private var relief: mods.eln.sim.fluid.FluidDevice.Vent? = null
    private var loaded: DoubleArray? = null
    /** An old save's tank (a 2-bucket fluid tank) and its flange's stub, put into the vessel when it is made. */
    private var legacyTank: net.neoforged.neoforge.fluids.FluidStack? = null
    private var legacyStub: DoubleArray? = null

    /** Its fluid capability: a view of the vessel (buckets; other mods' pipes). */
    private val view = mods.eln.fluid.NodeFluidView(mods.eln.fluid.NodeFluidView.Spec().intake().liquidShare(TANK_FILL))

    /**
     * Heat drawn from the fuel and not yet given (J, after heatFurnaceFuelFactor): the burner takes fuel out of the vessel
     * between fluid steps (beforeFluidStep), the control process spends it.
     */
    private var bank = 0.0
    /** The heat the control process wanted over the last fluid step (J): what the next draw refills. */
    private var wanted = 0.0
    /** The coking index of what it burnt last (residue 1 ... light cuts 0). */
    private var cokingIndex = 0.0
    /** What is at the burner, as of the last fluid step: a fuel's species, water (it boils off), or -1 for nothing. */
    var atBurner = -1
        private set
    /** Meters (not saved): fuel the burner drew (kg) and heat it gave (J), since it was loaded. */
    var fuelBurntKg = 0.0
        private set
    /** Fuel gas from its head space the flame burnt (kg, since loaded). */
    var gasBurntKg = 0.0
        private set
    var heatGivenJ = 0.0
        private set
    /** Heat its flame made (J, since loaded): what it drew x heating value; the burner gives it on with a lag. */
    var flameJ = 0.0
        private set
    /** Its flue: smoke out of the flange on top of its stack, or up the stack (as the coal oven's). */
    private val flue = mods.eln.fluid.Flue("flue")
    private var smoking = false

    override fun fluidLevel(): Any = node!!.coordonate.dimention

    override fun fluidPorts(): List<mods.eln.sim.fluid.FluidDevice.FluidPort> {
        val v = vessel ?: return listOf()
        if (flue.node == null) return listOf()
        val pos = node!!.coordonate.pos().asLong()
        val f = front.toEnumFacing().get3DDataValue()
        return listOf(mods.eln.sim.fluid.FluidDevice.FluidPort(v, pos, f, FLANGE_Y / TANK_H, PORT_K,
            mods.eln.fluid.PortBridge.endSpot(front, front, 4, 8)), flue.port(pos))
    }

    override fun fluidVents(): List<mods.eln.sim.fluid.FluidDevice.Vent> {
        val out = breather ?: return listOf()
        val inn = airIn ?: return listOf()
        val rel = relief ?: return listOf(out, inn)
        return listOf(out, inn, rel)
    }

    /**
     * Set the breather's vents by the tank's pressure against the air's: the gas vent and the vacuum breaker each open
     * over BREATHER_DEADBAND_PA (the gas vent's float shuts it once the liquid nears the top), the relief over RELIEF_PA.
     */
    private fun setBreather(v: mods.eln.sim.fluid.FluidNode) {
        val dp = v.P() - mods.eln.sim.fluid.Water.P_ATM
        val float = Math.max(0.0, Math.min(1.0, (FLOAT_SHUT - v.liquidVolume() / v.volume) / 0.02))
        breather?.open = float * Math.max(0.0, Math.min(1.0, (dp - BREATHER_DEADBAND_PA) / BREATHER_DEADBAND_PA))
        relief?.open = Math.max(0.0, Math.min(1.0, (dp - RELIEF_PA) / (0.1 * RELIEF_PA)))
        airIn?.open = Math.max(0.0, Math.min(1.0, (-dp - BREATHER_DEADBAND_PA) / BREATHER_DEADBAND_PA))
    }

    override fun beforeFluidStep(dt: Double) {
        val amb = mods.eln.sim.Ambient.kelvin(node!!.coordonate)
        val v = vessel
        if (v != null) {
            view.sync(v, amb)
            burn(v)
            setBreather(v)
        }
        if (flue.step(if (isBurnt()) 0.0 else actualHeatPower, dt, amb)) needPublish()   // (burnt: its fire is out)
        smoking = flue.smoking
    }

    override fun afterFluidStep(dt: Double) {
        flue.afterStep(mods.eln.sim.Ambient.kelvin(node!!.coordonate))
        val v = vessel ?: return
        checkPressure(v, dt)
        view.show(v)
        atBurner = burnerLayer(v)
    }

    /**
     * What the burner draws: the bottom liquid layer's main species (water, a fuel, or -1 for nothing there). A layer of
     * less than a milligram (the last of the water boiled off) doesn't count.
     */
    private fun burnerLayer(v: mods.eln.sim.fluid.FluidNode): Int {
        for (l in 0 until v.layers()) {
            var best = -1
            var most = 0.0
            var total = 0.0
            for (id in 0 until v.speciesBound()) if (v.layerOf(id) == l) {
                total += v.liquid(id)
                if (v.liquid(id) > most) { best = id; most = v.liquid(id) }
            }
            if (total > 1e-6) return best
        }
        return -1
    }

    /**
     * The burner takes what the control process wanted over the last step out of the vessel's bottom layer, as heat into
     * the bank: kg = heat / (heatFurnaceFuelFactor x the layer's heating value), by drainLayer (every species in it by its
     * share). Water there boils off at the flame's cost and gives nothing.
     */
    private fun burn(v: mods.eln.sim.fluid.FluidNode) {
        if (burnerLayer(v) == mods.eln.sim.fluid.SpeciesRegistry.WATER) {
            drown(v)
            wanted = 0.0
            return
        }
        val need = wanted - bank
        wanted = 0.0
        if (need <= 0) return
        val f0 = mods.eln.ElnConfig.heatFurnaceFuelFactor
        // fuel gas in its head space burns first, as far as the flame takes it (Baughn 2026-11-11: "in small amounts
        // it'd burn up inside it"): live crude's gas, or gas piped in
        var rest = need
        val reg = mods.eln.sim.fluid.SpeciesRegistry.get()
        for (g in 2 until v.speciesBound()) {
            val hvg = reg.byId(g).heatingValue
            if (rest <= 0 || hvg <= 0 || !reg.byId(g).gas()) continue   // (gases, not a liquid fuel's vapour)
            val kg = Math.min(v.vapour(g), rest / f0 / hvg)
            if (kg <= 1e-12) continue
            mods.eln.sim.fluid.EaUnits.drainVapour(v, g, kg)
            bank += kg * hvg * f0
            gasBurntKg += kg
            rest -= kg * hvg * f0
        }
        if (rest <= 0) return
        val id = burnerLayer(v)
        if (id < 0) return
        val hv = layerHeatingValue(v, id)
        if (hv <= 0) return
        val f = mods.eln.ElnConfig.heatFurnaceFuelFactor
        val index = layerCoking(v, id)
        val got = mods.eln.sim.fluid.EaUnits.drainLayer(v, id, rest / f / hv)
        if (got <= 0) return
        bank += got * hv * f
        fuelBurntKg += got
        cokingIndex = index
    }

    /**
     * Water reached a burning burner (Baughn 2026-11-11: "a lot of steam out the exhaust, and likely a collapsed fire with
     * no way to restart it"): the fire goes out and its switch drops; the hot burner flashes a little of the water to steam
     * up its stack. It lights again only once the water is out of the tank (buckets take the top layer, or its flange) and
     * the player switches it on.
     */
    private fun drown(v: mods.eln.sim.fluid.FluidNode) {
        if (!mainSwitch) return
        val nominal = FuelBurnerDescriptor.getDescriptor(inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot))?.producedHeatPower ?: 0.0
        // (the hot burner flashes it: by its fire, or by its heat if the fire just went out for want of fuel)
        val hot = Math.max(fire.level, Math.max(0.0, Math.min(1.0, thermalLoad.Tc / 400)))
        val kg = mods.eln.sim.fluid.EaUnits.drainLiquid(v, (bank + hot * nominal * DROWN_FLASH_S) / WATER_BOIL_J_PER_KG)
        val fn = flue.node
        if (fn != null && kg > 0) mods.eln.sim.fluid.EaUnits.addWater(fn, kg, 373.15, true)
        steamedKg += kg
        fire.level = 0.0
        bank = 0.0
        delivered = 0.0   // (the water quenches the burner)
        mainSwitch = false
        steamPuff = 2
        needPublish()
    }

    /** Is its main switch on? */
    fun switchedOn() = mainSwitch

    /** Water flashed to steam by drowning its fire (kg, since loaded); client ticks of steam puff left. */
    var steamedKg = 0.0
    private var steamPuff = 0

    /** The heating value (J/kg) of the layer species id is in: its species' by their shares. */
    private fun layerHeatingValue(v: mods.eln.sim.fluid.FluidNode, id: Int): Double {
        val total = mods.eln.sim.fluid.EaUnits.layerLiquid(v, id)
        if (total <= 0) return 0.0
        val layer = v.layerOf(id)
        val reg = mods.eln.sim.fluid.SpeciesRegistry.get()
        var hv = 0.0
        for (j in 2 until v.speciesBound()) if (v.layerOf(j) == layer) hv += v.liquid(j) / total * reg.byId(j).heatingValue
        return hv
    }

    /** The coking index of the layer species id is in: its species' `coking` by their shares. */
    private fun layerCoking(v: mods.eln.sim.fluid.FluidNode, id: Int): Double {
        val total = mods.eln.sim.fluid.EaUnits.layerLiquid(v, id)
        if (total <= 0) return 0.0
        val layer = v.layerOf(id)
        val reg = mods.eln.sim.fluid.SpeciesRegistry.get()
        var c = 0.0
        for (j in 2 until v.speciesBound()) if (v.layerOf(j) == layer) c += v.liquid(j) / total * reg.byId(j).coking
        return c
    }

    /** The share of species j in the layer species id is in (0 if j is absent or elsewhere). */
    private fun layerShare(v: mods.eln.sim.fluid.FluidNode, id: Int, j: Int): Double {
        if (j < 0 || v.layerOf(j) != v.layerOf(id)) return 0.0
        val total = mods.eln.sim.fluid.EaUnits.layerLiquid(v, id)
        return if (total > 0) v.liquid(j) / total else 0.0
    }

    /** Heat a kg of what is at its burner now gives (J/kg, after heatFurnaceFuelFactor; 0: water or nothing). */
    fun heatPerKgAtBurner(): Double {
        val v = vessel ?: return 0.0
        val id = burnerLayer(v)
        if (id < 0 || id == mods.eln.sim.fluid.SpeciesRegistry.WATER) return 0.0
        return layerHeatingValue(v, id) * mods.eln.ElnConfig.heatFurnaceFuelFactor
    }

    /** Put mB of fluid into its tank at the ambient, as a bucket would (a mixture alias dead: crude as its cuts). */
    fun putFuel(fluid: net.minecraft.world.level.material.Fluid, mB: Int) {
        val v = vessel ?: return
        val ph = mods.eln.fluid.FluidSpecies.of(fluid) ?: return
        mods.eln.sim.fluid.EaUnits.add(v, ph.id(), mB * mods.eln.fluid.FluidSpecies.kgPerMb(ph.id(), ph.vapour()),
            mods.eln.sim.Ambient.kelvin(node!!.coordonate), ph.vapour())
        view.show(v)
        atBurner = burnerLayer(v)
    }

    /** Its tank's liquid (mB of water's volume: every liquid's mB is the same volume). */
    fun fuelMb(): Double {
        val v = vessel ?: return 0.0
        return v.liquidVolume() * mods.eln.sim.fluid.Water.RHO_L / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER
    }

    override fun connectJob() {
        super.connectJob()
        if (flue.node != null && vessel != null) mods.eln.fluid.FluidWorld.add(this)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        mods.eln.fluid.FluidWorld.remove(this)
    }

    private val inventory_ = TransparentNodeElementInventory(2, 1, this)

    private var externalControlled by published(false)
    private var mainSwitch by published(false)

    private var manualControl by published(0.0)
    private var setTemperature by published(0.0)

    private var heaterControlValue = 0.0
    /** The burner's real level, lagging the control value (Fire.OIL). */
    val fire = mods.eln.sim.Fire(mods.eln.sim.Fire.OIL).also { it.instant = true }

    /**
     * Its burner's thermal inertia (Baughn 2026-11-11: from the burner's mass and the fuel's, 30 s): the flame heats the
     * burner, a lumped mass of BURNER_C_PER_W J/K per W of rating (about 6 kg of alloy for the big one) plus the fuel in
     * the tank (FUEL_CP), which passes heat on through G = rating / BURNER_DT. The heat it gives lags the flame by
     * tau = C / G: 30 s empty, a little more with a full tank.
     */
    private var delivered = 0.0
    /** The flame's own power now (W): what it burns, before the burner's lag. */
    var flamePower = 0.0
        private set

    /** Its burner's time constant now (s). */
    fun burnerTau(nominal: Double): Double {
        if (nominal <= 0) return 0.0
        val fuelKg = (vessel?.liquidVolume() ?: 0.0) * 850   // (a fuel's density, near enough)
        return BURNER_DT * (BURNER_C_PER_W * nominal + FUEL_CP * fuelKg) / nominal
    }
    private var actualHeatPower by published(0.0)

    /**
     * How coked its burner is (0..COKE_MAX; Baughn 2026-11-03: crude burns, but at a cost): fuel over the coking
     * balance cokes it (crude: half after an hour at its full power; residue ~6x faster, heavy oil ~0.6x), fuel under it
     * clears it again, and a coked burner fires that much less.
     * A right click with wool cleans it.
     */
    var coke = 0.0

    /** Its burner's heat power now (W), before the flue takes its share; 0 when burnt out (as Jade shows). */
    fun heatPower() = if (isBurnt()) 0.0 else actualHeatPower

    private val controlProcess = object : RegulatorProcess("controller") {
        override fun process(time: Double) {
            val nominalPower = if (mainSwitch)
                FuelBurnerDescriptor.getDescriptor(inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot))?.producedHeatPower ?: 0.0
            else
                0.0

            when {
                externalControlled -> {
                    setCmd(controlLoad.u / Eln.SVU)
                }
                else -> {
                    setCmd(manualControl)
                }
            }
            super.process(time)

            // (water at the burner: no fuel, it won't light, Baughn 2026-11-11)
            fire.step(heaterControlValue, nominalPower > 0 && (atBurner >= 0 && atBurner != mods.eln.sim.fluid.SpeciesRegistry.WATER || bank > 0), time)
            // (each kg gives heatFurnaceFuelFactor times its heating value: Baughn 2026-10-26, with turbines by the
            // expansion; the burner draws it into the bank between fluid steps)
            val want = fire.level * nominalPower * (1 - coke) * overfire() * time
            wanted += want
            val burnt = Math.min(want, bank)
            bank -= burnt
            flamePower = burnt / time
            flameJ += burnt
            // (the burner's mass between the flame and the furnace: the heat it gives follows the flame with tau)
            val tau = burnerTau(nominalPower)
            delivered += (flamePower - delivered) * (if (tau <= 0) 1.0 else Math.min(1.0, time / tau))
            if (delivered < 1e-3) delivered = 0.0
            heatGivenJ += delivered * time
            actualHeatPower = delivered
            // (the coking: per second at its full power on dead crude, 1 / (2 h); by the coking index of what burns, over
            // or under the balance: fuel better than a plausible refinery's clears the burner as it burns)
            val rate = cokeRate()
            if (rate != 0.0 && nominalPower > 0) coke = Math.max(0.0, Math.min(COKE_MAX, coke + rate * flamePower / nominalPower * time / (2 * 3600)))
            thermalLoad.PcTemp += mods.eln.fluid.Flue.HEAT_SHARE * actualHeatPower   // (the rest leaves in the smoke: Flue)
        }

        override fun getHit() = thermalLoad.Tc
        override fun setCmd(cmd: Double) {
            heaterControlValue = Math.max(0.0, cmd)
        }
    }

    private val thermalWatchdog = ThermalLoadWatchDog()

    init {
        thermalLoadList.add(thermalLoad)
        thermalFastProcessList.add(controlProcess)
        electricalLoadList.add(controlLoad)
        slowProcessList.add(NodePeriodicPublishProcess(transparentNode, 2.0, 1.0))
        slowProcessList.add(thermalWatchdog)

        thermalWatchdog.set(thermalLoad).setLimit((descriptor as FuelHeatFurnaceDescriptor).thermal)
            .set(Failure(this).machine())
    }

    // the control signal on either side, not the front or back (Baughn)
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?) = when {
        (side == front.left() || side == front.right()) && lrdu == LRDU.Down -> controlLoad
        else -> null
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = when {
        side == front.inverse && lrdu == LRDU.Down -> thermalLoad
        else -> null
    }

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?) = when (lrdu) {
        LRDU.Down -> when (side) {
            front.inverse -> NodeBase.maskThermal
            front.left(), front.right() -> NodeBase.maskElectricalInputGate
            else -> 0
        }
        else -> 0
    }

    override fun getFluidHandler() = view

    override fun multiMeterString(side: Direction?) = Utils.plotPower("P:", thermalLoad.power)

    override fun thermoMeterString(side: Direction) = Utils.plotCelsius("T:", thermalLoad.Tc)

    override fun initialize() {
        (descriptor as FuelHeatFurnaceDescriptor).thermal.applyTo(thermalLoad)
        inventoryChange(inventory_)
        val amb = mods.eln.sim.Ambient.kelvin(node!!.coordonate)
        makeVessel(amb)
        flue.make(node!!.coordonate.y + 0.75, amb)
        connect()
    }

    /** Wool on a coked burner: it's cleaned, and the wool used (Baughn 2026-11-03). Otherwise the screen opens. */
    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        val held = entityPlayer?.mainHandItem ?: return false
        if (coke <= 0.01 || !held.`is`(net.minecraft.tags.ItemTags.WOOL)) return false
        coke = 0.0
        if (!entityPlayer.isCreative) held.shrink(1)
        needPublish()
        return true
    }

    /** The vessel, from its save, or an old save's tank and stub, or empty at the ambient. */
    private fun makeVessel(amb: Double) {
        val v = mods.eln.sim.fluid.FluidNode(TANK_M3, node!!.coordonate.y.toDouble(), TANK_H, false)
        val saved = loaded
        if (saved != null) {
            mods.eln.fluid.FluidSaves.restore(v, saved)
            v.update()
        } else {
            v.fill(0.0, mods.eln.sim.fluid.Water.P_ATM, amb)
            legacyTank?.let { st ->
                val ph = mods.eln.fluid.FluidSpecies.of(st.fluid)
                if (ph != null && st.amount > 0)
                    mods.eln.sim.fluid.EaUnits.add(v, ph.id(), st.amount * mods.eln.fluid.FluidSpecies.kgPerMb(ph.id(), false), amb, false)
            }
            legacyStub?.let { a ->
                val stub = mods.eln.sim.fluid.FluidNode(mods.eln.fluid.PortBridge.STUB_VOLUME, 0.0, 0.1, false)
                mods.eln.fluid.FluidSaves.restore(stub, a)
                stub.update()
                if (stub.mWater > 0) mods.eln.sim.fluid.EaUnits.add(v, mods.eln.sim.fluid.SpeciesRegistry.WATER, stub.mWater, amb, false)
                val reg = mods.eln.sim.fluid.SpeciesRegistry.get()
                for (j in 2 until stub.speciesBound()) {
                    val m = stub.liquid(j) + stub.vapour(j)
                    if (m > 0) mods.eln.sim.fluid.EaUnits.add(v, j, m, amb, reg.byId(j).gas())
                }
            }
        }
        legacyTank = null
        legacyStub = null
        loaded = null
        vessel = v
        breather = mods.eln.sim.fluid.FluidDevice.Vent(v, 1.0, BREATHER_K).also { it.gasOnly = true }
        relief = mods.eln.sim.fluid.FluidDevice.Vent(v, 1.0, RELIEF_K)
        airIn = mods.eln.sim.fluid.FluidDevice.Vent(v, 1.0, BREATHER_K).also { it.inward = true }
        setBreather(v)
        view.show(v)
        atBurner = burnerLayer(v)
    }

    /** How many times its setting the pressure-fed burner fires: 1 + gauge over the deadband / OVERFIRE_PA. */
    fun overfire(): Double {
        val v = vessel ?: return 1.0
        return 1 + Math.max(0.0, v.P() - mods.eln.sim.fluid.Water.P_ATM - BREATHER_DEADBAND_PA) / OVERFIRE_PA
    }

    /** Fuel gas escaping through its relief (kg/s, last fluid step), and the ticks to its next ignition check. */
    var gasEscapingKgS = 0.0
        private set
    /** What left by its relief valve (kg, since loaded). */
    var reliefKg = 0.0
        private set
    private var fireCheck = 0

    /**
     * After a fluid step: fuel gas that left through the relief valve with the fire lit catches around it, over
     * GAS_FIRE_KG_S (Baughn 2026-11-11: an ad-hoc rule, as rooms and spills aren't tracked). Past BURST_PA it bursts.
     */
    private fun checkPressure(v: mods.eln.sim.fluid.FluidNode, dt: Double) {
        var kg = 0.0
        val reg = mods.eln.sim.fluid.SpeciesRegistry.get()
        for (vent in listOf(breather, relief)) {
            val fs = vent?.edge?.fSpecies ?: continue
            for (id in 2 until Math.min(fs.size, reg.size())) if (reg.byId(id).heatingValue > 0 && reg.byId(id).gas()) kg += Math.abs(fs[id])
        }
        gasEscapingKgS += (kg - gasEscapingKgS) * Math.min(1.0, dt / GAS_FIRE_S)
        val rf = relief?.edge?.flow ?: 0.0
        if (rf > 0) reliefKg += rf * dt
        val level = node!!.coordonate.world()
        if (gasEscapingKgS > GAS_FIRE_KG_S && fire.level > 0 && --fireCheck <= 0) {
            fireCheck = 10
            mods.eln.sim.process.destruct.Failure.ignite(level, node!!.coordonate.pos().above(), true)
        }
        val gauge = v.P() - mods.eln.sim.fluid.Water.P_ATM
        if (gauge > BURST_PA)
            mods.eln.sim.process.destruct.Failure(this).pressureVessel { v.P() * v.volume * Math.log(v.P() / mods.eln.sim.fluid.Water.P_ATM) }
                .fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, v.P())
    }

    /** Coking per second at full power, as a share of COKE per (2 h): by the coking index over the balance (< 0 clears). */
    private fun cokeRate() = if (atBurner >= 0 && atBurner != mods.eln.sim.fluid.SpeciesRegistry.WATER) COKE_PER_INDEX * (cokingIndex - COKE_BALANCE) else 0.0

    /** Thick black smoke while what it burns cokes the burner. */
    private fun sooty() = flamePower > 0 && cokingIndex > COKE_BALANCE

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(externalControlled)
        stream.writeBoolean(mainSwitch)
        stream.writeFloat(heaterControlValue.toFloat())
        stream.writeFloat(setTemperature.toFloat())
        stream.writeFloat(if (isBurnt()) 0f else actualHeatPower.toFloat())   // (burnt out: no flame, whatever it last made)
        stream.writeFloat(thermalLoad.Tc.toFloat())
        stream.writeInt(FuelBurnerDescriptor.getDescriptor(inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot))?.type ?: -1)
        stream.writeFloat(if (isBurnt()) 0f else fire.level.toFloat())
        stream.writeBoolean(fire.lighting())
        stream.writeBoolean(smoking && !isBurnt())
        stream.writeBoolean(sooty())
        stream.writeBoolean(steamPuff > 0)
        if (steamPuff > 0) steamPuff--
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (super.networkUnserialize(stream)) {
            ExternalControlledToggleEvent -> {
                externalControlled = !externalControlled
                inventoryChange(inventory_)
            }
            // (on only with a burner in its slot, as inventoryChange keeps it: a switch on without one was off after a reload)
            MainSwitchToggleEvent -> mainSwitch = !mainSwitch && !inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot).isEmpty
            SetManualControlValueEvent -> {
                manualControl = ClientValues.clamp(stream.readFloat().toDouble(), 0.0, 1.0, manualControl)   // (the screen's slider)
            }
            SetTemperatureEvent -> {
                setTemperature = ClientValues.clamp(stream.readFloat().toDouble(), 0.0, 900.0, setTemperature)
                controlProcess.target = setTemperature
            }
        }

        return TransparentNodeElement.unserializeNulldId
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        vessel?.let {
            mods.eln.fluid.FluidWorld.writeBackOf(it)
            mods.eln.fluid.FluidSaves.put(nbt, "vessel", it)
        }
        nbt.putDouble("bank", bank)
        nbt.putDouble("cokingIndex", cokingIndex)
        nbt.putBoolean("externalControlled", externalControlled)
        nbt.putBoolean("mainSwitch", mainSwitch)
        nbt.putDouble("heaterControlValue", heaterControlValue)
        nbt.putDouble("manualControl", manualControl)
        nbt.putDouble("setTemperature", setTemperature)
        nbt.putDouble("actualHeatPower", actualHeatPower)
        nbt.putDouble("burnerW", delivered)
        fire.write(nbt, "")
        flue.write(nbt)
        nbt.putDouble("coke", coke)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        loaded = mods.eln.fluid.FluidSaves.array(nbt, "vessel")
        if (loaded == null) {
            // (an old save: its 2-bucket fluid tank and its flange's PortBridge stub go into the vessel when it is made)
            val old = PreciseElementFluidHandler(TANK_MB.toInt())
            old.readFromNBT(nbt, "tank")
            legacyTank = old.getFluid().copy()
            legacyStub = mods.eln.fluid.FluidSaves.array(nbt, "fuelIn")
        }
        bank = nbt.getDoubleOr("bank", 0.0)
        cokingIndex = nbt.getDoubleOr("cokingIndex", nbt.getDoubleOr("residueShare", 0.0))
        externalControlled = nbt.getBooleanOr("externalControlled", false)
        mainSwitch = nbt.getBooleanOr("mainSwitch", false)
        heaterControlValue = nbt.getDoubleOr("heaterControlValue", 0.0)
        manualControl = ClientValues.finite(nbt.getDoubleOr("manualControl", 0.0), 0.0)
        setTemperature = ClientValues.finite(nbt.getDoubleOr("setTemperature", 0.0), 0.0)
        actualHeatPower = nbt.getDoubleOr("actualHeatPower", 0.0)
        delivered = nbt.getDoubleOr("burnerW", actualHeatPower)
        if (nbt.contains("fire")) fire.read(nbt, "") else fire.level = heaterControlValue
        flue.read(nbt)
        coke = nbt.getDoubleOr("coke", 0.0)
    }

    override fun hasGui() = true

    /**
     * Repaired, its burner stays off until the player switches it on (Baughn 2026-11-04, todo #218: a repair relit it
     * at the old setting and, with nothing taking its heat, it burnt out again; a burnt device opens no GUI).
     */
    /** Is its burner switched on (its main switch)? */
    fun burnerOn() = mainSwitch

    override fun repair() {
        val was = isBurnt()
        super.repair()
        if (was) mainSwitch = false
    }

    override fun getWaila(): MutableMap<String, String> {
        val info = HashMap<String, String>()
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.Tc))
        info.put(I18N.tr("Power"), Utils.plotPower("", if (isBurnt()) 0.0 else actualHeatPower))
        // (what's in its tank: Baughn 2026-11-03, Jade said nothing of it)
        val v = vessel
        if (v != null) {
            val mb = fuelMb()
            info.put(I18N.tr("Fuel tank"), if (mb < 0.5) I18N.tr("empty") else String.format("%.0f / %.0f mB: %s", mb, TANK_MB, mods.eln.sim.fluid.EaUnits.composition(v)))
            if (atBurner == mods.eln.sim.fluid.SpeciesRegistry.WATER) info.put(I18N.tr("Burner"), I18N.tr("water at the burner: the fire is out; drain it to relight"))
        }
        if (coke > 0.01 && atBurner != mods.eln.sim.fluid.SpeciesRegistry.WATER) info.put(I18N.tr("Burner"), String.format(I18N.tr("%.0f%% coked: fires %.0f%% (right-click with wool to clean)"), coke * 100, (1 - coke) * 100))
        val over = overfire()
        if (over > 1.01) info.put(I18N.tr("Fuel feed"), String.format(I18N.tr("pressure-fed at %.1f bar: fires %.1fx its setting"), (v!!.P() - mods.eln.sim.fluid.Water.P_ATM) / 1e5, over))
        if (gasEscapingKgS > GAS_FIRE_KG_S) info.put(I18N.tr("Relief"), I18N.tr("venting fuel gas!"))
        if (flue.node != null) info.put(I18N.tr("Flue"), flue.jade())
        if (v != null && mods.eln.fluid.FluidWorld.edgesOf(v).any { it !== breather?.edge && it !== airIn?.edge && it !== relief?.edge }) info.put(I18N.tr("Fuel"), I18N.tr("from the pipe on its front flange"))
        return info
    }

    override fun getInventory() = inventory_

    override fun inventoryChange(inventory: Container?) {
        mainSwitch = mainSwitch && !inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot).isEmpty

        val regulatorStack = inventory_.getItem(FuelHeatFurnaceContainer.RegulatorSlot)
        if (!regulatorStack.isEmpty && !externalControlled) {
            val regulator = Utils.getItemObject(regulatorStack) as IRegulatorDescriptor
            regulator.applyTo(controlProcess, 500.0, 20.0, 0.2, 0.1)
        } else {
            controlProcess.setManual()
        }
    }

}

class FuelHeatFurnaceRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(tileEntity, descriptor) {


    var type: Int? = null
    var externalControlled = false
    var mainSwitch = false

    var manualControl = Synchronizable(0f)
    var setTemperature = Synchronizable(0f)

    var heatPower = 0f
    var actualTemperature = 0f
    var fire = 0f
    var lighting = false
    /** Smoke up its stack (burning, nothing piped on its flue's flange). */
    var smoking = false
    /** Burning crude or residue: thick black smoke. */
    var sooty = false
    /** Its fire just drowned: a burst of steam up its stack. */
    var steaming = false
    private val smoke = mods.eln.anim.Emitter()
    private val steam = mods.eln.anim.Emitter()

    val sound = object : LoopedSound("eln:fuelheatfurnace", coordonate()) {
        override fun getPitch() = FuelBurnerDescriptor.pitchForType(type)
        override fun getVolume() = if (heatPower > 0) 0.01f + 0.00001f * heatPower else 0f
    }

    init {
        addLoopedSound(sound)
    }

    override fun draw() {
        front.glRotateXnRef()
        // the flame's brightness by its power against the big burner's, with a slight flicker
        val big = FuelBurnerDescriptor.powerForType(0)
        val k = (if (big > 0) (heatPower / big).toFloat() else fire) * (0.92f + 0.08f * Math.sin(flicker.toDouble()).toFloat() * Math.sin(flicker * 2.7).toFloat())
        (transparentNodedescriptor as FuelHeatFurnaceDescriptor).draw(type, mainSwitch, heatPower != 0f, fire, k)
    }

    /** A quick flicker of the oil flame's brightness (radians). */
    var flicker = 0f

    override fun refresh(deltaT: Float) {
        flicker = (flicker + deltaT * 11) % (20 * Math.PI).toFloat()
        // (smoke out of the stack's open flange, more of it the harder it burns)
        val n = smoke.step(deltaT.toDouble(), if (smoking) (2 + 10 * fire).toDouble() else 0.0)
        // (its drowned fire's steam: so many a second, where it was a chance a frame)
        val wisps = steam.step(deltaT.toDouble(), if (steaming) 25.0 else 0.0)
        val level = tileEntity.level ?: return
        val p = tileEntity.blockPos
        val plume = if (sooty) mods.eln.anim.Plume.SOOT else mods.eln.anim.Plume.SMOKE
        repeat(n) { plume.puff(level, p.x + 0.5, p.y + 1.02, p.z + 0.5, 0.06) }
        repeat(wisps) { mods.eln.anim.Plume.STEAM.puff(level, p.x + 0.5, p.y + 1.02, p.z + 0.5, 0.06) }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        externalControlled = stream.readBoolean()
        mainSwitch = stream.readBoolean()
        manualControl.value = stream.readFloat()
        setTemperature.value = stream.readFloat()
        heatPower = stream.readFloat()
        actualTemperature = stream.readFloat()
        type = stream.readInt().let { type ->
            when (type) {
                -1 -> null
                else -> type
            }
        }
        fire = stream.readFloat()
        lighting = stream.readBoolean()
        smoking = stream.readBoolean()
        sooty = stream.readBoolean()
        steaming = stream.readBoolean()
    }

}


// Slot ids of the 1.12 container. The menus themselves are built in gui/MenuSpecs.
class FuelHeatFurnaceContainer {
    companion object {
        val FuelBurnerSlot = 0
        val RegulatorSlot = 1
    }
}
