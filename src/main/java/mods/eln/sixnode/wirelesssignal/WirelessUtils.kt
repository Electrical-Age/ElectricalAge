package mods.eln.sixnode.wirelesssignal

import mods.eln.misc.Coordonate
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk

import java.util.*
import kotlin.collections.Map.Entry

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
        return if (getVirtualDistance(txC, rxC, distance) > range) false else true
    }

    data class WirelessRaytraceCache(val startCoordonate: Coordonate, val endCoordonate: Coordonate, val currentChunk: Chunk)

    public val raytraceCache: Hashtable<WirelessRaytraceCache, Double>
        get() = raytraceCache

    private fun getVirtualDistance(txC: Coordonate, rxC: Coordonate, distance: Double): Double {
        var virtualDistance = distance
        if (distance > 2) {
            var vx: Double
            var vy: Double
            var vz: Double
            val dx: Double
            val dy: Double
            val dz: Double
            vx = rxC.x + 0.5
            vy = rxC.y + 0.5
            vz = rxC.z + 0.5

            dx = (txC.x - rxC.x) / distance
            dy = (txC.y - rxC.y) / distance
            dz = (txC.z - rxC.z) / distance
            val c = Coordonate()
            c.setDimention(rxC.dimention)

            var idx = 0
            while (idx < distance - 1) {
                vx += dx
                vy += dy
                vz += dz
                c.x = vx.toInt()
                c.y = vy.toInt()
                c.z = vz.toInt()
                if (c.blockExist) {
                    val b = c.block
                    val w = c.world()

                    virtualDistance += if (b.isOpaqueCube && !b.isAir(w, c.x, c.y, c.z))
                        2.0
                    else
                        0.0
                }
                idx++
            }
        }
        return virtualDistance
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
