package mods.eln.electricalantennarx;


import java.io.DataInputStream;

import mods.eln.misc.LRDU;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;

public class ElectricalAntennaRxRender extends TransparentNodeElementRender{

	public ElectricalAntennaRxRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (ElectricalAntennaRxDescriptor) descriptor;
	}
	
	ElectricalAntennaRxDescriptor descriptor;

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		front.glRotateXnRef();
		rot.glRotateOnX();
		descriptor.draw();
	}
	LRDU rot;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		rot = rot.deserialize(stream);
	}

}
