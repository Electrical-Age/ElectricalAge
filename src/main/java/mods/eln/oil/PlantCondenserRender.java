package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** The plant condenser (the Steam Condenser's model until its own). */
public class PlantCondenserRender extends TransparentNodeElementRender {
    final PlantCondenserDescriptor desc;

    public PlantCondenserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (PlantCondenserDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
