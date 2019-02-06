package mods.eln.item.electricalitem

import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.wiki.Data
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraftforge.common.util.ChunkCoordComparator
import java.util.*
import kotlin.collections.HashMap


class ElectricalAxe(name: String, strengthOn: Float, strengthOff: Float,
                    energyStorage: Double, energyPerBlock: Double, chargePower: Double)
    : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getStrVsBlock(stack: ItemStack, state: IBlockState?): Float {
        return when {
            state != null && (state.material === Material.WOOD || state.material === Material.PLANTS || state.material === Material.VINE) -> getStrength(stack)
            else -> super.getStrVsBlock(stack, state)
        }
    }


    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer?): ActionResult<ItemStack> {
        if (!w.isRemote) {
            setCapitation(p, s, !getCapitation(s))
        }
        return ActionResult(EnumActionResult.PASS, s)
    }

    private fun getCapitation(stack: ItemStack): Boolean {
        return getNbt(stack).getBoolean("capitation")
    }

    private fun setCapitation(p: EntityPlayer?, stack: ItemStack, capitation: Boolean) {
        getNbt(stack).setBoolean("capitation", capitation)
        if (p != null) {
            Utils.addChatMessage(p, "Set treecapitation to $capitation")
        }
    }

    override fun onBlockDestroyed(stack: ItemStack, w: World, state: IBlockState, pos: BlockPos, entity: EntityLivingBase?): Boolean {
        return if (entity is EntityPlayer && getCapitation(stack)) {
            TreeCapitation.addBlockSwapper(
                world = w,
                player = entity,
                tool = this,
                stack = stack,
                leaves = true,
                origCoords = pos
            )
            true
        } else {
            super.onBlockDestroyed(stack, w, state, pos, entity)
        }
    }
}


/**
 * This code was copied from the Botania Terrasteel Axe, with permission from the Botania license.
 * It was converted to Kotlin, modified to fit the Eln environment, and some Botania-specific code removed.
 *
 * It still refers to Terrasteel, because it behaves like a terrasteel axe. And not...
 * TODO: While this method works, it's not very Eln-y. I'd prefer to drop the tree on the ground... then pop it again.
 *
 * The original code is here: https://github.com/Vazkii/Botania/blob/1.7.10-final/src/main/java/vazkii/botania/common/item/equipment/tool/terrasteel/ItemTerraAxe.java
 */
object TreeCapitation : IProcess {
    /**
     * The number of blocks per tick which the Terra Truncator will
     * collect.
     */
    const val BLOCK_SWAP_RATE = 10

    /**
     * The maximum radius (in blocks) which the Terra Truncator will go
     * in order to try and murder/cut down the tree.
     */
    const val BLOCK_RANGE = 32

    /**
     * The maximum number of leaf blocks which the Terra Truncator will chew/go
     * through once a leaf block is encountered.
     */
    const val LEAF_BLOCK_RANGE = 3

    /**
     * Represents the range which a single block will scan when looking
     * for the next candidates for swapping. 1 is a good default.
     */
    const val SINGLE_BLOCK_RADIUS = 1

    /**
     * Represents a map of dimension IDs to a set of all block swappers
     * active in that dimension.
     */
    private val blockSwappers: MutableMap<Int, List<BlockSwapper>> = HashMap()

    override fun process(time: Double) {
        for ((dim, swappers) in blockSwappers) {
            // Iterate through all of our swappers, removing any
            // which no longer need to tick.
            blockSwappers[dim] = swappers.filter { it.tick() }
        }
    }

    /**
     * Adds a new block swapper to the provided world as the provided player.
     * Block swappers are only added on the server, and a marker instance
     * which is not actually ticked but contains the proper passed in
     * information will be returned to the client.
     *
     * @param world The world to add the swapper to.
     * @param player The player who is responsible for this swapper.
     * @param tool The electrical tool which caused this swapper.
     * @param origCoords The original coordinates the swapper should start at.
     * @param leaves If true, will treat leaves specially (see the BlockSwapper
     * documentation).
     * @return The created block swapper.
     */
    fun addBlockSwapper(world: World, player: EntityPlayer, tool: ElectricalTool, origCoords: BlockPos, leaves: Boolean, stack: ItemStack) {
        val swapper = BlockSwapper(world, player, tool, origCoords, BLOCK_RANGE, leaves, stack)

        // Block swapper registration should only occur on the server
        if (world.isRemote)
            return

        val dim = world.provider.dimension
        blockSwappers[dim] = blockSwappers[dim]?.plus(swapper) ?: listOf(swapper)
    }

    /**
     * A block swapper for the Terra Truncator, which uses a standard
     * Breadth First Search to try and murder/cut down trees.
     *
     * The Terra Truncator will look up to BLOCK_RANGE blocks to find wood
     * to cut down (only cutting down adjacent pieces of wood, so it doesn't
     * jump through the air). However, the tool will only go through
     * LEAF_BLOCK_RANGE leave blocks in order to prevent adjacent trees which
     * are connected only by leaves from being devoured as well.
     *
     * The leaf restriction is implemented by reducing the number of remaining
     * steps to the min of LEAF_BLOCK_RANGE and the current range. The restriction
     * can be removed entirely by setting the "leaves" variable to true, in which
     * case leaves will be treated normally.
     */
    private class BlockSwapper
    /**
     * Creates a new block swapper with the provided parameters.
     * @param world The world the swapper is in.
     * @param player The player responsible for creating this swapper.
     * @param tool The electrical tool responsible for creating this swapper.
     * @param origCoords The original coordinates this swapper should start at.
     * @param range The range this swapper should swap in.
     * @param leaves If true, leaves will be treated specially and
     * severely reduce the radius of further spreading when encountered.
     */
    (
        /**
         * The world the block swapper is doing the swapping in.
         */
        private val world: World,
        /**
         * The player the swapper is swapping for.
         */
        private val player: EntityPlayer,
        /**
         * The Terra Truncator which created this swapper.
         */
        private val tool: ElectricalTool,
        /**
         * The origin of the swapper (eg, where it started).
         */
        private val origin: BlockPos,
        /**
         * The initial range which this block swapper starts with.
         */
        private val range: Int,
        /**
         * Denotes whether leaves should be treated specially.
         */
        private val treatLeavesSpecial: Boolean,

        /**
         * The electrical tool which crated this swapper.
         */
        val stack: ItemStack) {

        /**
         * The priority queue of all possible candidates for swapping.
         */
        private val candidateQueue: PriorityQueue<SwapCandidate>

        /**
         * The set of already swaps coordinates which do not have
         * to be revisited.
         */
        private val completedCoords: MutableSet<BlockPos>

        init {

            this.candidateQueue = PriorityQueue<SwapCandidate>()
            this.completedCoords = HashSet()

            // Add the origin to our candidate queue with the original range
            candidateQueue.offer(SwapCandidate(this.origin, this.range))
        }

        /**
         * Ticks this Block Swapper, which allows it to swap BLOCK_SWAP_RATE
         * further blocks and expands the breadth first search. The return
         * value signifies whether or not the block swapper has more blocks
         * to swap, or if it has finished swapping.
         * @return True if the block swapper has more blocks to swap, false
         * otherwise (implying it can be safely removed).
         */
        fun tick(): Boolean {
            // If empty, this swapper is done.
            if (candidateQueue.isEmpty())
                return false

            var remainingSwaps = BLOCK_SWAP_RATE
            while (remainingSwaps > 0 && !candidateQueue.isEmpty()) {
                val candidate = candidateQueue.poll()

                // If we've already completed this location, move along, as this
                // is just a suboptimal one.
                if (completedCoords.contains(candidate.coordinates))
                    continue

                // If this candidate is out of range, discard it.
                if (candidate.range <= 0)
                    continue

                // Otherwise, perform the break and then look at the adjacent tiles.
                // This is a ridiculous function call here.
                removeBlockWithDrops(
                    player = player,
                    tool = tool,
                    stack = stack,
                    world = world,
                    pos = candidate.coordinates
                )

                remainingSwaps--

                completedCoords.add(candidate.coordinates)

                // Then, go through all of the adjacent blocks and look if
                // any of them are any good.
                for (adj in adjacent(candidate.coordinates)) {
                    val state = world.getBlockState(adj)

                    val isWood = state.block.isWood(world, adj)
                    val isLeaf = state.block.isLeaves(state, world, adj)

                    // If it's not wood or a leaf, we aren't interested.
                    if (!isWood && !isLeaf)
                        continue

                    // If we treat leaves specially and this is a leaf, it gets
                    // the minimum of the leaf range and the current range - 1.
                    // Otherwise, it gets the standard range - 1.
                    val newRange = if (treatLeavesSpecial && isLeaf)
                        Math.min(LEAF_BLOCK_RANGE, candidate.range - 1)
                    else
                        candidate.range - 1

                    candidateQueue.offer(SwapCandidate(adj, newRange))
                }
            }

            // If we did any iteration, then hang around until next tick.
            return true
        }

        fun adjacent(original: BlockPos): List<BlockPos> {
            val coords = ArrayList<BlockPos>()
            // Visit all the surrounding blocks in the provided radius.
            // Gotta love these nested loops, right?
            for (dx in -SINGLE_BLOCK_RADIUS..SINGLE_BLOCK_RADIUS)
                for (dy in -SINGLE_BLOCK_RADIUS..SINGLE_BLOCK_RADIUS)
                    for (dz in -SINGLE_BLOCK_RADIUS..SINGLE_BLOCK_RADIUS) {
                        // Skip the central tile.
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue

                        coords.add(BlockPos(original.x + dx, original.y + dy, original.z + dz))
                    }

            return coords
        }

        /**
         * Represents a potential candidate for swapping/removal. Sorted by
         * range (where a larger range is more preferable). As we're using
         * a priority queue, which is a min-heap internally, larger ranges
         * are considered "smaller" than smaller ranges (so they show up in the
         * min-heap first).
         */
        class SwapCandidate
        /**
         * Constructs a new Swap Candidate with the provided
         * coordinates and range.
         * @param coordinates The coordinates of this candidate.
         * @param range The remaining range of this candidate.
         */
        (
            /**
             * The location of this swap candidate.
             */
            var coordinates: BlockPos,
            /**
             * The remaining range of this swap candidate.
             */
            var range: Int) : Comparable<SwapCandidate> {

            override fun compareTo(other: SwapCandidate): Int {
                // Aka, a bigger range implies a smaller value, meaning
                // bigger ranges will be preferred in a min-heap
                return other.range - range
            }

            override fun equals(other: Any?): Boolean {
                if (other !is SwapCandidate) return false

                val cand = other as SwapCandidate?
                return coordinates == cand!!.coordinates && range == cand.range
            }

            override fun hashCode(): Int {
                var result = coordinates.hashCode()
                result = 31 * result + range
                return result
            }
        }
    }

    /**
     * The bits below, however, are from ToolCommons.java. Mostly. Maybe about half, by now.
     */
    fun removeBlockWithDrops(player: EntityPlayer, tool: ElectricalTool, stack: ItemStack, world: World, pos: BlockPos) {
        if (world.isRemote)
            return

        val state = world.getBlockState(pos)
        val block = state.block

        if (!block.isAir(state, world, pos) && state.getPlayerRelativeBlockHardness(player, world, pos) > 0) {
            if (!block.canHarvestBlock(world, pos, player))
                return

            if (!player.capabilities.isCreativeMode) {
                tool.subtractEnergyForBlockBreak(stack, state)
                if (tool.getEnergy(stack) > 0) {
                    block.onBlockHarvested(world, pos, state, player)

                    if (block.removedByPlayer(state, world, pos, player, true)) {
                        block.onBlockDestroyedByPlayer(world, pos, state)
                        block.harvestBlock(world, player, pos, state, null, stack)
                    }
                }
            } else {
                world.setBlockToAir(pos)
            }
        }
    }
}
