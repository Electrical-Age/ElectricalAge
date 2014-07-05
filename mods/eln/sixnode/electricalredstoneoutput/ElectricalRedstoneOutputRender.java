package mods.eln.sixnode.electricalredstoneoutput;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class ElectricalRedstoneOutputRender extends SixNodeElementRender {

	ElectricalRedstoneOutputDescriptor descriptor;
	public ElectricalRedstoneOutputRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalRedstoneOutputDescriptor) descriptor;
	}

	float factor;


	float factorFiltred = 0;
	
	@Override
	public void draw() {
		super.draw();

		drawSignalPin(front.right(),descriptor.pinDistance);
		
		descriptor.draw(redOutput);
	}

	int redOutput;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			redOutput = stream.readByte();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
}
