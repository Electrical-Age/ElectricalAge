package mods.eln.sixnode.electricalfiredetector

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sound.LoopedSound
import net.minecraft.client.audio.ISound
import net.minecraft.entity.player.EntityPlayer
import java.io.DataInputStream
import java.io.IOException

class ElectricalFireDetectorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor)
    : SixNodeElementRender(tileEntity, side, descriptor) {
    val descriptor = descriptor as ElectricalFireDetectorDescriptor

    var powered = false
    var firePresent = false
    var ledOn = false

    val inventory: SixNodeElementInventory?

    init {
        if (this.descriptor.batteryPowered) {
            inventory = SixNodeElementInventory(1, 64, this)
            addLoopedSound(object : LoopedSound("eln:FireAlarm",
                Coordinate(tileEntity),
                ISound.AttenuationType.LINEAR) {
                override fun getVolume() = if (firePresent) 1f else 0f
            })
        } else {
            inventory = null
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

    override fun getCableRender(lrdu: LRDU) = Eln.instance.signalCableDescriptor.render!!

    override fun newGuiDraw(side: Direction, player: EntityPlayer) = if (inventory != null)
        ElectricalFireDetectorGui(player, inventory, this) else null
}
