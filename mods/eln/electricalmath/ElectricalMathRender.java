package mods.eln.electricalmath;

import java.io.DataInputStream;
import java.io.IOException;

import javax.management.Descriptor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
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
		interpolator = new PhysicalInterpolator(0.4f,8.0f,0.9f,0.2f);
		coord = new Coordonate(tileEntity);
		ledOn[0] = true;
		ledOn[4] = true;
	}
	ElectricalMathDescriptor descriptor;
	Coordonate coord;
	PhysicalInterpolator interpolator;
	
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalMathGui(player, inventory, this);
	}
	
	
	
	String expression;


	public int redstoneRequired;
	public boolean equationIsValid;

	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			expression = stream.readUTF();
			redstoneRequired = stream.readInt();
			equationIsValid = stream.readBoolean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	float ledTime = 0f;
	
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		super.draw();
		

		
		ledTime += FrameTime.get();

		if(ledTime > 0.4){
			for(int idx = 1;idx <= 3;idx++){
				ledOn[idx] =  Math.random() < 0.3; 
			}
			for(int idx = 5;idx <= 7;idx++){
				ledOn[idx] =  Math.random() < 0.3; 
			}
			
			ledTime = 0;
		}
		
		if(Utils.isPlayerAround(tileEntity.worldObj,coord.getAxisAlignedBB(0)) == false)
			interpolator.setTarget(0f);
		else
			interpolator.setTarget(1f);
		
		
		interpolator.stepGraphic();
		
		
		LRDU.Down.glRotateOnX();
		descriptor.draw(interpolator.get(),ledOn);
	}
	
	boolean[] ledOn = new boolean[8];
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
}
