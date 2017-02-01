package mods.eln.client

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase
import mods.eln.misc.Utils
import net.minecraft.launchwrapper.LogWrapper.log
import java.util.*

class UuidManager {
    internal val entities = HashMap <Int, IUuidEntity>()

    init {
        FMLCommonHandler.instance().bus().register(this)
    }

    fun add(uuid: ArrayList<Int>, e: IUuidEntity) {
        if (uuid.size != 1) {
            Utils.fatal()
        }
        val uuid = uuid.single()
        if (entities.containsKey(uuid)) {
            Utils.fatal()
        }
        entities.put(uuid, e)
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == Phase.END) return

        val i = entities.iterator()

        while (i.hasNext()) {
            val p = i.next()
            if (!p.value.isAlive) {
                i.remove()
            }
        }
    }

    fun kill(uuid: Int) {
        entities.remove(uuid)?.kill()
    }
}
