package mods.eln.item.electricalitem

import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class ElectricalAxe(name: String, strengthOn: Float, strengthOff: Float,
                    energyStorage: Double, energyPerBlock: Double, chargePower: Double) : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getStrVsBlock(stack: ItemStack, block: Block?): Float {
        val value = if (block != null && (block.material === Material.wood || block.material === Material.plants || block.material === Material.vine)) getStrength(stack) else super.getStrVsBlock(stack, block)
        Utils.println(value)
        return value
    }
}
