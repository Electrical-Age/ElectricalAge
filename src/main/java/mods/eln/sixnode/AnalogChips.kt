package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.*
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
import mods.eln.sixnode.SummingUnitElement.Companion.GainChangedEvents
import mods.eln.wiki.Data
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

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

    override fun getWaila(): MutableMap<String,String> = function.getWaila(
            inputPins.map { if (it != null && it.connectedComponents.count() > 0) it.u else null }.toTypedArray(),
            outputPin.u)

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
            Pair("Inputs", (1..inputCount).map {"${inputColors[it - 1]}${Utils.plotVolt("", inputs[it -1] ?: 0.0)}"}.joinToString(" ")),
            Pair("Output", Utils.plotVolt("", output))
    )

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {}
    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {}
}

class OpAmp: AnalogFunction() {
    override val inputCount = 2
    override val infos = I18N.tr("Operational Amplifier - DC coupled\nhigh-gain voltage amplifier with\ndifferential input. Can be used to\ncompare voltages or a configurable amplifier.")

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double =
            10000 * ((inputs[0] ?: 0.0) - (inputs[1] ?: 0.0))
}

class PIDRegulator: AnalogFunction() {
    override val hasState = true
    override val inputCount = 2
    override val infos = I18N.tr("Proportional–integral–derivative controller. A PID\ncontroller continuously calculates an error value as\nthe difference between a desired setpoint and a measured\nprocess variable and applies a correction based on\nproportional, integral, and derivative terms.")

    internal var Kp = 1.0
    internal var Ki = 0.0
        set(value) {
            field = value
            errorIntegral = 0.0
        }
    internal var Kd = 0.0

    private var lastError = 0.0
    private var errorIntegral = 0.0

    override fun process(inputs: Array<Double?>, deltaTime: Double): Double {
        val error = (inputs[0] ?: 0.0) - (inputs[1] ?: 0.0)
        errorIntegral += error * deltaTime
        val result = Kp * error + Ki * errorIntegral + Kd * (error - lastError) / deltaTime
        lastError = error
        return result
    }

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        Kp = nbt?.getDouble("Kp") ?: 1.0
        Ki = nbt?.getDouble("Ki") ?: 0.0
        Kd = nbt?.getDouble("Kd") ?: 0.0
        errorIntegral = nbt?.getDouble("errorIntegral") ?: 0.0
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setDouble("Kp", Kp)
        nbt?.setDouble("Ki", Ki)
        nbt?.setDouble("Kd", Kd)
        nbt?.setDouble("errorIntegral", errorIntegral)
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info[I18N.tr("Params")] = "Kp = $Kp, Ki = $Ki, Kd = $Kd"
        return info
    }
}

class PIDRegulatorElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor):
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
                KpParameterChangedEvent -> (function as PIDRegulator).Kp = stream?.readFloat()?.toDouble() ?: 0.0
                KiParameterChangedEvent -> (function as PIDRegulator).Ki = stream?.readFloat()?.toDouble() ?: 0.0
                KdParameterChangerEvent -> (function as PIDRegulator).Kd = stream?.readFloat()?.toDouble() ?: 0.0
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class PIDRegulatorRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        AnalogChipRender(entity, side, descriptor) {
    internal var Kp = 1f
    internal var Ki = 0f
    internal var Kd = 0f

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = PIDRegulatorGui(this)

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

class PIDRegulatorGui(val render: PIDRegulatorRender): GuiScreenEln() {
    var KpBar: GuiVerticalTrackBar? = null
    var KiBar: GuiVerticalTrackBar? = null
    var KdBar: GuiVerticalTrackBar? = null

    override fun initGui() {
        super.initGui()

        KpBar = newGuiVerticalTrackBar(10, 20, 20, 80)
        KpBar?.setRange(0f, 20f)
        KpBar?.value = render.Kp
        KiBar = newGuiVerticalTrackBar(40, 20, 20, 80)
        KiBar?.value = render.Ki
        KdBar = newGuiVerticalTrackBar(70, 20, 20, 80)
        KdBar?.value = render.Kd
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        KpBar?.setComment(0, KpBar?.value.toString())
        KiBar?.setComment(0, KiBar?.value.toString())
        KdBar?.setComment(0, KdBar?.value.toString())
    }

    override fun guiObjectEvent(`object`: IGuiObject?) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)

            render.preparePacketForServer(stream)

            when (`object`) {
                KpBar -> {
                    stream.writeByte(PIDRegulatorElement.KpParameterChangedEvent)
                    stream.writeFloat(KpBar?.value ?: 0f)
                }
                KiBar -> {
                    stream.writeByte(PIDRegulatorElement.KiParameterChangedEvent)
                    stream.writeFloat(KiBar?.value ?: 0f)
                }
                KdBar -> {
                    stream.writeByte(PIDRegulatorElement.KdParameterChangerEvent)
                    stream.writeFloat(KdBar?.value ?: 0f)
                } else -> return
            }

            render.sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun newHelper() = GuiHelper(this, 214, 118, "pid.png")
}

open class VoltageControlledSawtoothOscillator: AnalogFunction() {
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

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        out = nbt?.getDouble("out") ?: 0.0
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setDouble("out", out)
    }
}

class VoltageControlledSineOscillator: VoltageControlledSawtoothOscillator() {
    override fun process(inputs: Array<Double?>, deltaTime: Double) =
            25.0 + 25.0 * Math.sin(Math.PI * super.process(inputs, deltaTime) / 25.0)
}

class Amplifier: AnalogFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos = I18N.tr("An amplifier increases the voltage\nof an input signal by a configurable\ngain and outputs that voltage.")

    internal var gain = 1.0

    override fun process(inputs: Array<Double?>, deltaTime: Double) = gain * (inputs[0] ?: 0.0)

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        gain = nbt?.getDouble("gain") ?: 1.0
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setDouble("gain", gain)
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gain"] = Utils.plotValue(gain)
        return info
    }
}

class AmplifierElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor):
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
                GainChangedEvent -> (function as Amplifier).gain = stream?.readFloat()?.toDouble() ?: 0.0
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class AmplifierRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        AnalogChipRender(entity, side, descriptor) {
    internal var gain = 1f

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = AmplifierGui(this)

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            gain = stream?.readFloat() ?: 1f
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class AmplifierGui(val render: AmplifierRender): GuiScreenEln() {
    private var gainTF: GuiTextFieldEln? = null

    override fun initGui() {
        super.initGui()

        gainTF = newGuiTextField(6, 6, 50)
        gainTF?.setComment(0, I18N.tr("Gain"))
        gainTF?.setText(render.gain)
        gainTF?.setObserver { textField, text ->
            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)

                render.preparePacketForServer(stream)

                stream.writeByte(AmplifierElement.GainChangedEvent)
                stream.writeFloat(text.toFloat())

                render.sendPacketToServer(bos)
            } catch (e: NumberFormatException) {
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun newHelper() = GuiHelper(this, 62, 24)
}

class VoltageControlledAmplifier: AnalogFunction() {
    override val inputCount = 2
    override val infos = I18N.tr("A voltage-controlled amplifier (VCA)\nis an electronic amplifier that varies\nits gain depending on the control voltage.")

    override fun process(inputs: Array<Double?>, deltaTime: Double) = (inputs[1] ?: 5.0) / 5.0 * (inputs[0] ?: 0.0)

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gain"] = Utils.plotValue((inputs[1] ?: 5.0) / 5.0)
        return info
    }
}

class SummingUnit() : AnalogFunction() {
    override val hasState = true
    override val inputCount = 3
    override val infos = I18N.tr("The summing unit outputs the sum of\nthe tree wighted inputs at it's output.The\ngain for each input can be configured.")

    internal val gains = arrayOf(1.0, 1.0, 1.0)

    override fun process(inputs: Array<Double?>, deltaTime: Double) =
            gains[0] * (inputs[0] ?: 0.0) + gains[1] * (inputs[1] ?: 0.0) + gains[2] * (inputs[2] ?: 0.0)

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        for (i in gains.indices) {
            gains[i] = nbt?.getDouble("gain$i") ?: 1.0
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        for (i in gains.indices) {
            nbt?.setDouble("gain$i", gains[i])
        }
    }

    override fun getWaila(inputs: Array<Double?>, output: Double): MutableMap<String, String> {
        val info = super.getWaila(inputs, output)
        info["Gains"] = (0..2).map { "${inputColors[it]}${Utils.plotValue(gains[it])}" }.joinToString(" ")
        return info
    }
}

class SummingUnitElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor):
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
                GainChangedEvents[0] -> (function as SummingUnit).gains[0] = stream?.readFloat()?.toDouble() ?: 1.0
                GainChangedEvents[1] -> (function as SummingUnit).gains[1] = stream?.readFloat()?.toDouble() ?: 1.0
                GainChangedEvents[2] -> (function as SummingUnit).gains[2] = stream?.readFloat()?.toDouble() ?: 1.0
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class SummingUnitRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        AnalogChipRender(entity, side, descriptor) {
    internal var gains = floatArrayOf(1f, 1f, 1f)

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = SummingUnitGui(this)

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            for (i in gains.indices) { gains[i] = stream?.readFloat() ?: 1f }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class SummingUnitGui(val render: SummingUnitRender): GuiScreenEln() {
    private var gainTFs = arrayOfNulls<GuiTextFieldEln>(3)

    override fun initGui() {
        super.initGui()

        for (i in gainTFs.indices) {
            gainTFs[i] = newGuiTextField(6, 6 + 20 * i, 50)
            gainTFs[i]?.setText(render.gains[i])
            gainTFs[i]?.setObserver { textField, text ->
                try {
                    val bos = ByteArrayOutputStream()
                    val stream = DataOutputStream(bos)

                    render.preparePacketForServer(stream)

                    stream.writeByte(GainChangedEvents[i])
                    stream.writeFloat(text.toFloat())

                    render.sendPacketToServer(bos)
                } catch (e: NumberFormatException) {
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        gainTFs[0]?.setComment(0, I18N.tr("Gain for input \u00a741"))
        gainTFs[1]?.setComment(0, I18N.tr("Gain for input \u00a722"))
        gainTFs[2]?.setComment(0, I18N.tr("Gain for input \u00a713"))
    }

    override fun newHelper() = GuiHelper(this, 62, 64)
}
