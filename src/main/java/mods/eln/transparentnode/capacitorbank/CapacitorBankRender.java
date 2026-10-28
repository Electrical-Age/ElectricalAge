package mods.eln.transparentnode.capacitorbank;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

/** The capacitor bank (CapacitorBankDescriptor): the rack of cans. */
public class CapacitorBankRender extends TransparentNodeElementRender {
    final CapacitorBankDescriptor desc;

    public CapacitorBankRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (CapacitorBankDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }
}
