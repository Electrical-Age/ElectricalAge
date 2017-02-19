package mods.eln.gui

import mods.eln.misc.Utils
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

class ItemStackFilter {

    internal var itemId: Int = 0
    internal var damageMask: Int = 0
    internal var damageValue: Int = 0

    constructor(item: Item, damageMask: Int, damageValue: Int) {
        this.itemId = Item.getIdFromItem(item)
        this.damageMask = damageMask
        this.damageValue = damageValue
    }

    constructor(block: Block, damageMask: Int, damageValue: Int) {
        this.itemId = Utils.getItemId(block)
        this.damageMask = damageMask
        this.damageValue = damageValue
    }

    constructor(item: Item) {
        this.itemId = Item.getIdFromItem(item)
        this.damageMask = 0
        this.damageValue = 0
    }

    constructor(block: Block) {
        this.itemId = Utils.getItemId(block)
        this.damageMask = 0
        this.damageValue = 0
    }

    fun tryItemStack(itemStack: ItemStack): Boolean {// caca1.5.1
        if (Utils.getItemId(itemStack) != itemId)
            return false
        if (itemStack.itemDamage and damageMask != damageValue)
            return false
        return true
    }

    companion object {

        fun OreDict(name: String): Array<ItemStackFilter> {
            val ores = OreDictionary.getOres(name)
            return ores.map {
                ItemStackFilter(it.item, 0xff, it.itemDamage)
            }.toTypedArray()
        }
    }
}
