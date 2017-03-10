package mods.eln.sixnode.logicgate

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.Node
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.sixnode.AnalogFunction
import mods.eln.wiki.Data
import net.minecraft.client.gui.GuiButton
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

open class LogicGateDescriptor(name: String, obj: Obj3D?, functionName: String, functionClass: Class<out LogicFunction>,
                               elementClass: Class<out LogicGateElement>, renderClass: Class<out LogicGateRender>) :
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

    constructor(name: String, obj: Obj3D?, functionName: String, functionClass: Class<out LogicFunction>) :
        this(name, obj, functionName, functionClass, LogicGateElement::class.java, LogicGateRender::class.java) {
    }

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

open class LogicGateElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElement(node, side, sixNodeDescriptor) {
    private val descriptor = sixNodeDescriptor as LogicGateDescriptor

    private val outputPin = NbtElectricalGateOutput("output")
    private val outputProcess = NbtElectricalGateOutputProcess("outputProcess", outputPin)
    private val inputPins = arrayOfNulls<NbtElectricalGateInput>(3)

    protected val function = if (descriptor.function.hasState) descriptor.function.javaClass.newInstance()
    else descriptor.function

    init {
        electricalLoadList.add(outputPin)
        for (i in 0..descriptor.function.inputCount - 1) {
            inputPins[i] = NbtElectricalGateInput("input$i")
            electricalLoadList.add(inputPins[i])
        }

        electricalComponentList.add(outputProcess)
        electricalProcessList.add(IProcess {
            val inputs = arrayOfNulls<Double?>(3)
            for (i in 0..2) {
                val inputPin = inputPins[i]
                if (inputPin != null && inputPin.connectedComponents.count() > 0) {
                    inputs[i] = inputPin.normalized
                }
            }

            val output = function.process(inputs)
            outputProcess.setOutputNormalizedSafe(if (output) 1.0 else 0.0)
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
        front -> Node.maskElectricalOutputGate;
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
        builder.append(tr(" O: ")).append(if (outputProcess.u == 50.0) "1" else "0")
        return builder.toString()
    }

    override fun getWaila(): MutableMap<String, String> = function.getWaila(
        inputPins.map { if (it != null && it.connectedComponents.count() > 0) it.normalized else null }.toTypedArray(),
        outputPin.u / Eln.SVU)

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

open class LogicGateRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as LogicGateDescriptor

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        descriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor? = when (lrdu) {
        front -> Eln.instance.signalCableDescriptor.render
        front.inverse() -> if (descriptor.function.inputCount >= 1) Eln.instance.signalCableDescriptor.render else null
        front.left() -> if (descriptor.function.inputCount >= 2) Eln.instance.signalCableDescriptor.render else null
        front.right() -> if (descriptor.function.inputCount >= 3) Eln.instance.signalCableDescriptor.render else null
        else -> null
    }
}

abstract class LogicFunction : INBTTReady {
    open val hasState = false
    abstract val inputCount: Int
    abstract val infos: String

    private fun Double.toDigital() = if (this <= 0.2) false
    else if (this >= 0.6) true
    else Math.random() > 0.5

    protected fun Double?.toDigitalString(): String = when {
        this == null -> "-"
        this >= 0.6 -> I18N.tr("ON")
        this <= 0.2 -> I18N.tr("OFF")
        else -> I18N.tr("UNDEF")
    }

    private fun Array<Double?>.toDigital(): List<Boolean?> = this.map { it?.toDigital() }

    open fun process(inputs: Array<Double?>): Boolean = process(inputs.toDigital())
    open fun process(inputs: List<Boolean?>): Boolean = false

    open fun getWaila(inputs: Array<Double?>, output: Double) = mutableMapOf(
        Pair("Inputs", (1..inputCount).map { "${AnalogFunction.inputColors[it - 1]}${inputs[it - 1].toDigitalString()}" }.joinToString(" ")),
        Pair("Output", output.toDigitalString())
    )

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {}
    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {}
}

class Not : LogicFunction() {
    override val inputCount = 1
    override val infos =
        tr("Inverts the input signal.\nOutputs a voltage representing the\nopposite logic-level to its input.")

    override fun process(inputs: List<Boolean?>): Boolean = !(inputs[0] ?: false)
}

open class And : LogicFunction() {
    override val inputCount = 3
    override val infos =
        tr("Implements logical conjunction.\nA 1 (high) output results only if all of\nthe three inputs to the AND gate are 1 (high).")

    override fun process(inputs: List<Boolean?>): Boolean =
        (inputs[0] ?: true) && (inputs[1] ?: true) && (inputs[2] ?: true)
}

class Nand : And() {
    override val infos =
        tr("Its output is complement (inverted)\nto that of the AND gate.")

    override fun process(inputs: List<Boolean?>): Boolean = !super.process(inputs);
}

open class Or : LogicFunction() {
    override val inputCount = 3
    override val infos =
        tr("Implements logical disjunction.\nA 1 (high) output results if at least\none input to the gate is 1 (high).")

    override fun process(inputs: List<Boolean?>): Boolean =
        (inputs[0] ?: false) || (inputs[1] ?: false) || (inputs[2] ?: false)
}

class Nor : Or() {
    override val infos =
        tr("Its output is complement (inverted)\nto that of the OR gate.")

    override fun process(inputs: List<Boolean?>): Boolean = !super.process(inputs)
}

open class Xor : LogicFunction() {
    override val inputCount = 3
    override val infos =
        tr("Implements an exclusive or.\nAn output of 1 (high) results if one or\nall three inputs to the gate are 1 (high).")

    override fun process(inputs: List<Boolean?>): Boolean =
        (inputs[0] ?: false) xor (inputs[1] ?: false) xor (inputs[2] ?: false)
}

class XNor : Xor() {
    override val infos = tr("Its output is complement (inverted)\nto that of the XOR gate.")

    override fun process(inputs: List<Boolean?>): Boolean = !super.process(inputs)
}

class SchmittTrigger : LogicFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos =
        tr("If the input voltage is lower than 10V, the\noutput is 0 (low), if the output is bigger or\nequal to 30V, the output will be 1 (high). For\nall voltages in between, the output does not change.")
    private var state = false

    override fun process(inputs: Array<Double?>): Boolean {
        val input = inputs[0]
        if (input != null) {
            if (input <= 0.2) {
                state = false
            } else if (input >= 0.6) {
                state = true
            }
        }

        return state
    }

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        state = nbt?.getBoolean(str + "state") ?: false
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setBoolean(str + "state", state)
    }
}

class Oscillator : LogicFunction() {
    override val hasState = true
    override val inputCount = 1
    override val infos =
        tr("Outputs a rectangular signal which's frequency\ndepends to the input voltage. The higher the\ninput voltage - the higher the frequency.")
    private var ramp = 0.0
    private var state = false

    override fun process(inputs: Array<Double?>): Boolean {
        ramp += Math.pow(50.0, (inputs[0] ?: 0.0)) / 50
        if (ramp >= 1) {
            ramp = 0.0
            state = !state
        }
        return state
    }

    override fun getWaila(inputs: Array<Double?>, output: Double) = mutableMapOf(
        Pair("Inputs", "${AnalogFunction.inputColors[0]} ${Utils.plotVolt("", inputs[0] ?: 0.0)}"),
        Pair("Output", output.toDigitalString())
    )

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        ramp = nbt?.getDouble(str + "ramp") ?: 0.0
        state = nbt?.getBoolean(str + "state") ?: false
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setDouble(str + "ramp", ramp)
        nbt?.setBoolean(str + "state", state)
    }
}

abstract class TriggeredLogicFunction(private val triggerIndex: Int) : LogicFunction() {
    override final val hasState = true

    private var trigger = false
    private var state = false

    override final fun process(inputs: Array<Double?>): Boolean = super.process(inputs)

    override final fun process(inputs: List<Boolean?>): Boolean {
        val newTrigger = inputs.elementAtOrNull(triggerIndex) ?: false
        if (newTrigger != trigger) {
            if (newTrigger)
                state = onRisingEdge(inputs, state)
            else
                state = onFallingEdge(inputs, state)
            trigger = newTrigger
        }
        return state
    }

    open fun onRisingEdge(inputs: List<Boolean?>, state: Boolean): Boolean = state
    open fun onFallingEdge(inputs: List<Boolean?>, state: Boolean): Boolean = state

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        trigger = nbt?.getBoolean(str + "trigger") ?: false
        state = nbt?.getBoolean(str + "state") ?: false
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setBoolean(str + "trigger", trigger)
        nbt?.setBoolean(str + "state", state)
    }
}

class DFlipFlop : TriggeredLogicFunction(1) {
    override val inputCount = 2
    override val infos =
        tr("The D flip-flop captures the value\nof the D-input at a rising edge\nportion of the clock cycle.")

    override fun onRisingEdge(inputs: List<Boolean?>, state: Boolean): Boolean = inputs[0] ?: false
}

class JKFlipFlop : TriggeredLogicFunction(0) {
    override val inputCount = 3
    override val infos =
        tr("If the input J is 1 (high) and K is 0 (low)\nduring a clock pulse, the output becomes 1 (high).\nIf J is 0 (low) and K is 1 (high) during the pulse,\nthe output becomes 0 (low). If both inputs are 0 (low)\nduring the clock pulse, the state is maintained. If both\ninputs are 1 (high) the input is toggled if a rising edge\nwas detected at the clock input.")

    override fun onRisingEdge(inputs: List<Boolean?>, state: Boolean): Boolean =
        when (Pair(inputs[1] ?: true, inputs[2] ?: true)) {
            Pair(true, false) -> true
            Pair(false, true) -> false
            Pair(true, true) -> !state
            else -> state
        }
}

class PalDescriptor(name: String, obj: Obj3D?) : LogicGateDescriptor(name, obj, "PAL", Pal::class.java,
    PalElement::class.java, PalRender::class.java) {}

class PalElement(node: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    LogicGateElement(node, side, descriptor) {
    companion object {
        val TruthTablePositionClickedEvent = 1
    }

    override fun hasGui(): Boolean = true

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        try {
            (function as Pal).truthTable.forEach { stream?.writeBoolean(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)
        try {
            when (stream?.readByte()?.toInt()) {
                TruthTablePositionClickedEvent -> {
                    val position = stream.readInt()
                    if (position in 0..7) {
                        val truthTable = (function as Pal).truthTable
                        truthTable[position] = !truthTable[position]
                        needPublish()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class PalRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    LogicGateRender(entity, side, descriptor) {
    val truthTable = Array(8, { false })

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? {
        return PalGui(this)
    }

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        try {
            for (i in 0..7) {
                truthTable[i] = stream?.readBoolean() ?: false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class PalGui(val render: PalRender) : GuiScreenEln() {
    val buttons = arrayOfNulls<GuiButton>(8)

    override fun initGui() {
        super.initGui()

        for (i in 0..7) {
            buttons[i] = newGuiButton(42 + (i % 4) * 22, 34 + (i / 4) * 22, 20, "")
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        for (i in 0..7) {
            buttons[i]?.displayString = if (render.truthTable[i]) "1" else "0"
        }
    }

    override fun guiObjectEvent(sender: IGuiObject?) {
        try {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)

            render.preparePacketForServer(stream)

            stream.writeByte(PalElement.TruthTablePositionClickedEvent)
            stream.writeInt(buttons.indexOf(sender as GuiButton))

            render.sendPacketToServer(bos)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun newHelper(): GuiHelper? = GuiHelper(this, 160, 110, "pal.png")
}

class Pal : LogicFunction() {
    override val inputCount = 3
    override val infos =
        tr("A Programmable Array Logic (PAL) is a programmable\nlogic device semiconductors used to  implement any logic\nfunction in only one digital circuit. The function is\nstateless, which means that no intermediate state is saved.")
    val truthTable = Array(8, { false })

    private operator fun Boolean.times(factor: Int): Int = if (this) factor else 0

    private fun Array<Boolean>.toInt(): Int {
        val value = (0..this.count() - 1).filter { this[it] }.sumBy { 1.shl(it) }
        return value
    }

    private fun Array<Boolean>.fromInt(value: Int) {
        for (i in 0..this.count() - 1) {
            this[i] = (value and 1.shl(i)) != 0
        }
    }

    override fun process(inputs: List<Boolean?>): Boolean =
        truthTable[(inputs[0] ?: false) * 4 +
            (inputs[2] ?: false) * 2 +
            ((inputs[2] ?: false) xor (inputs[1] ?: false)) * 1]

    override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
        truthTable.fromInt(nbt?.getInteger(str + "truthTable") ?: 0)
    }

    override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
        nbt?.setInteger(str + "truthTable", truthTable.toInt())
    }
}
