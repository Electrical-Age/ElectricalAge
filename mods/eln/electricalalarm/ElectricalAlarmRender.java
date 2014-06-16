package mods.eln.electricalalarm;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.electricalrelay.ElectricalRelayGui;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

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
		interpol.setTarget(warm ? descriptor.rotSpeed : 0f);
		interpol.stepGraphic();
		
		rotAlpha += interpol.get() * FrameTime.get();

		//front.glRotateOnX();		
		descriptor.draw(warm, rotAlpha);
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
