package mods.eln.mechanical

import mods.eln.gl.GL11

import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream

class JointHubDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    JointHubElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("main"))
    override val rotating = arrayOf(obj.getPart("rotor"))
    /** Its shaft across: couplings on its x faces, turning about x (tools/models/shaftparts.py). */
    val rotatingX = obj.getPart("rotor_x")

    override fun draw(angle: Double) {
        super.draw(angle)
        rotatingX?.draw(((angle * 360).toDouble() / 2.0 / Math.PI).toFloat(), 1f, 0f, 0f)
    }

    override fun getFrontFromPlace(side: Direction?, entityLiving: LivingEntity?): Direction? = Direction.XP
}

class JointHubElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    override val shaftInertia = inertiaOf(0.02, 16000.0)
    private var connectedSides = DirectionSet()

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = 0

    override fun thermoMeterString(side: Direction?): String? = null

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(Direction.XP, Direction.ZP, Direction.XN, Direction.ZN)

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float,
                                  vz: Float): Boolean = false

    override fun connectedOnSide(direction: Direction) {
        connectedSides.add(direction)
        needPublish()
    }

    override fun disconnectedOnSide(direction: Direction) {
        connectedSides.remove(direction)
        needPublish()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        connectedSides.serialize(stream)
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        connectedSides.writeToNBT(nbt, "connectedSides")
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        connectedSides.readFromNBT(nbt, "connectedSides")
    }

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        return info
    }
}

class JointHubRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender: CableRenderDescriptor? = null
    val connectedSides = DirectionSet()

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connectedSides.deserialize(stream)
    }
}

