package mods.eln.thermaldissipatoractive;

import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;

public class ThermalDissipatorActiveRender extends TransparentNodeElementRender{
	ThermalDissipatorActiveDescriptor descriptor;
	public ThermalDissipatorActiveRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (ThermalDissipatorActiveDescriptor) descriptor;
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		
	}

}
