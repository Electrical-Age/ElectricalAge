package mods.eln.transparentnode.thermaldissipatorpassive;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.node.transparent.TransparentNodeRender;

public class ThermalDissipatorPassiveRender extends TransparentNodeElementRender{
	ThermalDissipatorPassiveDescriptor descriptor;
	public ThermalDissipatorPassiveRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (ThermalDissipatorPassiveDescriptor) descriptor;
	}

	@Override
	public void draw() {
		front.glRotateXnRef();
		
		descriptor.draw();
	}

}
