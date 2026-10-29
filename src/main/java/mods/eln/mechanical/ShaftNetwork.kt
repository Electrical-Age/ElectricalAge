package mods.eln.mechanical

import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.node.NodeManager
import mods.eln.sim.process.destruct.ShaftSpeedWatchdog
import net.minecraft.nbt.CompoundTag
import java.util.*


// Speed above which shafts will (by default) explode.
val absoluteMaximumShaftSpeed = 1000.0
// "Standard" drag, in J/t per rad.
val defaultDrag = 0.02
/** The speed shaft machines are rated at (rad/s): the generators' 800. */
const val nominalShaftRads = 800.0
/** A floor for a network's inertia (kg m^2), so an empty or tiny network has a defined speed. */
const val minimumInertia = 1e-4

/**
 * Moment of inertia (kg m^2) from an inertia constant: h seconds of `power` (W) stored at `rads` (Baughn 2026-10-29:
 * realistic values; a turbine-generator train is a few seconds, a flywheel tens).
 */
fun inertiaOf(h: Double, power: Double, rads: Double = nominalShaftRads) = 2 * h * power / (rads * rads)


/**
 * Represents the connection between all blocks that are part of the same shaft network.
 */
class ShaftNetwork() : INBTTReady {
    val elements = HashSet<ShaftElement>()

    constructor(first: ShaftElement) : this() {
        elements.add(first)
    }

    // Aggregate properties of the (current) shaft:
    /** Moment of inertia (kg m^2): the sum of its blocks'. */
    val inertia: Double
        get() {
            var sum = 0.0
            for (e in elements) {
                sum += e.shaftInertia
            }
            return Math.max(sum, minimumInertia)
        }
    var rads = 0.0
    var radsLastPublished = rads

    /**
     * Kinetic energy, 1/2 I w^2 (Baughn 2026-10-29: fix the physics; 1.7.10 stored mass^2 x 0.25 J per rad/s, linear in
     * speed, so a heavier line's inertia grew with the square of its mass and a joule bought as much speed at 10 rad/s as
     * at 800). Setting it sets the speed; the speed is what's saved.
     */
    var energy: Double
        get() = 0.5 * inertia * rads * rads
        set(value) {
            rads = Math.sqrt(Math.max(0.0, 2 * value / inertia))
            if (radsLastPublished > rads * 1.05 || radsLastPublished < rads * 0.95) {
                elements.forEach { it.needPublish() }
                radsLastPublished = rads
            }
        }

    /**
     * Drive the shaft with up to `power` (W) for `time` (s), but with no more than `maxTorque` (N m): a machine pushing
     * from standstill accelerates the shaft at torque / inertia, not instantly (energy alone would give any joule at
     * 0 rad/s a jump in speed). Returns the energy it took (J).
     */
    fun drive(power: Double, time: Double, maxTorque: Double): Double {
        if (power <= 0 || time <= 0) return 0.0
        val i = inertia
        val wTorque = rads + maxTorque / i * time
        val eTorque = 0.5 * i * wTorque * wTorque - energy
        val e = Math.min(power * time, eTorque)
        energy += e
        return e
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
            // We ran out of network. Any elements remaining in unseen should thus form a new network, turning as
            // fast as the shaft did (they started at 0 rad/s: a cut spinning shaft stopped dead past the cut; AGE 4fd31c68)
            shaft = ShaftNetwork()
            shaft.rads = rads
        }
    }

    private fun getNeighbours(from: ShaftElement): ArrayList<ShaftNeighbour> {
        val c = Coordonate()
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

    override fun readFromNBT(nbt: CompoundTag, str: String?) {
        rads = nbt.getFloatOr(str + "rads", 0f).toDouble()
    }

    override fun writeToNBT(nbt: CompoundTag, str: String?) {
        nbt.putFloat(str + "rads", rads.toFloat())
    }

}

interface ShaftElement {
    var shaft: ShaftNetwork
    /** Moment of inertia (kg m^2); see inertiaOf. */
    val shaftInertia: Double
    val shaftConnectivity: Array<Direction>
    fun coordonate(): Coordonate

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
