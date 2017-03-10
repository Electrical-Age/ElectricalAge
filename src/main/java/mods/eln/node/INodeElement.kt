package mods.eln.node

import net.minecraft.inventory.IInventory

interface INodeElement {
    fun needPublish()
    fun reconnect()
    fun inventoryChange(inventory: IInventory?)
}
