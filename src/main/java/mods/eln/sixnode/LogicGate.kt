package mods.eln.sixnode.logicgate

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.Node
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer

class LogicGateDescriptor(name: String, obj: Obj3D?, functionName: String, functionClass: Class<out LogicFunction>):
        SixNodeDescriptor(name, LogicGateElement::class.java, LogicGateRender::class.java) {
    private val case = obj?.getPart("Case")
    private val top = obj?.getPart(functionName)
    private val pins = arrayOfNulls<Obj3D.Obj3DPart>(4)
    internal val function = functionClass.newInstance()

    init {
        pins[0] = obj?.getPart("Output")
        for (i in 1..function.inputCount) pins[i] = obj?.getPart("Input$i")
    }

    fun draw() {
        pins.forEach { it?.draw() }
        case?.draw()
        top?.draw()
    }
}

class LogicGateElement(node: SixNode, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElement(node, side, descriptor) {
    private val gateDescriptor = descriptor as LogicGateDescriptor
    private val outputPin = NbtElectricalGateOutput("output")
    private val outputProcess = NbtElectricalGateOutputProcess("outputProcess", outputPin)
    private val inputPins = arrayOfNulls<NbtElectricalGateInput>(3)

    init {
        electricalLoadList.add(outputPin)

        for (i in 0..gateDescriptor.function.inputCount - 1) {
            inputPins[i] = NbtElectricalGateInput("input$i")
            electricalLoadList.add(inputPins[i])
        }

        electricalComponentList.add(outputProcess)
        electricalProcessList.add(object: IProcess {
            override fun process(time: Double) {
                val inputs = arrayOfNulls<Boolean?>(3)
                for (i in 0..2) {
                    val inputPin = inputPins[i]
                    if (inputPin != null && inputPin.connectedComponents.count() > 0) {
                        if (inputPin.stateHigh()) {
                            inputs[i] = true
                        } else if (inputPin.stateLow()) {
                            inputs[i] = false
                        } else {
                            inputs[i] = Math.random() > 0.5
                        }
                    }
                }

                val output = gateDescriptor.function.process(inputs[0], inputs[1], inputs[2])
                outputProcess.setOutputNormalizedSafe(if (output) 1.0 else 0.0)
            }
        })
    }

    override fun getElectricalLoad(lrdu: LRDU?): ElectricalLoad? = when (lrdu) {
            front -> outputPin
            front.inverse() -> inputPins[0]
            front.left() -> inputPins[1]
            front.right() -> inputPins[2]
            else -> null
        }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? = null

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

    override fun thermoMeterString(): String? = null

    override fun initialize() {}

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            sixNode.reconnect()
            sixNode.needPublish = true
            return true
        } else {
            return false
        }
    }
}

class LogicGateRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as LogicGateDescriptor

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

interface LogicFunction {
    val inputCount: Int
    fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean
}

class Not: LogicFunction {
    override val inputCount = 1
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean = !(a ?: false)
}

class And: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean = (a ?: true) && (b ?: true) && (c ?: true)
}

class Or: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean = (a ?: false) || (b ?: false) || (c ?: false)
}

class Nand: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean = !((a ?: true) && (b ?: true) && (c ?: true))
}

class Nor: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean =
            !((a ?: false) || (b ?: false) || (c ?: false))
}

class Xor: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean =
            (a ?: false) xor (b ?: false) xor (c ?: false)
}

class XNor: LogicFunction {
    override val inputCount = 3
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean =
            !((a ?: false) xor (b ?: false) xor (c ?: false))
}

class DFlipFlop: LogicFunction {
    override val inputCount = 2
    private var triggered = false
    private var output = false
    override fun process(a: Boolean?, b: Boolean?, c: Boolean?): Boolean {
        if (!triggered && (b ?: false)) {
            triggered = true
            output = a ?: false
        } else if (triggered && !(b ?: false)) {
            triggered = false
        }
        return output
    }
}
