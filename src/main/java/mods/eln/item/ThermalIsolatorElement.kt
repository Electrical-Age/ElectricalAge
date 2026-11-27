package mods.eln.item

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ThermalIsolatorElement(name: String, val conductionFactor: Double, val Tmax: Double) : GenericItemUsingDamageDescriptorUpgrade(name) {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Furnace insulation upgrade (Organic Asbestos™)."))
        list.add(tr("Max temp: %1$ | Insulation: %2$", Utils.plotCelsius(Tmax), Utils.plotPercent(conductionFactor)))
    }
}
