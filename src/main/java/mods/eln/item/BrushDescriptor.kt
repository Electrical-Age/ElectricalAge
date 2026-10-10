package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Paint brush for cables (one item per colour; 1.12: the colour was the low 4 bits of the item damage, which on 26.x
 * is the 1.12 id this descriptor was registered under). Item rendering (dry overlay) comes back in P3.
 */
class BrushDescriptor(name: String) : GenericItemUsingDamageDescriptor(name) {

    override fun getName(stack: ItemStack): String {
        val color = getColor(stack)
        val life = getLife(stack)
        return if (color == 15 && life == 0) "Empty " + super.getName(stack) else super.getName(stack)
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    fun getColor(stack: ItemStack) = parentItemDamage and 0xF

    private fun getLife(stack: ItemStack?) = stack?.let { ItemNbt.get(it) }?.getIntOr("life", 0) ?: 32

    fun setLife(stack: ItemStack, life: Int) {
        val nbt = ItemNbt.get(stack) ?: CompoundTag()
        nbt.putInt("life", life)
        ItemNbt.set(stack, nbt)
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putInt("life", 32)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (itemStack != null) {
            val creative = entityPlayer?.isCreative ?: false
            list.add(tr("Can paint %1$ blocks", if (creative) "infinite" else getLife(itemStack)))
        }
    }

    fun use(stack: ItemStack, entityPlayer: Player): Boolean {
        val creative = entityPlayer.isCreative
        var life = getLife(stack)
        return if (creative || life != 0) {
            if (!creative) {
                --life
                setLife(stack, life)
            }
            true
        } else {
            Utils.sendMessage(entityPlayer, tr("Brush is dry"))
            false
        }
    }
}
