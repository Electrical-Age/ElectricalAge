package mods.eln.client

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase

import java.util.ArrayList
import java.util.LinkedList

class UuidManager {
    internal var eList = LinkedList<Pair>()

    class Pair internal constructor(internal var uuid: ArrayList<Int>?, internal var e: IUuidEntity)

    init {
        FMLCommonHandler.instance().bus().register(this)
    }

    fun add(uuid: ArrayList<Int>, e: IUuidEntity) {
        eList.add(Pair(uuid, e))
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == Phase.END) return

        val i = eList.iterator()

        while (i.hasNext()) {
            val p = i.next()
            if (!p.e.isAlive) {
                i.remove()
            }
        }
        //Utils.println(eList.size());
    }

    fun kill(uuid: Int) {
        val i = eList.iterator()
        while (i.hasNext()) {
            val p = i.next()
            if (p.uuid == null) continue
            for (pUuid in p.uuid!!) {
                if (pUuid === uuid) {
                    p.e.kill()
                    i.remove()
                }
            }
        }
    }
}
