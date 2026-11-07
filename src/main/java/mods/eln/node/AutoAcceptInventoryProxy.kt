package mods.eln.node

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

class AutoAcceptInventoryProxy(val inventory: Container) {
    interface ExistingItemHandler {
        fun handleExistingInventoryItem(itemStack: ItemStack)
    }

    class SimpleItemDropper(val node: NodeBase) : ExistingItemHandler {
        override fun handleExistingInventoryItem(itemStack: ItemStack) {
            node.dropItem(itemStack)
        }
    }

    private abstract class ItemAcceptor(val index: Int) {
        abstract fun take(itemStack: ItemStack?, inventory: Container): Boolean
    }

    private open class ItemAcceptorIfEmpty(index: Int, val acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptor(index) {
        override fun take(itemStack: ItemStack?, inventory: Container): Boolean {
            if (inventory.getItem(index).isEmpty) { // 1.12: never null
                GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        // the stack itself, with all its data (1.12 made a new one and copied only the energy,
                        // which lost an X-ray scanner's filters and renewed a used lamp)
                        inventory.setItem(index, itemStack!!.copyWithCount(1))
                        itemStack.count -= 1
                        return true
                    }
                }

                GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let { desc ->
                    if (acceptedItems.any { it.isAssignableFrom(desc.javaClass) }) {
                        inventory.setItem(index, itemStack!!.copyWithCount(1))
                        itemStack.count -= 1
                        return true
                    }
                }
            }

            return false
        }
    }

    private open class ItemAcceptorIfIncrement(index: Int, val maxItems: Int, acceptedItems: Array<out Class<out Any>>)
        : ItemAcceptorIfEmpty(index, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: Container): Boolean {
            if (super.take(itemStack, inventory)) return true

            val existingStack = inventory.getItem(index)
            if (existingStack?.count ?: 0 >= maxItems) return false

            val existingItemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(existingStack)
            val itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemDescriptor != null && existingItemDescriptor == itemDescriptor) {
                itemStack!!.count -= 1
                existingStack.count += 1
                return true
            }

            val existingItemBloackDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(existingStack)
            val itemBlockDescriptor = GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)

            if (existingItemBloackDescriptor != null && existingItemBloackDescriptor == itemBlockDescriptor) {
                itemStack!!.count -= 1
                existingStack.count += 1
                return true
            }

            return false
        }
    }

    private class ItemAcceptorAlways(index: Int, maxItems: Int, acceptedItems: Array<out Class<out Any>>,
                                     val existingItemHandler: ExistingItemHandler?)
        : ItemAcceptorIfIncrement(index, maxItems, acceptedItems) {
        override fun take(itemStack: ItemStack?, inventory: Container): Boolean {
            if (super.take(itemStack, inventory)) return true

            // TODO: What do we do with the item that is actually in the slot? For the moment it just disappears.

            GenericItemUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    existingItemHandler?.handleExistingInventoryItem(inventory.getItem(index))
                    inventory.setItem(index, itemStack!!.copyWithCount(1))
                    itemStack.count -= 1
                    return true
                }
            }

            GenericItemBlockUsingDamageDescriptor.getDescriptor(itemStack)?.let {
                if (acceptedItems.contains(it.javaClass)) {
                    existingItemHandler?.handleExistingInventoryItem(inventory.getItem(index))
                    inventory.setItem(index, itemStack!!.copyWithCount(1))
                    itemStack.count -= 1
                    return true
                }
            }

            return false
        }
    }

    /**
     * A cable slot (Baughn 2026-11-07: kits replace the 1.12 cable items): an electrical (or, if thermal, a thermal)
     * 1.12 cable item one at a time, or a block's piece (16 voxels) of a cable kit, up to maxBlocks of the same cable.
     */
    private class CableAcceptor(index: Int, val maxBlocks: Int, val thermal: Boolean) : ItemAcceptor(index) {
        fun fits(s: ItemStack) = mods.eln.node.voxel.CableItems.isElectrical(s) || thermal && mods.eln.node.voxel.CableItems.isThermal(s)

        override fun take(itemStack: ItemStack?, inventory: Container): Boolean {
            if (itemStack == null || itemStack.isEmpty || !fits(itemStack)) return false
            val existing = inventory.getItem(index)
            if (existing.isEmpty) {
                inventory.setItem(index, mods.eln.node.voxel.CableItems.take(itemStack, 16))
                return true
            }
            if (mods.eln.node.voxel.CableItems.blocks(existing) >= maxBlocks) return false
            if (mods.eln.node.voxel.CableItems.kitTier(existing) >= 0) {
                if (existing.item != itemStack.item || !sameSpec(existing, itemStack)) return false
                val piece = mods.eln.node.voxel.CableItems.take(itemStack, 16)
                mods.eln.node.voxel.CableItems.add(existing, mods.eln.node.voxel.CableItems.voxels(piece))
                return true
            }
            if (!ItemStack.isSameItemSameComponents(existing, itemStack)) return false
            itemStack.count -= 1
            existing.count += 1
            return true
        }

        fun sameSpec(a: ItemStack, b: ItemStack) =
            mods.eln.node.voxel.CableKitItem.specOf(a) == mods.eln.node.voxel.CableKitItem.specOf(b)
    }

    private val itemAcceptors: Array<ItemAcceptor?> = arrayOfNulls(inventory.containerSize)

    /** A cable slot taking 1.12 cable items or kit pieces (CableAcceptor). */
    fun acceptCable(index: Int, maxBlocks: Int, thermal: Boolean = false): AutoAcceptInventoryProxy {
        if (index >= 0 && index < itemAcceptors.count()) {
            itemAcceptors[index] = CableAcceptor(index, maxBlocks, thermal)
        }
        return this
    }

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

    fun take(itemStack: ItemStack?, nodeElement: INodeElement?, publish: Boolean = false,
             notifyInventoryChange: Boolean = false) =
        if (take(itemStack)) {
            if (publish) {
                nodeElement?.needPublish()
            }
            if (notifyInventoryChange) {
                nodeElement?.inventoryChange(inventory)
            }
            true
        } else
            false
}
