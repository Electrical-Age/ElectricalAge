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

    // takes in two points and gets the squared distance delta between them
    inline fun sqDistDelta(cx: Double, cy: Double, cz: Double, px: Double, py: Double, pz: Double) = (cx - px) * (cx - px) + (cy - py) * (cy - py) + (cz - pz) * (cz - pz)

    fun process(deltaT: Float) {
        remaining -= deltaT
        if (remaining <= 0) {
            val soundHandler = Minecraft.getMinecraft().soundHandler
            loops.forEach {
                // add 0.5 to put the point in the center of the block making sounds
                val pos = it.coord.pos
                val cx = pos.x + 0.5
                val cy = pos.y + 0.5
                val cz = pos.z + 0.5
                // get the player, and get the squared distance between the player and the block
                val player = Minecraft.getMinecraft().thePlayer
                val distDeltaSquared = sqDistDelta(cx, cy, cz, player.posX, player.posY, player.posZ)
                // when comparing, compare distDeltaSquared to the square of the distance delta that you are trying to compare against.
                if (it.volume > 0 && it.pitch > 0 && !soundHandler.isSoundPlaying(it) && distDeltaSquared < Eln.maxSoundDistance * Eln.maxSoundDistance) {
                    try {
                        soundHandler.playSound(it)
                    } catch (e: IllegalArgumentException) {
                        System.out.println(e)
                    }
                }
                if (distDeltaSquared >= Eln.maxSoundDistance * Eln.maxSoundDistance || it.volume == 0f || it.pitch == 0f) {
                    try {
                        soundHandler.stopSound(it)
                    }catch (e: Exception) {
                        System.out.println(e)
                    }
                }
            }

            remaining = updateInterval
        }
    }
}
