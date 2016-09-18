package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.*
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class JointHubDescriptor(baseName : String, obj : Obj3D): SimpleShaftDescriptor(baseName,
        JointHubElement::class, JointHubRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = emptyArray<Obj3D.Obj3DPart>()
    val staticOnAllSides = arrayOf(obj.getPart("Cap"))
    val rotatingOnAllSides = arrayOf(obj.getPart("Shaft"))

    override fun draw(angle: Double) {
        draw(angle, Direction.XP, DirectionSet());
    }

    fun draw(angle: Double, front: Direction, connectedSides: DirectionSet) {
        static.forEach { it.draw() }

        assert(rotatingOnAllSides.size > 0)
        val bb = rotatingOnAllSides[0].boundingBox()
        val centre = bb.centre()
        val ox = centre.xCoord
        val oy = centre.yCoord
        val oz = centre.zCoord
        var direction = front;
        for (i in 0..3) {
            if (connectedSides.contains(direction)) {
                val rotatingAngle = if (direction == Direction.XP || direction == Direction.ZN) angle else -angle
                preserveMatrix {
                    direction.glRotateXnRef()
                    GL11.glTranslated(ox, oy, oz)
                    GL11.glRotatef(((rotatingAngle * 360).toDouble() / 2.0 / Math.PI).toFloat(), 1f, 0f, 0f)
                    GL11.glTranslated(-ox, -oy, -oz)
                    rotatingOnAllSides.forEach { it.draw() }
                }
            } else {
                preserveMatrix {
                    direction.glRotateXnRef()
                    staticOnAllSides.forEach { it.draw() }
                }
            }

            direction = direction.left();
        }
    }

    override fun getFrontFromPlace(side: Direction?, entityLiving: EntityLivingBase?): Direction? = Direction.XP
}

class JointHubElement(node : TransparentNode, desc_ : TransparentNodeDescriptor): SimpleShaftElement(node, desc_) {
    override val shaftMass = 0.5
    private var connectedSides = DirectionSet()

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = 0

    override fun thermoMeterString(side: Direction?): String? = null

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(Direction.XP, Direction.ZP, Direction.XN, Direction.ZN)

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float,
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

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedSides.writeToNBT(nbt, "connectedSides")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
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

class JointHubRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): ShaftRender(entity, desc) {
    override val cableRender: CableRenderDescriptor? = null
    val desc = desc as JointHubDescriptor
    val connectedSides = DirectionSet()

    override fun draw() {
        front.glRotateXnRef()
        desc.draw(angle, front, connectedSides);
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connectedSides.deserialize(stream)
    }
}
