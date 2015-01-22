package mods.eln.sixnode.wirelesssignal.rx;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class WirelessSignalRxRender extends SixNodeElementRender{

	WirelessSignalRxDescriptor descriptor;
	
	public WirelessSignalRxRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (WirelessSignalRxDescriptor)descriptor;
		
	}

	
	
	
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		
		return Eln.instance.signalCableDescriptor.render;
	}
	
	
	
	@Override
	public void draw() {
		
		super.draw();
		
		drawSignalPin(new float[]{2,2,2,2});
		front.glRotateOnX();
		descriptor.draw(connection);
	}
	boolean connection;
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		
		return new WirelessSignalRxGui(this);
	}
	
	String channel;
	int selectedAggregator;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
			channel = stream.readUTF();
			connection = stream.readBoolean();
			selectedAggregator = stream.readByte();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}








}
