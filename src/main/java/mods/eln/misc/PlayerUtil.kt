package mods.eln.misc

import mods.eln.node.MeterItems
import net.minecraft.world.entity.player.Player

fun Player?.isHoldingMeter(): Boolean {
    if (this == null) return false
    val equippedItem = mainHandItem
    return (MeterItems.isMultiMeter(equippedItem)
        || MeterItems.isThermometer(equippedItem)
        || MeterItems.isAllMeter(equippedItem))
}
