package mods.eln.sixnode.wirelesssignal

import mods.eln.misc.Coordonate
import mods.eln.misc.Utils
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk

import java.util.*
import kotlin.collections.Map.Entry

data class WirelessRaytraceCache(val startCoordonate: Coordonate, val endCoordonate: Coordonate, val currentChunk: Chunk)

data class WirelessCacheData(val distance: Double, val ttl: Int)
// the TTL is measured in ticks, compared to MinecraftServer.getServer().tickCounter(). It is cumulative, NOT a countdown timer

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

    public val raytraceCache = Hashtable<WirelessRaytraceCache, WirelessCacheData>()

    private fun getVirtualDistance(txC: Coordonate, rxC: Coordonate, distance: Double): Double {
        var virtualDistance = distance
        if (distance > 2) {

            // generate chunk list
            val chunkList = ArrayList<Chunk>()
            chunkList.add(txC.chunk)
            chunkList.add(rxC.chunk)

            val tx = txC.x
            val tz = txC.z
            val rx = rxC.x
            val rz = rxC.z
            val xDiff = Math.abs(tx - rx)
            val zDiff = Math.abs(tz - rz)
            val xzHyp = pythagoreanHyp2D(xDiff.toDouble(), zDiff.toDouble())
            val dfx = (tx - rx) / xzHyp
            val dfz = (tz - rz) / xzHyp
            var vax = rxC.x + 0.5
            var vaz = rxC.z + 0.5

            val cord = Coordonate()
            cord.setDimention(rxC.dimention)

            cord.y = 1 // doesn't matter here.

            var indx = 0
            while (indx < xzHyp - 1) {
                vax += dfx
                vaz += dfz
                cord.x = vax.toInt()
                cord.z = vaz.toInt()
                chunkList.add(cord.chunk)
                indx++
            }

            // chunk list generated! Now to run getDistanceInChunk() for all chunks!

            var chunkLen = 0.0

            for (chunk in chunkList) {
                chunkLen += getDistanceInChunk(txC, rxC, chunk)
            }


            //return chunkLen

            // later, remove below code in this function entirely, once the code above works
            // we can also compare the new code to the old code's performance, over time.

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

            Utils.println("The total length by the new function is: " + chunkLen.toString() + " and the old length is: " + virtualDistance.toString())
        }
        return virtualDistance
    }

    private fun pythagoreanHyp2D(a: Double, b: Double):Double {
        return Math.sqrt(((a * a) + (b * b)))
    }

    private fun pythagoreanHyp3D(a: Double, b: Double, c: Double):Double {
        // the less well known pythagorean theorem for 3 dimensions
        return Math.sqrt(((a * a) + (b * b) + (c * c)))
    }

    private fun getDistanceInChunk(txC: Coordonate, rxC: Coordonate, currentChunk: Chunk): Double {

        // this variable holds the current cache key w.r.t. the arguments
        val key = WirelessRaytraceCache(txC, rxC, currentChunk)

        // check cache, firstly. This makes it go faaast! :D
        // if the item is not in cache, we create it
        val cacheDist = raytraceCache[key]
        if (cacheDist != null) {
            // if the cache is older than the ttl timestamp AND the chunk is loaded, then drop the cached object, and let it be re-calculated
            // if the cache is old, but the chunk isn't loaded, this means that nothing has happened in that chunk, and the calculation is still valid.
            if ((cacheDist.ttl <= MinecraftServer.getServer().tickCounter) && currentChunk.isChunkLoaded) {
                raytraceCache.remove(key)
            }else{
                return cacheDist.distance
            }
        }

        //cache miss? calculate it! (the rest of the function does this) This loads the current chunk, so we try not to do it often

        var virtualDistance: Double = 0.0 // fyi, this is the variable we will return at the end
        //break out x,y,z for the start and end locations.
        var tx: Double
        var ty: Double
        var tz: Double
        var rx: Double
        var ry: Double
        var rz: Double
        val chunkX = currentChunk.xPosition
        val chunkZ = currentChunk.zPosition
        val chunkXPos = ((chunkX * 16) + 15).toDouble()
        val chunkXNeg = (chunkX * 16).toDouble()
        val chunkZPos = ((chunkZ * 16) + 15).toDouble()
        val chunkZNeg = (chunkZ * 16).toDouble()
        val xDiff = Math.abs(txC.x - rxC.x).toDouble()
        val yDiff = Math.abs(txC.y - rxC.y)
        val zDiff = Math.abs(txC.z - rxC.z).toDouble()
        val xzHypotenuse = pythagoreanHyp2D(xDiff, zDiff)

        val obo = 1 //TODO: Fix this, it may be 0 or 1, I'm not sure. Off by one errors are great!!
        // it's used 4 times, so I made a variable so that search and replace gets the right ones

        // calculate tx entry point x and z
        if (txC.chunk == currentChunk) {
            // the entry point is in this chunk
            tx = txC.x.toDouble() + 0.5
            ty = txC.y.toDouble() + 0.5
            tz = txC.z.toDouble() + 0.5
        }else{
            //calcuate the entry point to the chunk from an adjacent chunk
            if (txC.x.toDouble() < chunkXNeg) {
                tx = chunkXNeg
            }else{
                tx = chunkXPos + obo //TODO: Fix this
            }
            if (txC.z.toDouble() < chunkZNeg) {
                tz = chunkZNeg
            }else {
                tz = chunkXPos + obo //TODO: Fix this
            }
        }

        //calculate rx entry point x and z
        if (rxC.chunk == currentChunk) {
            //the entry point is in this chunk
            rx = rxC.x.toDouble() + 0.5
            ry = rxC.y.toDouble() + 0.5
            rz = rxC.z.toDouble() + 0.5
        }else{
            //calcuate the entry point to the chunk from an adjacent chunk
            if (rxC.x.toDouble() < chunkXNeg) {
                rx = chunkXNeg
            }else{
                rx = chunkXPos + obo //TODO: Fix this
            }
            if (rxC.z.toDouble() < chunkZNeg) {
                rz = chunkZNeg
            }else {
                rz = chunkXPos + obo //TODO: Fix this
            }
        }

        //tHyp is the hypotenuse on the xz plane that is between the chunk in question and txC
        //rHyp is the hypotenuse on the xz plane that is between the chunk in question and rxC
        //cHyp is the tHyp for within the chunk on the xz plane
        val tHyp = pythagoreanHyp2D(tx, tz) - xzHypotenuse
        val rHyp = pythagoreanHyp2D(rx, rz) - xzHypotenuse
        val cHyp = xzHypotenuse - (tHyp - rHyp)
        val tPercHyp = tHyp / xzHypotenuse
        val rPercHyp = rHyp / xzHypotenuse

        // calculate y entry/exit point
        if (yDiff == 0) {
            ty = txC.y.toDouble()
            ry = rxC.y.toDouble()
        }else{
            if (txC.y < rxC.y) {
                ty = (tPercHyp * yDiff) + txC.y.toDouble()
                ry = rxC.y.toDouble() - (rPercHyp * yDiff)
            }else{
                ty = txC.y.toDouble() - (tPercHyp * yDiff)
                ry = (rPercHyp * yDiff) + rxC.y.toDouble()
            }
        }

        // betterDistance gives the distance of the hypotenuse in 3D space for this chunk. It does not consider the objects in the chunk
        val betterDistance = pythagoreanHyp3D(Math.abs(tx - rx), Math.abs(ty - ry), Math.abs(tz - rz))
        virtualDistance = betterDistance

        // calculate the distance through each block. This will be fun :) Here is where we touch the world itself.

        //TODO: create the proper vritual distance. This will work on top of the betterDistance variable.

        // this is the distance that the raytrace is in /this/ chunk for. The getVirtualDistance() function above will add all chunks together, to get the whole distance
        raytraceCache[key] = WirelessCacheData(virtualDistance, MinecraftServer.getServer().tickCounter + (5 * 20)) // 5 second TTL, with 20TPS nominal
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
