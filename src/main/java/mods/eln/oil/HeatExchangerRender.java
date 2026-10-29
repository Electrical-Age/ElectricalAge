package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** The heat exchanger (the Steam Condenser's model until its own). */
public class HeatExchangerRender extends TransparentNodeElementRender {
    final HeatExchangerDescriptor desc;

    public HeatExchangerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (HeatExchangerDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
