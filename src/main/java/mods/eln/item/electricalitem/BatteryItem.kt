package mods.eln.item.electricalitem

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper

import mods.eln.i18n.I18N.tr

class BatteryItem(name: String, private var energyStorage: Double, internal var chargePower: Double, internal var dischargePower: Double, private val priority: Int) : GenericItemUsingDamageDescriptor(name), IItemEnergyBattery {

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getDefaultNBT(): NBTTagCompound? {
        val nbt = NBTTagCompound()
        nbt.setDouble("energy", 0.0)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Charge power: %1\$W", Utils.plotValue(chargePower)))
        list.add(tr("Discharge power: %1\$W", Utils.plotValue(dischargePower)))
        if (itemStack != null) {
            list.add(tr("Stored energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                    (getEnergy(itemStack) / energyStorage * 100).toInt()))
        }
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).setDouble("energy", Math.max(0.0, value))
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return dischargePower
    }

    override fun getPriority(stack: ItemStack): Int {
        return priority
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        super.renderItem(type, item, *data)
        if (type == ItemRenderType.INVENTORY) {
            UtilsClient.drawEnergyBare(type, (getEnergy(item) / getEnergyMax(item)).toFloat())
        }
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}
}
