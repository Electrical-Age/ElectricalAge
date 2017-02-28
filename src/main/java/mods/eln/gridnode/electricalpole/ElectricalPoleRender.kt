package mods.eln.gridnode.electricalpole

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.gridnode.GridRender
import mods.eln.misc.LRDUMask
import mods.eln.misc.SlewLimiter
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sound.LoopedSound
import net.minecraft.client.audio.ISound

import java.io.DataInputStream
import java.io.IOException

class ElectricalPoleRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity, descriptor) {

    internal var cableRenderType: CableRenderType? = null
    internal var eConn = LRDUMask()

    private val descriptor: ElectricalPoleDescriptor
    private val load = SlewLimiter(0.5f)

    init {
        this.descriptor = descriptor as ElectricalPoleDescriptor

        if (this.descriptor.includeTransformer) {
            addLoopedSound(object : LoopedSound("eln:Transformer", coordonate(), ISound.AttenuationType.LINEAR) {
                override fun getVolume(): Float {
                    if (load.position > this@ElectricalPoleRender.descriptor.minimalLoadToHum)
                        return 0.05f * (load.position - this@ElectricalPoleRender.descriptor.minimalLoadToHum) / (1 - this@ElectricalPoleRender.descriptor.minimalLoadToHum)
                    else
                        return 0f
                }
            })
        }
    }

    override fun draw() {
        super.draw()
        cableRenderType = drawCable(front.down(), Eln.instance.stdCableRender3200V, eConn, cableRenderType)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        eConn.deserialize(stream)
        cableRenderType = null
        try {
            load.target = stream.readFloat()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        load.step(deltaT)
    }
}
