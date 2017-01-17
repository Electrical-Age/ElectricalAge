package mods.eln.ore

import mods.eln.misc.Coordonate
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import java.util.*

object OreUtilities {
    private val blockCache = HashMap<String, Boolean>()

    fun getBlockDrops(silktouchWanted: Boolean, coord: Coordonate): List<ItemStack> {
        val block = coord.world().getBlock(coord.x, coord.y, coord.z)
        val meta = coord.world().getBlockMetadata(coord.x, coord.y, coord.z)
        if (silktouchWanted && block.canSilkHarvest(coord.world(), null, coord.x, coord.y, coord.z, meta)) {
            return listOf(ItemStack(block, 1, meta))
        } else {
            return block.getDrops(coord.world(), coord.x, coord.y, coord.z, meta, 1)
        }
    }

    fun isOre(coord: Coordonate): Boolean {
        return isOre(getBlockDrops(true, coord).firstOrNull())
    }

    fun isOre(stack: ItemStack?): Boolean {
        if (stack == null) return false
        val key = Item.itemRegistry.getNameForObject(stack.item)
        return blockCache.computeIfAbsent(key, {
            OreDictionary.getOreIDs(stack).any {
                val oreName = OreDictionary.getOreName(it)
                oreName.startsWith("ore")
            }
        })
    }
}
