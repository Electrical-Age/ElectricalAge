package mods.eln.item.electricalitem

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.sixnode.lampsocket.LightBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World

abstract class LampItem(name: String) : GenericItemUsingDamageDescriptor(name) {

    internal abstract fun getLightState(stack: ItemStack): Int

    internal abstract fun getRange(stack: ItemStack): Int

    internal abstract fun getLight(stack: ItemStack): Int

    override fun onUpdate(stack: ItemStack, world: World, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isRemote) return
        if (getLightState(stack) == 0) return
        val light = getLight(stack)
        if (light == 0) return

        for (yOffset in 0..1) {
            var x = entity.posX
            var y = entity.posY + 1.62 - yOffset
            var z = entity.posZ

            val v = entity.lookVec.scale(0.25)

            val range = getRange(stack) + 1
            var rCount = 0

            for (idx in 0 until range) {
                x += v.xCoord
                y += v.yCoord
                z += v.zCoord

                val pos = BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z))
                val state = world.getBlockState(pos)
                val block = state.block
                if (!block.isAir(state, world, pos) && block !== Eln.lightBlock) {
                    x -= v.xCoord
                    y -= v.yCoord
                    z -= v.zCoord
                    break
                }
                rCount++
            }

            while (rCount > 0) {
                val pos = BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z))
                val state = world.getBlockState(pos)
                val block = state.block
                if (block.isAir(state, world, pos) || block === Eln.lightBlock) {
                    LightBlockEntity.addLight(world, pos, light, 10)
                    return
                }
                x -= v.xCoord
                y -= v.yCoord
                z -= v.zCoord
                rCount--
            }
        }
    }
}
