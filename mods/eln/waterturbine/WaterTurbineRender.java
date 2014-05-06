package mods.eln.waterturbine;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.client.FrameTime;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
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
	

	Coordonate waterCoord,waterCoordRight;
	RcInterpolator powerFactorFilter = new RcInterpolator(1);
	RcInterpolator dirFilter = new RcInterpolator(0.5f);
	WaterTurbineDescriptor descriptor;
	float alpha = 0;
	@Override
	public void draw() {
		//front.glRotateXnRef();
		float flowDir = waterCoord.getMeta() > waterCoordRight.getMeta() ? 1 : -1;
		if(Utils.isWater(waterCoord) == false) flowDir = 0;
		
		dirFilter.setTarget(flowDir);
		dirFilter.stepGraphic();
		powerFactorFilter.setTarget(dirFilter.get() * powerFactor);
		powerFactorFilter.stepGraphic();
		
		
		//System.out.println(powerFactorFilter.get());
		alpha += FrameTime.get() * 30 * (powerFactorFilter.get());
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
		
		waterCoord = this.descriptor.getWaterCoordonate(tileEntity.worldObj);
		waterCoord.setWorld(tileEntity.worldObj);
		waterCoord.applyTransformation(front, coordonate());
		waterCoordRight = new Coordonate(waterCoord);
		waterCoordRight.setWorld(tileEntity.worldObj);
		waterCoordRight.move(front.right());
	}
}
