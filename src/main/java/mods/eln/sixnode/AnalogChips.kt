package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

open class AnalogChipDescriptor(name: String, obj: Obj3D?, functionName: String,
                                functionClass: Class<out AnalogFunction>,
                               elementClass: Class<out AnalogChipElement>, renderClass: Class<out AnalogChipRender>):
        SixNodeDescriptor(name, elementClass, renderClass) {
    private val case = obj?.getPart("Case")
    private val top = obj?.getPart(functionName)
    private val pins = arrayOfNulls<Obj3D.Obj3DPart>(4)

    internal val function = functionClass.newInstance()

    init {
        pins[0] = obj?.getPart("Output")
        for (i in 1..function.inputCount) pins[i] = obj?.getPart("Input$i")

        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    constructor(name: String, obj: Obj3D?, functionName: String, functionClass: Class<out AnalogFunction>):
    this(name, obj, functionName, functionClass, AnalogChipElement::class.java, AnalogChipRender::class.java) {}

    fun draw() {
        pins.forEach { it?.draw() }
        case?.draw()
        top?.draw()
    }

    override fun handleRenderType(item: ItemStack?, type: IItemRenderer.ItemRenderType?): Boolean = true
    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?,
                                       helper: IItemRenderer.ItemRendererHelper?): Boolean =
            type != IItemRenderer.ItemRenderType.INVENTORY
    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType?, item: ItemStack?,
                                          helper: IItemRenderer.ItemRendererHelper?): Boolean =
            type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f)
            draw()
        }
    }

    override fun getFrontFromPlace(side: Direction?, player: EntityPlayer?): LRDU? =
            super.getFrontFromPlace(side, player).left()

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addSignal(newItemStack())
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (list != null) {
            function.infos.split("\n").forEach { list.add(it) }
        }
    }
}

open class AnalogChipElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor):
        SixNodeElement(node, side, sixNodeDescriptor) {
    private val descriptor = sixNodeDescriptor as AnalogChipDescriptor

    private val outputPin = NbtElectricalGateOutput("output")
    private val outputProcess = NbtElectricalGateOutputProcess("outputProcess", outputPin)
    private val inputPins = arrayOfNulls<NbtElectricalGateInput>(3)

    protected val function: AnalogFunction =
            if (descriptor.function.hasState) descriptor.function.javaClass.newInstance()
            else descriptor.function

    init {
        electricalLoadList.add(outputPin)
        for (i in 0..descriptor.function.inputCount - 1) {
            inputPins[i] = NbtElectricalGateInput("input$i")
            electricalLoadList.add(inputPins[i])
        }

        electricalComponentList.add(outputProcess)
        electricalProcessList.add(IProcess { time: Double ->
            val inputs = arrayOfNulls<Double?>(3)
            for (i in 0..2) {
                val inputPin = inputPins[i]
                if (inputPin != null && inputPin.connectedComponents.count() > 0) {
                    inputs[i] = inputPin.u
                }
            }

            outputProcess.setUSafe(function.process(inputs, time))
        })
    }

    override fun getElectricalLoad(lrdu: LRDU?): ElectricalLoad? = when (lrdu) {
        front -> outputPin
        front.inverse() -> inputPins[0]
        front.left() -> inputPins[1]
        front.right() -> inputPins[2]
        else -> null
    }

    override fun getConnectionMask(lrdu: LRDU?): Int = when (lrdu) {
        front -> Node.maskElectricalOutputGate
        front.inverse() -> if (inputPins[0] != null) Node.maskElectricalInputGate else 0
        front.left() -> if (inputPins[1] != null) Node.maskElectricalInputGate else 0
        front.right() -> if (inputPins[2] != null) Node.maskElectricalInputGate else 0
        else -> 0
    }

    override fun multiMeterString(): String? {
        val builder = StringBuilder()
        for (i in 1..3) {
            val pin = inputPins[i - 1]
            if (pin != null && pin.connectedComponents.count() > 0) {
                builder.append("I$i: ").append(if (pin.stateLow()) "0"
                else if (pin.stateHigh()) "1" else "?").append(", ")
            }
        }
        builder.append(I18N.tr(" O: ")).append(if (outputProcess.u == 50.0) "1" else "0")
        return builder.toString()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            sixNode.reconnect()
            needPublish()
            return true
        } else {
            return false
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)
        function.readFromNBT(nbt, "function")
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)
        function.writeToNBT(nbt, "function")
    }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? = null
    override fun thermoMeterString(): String? = null
    override fun initialize() {}
}

open class AnalogChipRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as AnalogChipDescriptor

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        descriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor? = when(lrdu) {
        front -> Eln.instance.signalCableDescriptor.render
        front.inverse() -> if (descriptor.function.inputCount >= 1) Eln.instance.signalCableDescriptor.render else null
        front.left() -> if (descriptor.function.inputCount >= 2) Eln.instance.signalCableDescriptor.render else null
        front.right() -> if (descriptor.function.inputCount >= 3) Eln.instance.signalCableDescriptor.render else null
        else -> null
    }
}

abstract class AnalogFunction: INBTTReady {
    open val hasState = false
    abstract val inputCount: Int
    abstract val infos: String

    internal fun Double.toDigital() = if (this <= 0.2) false
    else if (this >= 0.6) true
    else Math.random() > 0.5

    internal fun Array<Double?>.toDigital(): List<Boolean?> = this.map { it?.toDigital() }

    abstract fun process(inputs: Array<Double?>, deltaTime: Double): Double

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {}
    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {}
}

class OpAmp: AnalogFunction() {
    override val inputCount = 2
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double =
            10000 * ((inputs[0] ?: 0.0) - (inputs[1] ?: 0.0))
}

class PIDRegulator: AnalogFunction() {
    override val hasState = true
    override val inputCount = 2
    override val infos = "TODO"

    private var Kp = 1.0
    private var Ki = 0.0
    private var Kd = 0.0

    private var lastError = 0.0
    private var errorIntegral = 0.0


    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        val error = (inputs[0] ?: 0.0) - (inputs[1] ?: 0.0)
        errorIntegral += error * deltaTime
        val result = Kp * error + Ki * errorIntegral + Kd * (error - lastError) / deltaTime
        lastError = error
        return result
    }
}

class Sum: AnalogFunction() {
    override val inputCount = 3
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double =
            (inputs[0] ?: 0.0) + (inputs[1] ?: 0.0) + (inputs[2] ?: 0.0)
}

class Diff: AnalogFunction() {
    override val inputCount = 2
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double = (inputs[0] ?: 0.0) - (inputs[1] ?: 0.0)
}

class VCO: AnalogFunction() {
    override val inputCount = 1
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        // TODO
        return 0.0
    }
}

class VCA: AnalogFunction() {
    override val inputCount = 2
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        // TODO
        return 0.0
    }
}

class DAC: AnalogFunction() {
    override val inputCount = 3
    override val infos = "TODO"

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        val digitalInputs = inputs.toDigital()
        var output = 0.0
        if (digitalInputs[2] ?: false) output += 13.0 * Eln.SVU / 24.0
        if (digitalInputs[1] ?: false) output += 7.0 * Eln.SVU / 24.0
        if (digitalInputs[0] ?: false) output += 4.0 * Eln.SVU / 24.0
        return output
    }
}
