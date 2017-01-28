package mods.eln.node

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class AutoAcceptInventoryProxy(val inventory: IInventory) {
    interface ExistingItemHandler {
        fun handleExistingInventoryItem(itemStack: ItemStack)
    }

    class SimpleItemDropper(val node: NodeBase) : ExistingItemHandler {
        override fun handleExistingInventoryItem(itemStack: ItemStack) {
            node.dropItem(itemStack)
        }
    }

    private abstract class ItemAcceptor(val index: Int) {
        abstract fun take(itemStack: ItemStack?, inventory: IInventory): Boolean
    }

    private open class ItemAcceptorIfEmpty(index: Int, val acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptor(index) {
        override fun take(itemStack: ItemStack?, inventory: IInventory): Boolean {
            if (inventory.getStackInSlot(index) == null) {
                GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        itemStack!!.stackSize -= 1
                        inventory.setInventorySlotContents(index, desc.newItemStack())
                        return true
                    }
                }

                GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        itemStack!!.stackSize -= 1
                        inventory.setInventorySlotContents(index, desc.newItemStack())
                        return true
                    }
                }
            }

            return false
        }
    }

    private open class ItemAcceptorIfIncrement(index: Int, val maxItems: Int, acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptorIfEmpty(index, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: IInventory): Boolean {
            if (super.take(itemStack, inventory)) return true

            val existingStack = inventory.getStackInSlot(index)
            if (existingStack?.stackSize ?: 0 >= maxItems) return false

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

    private class ItemAcceptorAlways(index: Int, maxItems: Int, acceptedItems: Array<out Class<out Any>>,
                                     val existingItemHandler: ExistingItemHandler?)
        : ItemAcceptorIfIncrement(index, maxItems, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: IInventory): Boolean {
            if (super.take(itemStack, inventory)) return true

            // TODO: What do we do with the item that is actually in the slot? For the moment it just disappears.

            GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack!!.stackSize -= 1
                    existingItemHandler?.handleExistingInventoryItem(inventory.getStackInSlot(index))
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }

            GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack!!.stackSize -= 1
                    existingItemHandler?.handleExistingInventoryItem(inventory.getStackInSlot(index))
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }

            return false
        }
    }

    private val itemAcceptors: Array<ItemAcceptor?> = arrayOfNulls(inventory.sizeInventory)

    fun acceptIfEmpty(index: Int, vararg types: Class<out Any>): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfEmpty(index, types)
        }
        return this
    }

    fun acceptIfIncrement(index: Int, maxItems: Int, vararg types: Class<out Any>): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfIncrement(index, maxItems, types)
        }
        return this
    }

    fun acceptAlways(index: Int, maxItems: Int, existingItemHandler: ExistingItemHandler?,
                     vararg types: Class<out Any>): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorAlways(index, maxItems, types, existingItemHandler)
        }
        return this
    }

    fun take(itemStack: ItemStack?) = itemAcceptors.filterNotNull().any { it.take(itemStack, inventory) }

    fun take(itemStack: ItemStack?, publishable: IPublishable?) =
        if (take(itemStack)) {
            publishable?.needPublish()
            true
        } else
            false

    fun take(itemStack: ItemStack?, inventoryChangeListener: IInventoryChangeListener?) =
        if (take(itemStack)) {
            inventoryChangeListener?.inventoryChange(inventory)
            true
        } else
            false
}
