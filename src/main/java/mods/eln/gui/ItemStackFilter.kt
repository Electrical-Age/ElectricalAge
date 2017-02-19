package mods.eln.gui

import mods.eln.misc.Utils
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

data class ItemFilter(val itemId: Int, val damageMask: Int, val damageValue: Int)

class ItemStackFilter {

    private var whitelist: Array<ItemFilter>

    constructor(item: Item, damageMask: Int = 0, damageValue: Int = 0) {
        whitelist = arrayOf(ItemFilter(
            Item.getIdFromItem(item),
            damageMask,
            damageValue))
    }

    constructor(block: Block, damageMask: Int = 0, damageValue: Int = 0) {
        whitelist = arrayOf(ItemFilter(
            Utils.getItemId(block),
            damageMask,
            damageValue))
    }

    constructor(oreName: String) {
        whitelist = OreDictionary.getOres(oreName).map {
            ItemFilter(Item.getIdFromItem(it.item), 0xff, it.itemDamage)
        }.toTypedArray()
    }

    fun tryItemStack(itemStack: ItemStack): Boolean {
        return whitelist.any {
            Utils.getItemId(itemStack) == it.itemId && (itemStack.itemDamage and it.damageMask) == it.damageValue
        }
    }

    fun add(oreName: String) = apply {
        whitelist = whitelist.plus(ItemStackFilter(oreName).whitelist)
    }

    fun add(item: Item, damageMask: Int = 0, damageValue: Int = 0) = apply {
        whitelist = whitelist.plus(ItemStackFilter(item, damageMask, damageValue).whitelist)
    }

    fun add(block: Block, damageMask: Int = 0, damageValue: Int = 0) = apply {
        whitelist = whitelist.plus(ItemStackFilter(block, damageMask, damageValue).whitelist)
    }

    /**
     * De-duplicates the whitelist. Use if you suspect there may be duplicates.
     */
    fun optimize() {
        whitelist = whitelist.distinct().toTypedArray()
    }
}
