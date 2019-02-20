package mods.eln.item.electricalitem

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.sixnode.lampsocket.LightBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import net.minecraft.util.MathHelper
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

            val v = entity.lookVec

            v.xCoord *= 0.25
            v.yCoord *= 0.25
            v.zCoord *= 0.25

            val range = getRange(stack) + 1
            var rCount = 0

            for (idx in 0 until range) {
                x += v.xCoord
                y += v.yCoord
                z += v.zCoord

                val fx = MathHelper.floor_double(x)
                val fy = MathHelper.floor_double(y)
                val fz = MathHelper.floor_double(z)
                val block = world.getBlock(fx, fy, fz)
                if (!block.isAir(world, fx, fy, fz)) {
                    x -= v.xCoord
                    y -= v.yCoord
                    z -= v.zCoord
                    break
                }
                rCount++
            }

            while (rCount > 0) {
                var stride = 1
                val fx = MathHelper.floor_double(x)
                val fy = MathHelper.floor_double(y)
                val fz = MathHelper.floor_double(z)
                val block = world.getBlock(fx, fy, fz)
                if (block.isAir(world, fx, fy, fz)) {
                    LightBlockEntity.addLight(world, fx, fy, fz, light, 5)
                    stride = 3
                }
                x -= v.xCoord * stride
                y -= v.yCoord * stride
                z -= v.zCoord * stride
                rCount -= stride
            }
        }
    }
}
