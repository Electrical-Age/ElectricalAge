package mods.eln.waterturbine;

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

public class WaterTurbineRender extends TransparentNodeElementRender {


	public WaterTurbineRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (WaterTurbineDescriptor) descriptor;
	}
	RcInterpolator powerFactorFilter = new RcInterpolator(2);
	WaterTurbineDescriptor descriptor;
	float alpha = 0;
	@Override
	public void draw() {
		front.glRotateXnRef();
		
		powerFactorFilter.setTarget(powerFactor);
		powerFactorFilter.stepGraphic();
		
		alpha += FrameTime.get() * 10 * Math.sqrt(powerFactorFilter.get());
		if(alpha > 360) alpha -= 360;
		front.glRotateXnRef();
		descriptor.draw(alpha);
	}
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0 , 64, this);
	private float water;
	private float powerFactor;

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new WaterTurbineGuiDraw(player, inventory, this);
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
			powerFactor = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
