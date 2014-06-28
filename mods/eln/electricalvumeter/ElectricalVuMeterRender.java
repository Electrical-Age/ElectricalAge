package mods.eln.electricalvumeter;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;

public class ElectricalVuMeterRender extends SixNodeElementRender {

	ElectricalVuMeterDescriptor descriptor;
	
	public ElectricalVuMeterRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalVuMeterDescriptor) descriptor;
		interpolator = new PhysicalInterpolator(0.4f, 2.0f, 1.5f, 0.2f);
	}

	PhysicalInterpolator interpolator;
	float factor;	LRDU front;

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front,descriptor.pinDistance);

		descriptor.draw(descriptor.onOffOnly ? interpolator.getTarget() : interpolator.get(), UtilsClient.distanceFromClientPlayer(tileEntity), tileEntity);
	}

	
	@Override
	public void refresh(float deltaT) {

		interpolator.step(deltaT);

	}
	
	@Override
	public boolean cameraDrawOptimisation() {
		return false;
	}
	
	boolean boot = true;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b >> 4)&3);
			if(boot) {
				interpolator.setPos(stream.readFloat());
			}
			else {
				interpolator.setTarget(stream.readFloat());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
		if(boot) {
			boot = false;
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
}
