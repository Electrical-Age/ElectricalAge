package mods.eln.sixnode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer

class LogicGateDescriptor(name: String, obj: Obj3D?, functionName: String, val function: LogicFunction):
        SixNodeDescriptor(name, LogicGateElement::class.java, LogicGateRender::class.java) {
    private val case = obj?.getPart("Case")
    private val top = obj?.getPart(functionName)
    private val pins = arrayOfNulls<Obj3D.Obj3DPart>(4)

    init {
        pins[0] = obj?.getPart("Output")

        for (i in 1..function.inputCount) {
            pins[i] = obj?.getPart("Input$i")
        }
    }

    fun draw() {
        pins.forEach { it?.draw() }
        case?.draw()
        top?.draw()
    }
}

class LogicGateElement(node: SixNode, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElement(node, side, descriptor) {
    override fun getElectricalLoad(lrdu: LRDU?): ElectricalLoad? {
        return null
    }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU?): Int {
        return 0
    }

    override fun multiMeterString(): String? {
        return null
    }

    override fun thermoMeterString(): String? {
        return null
    }

    override fun initialize() {
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }
}

class LogicGateRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as LogicGateDescriptor

    override fun draw() = descriptor.draw()
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
        }
        return output
    }
}
