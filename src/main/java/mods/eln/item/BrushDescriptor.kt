package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagInt
import net.minecraft.util.ResourceLocation

class BrushDescriptor(name: String): GenericItemUsingDamageDescriptor(name) {

    private val icon = ResourceLocation("eln", "textures/items/" + name.toLowerCase().replace(" ", "") + ".png")

    override fun getName(stack: ItemStack): String {
        val creative = Minecraft.getMinecraft().player.capabilities.isCreativeMode
        val color = getColor(stack)
        val life = getLife(stack)
        return if (!creative && color == 15 && life == 0) "Empty " + super.getName(stack) else super.getName(stack)
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    fun getColor(stack: ItemStack) = stack.itemDamage and 0xF

    private fun getLife(stack: ItemStack?): Int = stack?.tagCompound?.getInteger("life") ?: 32

    fun setLife(stack: ItemStack, life: Int) {
        stack.setTagInfo("life", NBTTagInt(life))
    }

    override fun getDefaultNBT(): NBTTagCompound? {
        val nbt = NBTTagCompound()
        nbt.setInteger("life", 32)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (itemStack != null) {
            val creative = Minecraft.getMinecraft().player.capabilities.isCreativeMode
            list.add(tr("Can paint %s blocks", if (creative) "infinite" else itemStack.tagCompound!!.getInteger("life")))
        }
    }

    fun use(stack: ItemStack, entityPlayer: EntityPlayer): Boolean {
        val creative = entityPlayer.capabilities.isCreativeMode
        var life = stack.tagCompound!!.getInteger("life")
        return if (creative || life != 0) {
            if (!creative) {
                --life
                stack.tagCompound!!.setInteger("life", life)
            }
            true
        } else {
            Utils.sendMessage(entityPlayer, tr("Brush is dry"))
            false
        }
    }

// TODO(1.10): Reimplement brush coloring
//    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
//        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
//            val creative = Minecraft.getMinecraft().player.capabilities.isCreativeMode
//            UtilsClient.drawIcon(type, icon)
//            if (!creative) {
//                GL11.glColor4f(1f, 1f, 1f, 0.75f - 0.75f * getLife(item) / 32f)
//                UtilsClient.drawIcon(type, dryOverlay)
//                GL11.glColor3f(1f, 1f, 1f)
//            }
//        } else {
//            super.renderItem(type, item, *data)
//        }
//    }

    companion object {
        private val dryOverlay = ResourceLocation("eln", "textures/items/brushdryoverlay.png")
    }
}
