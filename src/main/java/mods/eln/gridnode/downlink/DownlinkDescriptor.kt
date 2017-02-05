package mods.eln.gridnode.downlink

import mods.eln.gridnode.GridDescriptor
import mods.eln.misc.Obj3D
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor

/**
 * Created by svein on 24/08/15.
 */
class DownlinkDescriptor(name: String, obj: Obj3D, cableTexture: String, cableDescriptor: ElectricalCableDescriptor) : GridDescriptor(name, obj, DownlinkElement::class.java, DownlinkRender::class.java, cableTexture, cableDescriptor)
