package mods.eln.item

import mods.eln.generic.GenericItemUsingDamage
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class FuelBurnerDescriptor(name: String, val  producedHeatPower: Double, val type: Int, val soundPitch: Float) :
        GenericItemUsingDamageDescriptorUpgrade(name) {
    companion object {
        fun getDescriptor(itemStack: ItemStack?) =
                (itemStack?.item as? GenericItemUsingDamage<*>)?.getDescriptor(itemStack) as? FuelBurnerDescriptor
    }

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Burn unit for the gas heat furnace."))
        list.add(Utils.plotPower(I18N.tr("Produced heat power: "), producedHeatPower))
    }
}
