package mods.eln.thermalsensor;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import mods.eln.sim.PhysicalConstant;
import mods.eln.thermalcable.ThermalCableDescriptor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ThermalSensorRender extends SixNodeElementRender{

	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);
	ThermalSensorDescriptor descriptor;
	long time;
	public ThermalSensorRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ThermalSensorDescriptor) descriptor;
		time = System.currentTimeMillis();
	}


	LRDU front;

	@Override
	public void draw() {
		super.draw();	
		front.glRotateOnX();
		descriptor.draw();	

	}
	
	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.cableRender;
	}
	*/
	

	int typeOfSensor = 0; 
	float lowValue = 0,highValue = 50;

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			typeOfSensor = b & 0x3;
			lowValue = (float) (stream.readFloat() + PhysicalConstant.Tamb);
			highValue = (float) (stream.readFloat() + PhysicalConstant.Tamb);
			ItemStack stack = Utils.unserialiseItemStack(stream);
			GenericItemBlockUsingDamageDescriptor desc = ThermalCableDescriptor.getDescriptor(stack);
			if(desc instanceof ThermalCableDescriptor) cable = (ThermalCableDescriptor) desc;
			else cable = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
	
	ThermalCableDescriptor cable;
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ThermalSensorGui(player,inventory,this);
	}
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if(descriptor.temperatureOnly==false)
		{
			if(front.left() == lrdu && cable != null) return cable.render;
			if(front.right() == lrdu && cable != null) return cable.render;
			if(front == lrdu) return  Eln.instance.signalCableDescriptor.render;
		}
		else
		{
			if(front.inverse() == lrdu && cable != null) return cable.render;
			if(front == lrdu) return  Eln.instance.signalCableDescriptor.render;
		}
		return null;
	}
}
