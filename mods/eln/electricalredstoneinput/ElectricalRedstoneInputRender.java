package mods.eln.electricalredstoneinput;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class ElectricalRedstoneInputRender extends SixNodeElementRender {

	ElectricalRedstoneInputDescriptor descriptor;
	public ElectricalRedstoneInputRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalRedstoneInputDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(),descriptor.pinDistance);

		LRDU.Down.glRotateOnX();
		descriptor.draw(redLevel);
	}

	byte redLevel;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			redLevel = stream.readByte();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
}
