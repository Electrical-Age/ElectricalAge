package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** Draws the pump (placeholder: the compressor model). */
public class OilPumpRender extends TransparentNodeElementRender {
    final OilPumpDescriptor desc;

    public OilPumpRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        this.desc = (OilPumpDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
