package mods.eln.powerprimitives;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

/** The six-node Power Capacitor's and Power Inductor's render: pins when off centre, the model turned to the front. */
public class PowerPrimitiveSixRender extends SixNodeElementRender {

    final PowerPrimitiveSixDescriptor descriptor;

    public PowerPrimitiveSixRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (PowerPrimitiveSixDescriptor) descriptor;
    }

    @Override
    public void draw() {
        drawPins();   // (off centre)
        descriptor.turnToFront(front);
        if (slot >= 6) descriptor.drawShort();
        else descriptor.draw();
    }
}
