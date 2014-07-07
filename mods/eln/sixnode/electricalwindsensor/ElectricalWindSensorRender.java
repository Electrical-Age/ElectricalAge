package mods.eln.sixnode.electricalwindsensor;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class ElectricalWindSensorRender extends SixNodeElementRender {

	ElectricalWindSensorDescriptor descriptor;

	public ElectricalWindSensorRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalWindSensorDescriptor) descriptor;
	}

	float alpha = 0;
	float wind = 0;

	RcInterpolator windFilter = new RcInterpolator(5);

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(),new float[]{2,2,2,2});

		descriptor.draw(alpha);
	}

	@Override
	public void refresh(float deltaT) {
		windFilter.step(deltaT);
		alpha += windFilter.get() * deltaT * 20;
		if (alpha > 360)
			alpha -= 360;
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		
		return Eln.instance.signalCableDescriptor.render;
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
			wind = stream.readFloat();
			windFilter.setTarget(wind);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}
