package mods.eln.transparentnode.resincollector;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Industrial Resin Collector (IndustrialResinCollectorDescriptor): its model and status LEDs. */
public class IndustrialResinCollectorRender extends TransparentNodeElementRender {
    final IndustrialResinCollectorDescriptor desc;
    boolean powered, running;

    public IndustrialResinCollectorRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (IndustrialResinCollectorDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(powered, running);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            powered = stream.readBoolean();
            running = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
