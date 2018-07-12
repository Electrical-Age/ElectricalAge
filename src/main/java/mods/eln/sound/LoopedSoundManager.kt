package mods.eln.sound

import mods.eln.Eln
import net.minecraft.client.Minecraft

class LoopedSoundManager(val updateInterval: Float = 0.5f) {
    private var remaining = 0f
    private val loops = mutableSetOf<LoopedSound>()

    fun add(loop: LoopedSound?) {
        if (loop != null && loop.active) {
            loops.add(loop)
        }
    }

    fun dispose() = loops.forEach { it.active = false }

    fun process(deltaT: Float) {
        remaining -= deltaT
        if (remaining <= 0) {
            val soundHandler = Minecraft.getMinecraft().soundHandler
            loops.forEach {
                val x = it.coord.x
                val y = it.coord.y
                val z = it.coord.z
                val player = Minecraft.getMinecraft().thePlayer
                val distance = player.getDistance(x.toDouble(), y.toDouble(), z.toDouble())
                if (it.volume > 0 && it.pitch > 0 && !soundHandler.isSoundPlaying(it) && distance < Eln.maxSoundDistance) {
                    try {
                        soundHandler.playSound(it)
                    } catch (e: IllegalArgumentException) {
                        System.out.println(e)
                    }
                }
                if (distance >= Eln.maxSoundDistance || it.volume == 0f || it.pitch == 0f) {
                    try{
                        soundHandler.stopSound(it)
                    } catch (e: Exception) {
                        System.out.println(e)
                    }
                }
            }
            remaining = updateInterval
        }
    }
}
