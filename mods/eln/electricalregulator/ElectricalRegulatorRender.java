package mods.eln.electricalregulator;

import mods.eln.misc.Direction;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class ElectricalRegulatorRender extends SixNodeElementRender {

	public ElectricalRegulatorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
	}

	@Override
	public void draw() {
	}
}
