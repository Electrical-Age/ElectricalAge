package mods.eln.sound

import mods.eln.misc.Coordonate
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource

// 1.12: ISound grew createAccessor/getSound/getCategory; PositionedSound provides them.
// 1.12: the sample is resolved to a registered sound event (and its sounds.json category) by ElnSounds.
abstract class LoopedSound(val sample: String, val coord: Coordonate,
                           val attentuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR) :
    PositionedSound(ElnSounds.location(sample), ElnSounds.category(sample)), ITickableSound {
    var active = true

    override final fun getXPosF() = coord.x.toFloat() + 0.5f
    override final fun getYPosF() = coord.y.toFloat() + 0.5f
    override final fun getZPosF() = coord.z.toFloat() + 0.5f
    override final fun canRepeat() = true
    override final fun getAttenuationType() = attentuationType

    override fun getPitch() = 1f
    override fun getVolume() = 1f
    override fun isDonePlaying() = !active

    override fun getRepeatDelay() = 0
    override fun update() {}
}
