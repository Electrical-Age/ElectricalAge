package mods.eln.item.electricalitem

import mods.eln.generic.genericArmorItem
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraft.world.World
import net.minecraftforge.common.ISpecialArmor

class ElectricalArmor(materialIn: ItemArmor.ArmorMaterial,
                      renderSlotIn: Int,
                      equipmentSlotIn: EntityEquipmentSlot,
                      private var energyStorage: Double,
                      internal var chargePower: Double,
                      private var ratioMax: Double,
                      private var ratioMaxEnergy: Double,
                      private var energyPerDamage: Double) : genericArmorItem(materialIn, renderSlotIn, equipmentSlotIn), IItemEnergyBattery, ISpecialArmor {

    private val defaultNBT: NBTTagCompound
        get() {
            val nbt = NBTTagCompound()
            nbt.setDouble("energy", 0.0)
            nbt.setBoolean("powerOn", false)
            nbt.setInteger("rand", (Math.random() * 0xFFFFFFF).toInt())
            return nbt
        }

    init {
        Data.addPortable(ItemStack(this))
    }

    override fun getProperties(player: EntityLivingBase, armor: ItemStack, source: DamageSource, damage: Double, slot: Int): ISpecialArmor.ArmorProperties {
        return ISpecialArmor.ArmorProperties(100, Math.min(1.0, getEnergy(armor) / ratioMaxEnergy) * ratioMax, (getEnergy(armor) / energyPerDamage * 25.0).toInt())
    }

    override fun getArmorDisplay(player: EntityPlayer, armor: ItemStack, slot: Int): Int {
        return (Math.min(1.0, getEnergy(armor) / ratioMaxEnergy) * ratioMax * 20.0).toInt()
    }

    override fun damageArmor(entity: EntityLivingBase, stack: ItemStack, source: DamageSource, damage: Int, slot: Int) {
        var e = getEnergy(stack)
        e = Math.max(0.0, e - damage * energyPerDamage)
        setEnergy(stack, e)
        Utils.println("armor hit  damage=" + damage + " energy=" + e + " energyLost=" + damage * energyPerDamage)
    }

    override fun getIsRepairable(par1ItemStack: ItemStack?, par2ItemStack: ItemStack): Boolean {
        return false
    }

    override fun hasColor(par1ItemStack: ItemStack): Boolean {
        return false
    }

    private fun getNbt(stack: ItemStack): NBTTagCompound {
        val nbt: NBTTagCompound? = stack.tagCompound
        if (nbt == null) {
            stack.tagCompound = defaultNBT
        }
        return stack.tagCompound!!
    }

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        super.addInformation(stack, worldIn, tooltip, flagIn)
        tooltip.add(tr("Charge power: %sW", chargePower.toInt()))
        tooltip.add(tr("Stored energy: %sJ (%s)", getEnergy(stack),
                (getEnergy(stack) / energyStorage * 100).toInt()))
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).setDouble("energy", value)
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return 0.0
    }

    override fun getPriority(stack: ItemStack): Int {
        return 0
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}

    override fun getItemEnchantability(): Int {
        return 0
    }
}
