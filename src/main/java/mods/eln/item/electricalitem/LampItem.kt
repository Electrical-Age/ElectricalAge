package mods.eln.item.electricalitem


import mods.eln.compat.WorldCompat
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.sixnode.lampsocket.LightBlockEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.util.Mth
import net.minecraft.world.level.Level

abstract class LampItem(name: String) : GenericItemUsingDamageDescriptor(name) {

    internal abstract fun getLightState(stack: ItemStack): Int

    internal abstract fun getRange(stack: ItemStack): Int

    internal abstract fun getLight(stack: ItemStack): Int

    /** 26.x, for Stasis Delvers: the light this lamp gives now (0 = off), for a beam drawn on the client. */
    fun beamLight(stack: ItemStack): Int = if (getLightState(stack) == 0) 0 else getLight(stack)

    companion object {
        /**
         * 26.x, for Stasis Delvers: if set and true for the holder, a client draws the beam, so onUpdate places one
         * light block at the holder's eyes (no mobs spawning right around them) instead of the trail along the beam,
         * which lags behind a turning player. Unset: as in 1.12.
         */
        @JvmField
        @Volatile
        var clientBeam: java.util.function.Predicate<Entity>? = null
    }

    override fun onUpdate(stack: ItemStack, world: Level, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isClientSide()) return
        if (getLightState(stack) == 0) return
        val light = getLight(stack)
        if (light == 0) return

        val beam = clientBeam
        if (beam != null && beam.test(entity)) {
            val fx = Mth.floor(entity.getX())
            val fy = Mth.floor(entity.getY() + 1.62)
            val fz = Mth.floor(entity.getZ())
            if (WorldCompat.isAir(world, fx, fy, fz)) LightBlockEntity.addLight(world, fx, fy, fz, light, 5)
            return
        }

        for (yOffset in 0..1) {
            var x = entity.getX()
            var y = entity.getY() + 1.62 - yOffset
            var z = entity.getZ()

            val v = entity.lookAngle.scale(0.25) // Vec3d is immutable in 1.12

            val range = getRange(stack) + 1
            var rCount = 0

            for (idx in 0 until range) {
                x += v.x
                y += v.y
                z += v.z

                val fx = Mth.floor(x)
                val fy = Mth.floor(y)
                val fz = Mth.floor(z)
                if (!WorldCompat.isAir(world, fx, fy, fz)) {
                    x -= v.x
                    y -= v.y
                    z -= v.z
                    break
                }
                rCount++
            }

            while (rCount > 0) {
                var stride = 1
                val fx = Mth.floor(x)
                val fy = Mth.floor(y)
                val fz = Mth.floor(z)
                if (WorldCompat.isAir(world, fx, fy, fz)) {
                    LightBlockEntity.addLight(world, fx, fy, fz, light, 5)
                    stride = 3
                }
                x -= v.x * stride
                y -= v.y * stride
                z -= v.z * stride
                rCount -= stride
            }
        }
    }
}
