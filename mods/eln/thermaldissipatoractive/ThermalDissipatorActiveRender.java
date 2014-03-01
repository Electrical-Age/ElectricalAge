package mods.eln.thermaldissipatoractive;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.client.FrameTime;
import mods.eln.misc.RcInterpolator;
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
	RcInterpolator rc = new RcInterpolator(2f);
	@Override
	public void draw() {
		front.glRotateXnRef();
		
		rc.setTarget(powerFactor);
		rc.stepGraphic();
		alpha += rc.get() * 360f * FrameTime.get();
		while(alpha > 360f) alpha -= 360f;
		descriptor.draw(alpha);
	}
	float alpha = 0;
	float powerFactor;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			powerFactor = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
