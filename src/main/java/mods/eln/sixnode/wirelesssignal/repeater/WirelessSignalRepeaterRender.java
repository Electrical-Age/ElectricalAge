package mods.eln.sixnode.wirelesssignal.repeater;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.init.Cable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class WirelessSignalRepeaterRender extends SixNodeElementRender {

    WirelessSignalRepeaterDescriptor descriptor;

    public WirelessSignalRepeaterRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (WirelessSignalRepeaterDescriptor) descriptor;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Cable.Companion.getSignal().descriptor.render;
    }

    @Override
    public void draw() {
        super.draw();
        front.glRotateOnX();
        descriptor.draw();
    }
}
