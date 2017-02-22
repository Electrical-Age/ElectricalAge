package mods.eln.gridnode.transformer

import mods.eln.gridnode.GridDescriptor
import mods.eln.misc.Obj3D
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sound.SoundCommand

/**
 * Created by svein on 07/08/15.
 */
class GridTransformerDescriptor(name: String, obj: Obj3D, cableTexture: String, cableDescriptor: ElectricalCableDescriptor) : GridDescriptor(name, obj, GridTransformerElement::class.java, GridTransformerRender::class.java, cableTexture, cableDescriptor, 12) {
    val minimalLoadToHum = 0.2f
    var highLoadSound = SoundCommand("eln:Transformer", 1.6)

    override fun rotationIsFixed(): Boolean {
        return true
    }

}

