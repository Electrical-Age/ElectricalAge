package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The tank: its vessel, and the water in its sight glass (TankElement's fill, published). */
public class TankRender extends TransparentNodeElementRender {
    final TankDescriptor desc;
    float fill;

    public TankRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (TankDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(fill);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            fill = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
