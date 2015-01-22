package mods.eln.sixnode.electricaltimeout;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalTimeoutRender extends SixNodeElementRender {

	ElectricalTimeoutDescriptor descriptor;
	long time;
	public ElectricalTimeoutRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalTimeoutDescriptor) descriptor;
		time = System.currentTimeMillis();
	}

	//PhysicalInterpolator interpolator = new PhysicalInterpolator(0.2f, 2.0f, 1.5f, 0.2f);

	@Override
	public void draw() {
		super.draw();
		front.glRotateOnX();
	
		descriptor.draw(timeoutCounter / timeoutValue);
	}
	
	@Override
	public void refresh(float deltaT) {
		if(inputState == false) {
			timeoutCounter -= deltaT;
			if(timeoutCounter < 0f) timeoutCounter = 0f;
		}
	}
	
	@Override
	public boolean cameraDrawOptimisation() {
		return false;
	}

	float timeoutValue = 0, timeoutCounter = 0;
	boolean inputState;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			timeoutValue = stream.readFloat();
			timeoutCounter = stream.readFloat();
			inputState = stream.readBoolean();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalTimeoutGui(player,this);
	}
}
