package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class BoilerRender extends TransparentNodeElementRender {
    final BoilerDescriptor desc;
    /** The gauges (Baughn: the dials should work): the gauge glass's fill (0..1) and the water temperature (C). */
    float fill, celsius;

    public BoilerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (BoilerDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(fill, celsius);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            fill = stream.readFloat();
            celsius = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
