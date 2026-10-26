package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The condenser: its model, the duct toward its turbine (CondenserElement.turbineSide, published). */
public class CondenserRender extends TransparentNodeElementRender {
    final CondenserDescriptor desc;
    int turbineSide = -1;

    public CondenserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (CondenserDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        String duct = null;
        if (turbineSide >= 0) {
            var d = mods.eln.misc.Direction.fromInt(turbineSide);
            duct = d == front.right() ? "duct_right" : d == front.getInverse() ? "duct_back" : d == front.left() ? "duct_left" : null;
        }
        desc.draw(duct);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            turbineSide = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
