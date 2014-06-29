package mods.eln.computercraftio;

import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;

public class ComputerCraftIoRender extends TransparentNodeElementRender{

	ComputerCraftIoDescriptor descriptor;
	
	public ComputerCraftIoRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (ComputerCraftIoDescriptor) descriptor;
	}

	@Override
	public void draw() {
	}
}
