package mods.eln.transparentnode.computercraftio;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

public class ComputerCraftIoRender extends TransparentNodeElementRender {

	ComputerCraftIoDescriptor descriptor;
	
	public ComputerCraftIoRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (ComputerCraftIoDescriptor) descriptor;
	}

	@Override
	public void draw() {
	}
}
