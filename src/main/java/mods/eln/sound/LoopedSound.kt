package mods.eln.sound

import mods.eln.misc.Coordonate
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.ITickableSound
import net.minecraft.util.ResourceLocation

abstract class LoopedSound(val sample: String, val coord: Coordonate,
                           val attentuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR) : ITickableSound {
    override final fun getPositionedSoundLocation() = ResourceLocation(sample)
    override final fun getXPosF() = coord.x.toFloat() + 0.5f
    override final fun getYPosF() = coord.y.toFloat() + 0.5f
    override final fun getZPosF() = coord.z.toFloat() + 0.5f
    override final fun canRepeat() = true
    override final fun getAttenuationType() = attentuationType

    override fun getPitch() = 1f
    override fun getVolume() = 1f
    override fun isDonePlaying() = false

    override fun getRepeatDelay() = 0
    override fun update() {}
}
