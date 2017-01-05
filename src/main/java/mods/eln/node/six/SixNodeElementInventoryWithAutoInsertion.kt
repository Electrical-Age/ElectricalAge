package mods.eln.node.six

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class SixNodeElementInventoryWithAutoInsertion(size: Int, stackLimit: Int, element: SixNodeElement) :
        SixNodeElementInventory(size, stackLimit, element) {

    private abstract class ItemAcceptor(val index: Int, val acceptedItems: Array<out Class<out Any>> ) {
        abstract fun take(itemStack: ItemStack?, inventory: IInventory) : Boolean
    }

    private open class ItemAcceptorIfEmpty(index: Int, acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptor(index, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: IInventory) : Boolean {
            if (inventory.getStackInSlot(index) == null) {
                GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                    if (acceptedItems.contains(it.javaClass)) {
                        itemStack!!.stackSize -= 1
                        inventory.setInventorySlotContents(index, it.newItemStack())
                        return true
                    }
                }

                GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                    if (acceptedItems.contains(it.javaClass)) {
                        itemStack!!.stackSize -= 1
                        inventory.setInventorySlotContents(index, it.newItemStack())
                        return true
                    }
                }
            }

            return false
        }
    }

    private open class ItemAcceptorIfIncrement(index: Int, acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptorIfEmpty(index, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: IInventory) : Boolean {
            if (super.take(itemStack, inventory)) return true

            val existingStack = inventory.getStackInSlot(index)
            val existingItemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(existingStack)
            val itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemDescriptor != null && existingItemDescriptor == itemDescriptor) {
                itemStack!!.stackSize -= 1
                existingStack.stackSize += 1
                return true
            }

            val existingItemBloackDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(existingStack)
            val itemBlockDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemBloackDescriptor != null && existingItemBloackDescriptor == itemBlockDescriptor) {
                itemStack!!.stackSize -= 1
                existingStack.stackSize += 1
                return true
            }

            return false
        }
    }

    private class ItemAcceptorAlways(index: Int, acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptorIfIncrement(index, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: IInventory) : Boolean {
            if (super.take(itemStack, inventory)) return true

            // TODO: What do we do with the item that is actually in the slot? For the moment it just disappears.

            GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack!!.stackSize -= 1
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }

            GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack!!.stackSize -= 1
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }

            return false
        }
    }

    private val itemAcceptors: Array<ItemAcceptor?> = arrayOfNulls(size)

    fun acceptIfEmpty(index: Int, vararg types: Class<out Any>) : SixNodeElementInventoryWithAutoInsertion {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfEmpty(index, types)
        }
        return this
    }

    fun acceptIfIncrement(index: Int, vararg types: Class<out Any>) : SixNodeElementInventoryWithAutoInsertion {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfIncrement(index, types)
        }
        return this
    }

    fun acceptAlways(index: Int, vararg types: Class<out Any>) : SixNodeElementInventoryWithAutoInsertion {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorAlways(index, types)
        }
        return this
    }

    fun take(itemStack: ItemStack?) = itemAcceptors.filterNotNull().any { it.take(itemStack, this) }
}
