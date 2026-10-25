package mods.eln.sixnode

import mods.eln.sixnode.logicgate.CHIP_SCALE
import mods.eln.gl.GL11


import mods.eln.registry.ElnDeviceRegistry
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.Synchronizable
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.sixnode.SummingUnitElement.Companion.GainChangedEvents
import mods.eln.solver.Equation
import mods.eln.solver.IValue
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

open class AnalogChipDescriptor(name: String, obj: Obj3D?, functionName: String,
                                functionClass: Class<out AnalogFunction>,
                                elementClass: Class<out AnalogChipElement>, renderClass: Class<*>?) :
    SixNodeDescriptor(name, elementClass, renderClass) {
    private val case = obj?.getPart("Case")
    private val top = obj?.getPart(functionName)
    private val pins = arrayOfNulls<Obj3D.Obj3DPart>(4)

    internal val function = functionClass.newInstance()

    init {
        pins[0] = obj?.getPart("Output")
        for (i in 1..function.inputCount) pins[i] = obj?.getPart("Input$i")

        voltageLevelColor = VoltageLevelColor.SignalVoltage
        modelScale = CHIP_SCALE
    }

    /** Placed anywhere on a face of a voxel block, several to a face (Baughn: pack chips). */
    override fun placeAnywhere() = true

    override fun drawsOwnPins() = true

    override fun drawPlaced(side: Direction, front: LRDU) {
        front.glRotateOnX()
        draw()
    }

    /** The case, without its pins (3 voxels at CHIP_SCALE). */
    override fun bodyHalf(): Int {
        val b = case?.boundingBox() ?: return super.bodyHalf()
        val across = maxOf(-b.min.y, b.max.y, -b.min.z, b.max.z) * CHIP_SCALE * 16
        return Math.ceil(across - 0.01).toInt().coerceIn(1, 8)
    }

    constructor(name: String, obj: Obj3D?, functionName: String, functionClass: Class<out AnalogFunction>) :
        this(name, obj, functionName, functionClass, AnalogChipElement::class.java, null)
    fun draw() {
        GL11.glPushMatrix()
        GL11.glScalef(CHIP_SCALE, CHIP_SCALE, CHIP_SCALE)
        pins.forEach { it?.draw() }
        case?.draw()
        top?.draw()
        GL11.glPopMatrix()
    }


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)
    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    override fun hasItemDraw() = true

    override fun drawItem(entity: Boolean) {
        GL11.glTranslatef(0.0f, 0.0f, -0.2f)
        GL11.glScalef(1.25f / CHIP_SCALE, 1.25f / CHIP_SCALE, 1.25f / CHIP_SCALE)   // (the item as big as before)
        GL11.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f)
        draw()

    }


    override fun getFrontFromPlace(side: Direction?, player: Player?): LRDU? =
        super.getFrontFromPlace(side, player).left()

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addSignal(newItemStack())
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (list != null) function.infos.split("\n").forEach { list.add(it) }
    }
}

open class AnalogChipElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
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

    override fun getWaila(): MutableMap<String, String> = function.getWaila(
        inputPins.map { if (it != null && it.connectedComponents.count() > 0) it.u else null }.toTypedArray(),
        outputPin.u)

    override fun readFromNBT(nbt: CompoundTag?) {
        super.readFromNBT(nbt)
        function.readFromNBT(nbt, "function")
    }

    override fun writeToNBT(nbt: CompoundTag?) {
        super.writeToNBT(nbt)
        function.writeToNBT(nbt, "function")
    }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? = null
    override fun thermoMeterString(): String? = null
    override fun initialize() {}
}

open class AnalogChipRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as AnalogChipDescriptor

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        descriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor? = when (lrdu) {
        front -> ElnDeviceRegistry.signalCableDescriptor.render
        front.inverse() -> if (descriptor.function.inputCount >= 1) ElnDeviceRegistry.signalCableDescriptor.render else null
        front.left() -> if (descriptor.function.inputCount >= 2) ElnDeviceRegistry.signalCableDescriptor.render else null
        front.right() -> if (descriptor.function.inputCount >= 3) ElnDeviceRegistry.signalCableDescriptor.render else null
        else -> null
    }
}


abstract class AnalogFunction : INBTTReady {
    companion object {
        val inputColors = arrayOf("§c", "§a", "§9")
    }

    open val hasState = false
    abstract val inputCount: Int
    abstract val infos: String

    internal fun Double.toDigital() = if (this <= 0.2) false
    else if (this >= 0.6) true
    else Math.random() > 0.5

    internal fun Array<Double?>.toDigital(): List<Boolean?> = this.map { it?.toDigital() }

    abstract fun process(inputs: Array<Double?>, deltaTime: Double): Double

    open fun getWaila(inputs: Array<Double?>, output: Double) = mutableMapOf(
        Pair("Inputs", (1..inputCount).map { "${inputColors[it - 1]}${Utils.plotVolt("", inputs[it - 1] ?: 0.0)}" }.joinToString(" ")),
        Pair("Output", Utils.plotVolt("", output))
    )

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {}
    override fun writeToNBT(nbt: CompoundTag?, str: String?) {}
}

class OpAmp : AnalogFunction() {
    override val inputCount = 2
    override val infos: String = I18N.tr("Operational Amplifier - DC coupled\nhigh-gain voltage amplifier with\ndifferential input. Can be used to\ncompare voltages or as configurable amplifier.")

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double =
        10000 * ((inputs[0] ?: 0.0) - (inputs[1] ?: 0.0))
}

class PIDRegulator : AnalogFunction() {
    override val hasState = true
    override val inputCount = 2
    override val infos: String = I18N.tr("Proportional–integral–derivative controller. A PID\ncontroller continuously calculates an error value as\nthe difference between a desired setpoint and a measured\nprocess variable and applies a correction based on\nproportional, integral, and derivative terms.")

    internal var Kp = 1.0
    internal var Ki = 0.0
    internal var Kd = 0.0
    private val pid = Equation.Pid()

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        pid.setOperator(arrayOf(
            IValue { (inputs[0] ?: 0.0) / Eln.SVU },
            IValue { (inputs[1] ?: 0.0) / Eln.SVU } ,
            IValue { Kp }, IValue { Ki} , IValue { Kd }
        ))
        pid.process(deltaTime)
        return Eln.SVU * pid.value
    }

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        Kp = nbt?.getDoubleOr("Kp", 0.0) ?: 1.0
        Ki = nbt?.getDoubleOr("Ki", 0.0) ?: 0.0
        Kd = nbt?.getDoubleOr("Kd", 0.0) ?: 0.0
        pid.readFromNBT(nbt, "pid")
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        nbt?.putDouble("Kp", Kp)
        nbt?.putDouble("Ki", Ki)
        nbt?.putDouble("Kd", Kd)
        pid.writeToNBT(nbt, "pid")
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info[I18N.tr("Params")] = "Kp = $Kp, Ki = $Ki, Kd = $Kd"
        if (Eln.wailaEasyMode) {
            info[I18N.tr("State")] = "Si = ${pid.iStack}"
        }
        return info
    }
}

class PIDRegulatorElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    AnalogChipElement(node, side, sixNodeDescriptor) {
    companion object {
        val KpParameterChangedEvent = 1
        val KiParameterChangedEvent = 2
        val KdParameterChangerEvent = 3
    }

    override fun hasGui() = true

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        try {
            with(function as PIDRegulator) {
                stream?.writeFloat(Kp.toFloat())
                stream?.writeFloat(Ki.toFloat())
                stream?.writeFloat(Kd.toFloat())
            }
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)
        try {
            when (stream?.readByte()?.toInt()) {
                KpParameterChangedEvent -> (function as PIDRegulator).Kp = stream.readFloat().toDouble()
                KiParameterChangedEvent -> (function as PIDRegulator).Ki = stream.readFloat().toDouble()
                KdParameterChangerEvent -> (function as PIDRegulator).Kd = stream.readFloat().toDouble()
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class PIDRegulatorRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    AnalogChipRender(entity, side, descriptor) {
    internal var Kp = 1f
    internal var Ki = 0f
    internal var Kd = 0f

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            Kp = stream?.readFloat() ?: 1f
            Ki = stream?.readFloat() ?: 0f
            Kd = stream?.readFloat() ?: 0f
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


// 26.x port: class PIDRegulatorGui removed (client; redone in P3/P4)


open class VoltageControlledSawtoothOscillator : AnalogFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos = I18N.tr("A voltage-controlled oscillator or VCO is\nan electronic oscillator whose oscillation\nfrequency is controlled by a voltage input.")

    private var out = 0.0

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        out += Math.pow(50.0, (inputs[0] ?: 0.0) / 50) * 2 * deltaTime
        if (out > Eln.SVU) {
            out = 0.0
        }
        return out
    }

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        out = nbt?.getDoubleOr("out", 0.0) ?: 0.0
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        nbt?.putDouble("out", out)
    }
}

class VoltageControlledSineOscillator : VoltageControlledSawtoothOscillator() {
    override fun process(inputs: Array<Double?>, deltaTime: Double) =
        25.0 + 25.0 * Math.sin(Math.PI * super.process(inputs, deltaTime) / 25.0)
}

class Amplifier : AnalogFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos = I18N.tr("An amplifier increases the voltage\nof an input signal by a configurable\ngain and outputs that voltage.")

    internal var gain = 1.0

    override fun process(inputs: Array<Double?>, deltaTime: Double) = gain * (inputs[0] ?: 0.0)

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        gain = nbt?.getDoubleOr("gain", 0.0) ?: 1.0
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        nbt?.putDouble("gain", gain)
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gain"] = Utils.plotValue(gain)
        return info
    }
}

class AmplifierElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    AnalogChipElement(node, side, sixNodeDescriptor) {

    companion object {
        val GainChangedEvent = 1
    }

    override fun hasGui() = true

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)

        try {
            with(function as Amplifier) {
                stream?.writeFloat(gain.toFloat())
            }
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)

        try {
            when (stream?.readByte()?.toInt()) {
                GainChangedEvent -> (function as Amplifier).gain = stream.readFloat().toDouble()
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class AmplifierRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    AnalogChipRender(entity, side, descriptor) {
    internal var gain = 1f

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            gain = stream?.readFloat() ?: 1f
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


// 26.x port: class AmplifierGui removed (client; redone in P3/P4)


class VoltageControlledAmplifier : AnalogFunction() {
    override val inputCount = 2
    override val infos = I18N.tr("A voltage-controlled amplifier (VCA)\nis an electronic amplifier that varies\nits gain depending on the control voltage.")

    override fun process(inputs: Array<Double?>, deltaTime: Double) = (inputs[1] ?: 5.0) / 5.0 * (inputs[0] ?: 0.0)

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gain"] = Utils.plotValue((inputs[1] ?: 5.0) / 5.0)
        return info
    }
}

class SummingUnit : AnalogFunction() {
    override val hasState = true
    override val inputCount = 3
    override val infos = I18N.tr("The summing unit outputs the sum of\nthe three weighted inputs.The\ngain for each input can be configured.")

    internal val gains = arrayOf(1.0, 1.0, 1.0)

    override fun process(inputs: Array<Double?>, deltaTime: Double) =
        gains[0] * (inputs[0] ?: 0.0) + gains[1] * (inputs[1] ?: 0.0) + gains[2] * (inputs[2] ?: 0.0)

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        for (i in gains.indices) {
            gains[i] = nbt?.getDoubleOr("gain$i", 0.0) ?: 1.0
        }
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        for (i in gains.indices) {
            nbt?.putDouble("gain$i", gains[i])
        }
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gains"] = (0..2).map { "${inputColors[it]}${Utils.plotValue(gains[it])}" }.joinToString(" ")
        return info
    }
}

class SummingUnitElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    AnalogChipElement(node, side, sixNodeDescriptor) {

    companion object {
        val GainChangedEvents = arrayOf(1, 2, 3)
    }

    override fun hasGui() = true

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)

        try {
            with(function as SummingUnit) {
                gains.forEach {
                    stream?.writeFloat(it.toFloat())
                }
            }
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)

        try {
            when (stream?.readByte()?.toInt()) {
                GainChangedEvents[0] -> (function as SummingUnit).gains[0] = stream.readFloat().toDouble()
                GainChangedEvents[1] -> (function as SummingUnit).gains[1] = stream.readFloat().toDouble()
                GainChangedEvents[2] -> (function as SummingUnit).gains[2] = stream.readFloat().toDouble()
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class SummingUnitRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    AnalogChipRender(entity, side, descriptor) {
    internal var gains = floatArrayOf(1f, 1f, 1f)

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            for (i in gains.indices) {
                gains[i] = stream?.readFloat() ?: 1f
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


// 26.x port: class SummingUnitGui removed (client; redone in P3/P4)


class SampleAndHold : AnalogFunction() {
    override val hasState = true
    override val inputCount = 2
    override val infos = I18N.tr("Samples the voltage of a varying analog signal when\nthe clock input changes from 0 to 1 and holds its\noutput voltage at a constant level until next clock pulse.\nYou can see it as an analog D-Flipflop.")
    private var clock = false
    private var value = 0.0

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        val clock = inputs[1] ?: 0.0 > 0.5
        if (clock && !this.clock) value = inputs[0] ?: 0.0
        this.clock = clock
        return value
    }

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        clock = nbt?.getBooleanOr("clock", false) ?: false
        value = nbt?.getDoubleOr("value", 0.0) ?: 0.0
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        nbt?.putBoolean("clock", clock)
        nbt?.putDouble("value", value)
    }
}

class Filter: AnalogFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos = I18N.tr("Lowpass filter - Passes signals with a\nfrequency lower than a certain cutoff frequency\nand attenuates signals with frequencies higher\nthan the cutoff frequency.")

    internal var feedback = 2.0 * Math.PI * 5.0
    private var output = 0.0

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        output = Utils.limit(output + ((inputs[0] ?: 0.0) - output) * feedback * deltaTime, 0.0, 50.0)
        return output
    }

    override fun readFromNBT(nbt: CompoundTag?, str: String?) {
        nbt?.apply {
            feedback = getDoubleOr("feedback", 0.0)
            output = getDoubleOr("output", 0.0)
        }
    }

    override fun writeToNBT(nbt: CompoundTag?, str: String?) {
        nbt?.apply {
            putDouble("feedback", feedback)
            putDouble("output", output)
        }
    }
}

class FilterElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    AnalogChipElement(node, side, sixNodeDescriptor) {

    enum class Event(val value: Byte) {
        CUTOFF_FREQUENCY_CHANGED(1)
    }

    private var cutOffFrequency = Eln.electricalFrequency / 4.0
        get() = (function as Filter).feedback / (2.0 * Math.PI)
        set(value) {
            field = value
            (function as Filter).feedback = 2.0 * Math.PI * field
        }

    override fun hasGui() = true

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)

        try {
            stream?.writeFloat(cutOffFrequency.toFloat())
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)

        try {
            when (stream.readByte()) {
                Event.CUTOFF_FREQUENCY_CHANGED.value -> cutOffFrequency = stream.readFloat().toDouble()
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class FilterRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    AnalogChipRender(entity, side, descriptor) {
    internal var cutOffFrequency = Synchronizable(Eln.electricalFrequency.toFloat() / 4f)

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            cutOffFrequency.value = stream.readFloat()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


// 26.x port: class FilterGui removed (client; redone in P3/P4)

