package mods.eln.mechanical

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

class VerticalHubDescriptor(baseName: String, obj: Obj3D):
    SimpleShaftDescriptor(
        baseName,
        VerticalHubElement::class,
        VerticalHubRender::class,
        EntityMetaTag.Basic
    )
{
    override val obj = obj
    override val static = arrayOf(obj.getPart("Cowl"))
    override val rotating = emptyArray<Obj3D.Obj3DPart>()

    val staticOnDisconnectedSides = arrayOf(obj.getPart("Cap"))
    val rotatingOnConnectedSides = arrayOf(obj.getPart("Shaft"))
    val staticOnOpaqueSides = arrayOf(obj.getPart("Stand"))

    override fun draw(angle: Double) {
        draw(angle, DirectionSet(), DirectionSet())
    }

    fun draw(angle: Double, connectedSides: DirectionSet, standingSides: DirectionSet) {
        val bb = rotatingOnConnectedSides[0].boundingBox()
        val center = bb.centre()
        val ox = center.xCoord
        val oy = center.yCoord
        val oz = center.zCoord

        var renderCowl = true

        if(standingSides.isEmpty()) {
            when(connectedSides.size) {
                1 -> renderCowl = false
                2 -> for(axis in Direction.axes) {
                    val axisSet = DirectionSet()
                    axis.forEach { axisSet.add(it) }
                    if(Direction.all.filter {
                        connectedSides.contains(it) != axisSet.contains(it)
                    }.isEmpty()) {
                        renderCowl = false
                        break
                    }
                }
            }
        }

        if(renderCowl) static.forEach { it.draw() }

        for(dir in Direction.all) {
            val corDir = when(dir) {
                Direction.YN -> Direction.YP
                Direction.YP -> Direction.YN
                else -> dir
            }
            if(connectedSides.contains(dir)) {
                val ang = if (dir == Direction.XP || dir == Direction.ZN || dir == Direction.YP) {
                    angle
                } else {
                    -angle
                }
                preserveMatrix {
                    corDir.glRotateXnRef()
                    GL11.glTranslated(ox, oy, oz)
                    GL11.glRotatef(((ang * 360).toDouble() / 2 / Math.PI).toFloat(), 1f, 0f, 0f)
                    GL11.glTranslated(-ox, -oy, -oz)
                    rotatingOnConnectedSides.forEach { it.draw() }
                }
            } else if(standingSides.contains(dir)) {
                preserveMatrix {
                    corDir.glRotateXnRef()
                    staticOnOpaqueSides.forEach { it.draw() }
                }
            } else if(renderCowl) {
                preserveMatrix {
                    corDir.glRotateXnRef()
                    staticOnDisconnectedSides.forEach { it.draw() }
                }
            }
        }
    }

    override fun getFrontFromPlace(side: Direction?, entityLiving: EntityLivingBase?): Direction = Direction.XP

    override fun mustHaveFloor(): Boolean = false
}

class VerticalHubElement(node: TransparentNode, desc_: TransparentNodeDescriptor):
    SimpleShaftElement(node, desc_)
{
    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(Direction.XP, Direction.YP, Direction.ZP, Direction.XN, Direction.YN, Direction.ZN)

    private var connectedSides = DirectionSet()
    private var standingSides = DirectionSet()

    override fun initialize() {
        super.initialize()
        scanStandingSides()
    }

    fun scanStandingSides() {
        standingSides.clear()
        for(dir in Direction.all) {
            if(connectedSides.contains(dir)) continue
            val test = coordonate().moved(dir)
            if(test.block.isOpaqueCube)
                standingSides.add(dir)
        }
    }

    override fun connectedOnSide(direction: Direction, net: ShaftNetwork) {
        connectedSides.add(direction)
    }

    override fun disconnectedOnSide(direction: Direction, net: ShaftNetwork?) {
        connectedSides.remove(direction)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        connectedSides.serialize(stream)
        standingSides.serialize(stream)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedSides.writeToNBT(nbt, "connectedSides")
        standingSides.writeToNBT(nbt, "standingSides")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        connectedSides.readFromNBT(nbt, "connectedSides")
        standingSides.readFromNBT(nbt, "standingSides")
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = 0

    override fun thermoMeterString(side: Direction?): String = ""

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean = false
    override fun onNeighborBlockChange() {
        scanStandingSides()
    }
}

class VerticalHubRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor):
    ShaftRender(entity, desc)
{
    val desc = desc as VerticalHubDescriptor
    val connectedSides = DirectionSet()
    val standingSides = DirectionSet()

    override fun draw() {
        front.glRotateXnRef()
        desc.draw(angle, connectedSides, standingSides)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connectedSides.deserialize(stream)
        standingSides.deserialize(stream)
    }
}
