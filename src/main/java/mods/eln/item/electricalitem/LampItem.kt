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

    /** The light this lamp gives now (0 = off), for the beam drawn on the client (client/LampBeam). */
    fun beamLight(stack: ItemStack): Int = if (getLightState(stack) == 0) 0 else getLight(stack)

    companion object {
        /**
         * The beam an entity casts (the one light for every EA lamp, Baughn 2026-10-31): the brightest lit flashlight in
         * either hand, the worn headlamp, or the front lamp of the bike it rides, else 0. For the client's beam drawing
         * (client/LampBeam).
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
            val bike = entity.vehicle
            if (bike is mods.eln.bike.ElectricBike && bike.lampOn() && bike.controllingPassenger === entity)
                best = maxOf(best, mods.eln.bike.ElectricBike.LAMP_LIGHT)
            return best
        }

        /**
         * The server's half of a lamp's light (hand-held, worn, or the bike's): one light block at the holder's eyes;
         * the beam itself is drawn on each client (client/LampBeam). Baughn 2026-11-06: no trail of light blocks
         * along the beam (1.12's, which lagged behind a turning player), and no fallback to it.
         */
        @JvmStatic
        fun light(world: Level, entity: Entity, light: Int) {
            val fx = Mth.floor(entity.getX())
            val fy = Mth.floor(entity.getY() + 1.62)
            val fz = Mth.floor(entity.getZ())
            if (WorldCompat.isAir(world, fx, fy, fz)) LightBlockEntity.addLight(world, fx, fy, fz, light, 5)
        }
    }

    override fun onUpdate(stack: ItemStack, world: Level, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isClientSide()) return
        if (getLightState(stack) == 0) return
        val light = getLight(stack)
        if (light == 0) return
        light(world, entity, light)
    }
}
