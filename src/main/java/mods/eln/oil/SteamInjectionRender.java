package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** Draws the steam injection wellhead (the 50V oil pump's model until its own). */
public class SteamInjectionRender extends TransparentNodeElementRender {
    final SteamInjectionDescriptor desc;

    public SteamInjectionRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        this.desc = (SteamInjectionDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
