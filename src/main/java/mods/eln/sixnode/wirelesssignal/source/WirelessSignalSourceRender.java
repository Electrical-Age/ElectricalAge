package mods.eln.sixnode.wirelesssignal.source;

import ibxm.Channel;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class WirelessSignalSourceRender extends SixNodeElementRender{

	
	WirelessSignalSourceDescriptor descriptor;
	public WirelessSignalSourceRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (WirelessSignalSourceDescriptor) descriptor;
		
		interpolator = new RcInterpolator(this.descriptor.render.speed);
	}



	RcInterpolator interpolator;
	
	@Override
	public void draw() {
		
		super.draw();
		LRDU.Down.glRotateOnX();
		descriptor.draw(interpolator.get(), UtilsClient.distanceFromClientPlayer(this.tileEntity), tileEntity);
	}
	
	@Override
	public void refresh(float deltaT) {
		interpolator.setTarget((float) ((state ? 1 : 0)));
		interpolator.step(deltaT);
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		
		return Eln.instance.signalCableDescriptor.render;
	}
	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		
		return new WirelessSignalSourceGui(this);
	}
	
	String channel;
	boolean state = false;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
			channel = stream.readUTF();
			state = stream.readBoolean();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}





	
}
