package mods.eln.electricalsource;

import java.io.DataInputStream;
import java.io.IOException;

import javax.management.Descriptor;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalSourceRender extends SixNodeElementRender{

	ElectricalSourceDescriptor descriptor;


	public ElectricalSourceRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalSourceDescriptor)descriptor;
		// TODO Auto-generated constructor stub
	}




	double voltage = 0,current = 0;
	int color = 0;
	
	@Override
	public void draw() {
		super.draw();
		
		
		
		descriptor.draw();
		
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			
			color = (b>>4) & 0xF;
			voltage = stream.readFloat();

			needRedrawCable();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalSourceGui(this);
	}
	
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if(voltage < Eln.instance.lowVoltageCableDescriptor.electricalMaximalVoltage) return Eln.instance.lowVoltageCableDescriptor.render;
		if(voltage < Eln.instance.meduimVoltageCableDescriptor.electricalMaximalVoltage) return Eln.instance.meduimVoltageCableDescriptor.render;
		if(voltage < Eln.instance.highVoltageCableDescriptor.electricalMaximalVoltage) return Eln.instance.highVoltageCableDescriptor.render;

		return super.getCableRender(lrdu);
	}
}
