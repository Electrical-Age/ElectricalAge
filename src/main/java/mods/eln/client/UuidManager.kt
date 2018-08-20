package mods.eln.client

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase
import java.util.*

class UuidManager {
    internal val entities = HashMap <Int, IUuidEntity>()
    internal val uuids = HashMap <IUuidEntity, ArrayList<Int>>()

    init {
        FMLCommonHandler.instance().bus().register(this)
    }

    fun add(uuid: ArrayList<Int>, e: IUuidEntity) {
        uuid.forEach {
            entities.put(it, e)
            uuids.getOrPut(e, { ArrayList() }).add(it)
        }
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == Phase.END) return

        val i = entities.iterator()

        while (i.hasNext()) {
            val p = i.next()
            if (!p.value.isAlive) {
                uuids.remove(p.value)
                i.remove()
            }
        }
    }

    fun kill(uuid: Int) {
        entities.remove(uuid)?.apply {
            kill()
            uuids.remove(this)?.forEach { entities.remove(it) }
        }
    }
}
