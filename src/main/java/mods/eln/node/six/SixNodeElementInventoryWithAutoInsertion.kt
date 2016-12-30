package mods.eln.node.six

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.item.ItemStack

class SixNodeElementInventoryWithAutoInsertion(size: Int, stackLimit: Int, element: SixNodeElement) :
        SixNodeElementInventory(size, stackLimit, element) {
    private val acceptedItems : Array<Class<out Any>?> = arrayOfNulls(size)

    fun accept(index: Int, type: Class<out Any>): SixNodeElementInventoryWithAutoInsertion {
        if (index >= 0 && index < acceptedItems.count()) {
            acceptedItems[index] = type
        }
        return this
    }

    fun take(itemStack: ItemStack?) : Boolean {
        GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let { item ->
            acceptedItems.withIndex().find { item.javaClass == it.value && getStackInSlot(it.index) == null }?.let {
                itemStack!!.stackSize -= 1
                setInventorySlotContents(it.index, itemStack)
                return true
            }
        }

        GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let { item ->
            acceptedItems.withIndex().find { item.javaClass == it.value && getStackInSlot(it.index) == null }?.let {
                itemStack!!.stackSize -= 1
                setInventorySlotContents(it.index, itemStack)
                return true
            }
        }

        return false
    }
}
