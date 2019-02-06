package mods.eln.item.electricalitem

import mods.eln.wiki.Data
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class ElectricalPickaxe(name: String, strengthOn: Float, strengthOff: Float,
                        energyStorage: Double, energyPerBlock: Double, chargePower: Double) : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getStrVsBlock(stack: ItemStack, state: IBlockState): Float {
        return when {
            state.material in pickaxeEffectiveAgainst -> getStrength(stack)
            state.block in blocksEffectiveAgainst -> getStrength(stack)
            else -> super.getStrVsBlock(stack, state)
        }
    }

    private val pickaxeEffectiveAgainst = arrayOf(
        Material.IRON,
        Material.GLASS,
        Material.ANVIL,
        Material.ROCK
    )
}
