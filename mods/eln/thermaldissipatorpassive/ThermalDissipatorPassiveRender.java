package mods.eln.thermaldissipatorpassive;

import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;

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
