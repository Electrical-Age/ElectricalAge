package mods.eln.node

import net.minecraft.inventory.IInventory

interface IInventoryChangeListener {
    fun inventoryChange(inventory: IInventory?)
}
