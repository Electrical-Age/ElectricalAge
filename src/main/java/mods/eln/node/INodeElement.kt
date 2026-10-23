package mods.eln.node

import net.minecraft.world.Container

interface INodeElement {
    fun needPublish()
    fun reconnect()
    fun inventoryChange(inventory: Container?)
}
