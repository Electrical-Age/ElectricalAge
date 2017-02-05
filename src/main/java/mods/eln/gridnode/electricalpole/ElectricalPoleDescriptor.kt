package mods.eln.gridnode.electricalpole

import mods.eln.gridnode.GridDescriptor
import mods.eln.misc.Obj3D
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor

/**
 * Created by svein on 07/08/15.
 */
class ElectricalPoleDescriptor(name: String, obj: Obj3D, cableTexture: String, cableDescriptor: ElectricalCableDescriptor, val includeTransformer: Boolean) : GridDescriptor(name, obj, ElectricalPoleElement::class.java, ElectricalPoleRender::class.java, cableTexture, cableDescriptor) {
    val minimalLoadToHum = 0.2f

    init {
        if (includeTransformer) {
            rotating_parts.add(obj.getPart("trafo"))
            rotating_parts.add(obj.getPart("cables"))
            static_parts.add(obj.getPart("foot"))
        }
    }
}
