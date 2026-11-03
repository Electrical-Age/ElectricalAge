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

        if (on) {
            GL11.glColor3f(0f, 1f, 0f)
            UtilsClient.drawLight(powerLED)
        } else {
            GL11.glColor3f(0f, 0.5f, 0f)
            powerLED?.draw()
        }

        if (heating) {
            GL11.glColor3f(1f, 0f, 0f)
            UtilsClient.drawLight(heatLED)
        } else {
            GL11.glColor3f(0.5f, 0f, 0f)
            heatLED?.draw()
        }
        GL11.glColor3f(1f, 1f, 1f)

        if (glow != null && fire > 0.01f) {
            val k = power.coerceIn(0f, 1f)
            UtilsClient.disableLight()
            GL11.glColor4f(0.85f + 0.15f * k, 0.38f + 0.54f * k, 0.08f + 0.67f * k * k, 1f)
            val at = WINDOW_Y0 / 16f - 0.5f
            GL11.glPushMatrix()
            GL11.glTranslatef(0f, at, 0f)
            GL11.glScalef(1f, Math.min(1f, fire), 1f)
            GL11.glTranslatef(0f, -at, 0f)
            glow.draw()
            GL11.glPopMatrix()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            UtilsClient.enableLight()
        }
        if (glass != null) {
            UtilsClient.enableBlend()
            GL11.glColor4f(1f, 1f, 1f, 0.3f)
            glass.draw()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            UtilsClient.disableBlend()
        }
    }

    private var hitbox: ModelHitbox? = null

    /** Its model's hitbox (mods.eln.cad.models.FuelHeater), turned with its front. */
    override fun modelHitbox(): ModelHitbox? {
        if (hitbox == null) hitbox = ModelHitbox.of(obj)
        return hitbox
    }

    override fun hasItemDraw() = true

    /** The item: a medium burner, off. */
    override fun drawItem(entity: Boolean) = draw(1)

    companion object {
        /** The foot of the sight port (px; mods.eln.cad.models.FuelHeater WIN_Y0): the flame is scaled about it. */
        const val WINDOW_Y0 = 6.5f
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Generates heat when supplied with fuel."))
        list.add(I18N.tr("Burns gasoline, diesel, heavy oil and residue."))
        list.add(Utils.plotCelsius(I18N.tr("  Max. temperature: "), thermal.warmLimit))
    }
}

class FuelHeatFurnaceElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(transparentNode, descriptor) {
    companion object {
        val ExternalControlledToggleEvent: Byte = 0
        val MainSwitchToggleEvent: Byte = 1
        val SetManualControlValueEvent: Byte = 2
        val SetTemperatureEvent: Byte = 3
    }

    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val controlLoad = NbtElectricalGateInput("commandLoad")

    private val tank = PreciseElementFluidHandler(25)

    private val inventory_ = TransparentNodeElementInventory(2, 1, this)

    private var externalControlled by published(false)
    private var mainSwitch by published(false)

    private var manualControl by published(0.0)
    private var setTemperature by published(0.0)

    private var heaterControlValue = 0.0
    /** The burner's real level, lagging the control value (Fire.OIL). */
    val fire = mods.eln.sim.Fire(mods.eln.sim.Fire.OIL)
    private var actualHeatPower by published(0.0)

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
            val availableEnergy = tank.drainEnergy(fire.level * nominalPower * time / f) * f
            actualHeatPower = availableEnergy / time
            thermalLoad.PcTemp += actualHeatPower
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
        connect()
    }

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(externalControlled)
        stream.writeBoolean(mainSwitch)
        stream.writeFloat(heaterControlValue.toFloat())
        stream.writeFloat(setTemperature.toFloat())
        stream.writeFloat(actualHeatPower.toFloat())
        stream.writeFloat(thermalLoad.Tc.toFloat())
        stream.writeInt(FuelBurnerDescriptor.getDescriptor(inventory_.getItem(FuelHeatFurnaceContainer.FuelBurnerSlot))?.type ?: -1)
        stream.writeFloat(fire.level.toFloat())
        stream.writeBoolean(fire.lighting())
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
    }

    override fun hasGui() = true

    override fun getWaila(): MutableMap<String, String> {
        val info = HashMap<String, String>()
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.Tc))
        info.put(I18N.tr("Power"), Utils.plotPower("", actualHeatPower))
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
    }

}


// Slot ids of the 1.12 container. The menus themselves are built in gui/MenuSpecs.
class FuelHeatFurnaceContainer {
    companion object {
        val FuelBurnerSlot = 0
        val RegulatorSlot = 1
    }
}
