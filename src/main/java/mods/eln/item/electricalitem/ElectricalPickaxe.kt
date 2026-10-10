package mods.eln.item.electricalitem

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ElectricalPickaxe(name: String, strengthOn: Float, strengthOff: Float,
                        energyStorage: Double, energyPerBlock: Double, chargePower: Double) : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Opens holes. Right-click to open smaller holes."))
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getStrVsBlock(stack: ItemStack, block: Block?): Float {
        var value = when {
            // 1.12 materials IRON, GLASS, ANVIL, ROCK -> 26.x: pickaxe-mineable blocks and glass
            block != null && block.defaultBlockState().let { it.`is`(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) || it.`is`(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) } -> getStrength(stack)
            else -> super.getStrVsBlock(stack, block)
        }
        if (blocksEffectiveAgainst.any { it == block }) {
            value = getStrength(stack)
        }
        return value
    }

    override fun onItemRightClick(s: ItemStack, w: Level, p: Player?): ItemStack {
        if (!w.isClientSide()) {
            setConservative(p, s, !getConservative(s))
        }
        return s
    }

    private fun getConservative(s: ItemStack) =
        getNbt(s).getBooleanOr("conservative", false)

    private fun setConservative(p: Player?, s: ItemStack, state: Boolean) {
        editNbt(s) { it.putBoolean("conservative", state) }
        if (p != null) {
            Utils.sendMessage(p, "Set land conservation to $state")
        }
    }

    override fun onBlockDestroyed(stack: ItemStack, w: Level, block: Block, x: Int, y: Int, z: Int, entity: LivingEntity): Boolean {
        val ok = super.onBlockDestroyed(stack, w, block, x, y, z, entity)
        if (entity !is Player) return ok
        if (!ok) return ok
        if (!getConservative(stack)) {
            for (a in (-1..1)) {
                for (b in (-1..0)) {
                    for (c in (-1..1)) {
                        if (a == 0 && b == 0 && c == 0) continue
                        val i = x+a
                        val j = y+b
                        val k = z+c
                        removeBlockWithDrops(entity, this, stack, w, i, j, k)
                    }
                }
            }
        }
        return ok
    }
}
