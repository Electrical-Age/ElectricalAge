package mods.eln.sixnode.electricalfiredetector


import mods.eln.registry.ElnDeviceRegistry
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sound.LoopedSound
import net.minecraft.world.entity.player.Player
import java.io.DataInputStream
import java.io.IOException

class ElectricalFireDetectorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElementRender(tileEntity, side, descriptor) {
    val descriptor = descriptor as ElectricalFireDetectorDescriptor

    var powered = false
    var firePresent = false
    var ledOn = false

    init {
        if (this.descriptor.batteryPowered) {
            addLoopedSound(object : LoopedSound("eln:firealarm", Coordonate(tileEntity.blockPos, tileEntity.level), LoopedSound.Attenuation.LINEAR) {
                override fun getVolume() = if (firePresent) 1f else 0f
            })
        }
    }

    override fun draw() {
        super.draw()

        if (!descriptor.batteryPowered) {
            drawSignalPin(front.right(), descriptor.pinDistance)
        }

        descriptor.draw(ledOn)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            powered = stream.readBoolean()
            firePresent = stream.readBoolean()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    var time = 0f
    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        time += deltaT

        if (powered) {
            if (firePresent) {
                ledOn = firePresent
            } else {
                ledOn = (time * 5).toInt() % 25 == 0
            }
        } else {
            ledOn = false
        }
    }

    override fun getCableRender(lrdu: LRDU) = ElnDeviceRegistry.signalCableDescriptor.render!!

}
