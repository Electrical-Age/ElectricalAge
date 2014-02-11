package mods.eln.windturbine;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class WindTurbineRender extends TransparentNodeElementRender {


	public WindTurbineRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (WindTurbineDescriptor) descriptor;
	}
	RcInterpolator powerFactorFilter = new RcInterpolator(2);
	WindTurbineDescriptor descriptor;
	float alpha = 0;
	@Override
	public void draw() {
		powerFactorFilter.setTarget(powerFactor);
		powerFactorFilter.stepGraphic();
		
		alpha += FrameTime.get() * descriptor.speed * Math.sqrt(powerFactorFilter.get());
		if(alpha > 360) alpha -= 360;
		front.glRotateXnRef();
		descriptor.draw(alpha);
	}
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0 , 64, this);
	private float wind;
	private float powerFactor;

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new WindTurbineGuiDraw(player, inventory, this);
	}

	
	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			wind = stream.readFloat();
			powerFactor = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
