package mods.eln.misc

import mods.eln.Eln
import net.minecraft.entity.player.EntityPlayer

fun EntityPlayer?.isHoldingMeter(): Boolean {
    if (this == null) return false
    val equippedItem = currentEquippedItem
    return (Eln.multiMeterElement.checkSameItemStack(equippedItem)
        || Eln.thermometerElement.checkSameItemStack(equippedItem)
        || Eln.allMeterElement.checkSameItemStack(equippedItem))
}
