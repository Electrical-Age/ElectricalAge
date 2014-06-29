package mods.eln.electricalalarm;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalAlarmRender extends SixNodeElementRender {

	ElectricalAlarmDescriptor descriptor;
	public ElectricalAlarmRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalAlarmDescriptor) descriptor;
	}

	LRDU front;

	RcInterpolator interpol = new RcInterpolator(0.4f);
	
	@Override
	public void draw() {
		super.draw();
		
		drawSignalPin(front,descriptor.pinDistance);

		//front.glRotateOnX();		
		descriptor.draw(warm, rotAlpha);
	}
	@Override
	public void refresh(float deltaT) {
		interpol.setTarget(warm ? descriptor.rotSpeed : 0f);
		interpol.step(deltaT);
		
		rotAlpha += interpol.get() * deltaT;
		
	}
	
	float rotAlpha = 0;
	boolean warm = false;
	boolean mute = false;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			warm = (b & 1) != 0 ? true : false;
			mute = stream.readBoolean();
			Utils.println("WARM : " + warm);
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
		return new ElectricalAlarmGui(player,this);
	}
}
