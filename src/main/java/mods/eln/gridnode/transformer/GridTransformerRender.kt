package mods.eln.gridnode.transformer

import mods.eln.gridnode.GridRender
import mods.eln.misc.SlewLimiter
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sound.LoopedSound
import net.minecraft.client.audio.ISound
import java.io.DataInputStream

class GridTransformerRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity, descriptor) {
    val desc = descriptor as GridTransformerDescriptor
    private var load = SlewLimiter(0.5f)

    init {
        addLoopedSound(object : LoopedSound("eln:Transformer", coordonate(), ISound.AttenuationType.LINEAR) {
            override fun getVolume() = Math.max(0f, (load.position - desc.minimalLoadToHum) / (1 - desc.minimalLoadToHum))
        })
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        load.target = stream.readFloat()
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        load.step(deltaT)
    }
}
