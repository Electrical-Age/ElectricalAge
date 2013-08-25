package mods.eln.teleporter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import mods.eln.misc.Direction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;

public class TeleporterRender extends TransparentNodeElementRender{

	TeleporterDescriptor descriptor;
	public TeleporterRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (TeleporterDescriptor) descriptor;
	}
	@Override
	public void draw() {
		descriptor.draw();
	}

	String name,targetName;
	float chargePower,chargePowerLast;
	boolean chargePowerNew;
	byte state;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);

		try {
			name = stream.readUTF();
			targetName = stream.readUTF();
			chargePower = stream.readFloat();
			state = stream.readByte();
			
			
			if(chargePower != chargePowerLast){
				chargePowerNew = true;
			}
			chargePowerLast = chargePower;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new TeleporterGui(player, this);
	}
	
	
	@Override
	public void serverPacketUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.serverPacketUnserialize(stream);
		try {
			stream.readByte();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
