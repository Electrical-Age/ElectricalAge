package mods.eln.gridnode.downlink;

import mods.eln.gridnode.GridDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * Created by svein on 24/08/15.
 */
public class DownlinkDescriptor extends GridDescriptor {
    public DownlinkDescriptor(String name, Obj3D obj, String cableTexture, ElectricalCableDescriptor cableDescriptor) {
        super(name, obj, DownlinkElement.class, DownlinkRender.class, cableTexture, cableDescriptor);
    }
}
