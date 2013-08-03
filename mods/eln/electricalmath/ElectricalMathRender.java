package mods.eln.electricalmath;

import java.io.DataInputStream;
import java.io.IOException;

import javax.management.Descriptor;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import mods.eln.node.SixNodeRender;

public class ElectricalMathRender extends SixNodeElementRender{

	public ElectricalMathRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalMathDescriptor)descriptor;
	}
	ElectricalMathDescriptor descriptor;
	
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalMathGui(player, inventory, this);
	}
	
	
	
	String expression;


	public int redstoneRequired;
	

	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			expression = stream.readUTF();
			redstoneRequired = stream.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}
