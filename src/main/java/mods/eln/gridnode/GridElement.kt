package mods.eln.gridnode

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.Vec3
import org.apache.commons.lang3.tuple.Pair

import java.io.DataOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

/**
 * Created by svein on 22/08/15.
 */
abstract class GridElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor, internal var connectRange: Int) : TransparentNodeElement(transparentNode, descriptor) {
    var gridLinkList = HashSet<GridLink>()
    var gridLinksBooting = HashSet<GridLink>()
    private val desc: GridDescriptor
    private var idealRenderingAngle: Float = 0.toFloat()

    init {
        this.desc = descriptor as GridDescriptor
    }

    /* Connect one GridNode to another. */
    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        // Check if user is holding an appropriate tool.
        val stack = entityPlayer.currentEquippedItem
        val itemDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stack)
        if (itemDesc is ElectricalCableDescriptor) {
            return onTryGridConnect(entityPlayer, stack, itemDesc, side)
        }
        // TODO: Scissors. Break the connection without breaking the pole.
        return false
    }

    private fun onTryGridConnect(entityPlayer: EntityPlayer, stack: ItemStack, cable: ElectricalCableDescriptor, side: Direction): Boolean {
        // First node, or second node?
        val uuid = entityPlayer.persistentID
        val p = pending[uuid]
        var other: GridElement? = null
        if (p != null) {
            other = GridLink.getElementFromCoordinate(p.left)
        }
        // Check if it's the *correct* cable descriptor.
        if (cable != desc.cableDescriptor) {
            Utils.addChatMessage(entityPlayer, "Wrong cable, you need " + desc.cableDescriptor.name)
            return true
        }
        if (other == null || other === this) {
            Utils.addChatMessage(entityPlayer, "Setting starting point")
            pending.put(uuid, Pair.of(this.coordonate(), side))
        } else {
            val distance = other.coordonate().trueDistanceTo(this.coordonate())
            val cableLength = Math.ceil(distance).toInt()
            val range = Math.min(connectRange, other.connectRange)
            if (stack.stackSize < distance) {
                Utils.addChatMessage(entityPlayer, "You need $cableLength units of cable")
            } else if (distance > range) {
                Utils.addChatMessage(entityPlayer, "Cannot connect, range " + Math.ceil(distance) + " and limit " + range + " blocks")
            } else if (!this.canConnect(other)) {
                Utils.addChatMessage(entityPlayer, "Cannot connect these two objects")
            } else if (!this.validLOS(other)) {
                Utils.addChatMessage(entityPlayer, "Cannot connect, no line of sight")
            } else {
                if (GridLink.addLink(this, other, side, p!!.right, cable, cableLength)) {
                    Utils.addChatMessage(entityPlayer, "Added connection")
                    stack.splitStack(cableLength)
                } else {
                    Utils.addChatMessage(entityPlayer, "Already connected")
                }
            }
            pending.remove(uuid)
        }
        return true
    }

    override fun initialize() {
        connect()
        for (link in gridLinksBooting) {
            link.connect()
        }
        gridLinksBooting.clear()
        updateIdealRenderAngle()
    }

    override fun connectJob() {
        super.connectJob()
        for (link in gridLinkList) {
            link.connect()
        }
    }

    override fun disconnectJob() {
        super.disconnectJob()
        for (link in gridLinkList) {
            link.disconnect()
        }
    }

    override fun onBreakElement() {
        super.onBreakElement()
        val copy = HashSet(gridLinkList)
        for (link in copy) {
            node.dropItem(link.onBreakElement())
        }
    }

    override fun selfDestroy() {
        super.selfDestroy()
        val copy = HashSet(gridLinkList)
        for (link in copy) {
            link.selfDestroy()
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)

        var i: Int? = 0
        val gridLinks = Utils.newNbtTagCompund(nbt, "gridLinks")
        for (link in gridLinkList) {
            link.writeToNBT(Utils.newNbtTagCompund(gridLinks, i!!.toString()), "")
            i++
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)

        assert(gridLinkList.isEmpty())
        val gridLinks = nbt.getCompoundTag("gridLinks")
        var i: Int? = 0
        while (true) {
            val linkTag = gridLinks.getCompoundTag(i!!.toString())
            if (linkTag.hasNoTags())
                break
            gridLinksBooting.add(GridLink(linkTag, ""))
            i++
        }
    }

    abstract fun getGridElectricalLoad(side: Direction): ElectricalLoad?

    // TODO: This should check if the wire isn't passing through blocks.
    private fun validLOS(other: GridElement): Boolean {
        return true
    }

    // Return false if connecting grid elements that can't connect.
    protected fun canConnect(other: GridElement): Boolean {
        return true
    }

    // TODO: One pole turns, all connected cables should be recalculated,
    // not just the ones being rendered here.
    /* Compute a rendering angle that minimizes any straight-on cables. */
    fun updateIdealRenderAngle() {
        if (desc.rotationIsFixed()) {
            when (front) {
                Direction.XN -> idealRenderingAngle = 0f
                Direction.XP -> idealRenderingAngle = 180f
                Direction.YN -> idealRenderingAngle = 90f
                Direction.YP -> idealRenderingAngle = 90f
                Direction.ZN -> idealRenderingAngle = 270f
                Direction.ZP -> idealRenderingAngle = 90f
            }
            //System.out.println(idealRenderingAngle);
        } else if (gridLinkList.size == 0) {
            idealRenderingAngle = 0f
        } else {
            // Compute angles.
            val angles = DoubleArray(gridLinkList.size)
            var i = 0
            for (link in gridLinkList) {
                var vec = link.a.subtract(link.b)
                // Angles 180 degrees apart are equivalent.
                if (vec.z < 0)
                    vec = vec.negate()
                val h = Math.sqrt((vec.x * vec.x + vec.z * vec.z).toDouble())
                angles[i++] = Math.acos(vec.x / h)
            }
            // This could probably be optimised with a bit of math, but w.e.
            var optAngle = 0.0
            var optErr = java.lang.Double.POSITIVE_INFINITY
            i = 0
            while (i < 128) {
                // Check a half-circle.
                val angle = Math.PI * i / 128.0
                var error = 0.0
                for (a in angles) {
                    val err = Math.abs(Math.sin(angle - a))
                    error += err * err * err
                }
                if (error < optErr) {
                    optAngle = angle
                    optErr = error
                }
                i++
            }
            idealRenderingAngle = Math.toDegrees(-optAngle).toFloat()
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeFloat(idealRenderingAngle)
            // Each wire pair should be drawn by exactly one node.
            // Check for which ones it's this one.
            val ourLinks = ArrayList<GridLink>()
            for (link in gridLinkList) {
                if (link.a == coordonate()/* && link.connected*/) {
                    ourLinks.add(link)
                }
            }
            // The renderer needs to know, for each catenary:
            // - Vec3 of the starting point.
            // - Vec3 of the end point.
            // There's a finite number of starting points, and a potentially unlimited number of endpoints...
            // But until we get protocol buffers or something, simple remains good.
            // So we'll just send pairs, even if there's some duplication.
            stream.writeInt(ourLinks.size)
            for (link in ourLinks) {
                val target = link.getOtherElement(this)
                val ourSide = link.getSide(this)
                val theirSide = link.getSide(target)
                // It's always the "a" side doing this.
                val offset = link.b.subtract(link.a)
                for (i in 0..1) {
                    val start = getCablePoint(ourSide, i)
                    start.rotateAroundY(Math.toRadians(idealRenderingAngle.toDouble()).toFloat())
                    var end = target.getCablePoint(theirSide, i)
                    end.rotateAroundY(Math.toRadians(target.idealRenderingAngle.toDouble()).toFloat())
                    end = end.addVector(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
                    writeVec(stream, start)
                    writeVec(stream, end)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    protected open fun getCablePoint(side: Direction, i: Int): Vec3 {
        if (i >= 2) throw AssertionError("Invalid cable point index")
        val part = (if (i == 0) desc.plus else desc.gnd)[0]
        val bb = part.boundingBox()
        return bb.centre()
    }

    @Throws(IOException::class)
    private fun writeVec(stream: DataOutputStream, sp: Vec3) {
        stream.writeFloat(sp.xCoord.toFloat())
        stream.writeFloat(sp.yCoord.toFloat())
        stream.writeFloat(sp.zCoord.toFloat())
    }

    override fun multiMeterString(side: Direction): String {
        val electricalLoad = getGridElectricalLoad(side)
        return Utils.plotUIP(electricalLoad?.u ?: 0.0, electricalLoad?.i ?: 0.0)
    }

    override fun thermoMeterString(side: Direction): String? {
        val thermalLoad = getThermalLoad(side, LRDU.Up)
        return Utils.plotCelsius("T", thermalLoad.Tc)
    }

    companion object {
        /**
         * The last place any given player tried to link two instance nodes.
         */
        private val pending = HashMap<UUID, Pair<Coordonate, Direction>>()
    }
}
