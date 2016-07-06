package mods.eln.sixnode.electricalfiredetector;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalFireDetectorRender extends SixNodeElementRender {

	ElectricalFireDetectorDescriptor descriptor;

	boolean powered = false;
    boolean firePresent = false;
	boolean ledOn = false;

	public ElectricalFireDetectorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalFireDetectorDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(), descriptor.pinDistance);

		descriptor.draw(ledOn);
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			powered = stream.readBoolean();
            firePresent = stream.readBoolean();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	float time = 0;
	@Override
	public void refresh(float deltaT) {
		time += deltaT;

		if (powered) {
			if (firePresent) {
				ledOn = firePresent;
			} else {
				ledOn = (int)(time * 5) % 25 == 0;
			}
		} else {
			ledOn = false;
		}
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
}
