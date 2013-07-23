package mods.eln.wirelesssignal;

import ibxm.Channel;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class WirelessSignalTxRender extends SixNodeElementRender{

	
	WirelessSignalTxDescriptor descriptor;
	public WirelessSignalTxRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (WirelessSignalTxDescriptor) descriptor;
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		super.draw();
		front.glRotateOnX();
		descriptor.draw();
	}
	
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new WirelessSignalTxGui(this);
	}
	
	String channel;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			channel = stream.readUTF();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}





	
}
