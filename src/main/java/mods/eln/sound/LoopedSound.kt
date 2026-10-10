package mods.eln.sound

import mods.eln.misc.Coordonate

/**
 * A sound an element render keeps playing while it wants to (1.12 LoopedSound, a PositionedSound). On 26.x this is
 * only the description, server-safe: renders register theirs with addLoopedSound, and the client plays them (R4).
 */
abstract class LoopedSound @JvmOverloads constructor(val sample: String, val coord: Coordonate, val attenuation: Attenuation = Attenuation.LINEAR) {
    enum class Attenuation { NONE, LINEAR }

    var active = true

    open fun getPitch() = 1f
    open fun getVolume() = 1f
}
