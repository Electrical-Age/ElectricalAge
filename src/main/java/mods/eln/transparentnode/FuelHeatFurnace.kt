package mods.eln.transparentnode

import mods.eln.sound.LoopedSound

import mods.eln.gl.GL11

import mods.eln.Eln
import mods.eln.fluid.FuelRegistry
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
        list.add(I18N.tr("Burns gasoline, diesel, heavy oil, residue and crude oil."))
        list.add(I18N.tr("Crude oil and residue coke its burner: clean it with wool."))
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
    }

    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val controlLoad = NbtElectricalGateInput("commandLoad")

    /** Its fuel tank: two buckets (Baughn 2026-11-03: it fills by bucket; the 1.12 tank held 25 mB, under a bucket). */
    private val tank = PreciseElementFluidHandler(2000)

    /** On EA pipes (Baughn 2026-11-03): fuel in on the flange low on its front (mods.eln.cad.models.FuelHeater). */
    private val fuelIn = mods.eln.fluid.PortBridge("fuelIn", mods.eln.fluid.PortBridge.Mode.IN, 50.0, 0.0)
    /** Its flue: smoke out of the flange on top of its stack, or up the stack (as the coal oven's). */
    private val flue = mods.eln.fluid.Flue("flue")
    private var smoking = false

    override fun fluidLevel(): Any = node!!.coordonate.dimention

    override fun fluidPorts(): List<mods.eln.sim.fluid.FluidDevice.FluidPort> {
        if (fuelIn.stub == null || flue.node == null) return listOf()
        val pos = node!!.coordonate.pos().asLong()
        val f = front.toEnumFacing().get3DDataValue()
        return listOf(fuelIn.port(pos, f, mods.eln.fluid.PortBridge.endSpot(front, front, 4, 8)), flue.port(pos))
    }

    override fun beforeFluidStep(dt: Double) {
        val amb = mods.eln.sim.Ambient.kelvin(node!!.coordonate)
        fuelIn.step(tank, dt, amb)
        if (flue.step(if (isBurnt()) 0.0 else actualHeatPower, dt, amb)) needPublish()   // (burnt: its fire is out)
        smoking = flue.smoking
    }

    override fun afterFluidStep(dt: Double) {
        flue.afterStep(mods.eln.sim.Ambient.kelvin(node!!.coordonate))
    }

    override fun connectJob() {
        super.connectJob()
        if (flue.node != null) mods.eln.fluid.FluidWorld.add(this)
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
    val fire = mods.eln.sim.Fire(mods.eln.sim.Fire.OIL)
    private var actualHeatPower by published(0.0)

    /**
     * How coked its burner is (0..COKE_MAX; Baughn 2026-11-03: crude burns, but at a cost): burning crude (residue at
     * half the rate) cokes it, and a coked burner fires that much less (half after an hour at its full power on crude).
     * A right click with wool cleans it.
     */
    var coke = 0.0

    /** Its burner's heat power now (W), before the flue takes its share. */
    fun heatPower() = actualHeatPower

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

            fire.step(heaterControlValue, nominalPower > 0 && tank.fluidAmount > 0, time)
            // (each mB gives heatFurnaceFuelFactor times its heat value: Baughn 2026-10-26, with turbines by the expansion)
            val f = mods.eln.ElnConfig.heatFurnaceFuelFactor
            val fuel = tank.getFluid()
            val availableEnergy = tank.drainEnergy(fire.level * nominalPower * (1 - coke) * time / f) * f
            actualHeatPower = availableEnergy / time
            // (the coking: per second at its full power on crude, 1 / (2 h); residue at half that)
            val rate = cokeRate(fuel)
            if (rate > 0 && nominalPower > 0) coke = Math.min(COKE_MAX, coke + rate * actualHeatPower / nominalPower * time / (2 * 3600))
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

        tank.setFilter(FuelRegistry.fluidListToFluids(FuelRegistry.gasolineList + FuelRegistry.dieselList + FuelRegistry.heavyList))

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

    override fun getFluidHandler() = tank

    override fun multiMeterString(side: Direction?) = Utils.plotPower("P:", thermalLoad.power)

    override fun thermoMeterString(side: Direction) = Utils.plotCelsius("T:", thermalLoad.Tc)

    override fun initialize() {
        (descriptor as FuelHeatFurnaceDescriptor).thermal.applyTo(thermalLoad)
        inventoryChange(inventory_)
        val amb = mods.eln.sim.Ambient.kelvin(node!!.coordonate)
        fuelIn.make(node!!.coordonate.y + 4 / 16.0, amb)
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

    /** Coking per second at full power, as a share of COKE per (2 h): crude 1, residue 0.5, refined fuels 0. */
    private fun cokeRate(fuel: net.neoforged.neoforge.fluids.FluidStack): Double {
        if (fuel.isEmpty) return 0.0
        return when {
            fuel.fluid.isSame(mods.eln.oil.Oil.CRUDE.get()) -> 1.0
            fuel.fluid.isSame(mods.eln.oil.Oil.Cut.RESIDUE.source.get()) -> 0.5
            else -> 0.0
        }
    }

    /** Thick black smoke while it burns crude or residue. */
    private fun sooty() = cokeRate(tank.getFluid()) > 0

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
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (super.networkUnserialize(stream)) {
            ExternalControlledToggleEvent -> {
                externalControlled = !externalControlled
                inventoryChange(inventory_)
            }
            MainSwitchToggleEvent -> mainSwitch = !mainSwitch
            SetManualControlValueEvent -> {
                manualControl = stream.readFloat().toDouble()
            }
            SetTemperatureEvent -> {
                setTemperature = stream.readFloat().toDouble()
                controlProcess.target = setTemperature
            }
        }

        return TransparentNodeElement.unserializeNulldId
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
        nbt.putBoolean("externalControlled", externalControlled)
        nbt.putBoolean("mainSwitch", mainSwitch)
        nbt.putDouble("heaterControlValue", heaterControlValue)
        nbt.putDouble("manualControl", manualControl)
        nbt.putDouble("setTemperature", setTemperature)
        nbt.putDouble("actualHeatPower", actualHeatPower)
        fire.write(nbt, "")
        fuelIn.write(nbt)
        flue.write(nbt)
        nbt.putDouble("coke", coke)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
        externalControlled = nbt.getBooleanOr("externalControlled", false)
        mainSwitch = nbt.getBooleanOr("mainSwitch", false)
        heaterControlValue = nbt.getDoubleOr("heaterControlValue", 0.0)
        manualControl = nbt.getDoubleOr("manualControl", 0.0)
        setTemperature = nbt.getDoubleOr("setTemperature", 0.0)
        actualHeatPower = nbt.getDoubleOr("actualHeatPower", 0.0)
        if (nbt.contains("fire")) fire.read(nbt, "") else fire.level = heaterControlValue
        fuelIn.read(nbt)
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
        val f = tank.getFluid()
        info.put(I18N.tr("Fuel tank"), if (f.isEmpty) I18N.tr("empty") else "${tank.fluidAmount} / ${tank.capacity} mB ${f.hoverName.string}")
        if (coke > 0.01) info.put(I18N.tr("Burner"), String.format(I18N.tr("%.0f%% coked: fires %.0f%% (right-click with wool to clean)"), coke * 100, (1 - coke) * 100))
        if (flue.node != null) info.put(I18N.tr("Flue"), flue.jade())
        if (fuelIn.piped()) info.put(I18N.tr("Fuel"), I18N.tr("from the pipe on its front flange"))
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
    private var smokeDue = 0f

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
        val level = tileEntity.level ?: return
        if (!smoking) return
        // (smoke out of the stack's open flange, more of it the harder it burns)
        smokeDue += deltaT * (2 + 10 * fire)
        val p = tileEntity.blockPos
        while (smokeDue >= 1) {
            smokeDue -= 1
            val jx = (Math.random() - 0.5) * 0.12
            val jz = (Math.random() - 0.5) * 0.12
            level.addParticle(if (sooty) net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE else net.minecraft.core.particles.ParticleTypes.SMOKE,
                p.x + 0.5 + jx, p.y + 1.02, p.z + 0.5 + jz, 0.0, 0.06, 0.0)
        }
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
    }

}


// Slot ids of the 1.12 container. The menus themselves are built in gui/MenuSpecs.
class FuelHeatFurnaceContainer {
    companion object {
        val FuelBurnerSlot = 0
        val RegulatorSlot = 1
    }
}
