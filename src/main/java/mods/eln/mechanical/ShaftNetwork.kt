package mods.eln.mechanical

import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.node.NodeManager
import mods.eln.sim.process.destruct.ShaftSpeedWatchdog
import net.minecraft.nbt.NBTTagCompound
import java.util.*


// Speed above which shafts will (by default) explode.
val absoluteMaximumShaftSpeed = 1000.0
// "Standard" drag, in J/t per rad.
val defaultDrag = 0.02


/**
 * Represents the connection between all blocks that are part of the same shaft network.
 */
class ShaftNetwork() : INBTTReady {
    val elements = HashSet<ShaftElement>()

    constructor(first: ShaftElement) : this() {
        elements.add(first)
    }

    // Aggregate properties of the (current) shaft:
    val shapeFactor = 0.5
    val mass: Double
        get() {
            var sum = 0.0
            for (e in elements) {
                sum += e.shaftMass
            }
            return sum
        }
    var rads = 0.0
    var radsLastPublished = rads

    val joulePerRad: Double
        get() = mass * mass * shapeFactor / 2

    var energy: Double
        get() = joulePerRad * rads
        set(value) {
            rads = value / joulePerRad
            if (rads < 0) rads = 0.0
            if (radsLastPublished > rads * 1.05 || radsLastPublished < rads * 0.95) {
                elements.forEach { it.needPublish() }
                radsLastPublished = rads
            }
        }

    /**
     * Merge two shaft networks.
     *
     * @param other The shaft network to merge into this one. Destroyed.
     */
    fun mergeShafts(other: ShaftNetwork) {
        // TODO: Some kind of explosion-effect for severely mismatched speeds?
        // For now, let's be nice.
        rads = Math.min(rads, other.rads)

        assert(other != this)
        for (element in other.elements) {
            elements.add(element)
            element.shaft = this
        }
        other.elements.clear()
    }

    /**
     * Connect a ShaftElement to a shaft network, merging any relevant adjacent networks.
     * @param from The ShaftElement that changed.
     */
    fun connectShaft(from: ShaftElement) {
        assert(from in elements)
        val neighbours = getNeighbours(from)
        for (neighbour in neighbours) {
            if (neighbour.element.shaft != this) {
                mergeShafts(neighbour.element.shaft)

                // Inform the neighbour and the element itself that its shaft connectivity has changed.
                neighbour.element.connectedOnSide(neighbour.side.inverse)
                from.connectedOnSide(neighbour.side)
            }
        }
    }

    /**
     * Disconnect from a shaft network, because an element is dying.
     * @param from The IShaftElement that's going away.
     */
    fun disconnectShaft(from: ShaftElement) {
        elements.remove(from)
        // Going away momentarily, but...
        from.shaft = ShaftNetwork(from)
        // This may have split the network.
        // At the moment there's no better way to figure this out than by exhaustively walking it to check for
        // partitions. Basically fine, as they don't get very large, but a possible target for optimization later on.
        rebuildNetwork()

        // Inform all directly involved shafts about the change in connections.
        for (neighbour in getNeighbours(from)) {
            neighbour.element.disconnectedOnSide(neighbour.side.inverse)
            from.disconnectedOnSide(neighbour.side)
        }
    }

    /**
     * Walk the entire network, splitting as necessary.
     * Yes, this makes breaking a shaft block O(n). Not a problem right now.
     */
    internal fun rebuildNetwork() {
        val unseen = HashSet<ShaftElement>(elements)
        val queue = HashSet<ShaftElement>()
        var shaft = this;
        while (unseen.size > 0) {
            shaft.elements.clear();
            // Do a breadth-first search from an arbitrary element.
            val start = unseen.iterator().next()
            unseen.remove(start);
            queue.add(start);
            while (queue.size > 0) {
                val next = queue.iterator().next()
                queue.remove(next);
                shaft.elements.add(next);
                next.shaft = shaft
                val neighbours = getNeighbours(next)
                for (neighbour in neighbours) {
                    if (unseen.contains(neighbour.element)) {
                        unseen.remove(neighbour.element)
                        queue.add(neighbour.element)
                    }
                }
            }
            // We ran out of network. Any elements remaining in unseen should thus form a new network.
            shaft = ShaftNetwork()
        }
    }

    private fun getNeighbours(from: ShaftElement): ArrayList<ShaftNeighbour> {
        val c = Coordinate()
        val ret = ArrayList<ShaftNeighbour>(6)
        for (dir in from.shaftConnectivity) {
            c.copyFrom(from.coordonate())
            c.move(dir)
            val to = NodeManager.instance!!.getTransparentNodeFromCoordinate(c)
            if (to is ShaftElement) {
                for (dir2 in to.shaftConnectivity) {
                    if (dir2.inverse == dir) {
                        ret.add(ShaftNeighbour(dir, to))
                        break
                    }
                }
            }
        }
        return ret
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String?) {
        rads = nbt.getFloat(str + "rads").toDouble()
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String?) {
        nbt.setFloat(str + "rads", rads.toFloat())
    }

}

interface ShaftElement {
    var shaft: ShaftNetwork
    val shaftMass: Double
    val shaftConnectivity: Array<Direction>
    fun coordonate(): Coordinate

    fun initialize() {
        shaft.connectShaft(this)
    }

    fun needPublish()

    fun connectedOnSide(direction: Direction) {}

    fun disconnectedOnSide(direction: Direction) {}
}

fun createShaftWatchdog(shaftElement: ShaftElement): ShaftSpeedWatchdog {
    return ShaftSpeedWatchdog(shaftElement, absoluteMaximumShaftSpeed)
}

data class ShaftNeighbour(
    val side: Direction,
    val element: ShaftElement
)
