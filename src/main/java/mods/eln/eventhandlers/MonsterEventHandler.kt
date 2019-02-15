package mods.eln.eventhandlers

import mods.eln.entity.ReplicatorEntity
import mods.eln.misc.Coordonate
import mods.eln.misc.OctoTree
import net.minecraft.entity.boss.EntityWither
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import kotlin.math.roundToInt

/**
 * This is not an efficient way to do anything.
 *
 * The octree is a good start. It's fine in terms of CPU use, more or less, but that memory use scares me and is
 * also entirely pointless. I'll have to find (or invent) a better algorithm. Though, the octree implementation
 * supports clusterization so... maybe that's at least partially fine?
 *
 * It's also far too easy to imagine this counter getting desynced from reality.
 * Although it seems to be working so far.
 */

object MonsterEventHandler {
    private val trees: MutableMap<Int, OctoTree<Int>> = HashMap()

    fun onSpawn(event: EntityJoinWorldEvent) {
        val entity = event.entity
        if (entity is ReplicatorEntity || entity is EntityWither) {
            return
        }
        val x = entity.posX.roundToInt() + 32768
        val y = entity.posY.roundToInt() + 32768
        val z = entity.posZ.roundToInt() + 32768
        if (trees[entity.dimension]?.get(x, y, z) ?: 0 > 0) {
            event.isCanceled = true
        }
    }

    private fun doCube(center: Coordonate, range: Int, f: (Int) -> Int) {
        if (range <= 0) return
        val xOffset = center.x + 32768
        val yOffset = center.y + 32768
        val zOffset = center.z + 32768
        val tree = trees.getOrPut(center.dimention) { OctoTree(16) }
        for (x in (xOffset - range)..(xOffset + range)) {
            for (y in (yOffset - range)..(yOffset + range)) {
                for (z in (zOffset - range)..(zOffset + range)) {
                    tree.set(x, y, z, f(tree.get(x, y, z) ?: 0))
                }
            }
        }
    }

    fun registerMonsterBlock(coord: Coordonate, range: Int) {
        println("Registering block at $coord - $range")
        doCube(coord, range) { it + 1 }
    }

    fun unregisterMonsterBlock(coord: Coordonate, range: Int) {
        println("Unregistering block at $coord - $range")
        doCube(coord, range) { Math.max(0, it - 1) }
    }
}
