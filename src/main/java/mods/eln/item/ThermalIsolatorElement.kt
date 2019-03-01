package mods.eln.item

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class ThermalIsolatorElement(name: String, val conductionFactor: Double, val Tmax: Double) : GenericItemUsingDamageDescriptorUpgrade(name) {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Upgrade for the Stone Heat Furnace"))
        list.add(tr("Organic Asbestos™"))
        list.add(Utils.plotCelsius(tr("  Heat tolerance:"), Tmax))
        list.add(Utils.plotPercent(tr("  Insulation:"), conductionFactor))
    }
}
