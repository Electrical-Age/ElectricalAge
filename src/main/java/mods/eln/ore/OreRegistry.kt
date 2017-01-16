package mods.eln.ore

import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import java.util.*

/**
 * Keeps track of the various ores seen in the game.
 *
 * Not just Eln-created ores.
 */

object OreRegistry {
    private val blockCache = HashMap<Block, Boolean>()

    fun isOre(block: Block): Boolean = blockCache.computeIfAbsent(block, {
        OreDictionary.getOreIDs(ItemStack(block)).any {
            OreDictionary.getOreName(it).startsWith("ore")
        }
    })
}
