package mods.eln.sixnode.wirelesssignal

import mods.eln.misc.Coordonate
import mods.eln.misc.Line3f
import mods.eln.misc.Utils
import mods.eln.misc.Vec3f
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk

import java.util.*
import kotlin.collections.Map.Entry

data class WirelessRaytraceKey(val startCoordonate: Coordonate, val endCoordonate: Coordonate)

fun costOfBlockContaining(seg: Line3f, dim: Int) = seg.length() * when(seg.midpoint().toCoordonate(dim).block.material) {
    Material.air -> 1.0
    else -> 2.0
}

class WirelessCacheData(var segments: List<Line3f>, var dim: Int) {
    var validTick: Int = 0
    var cachedCost: Double = 0.0
    var costMap: MutableMap<Chunk, Double> = HashMap()
    var chunkMap: MutableMap<Chunk, MutableList<Line3f>> = HashMap()

    init {
        for(seg in segments) {
            chunkMap.getOrPut(seg.midpoint().toCoordonate(dim).chunk, { -> ArrayList<Line3f>() }).add(seg)
        }
        populateCosts(true)
    }

    fun populateCosts(full: Boolean = false): Double {
        var curTick = MinecraftServer.getServer().tickCounter
        if(validTick == curTick)
            return cachedCost
        validTick = curTick

        var total: Double = 0.0

        for((chunk, segments) in chunkMap) {
            if(full || chunk.isChunkLoaded) {
                var chunk_total = 0.0
                for(seg in segments) {
                    chunk_total += costOfBlockContaining(seg, dim)
                }
                costMap.put(chunk, chunk_total)
            }
            total += costMap.getOrDefault(chunk, 0.0)
        }

        cachedCost = total
        return total
    }
}


object WirelessUtils {

    fun getTx(root: IWirelessSignalSpot, txSet: HashMap<String, HashSet<IWirelessSignalTx>>, txStrength: HashMap<IWirelessSignalTx, Double>) {
        val spotSet = HashSet<IWirelessSignalSpot>()
        txSet.clear()
        txStrength.clear()
        getTx(root, txSet, txStrength, spotSet, true, 0.0)
    }

    private fun getTx(from: IWirelessSignalSpot, txSet: HashMap<String, HashSet<IWirelessSignalTx>>, txStrength: HashMap<IWirelessSignalTx, Double>, spotSet: HashSet<IWirelessSignalSpot>, isRoot: Boolean, strength: Double) {
        var strength = strength
        if (spotSet.contains(from)) return

        spotSet.add(from)

        if (!isRoot) {
            for (txs in from.tx.values) {
                for (tx in txs) {
                    if (isRoot)
                        strength = tx.range - getVirtualDistance(tx.coordonate, from.coordonate, tx.coordonate.trueDistanceTo(from.coordonate))
                    addTo(tx, strength, txSet, txStrength)
                }
            }
            for (spot in from.spot) {
                if (isRoot)
                    strength = spot.range - getVirtualDistance(spot.coordonate, from.coordonate, spot.coordonate.trueDistanceTo(from.coordonate))
                getTx(spot, txSet, txStrength, spotSet, false, strength)
            }
        } else {
            val spots = LinkedList<IWirelessSignalSpot>()
            spots.addAll(from.spot)

            val txs = LinkedList<IWirelessSignalTx>()
            for (txss in from.tx.values) {
                txs.addAll(txss)
            }

            var bestScore: Double
            var best: Any? = null
            while (!spots.isEmpty() || !txs.isEmpty()) {
                bestScore = java.lang.Double.MAX_VALUE
                for (spot in spots) {
                    val temp = spot.coordonate.trueDistanceTo(from.coordonate)
                    if (temp < bestScore) {
                        bestScore = temp
                        best = spot
                    }
                }

                for (tx in txs) {
                    val temp = tx.coordonate.trueDistanceTo(from.coordonate)
                    if (temp < bestScore) {
                        bestScore = temp
                        best = tx
                    }
                }

                if (best is IWirelessSignalSpot) {
                    val b = best
                    if (isRoot)
                        strength = b.range - getVirtualDistance(b.coordonate, from.coordonate, b.coordonate.trueDistanceTo(from.coordonate))
                    getTx(b, txSet, txStrength, spotSet, false, strength)
                    spots.remove(best)
                } else if (best == null) {
                    break
                } else if (best is IWirelessSignalTx){
                    val tx = best

                    if (isRoot)
                        strength = tx.range - getVirtualDistance(tx.coordonate, from.coordonate, tx.coordonate.trueDistanceTo(from.coordonate))
                    addTo(tx, strength, txSet, txStrength)
                    txs.remove(best)
                }else{
                    break
                }
            }
        }
    }

    private fun addTo(tx: IWirelessSignalTx, strength: Double, reg: MutableMap<String, HashSet<IWirelessSignalTx>>, txStrength: MutableMap<IWirelessSignalTx, Double>) {
        val channel = tx.channel
        var ch: HashSet<IWirelessSignalTx>? = reg[channel]
        if (ch != null && ch.contains(tx)) return
        if (ch == null)
            ch = HashSet<IWirelessSignalTx>()
            reg.put(channel, ch)
        ch.add(tx)
        txStrength.put(tx, strength)
    }

    /*
     *
	 * public static HashSet<IWirelessSignalTx> getTx(String channel,IWirelessSignalSpot root){ HashSet<IWirelessSignalTx> txSet = new HashSet<IWirelessSignalTx>(); getTx(channel, root,txSet); return txSet; }
	 *
	 * private static void getTx(String channel,IWirelessSignalSpot root,HashSet<IWirelessSignalTx> txSet){ for(IWirelessSignalSpot spot : root.getSpot()){ getTx(channel, spot, txSet); }
	 *
	 * if(channel != null){ ArrayList<IWirelessSignalTx> txs = root.getTx().get(channel); if(txs != null) txSet.addAll(txs); }else{ for(ArrayList<IWirelessSignalTx> txs : root.getTx().values()){ txSet.addAll(txs); } } }
	 */

    fun buildSpot(c: Coordonate, channel: String?, range: Int): WirelessSignalSpot {
        val txs = HashMap<String, ArrayList<IWirelessSignalTx>>()
        val spots = ArrayList<IWirelessSignalSpot>()

        for (spot in IWirelessSignalSpot.spots) {
            if (isInRange(spot.coordonate, c, spot.range.toDouble())) {
                spots.add(spot)
            }
        }

        if (channel != null) {
            val inRangeTx = ArrayList<IWirelessSignalTx>()

            val sameChannelTx = WirelessSignalTxElement.channelMap[channel]
            if (sameChannelTx != null) {
                for (tx in sameChannelTx) {
                    if (isInRange(tx.coordonate, c, tx.range.toDouble())) {
                        inRangeTx.add(tx)
                    }
                }
            }
            if (!inRangeTx.isEmpty())
                txs.put(channel, inRangeTx)
        } else {
            for ((key, value) in WirelessSignalTxElement.channelMap) {
                val inRangeTx = ArrayList<IWirelessSignalTx>()

                for (tx in value) {
                    if (isInRange(tx.coordonate, c, tx.range.toDouble())) {
                        inRangeTx.add(tx)
                    }
                }

                if (!inRangeTx.isEmpty())
                    txs.put(key, inRangeTx)
            }
        }

        return WirelessSignalSpot(txs, spots, c, range)
    }

    private fun isInRange(txC: Coordonate, rxC: Coordonate, range: Double): Boolean {
        val distance = txC.trueDistanceTo(rxC)
        if (distance > range) return false
        return getVirtualDistance(txC, rxC, distance) <= range
    }

    public val raytraceCache = Hashtable<WirelessRaytraceKey, WirelessCacheData>()

    private fun getDataForRaycast(txC: Coordonate, rxC: Coordonate): WirelessCacheData {
        val key = WirelessRaytraceKey(txC, rxC)

        var cache = raytraceCache[key]
        if(cache !== null) return cache

        val segments = Line3f.fromEndpoints(Vec3f.fromVolumeCenter(txC), Vec3f.fromVolumeCenter(rxC)).allSegmentsAlong()
        cache = WirelessCacheData(segments, txC.dimention)
        raytraceCache.put(key, cache)
        return cache
    }

    private fun getVirtualDistance(txC: Coordonate, rxC: Coordonate, distance: Double): Double {
        var cache = getDataForRaycast(txC, rxC)
        return cache.populateCosts()
    }

    class WirelessSignalSpot(internal var txs: HashMap<String, ArrayList<IWirelessSignalTx>>, internal var spots: ArrayList<IWirelessSignalSpot>, internal var coordonate: Coordonate, internal var range: Int) : IWirelessSignalSpot {

        override fun getTx(): HashMap<String, ArrayList<IWirelessSignalTx>> {
            return txs
        }

        override fun getSpot(): ArrayList<IWirelessSignalSpot> {
            return spots
        }

        override fun getCoordonate(): Coordonate {
            return coordonate
        }

        override fun getRange(): Int {
            return range
        }
    }
}
