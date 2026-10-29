package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A small electric motor on the shaft (Baughn 2026-10-29): spins a shaft up controllably (a gas turbine that can't
 * start itself, generators brought up to speed before they take load), and drives what sits against its side (a
 * condenser's pumps). Power in on its front; a signal on its back sets the speed it drives to (0-50 V: 0 to 800 rad/s;
 * no signal: 800). It only drives, never brakes; motor-generator sets work and lose what the two machines lose.
 */
class ShaftMotorDescriptor(
    name: String,
    obj: Obj3D,
    val cable: ElectricalCableDescriptor,
    val nominalU: Double,
    val nominalP: Double) :
    SimpleShaftDescriptor(name, ShaftMotorElement::class, null) {

    /** Electrical to shaft. */
    val efficiency = 0.85
    override val obj = obj
    override val static = arrayOf(obj.getPart("Cowl"), obj.getPart("Stand")).requireNoNulls()
    override val rotating = arrayOf(obj.getPart("Shaft")).requireNoNulls()
    /** (The Generator's model, thinner, until its own.) */
    override val drawScaleX = 0.5f

    init {
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable)
    }

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, par4: Boolean) {
        list.add("Spins a shaft up: power in on its front, a speed signal on its back")
        list.add("  (0-50 V: 0 to ${nominalShaftRads.toInt()} rad/s; no signal: ${nominalShaftRads.toInt()}).")
        list.add(Utils.plotVolt("Nominal voltage: ", nominalU) + Utils.plotPower("  Power: ", nominalP))
        list.add("Drives what sits against its side (a condenser's pumps).")
    }
}

class ShaftMotorRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    private val desc = desc as ShaftMotorDescriptor
    private val load = SlewLimiter(1f)

    init {
        mods.eln.sound.MachineSounds.electric(coordonate(), { rads / nominalShaftRads }, { load.position.toDouble() }, 1.6f, 0f, 0.6f)
            .forEach { addLoopedSound(it) }
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        load.step(deltaT)
    }

    override fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if (lrdu != LRDU.Down) return null
        return if (side == front) desc.cable.render else if (side == front.back()) Eln.stdCableRenderSignal else null
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        load.target = stream.readFloat()
    }
}

class ShaftMotorElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    val desc = desc_ as ShaftMotorDescriptor

    /** H = 0.5 s of its power: light. */
    override val shaftInertia = inertiaOf(0.5, desc.nominalP)
    /** Most torque (N m): its power at a quarter of 800 rad/s, so it starts a shaft from rest. */
    val maxTorque = desc.nominalP / (nominalShaftRads / 4)

    internal val load = NbtElectricalLoad("load")
    internal val resistor = Resistor(load, null)
    internal val speed = NbtElectricalGateInput("speed")

    /** Electrical power drawn (W) and shaft power given (W), last step. */
    var drawnW = 0.0
    var shaftW = 0.0
    var lastPublished = 0.0

    init {
        electricalLoadList.add(load)
        electricalLoadList.add(speed)
        electricalComponentList.add(resistor)
        electricalProcessList.add(MotorProcess())
        desc.cable.applyTo(load)
        resistor.setR(1e9)
    }

    /** The speed it drives to (rad/s). */
    fun targetRads() = if (speed.connectedComponents.count() > 0) speed.normalized * nominalShaftRads else nominalShaftRads

    /** How hard it drives now (0..1): what a pump against its side runs at. */
    fun driveShare() = if (desc.nominalP > 0) Math.min(1.0, shaftW / desc.nominalP) else 0.0

    inner class MotorProcess : IProcess {
        override fun process(time: Double) {
            // the energy the circuit gave last step, onto the shaft (under its torque limit; the rest is lost as heat)
            drawnW = resistor.p
            shaftW = shaft.drive(drawnW * desc.efficiency, time, maxTorque) / time
            // next step: full power until within 10 % of its target speed, less as it gets there; never more than the
            // torque limit allows at this speed, nor more than the voltage gives
            val u = load.u
            val target = targetRads()
            val err = if (target > 0) (target - shaft.rads) / target else -1.0
            val vShare = Math.min(1.0, Math.max(0.0, u / desc.nominalU))
            var want = Math.max(0.0, Math.min(1.0, err * 10)) * desc.nominalP * vShare * vShare
            want = Math.min(want, maxTorque * Math.max(shaft.rads, 20.0) / desc.efficiency)
            resistor.setR(if (want > 1 && u > 1) u * u / want else 1e9)
            if (Math.abs(drawnW - lastPublished) > 0.05 * desc.nominalP) {
                lastPublished = drawnW
                needPublish()
            }
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front -> load
            front.back() -> speed
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front -> NodeBase.maskElectricalPower
            front.back() -> NodeBase.maskElectricalInputGate
            else -> 0
        }
    }

    override fun multiMeterString(side: Direction?) =
        Utils.plotER(shaft.energy, shaft.rads) + Utils.plotUIP(load.u, load.current)

    override fun thermoMeterString(side: Direction?) = ""

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(Math.min(1.0, drawnW / desc.nominalP).toFloat())
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info["Speed"] = Utils.plotRads("", shaft.rads)
        info["Drives to"] = Utils.plotRads("", targetRads())
        info["Power"] = Utils.plotPower("", drawnW)
        if (Eln.wailaEasyMode) info["Voltage"] = Utils.plotVolt("", load.u)
        return info
    }
}
