package mods.eln.sound

import mods.eln.misc.Coordinate
import net.minecraft.client.audio.*
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory

abstract class LoopedSound(val sample: String, val coord: Coordinate,
                           val attentuationType: ISound.AttenuationType = ISound.AttenuationType.LINEAR) : ITickableSound {
    var active = true

    override final fun getSoundLocation() = ResourceLocation(sample)
    override final fun getXPosF() = coord.pos.x.toFloat() + 0.5f
    override final fun getYPosF() = coord.pos.y.toFloat() + 0.5f
    override final fun getZPosF() = coord.pos.z.toFloat() + 0.5f
    override final fun canRepeat() = true
    override final fun getAttenuationType() = attentuationType

    override fun getPitch() = 1f
    override fun getVolume() = 1f
    override fun isDonePlaying() = !active

    override fun getRepeatDelay() = 0
    override fun update() {}

    override fun getSound(): Sound {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO(1.10): This might be useful, maybe?
    override fun createAccessor(handler: SoundHandler?): SoundEventAccessor? = null

    override fun getCategory() = SoundCategory.BLOCKS
}
