package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

public class ColumnRender extends TransparentNodeElementRender {
    final ColumnDescriptor desc;

    public ColumnRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (ColumnDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
