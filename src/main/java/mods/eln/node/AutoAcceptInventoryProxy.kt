package mods.eln.node

import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.electricalinterface.IItemEnergyBattery
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class AutoAcceptInventoryProxy(val inventory: IInventory) {
    private val itemAcceptors: Array<ItemAcceptor?> = arrayOfNulls(inventory.sizeInventory)

    fun acceptIfEmpty(
        index: Int,
        vararg types: Class<out Any>
    ): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfEmpty(index, types)
        }
        return this
    }

    fun acceptIfIncrement(
        index: Int,
        maxItems: Int,
        vararg types: Class<out Any>
    ): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorIfIncrement(index, maxItems, types)
        }
        return this
    }

    fun acceptAlways(
        index: Int,
        maxItems: Int,
        existingItemHandler: ExistingItemHandler?,
        vararg types: Class<out Any>
    ): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = ItemAcceptorAlways(index, maxItems, types, existingItemHandler)
        }
        return this
    }

    fun take(itemStack: ItemStack) = itemAcceptors.filterNotNull().any { it.take(itemStack, inventory) }

    fun take(
        itemStack: ItemStack,
        nodeElement: INodeElement?,
        publish: Boolean = false,
        notifyInventoryChange: Boolean = false
    ) = if (take(itemStack)) {
            if (publish) {
                nodeElement?.needPublish()
            }
            if (notifyInventoryChange) {
                nodeElement?.inventoryChange(inventory)
            }
            true
        } else
            false

    interface ExistingItemHandler {
        fun handleExistingInventoryItem(itemStack: ItemStack)
    }

    class SimpleItemDropper(val node: NodeBase) : ExistingItemHandler {
        override fun handleExistingInventoryItem(itemStack: ItemStack) {
            node.dropItem(itemStack)
        }
    }

    private abstract class ItemAcceptor(val index: Int) {
        abstract fun take(itemStack: ItemStack, inventory: IInventory): Boolean
    }

    private open class ItemAcceptorIfEmpty(
        index: Int,
        val acceptedItems: Array<out Class<out Any>>
    ): ItemAcceptor(index) {
        override fun take(itemStack: ItemStack, inventory: IInventory): Boolean {
            // Do nothing if we already have a stack.
            if (inventory.getStackInSlot(index).isEmpty) {
                GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        // We can accept this stack.
                        val newItemStack = desc.newItemStack()
                        // Propagate battery power.
                        (desc as? IItemEnergyBattery)?.let { it.setEnergy(newItemStack, it.getEnergy(itemStack)) }
                        inventory.setInventorySlotContents(index, newItemStack)
                        // And decrement the one we're taking from.
                        itemStack.count -= 1
                        return true
                    }
                }

                GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        itemStack.count -= 1
                        inventory.setInventorySlotContents(index, desc.newItemStack())
                        return true
                    }
                }
            }
            return false
        }
    }

    private open class ItemAcceptorIfIncrement(
        index: Int,
        val maxItems: Int, acceptedItems: Array<out Class<out Any>>
    ): ItemAcceptorIfEmpty(index, acceptedItems) {
        override fun take(itemStack: ItemStack, inventory: IInventory): Boolean {
            if (super.take(itemStack, inventory)) return true

            val existingStack = inventory.getStackInSlot(index)
            if (existingStack.count >= maxItems) return false

            val existingItemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(existingStack)
            val itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemDescriptor != null && existingItemDescriptor == itemDescriptor) {
                itemStack.count -= 1
                existingStack.count += 1
                return true
            }

            val existingItemBlockDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(existingStack)
            val itemBlockDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemBlockDescriptor != null && existingItemBlockDescriptor == itemBlockDescriptor) {
                itemStack.count -= 1
                existingStack.count += 1
                return true
            }

            return false
        }
    }

    private class ItemAcceptorAlways(
        index: Int,
        maxItems: Int,
        acceptedItems: Array<out Class<out Any>>,
        val existingItemHandler: ExistingItemHandler?
    ): ItemAcceptorIfIncrement(
        index,
        maxItems,
        acceptedItems
    ) {
        override fun take(itemStack: ItemStack, inventory: IInventory): Boolean {
            if (super.take(itemStack, inventory)) return true
            if (itemStack.isEmpty) return false

            // TODO: What do we do with the item that is actually in the slot? For the moment it just disappears.
            GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack.count -= 1
                    val inSlot = inventory.getStackInSlot(index)
                    if (inSlot.isNotEmpty) {
                        existingItemHandler?.handleExistingInventoryItem(inSlot)
                    }
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }

            GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    itemStack.count -= 1
                    val inSlot = inventory.getStackInSlot(index)
                    if (inSlot.isNotEmpty) {
                        existingItemHandler?.handleExistingInventoryItem(inSlot)
                    }
                    inventory.setInventorySlotContents(index, it.newItemStack())
                    return true
                }
            }
            return false
        }
    }
}
