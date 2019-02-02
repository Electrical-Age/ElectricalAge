package mods.eln.item.electricalitem

import mods.eln.wiki.Data
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class ElectricalPickaxe(name: String, strengthOn: Float, strengthOff: Float,
                        energyStorage: Double, energyPerBlock: Double, chargePower: Double) : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getStrVsBlock(stack: ItemStack, block: Block?): Float {
        var value = when {
            block != null && (block.material === Material.iron || block.material === Material.glass || block.material === Material.anvil || block.material === Material.rock) -> getStrength(stack)
            else -> super.getStrVsBlock(stack, block)
        }
        if (blocksEffectiveAgainst.any { it == block }) {
            value = getStrength(stack)
        }
        return value
    }
}
