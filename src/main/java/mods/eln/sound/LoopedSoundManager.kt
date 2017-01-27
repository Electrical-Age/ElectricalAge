package mods.eln.sound

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
                if (!soundHandler.isSoundPlaying(it)) {
                    try {
                        soundHandler.playSound(it)
                    } catch (e: IllegalArgumentException) {}
                }
            }

            remaining = updateInterval
        }
    }
}
