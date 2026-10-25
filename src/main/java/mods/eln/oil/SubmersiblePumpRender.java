package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** Draws the submersible pump. */
public class SubmersiblePumpRender extends TransparentNodeElementRender {
    final SubmersiblePumpDescriptor desc;

    public SubmersiblePumpRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SubmersiblePumpDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
