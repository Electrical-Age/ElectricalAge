package mods.eln.item

import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class CaseItemDescriptor(name: String) : GenericItemUsingDamageDescriptorUpgrade(name) {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Can be used to encase EA items that support it"))
    }
}
