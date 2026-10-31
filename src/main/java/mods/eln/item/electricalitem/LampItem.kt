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

        /**
         * The beam an entity casts (the one light for every EA lamp, Baughn 2026-10-31): the brightest lit flashlight in
         * either hand or the worn headlamp, else 0. For the client's beam drawing (Stasis Delvers' FlashlightPost).
         */
        @JvmStatic
        fun beamLight(entity: Entity): Int {
            var best = 0
            if (entity is net.minecraft.world.entity.LivingEntity)
                for (s in listOf(entity.mainHandItem, entity.offhandItem)) {
                    val lamp = mods.eln.misc.Utils.getItemObject(s)
                    if (lamp is LampItem) best = maxOf(best, lamp.beamLight(s))
                }
            if (entity is Player) {
                val head = mods.eln.item.headlamp.HeadlampItem.worn(entity)
                if (head.item is mods.eln.item.headlamp.HeadlampItem) best = maxOf(best, (head.item as mods.eln.item.headlamp.HeadlampItem).light(head))
            }
            return best
        }

        /**
         * The server's half of a lamp's light (hand-held or worn): one light block at the eyes when a client draws the
         * beam (clientBeam), else the 1.12 trail of light blocks along the look.
         */
        @JvmStatic
        fun light(world: Level, entity: Entity, light: Int, range: Int) {
            val beam = clientBeam
            if (beam != null && beam.test(entity)) {
                val fx = Mth.floor(entity.getX())
                val fy = Mth.floor(entity.getY() + 1.62)
                val fz = Mth.floor(entity.getZ())
                if (WorldCompat.isAir(world, fx, fy, fz)) LightBlockEntity.addLight(world, fx, fy, fz, light, 5)
                return
            }
            placeBeam(world, entity, light, range)
        }

        /** Light blocks along the entity's look (two rows, from its eyes to the first solid block within range). */
        @JvmStatic
        fun placeBeam(world: Level, entity: Entity, light: Int, range0: Int) {
            for (yOffset in 0..1) {
                var x = entity.getX()
                var y = entity.getY() + 1.62 - yOffset
                var z = entity.getZ()

                val v = entity.lookAngle.scale(0.25) // Vec3d is immutable in 1.12

                val range = range0 + 1
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

    override fun onUpdate(stack: ItemStack, world: Level, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isClientSide()) return
        if (getLightState(stack) == 0) return
        val light = getLight(stack)
        if (light == 0) return
        light(world, entity, light, getRange(stack))
    }
}
