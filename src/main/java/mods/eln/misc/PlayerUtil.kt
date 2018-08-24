package mods.eln.misc

import mods.eln.Eln
import net.minecraft.entity.player.EntityPlayer

fun EntityPlayer?.isHoldingMeter(): Boolean {
    if (this == null) return false
    return heldEquipment.any({
        Eln.multiMeterElement.checkSameItemStack(it)
            || Eln.thermometerElement.checkSameItemStack(it)
            || Eln.allMeterElement.checkSameItemStack(it)
    })
}
