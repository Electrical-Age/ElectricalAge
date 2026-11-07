package mods.eln.transparentnode.desulfuriser;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Desulfuriser: its model and status LEDs. */
public class DesulfuriserRender extends TransparentNodeElementRender {
    final DesulfuriserDescriptor desc;
    boolean powered, running;

    public DesulfuriserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (DesulfuriserDescriptor) d;
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
