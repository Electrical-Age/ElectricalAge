package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** The plant condenser (mods.eln.cad.models.PlantCondenser), its drive coupling toward the motor that drives it. */
public class PlantCondenserRender extends TransparentNodeElementRender {
    final PlantCondenserDescriptor desc;

    public PlantCondenserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (PlantCondenserDescriptor) d;
    }

    int motorSide;

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(motorSide);
    }

    @Override
    public void networkUnserialize(java.io.DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            motorSide = stream.readByte();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
