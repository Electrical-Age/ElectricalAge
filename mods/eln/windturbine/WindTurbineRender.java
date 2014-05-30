package mods.eln.windturbine;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.client.FrameTime;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.sound.SoundClient;
import mods.eln.sound.SoundParam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class WindTurbineRender extends TransparentNodeElementRender {

	private boolean soundPlaying = false;
	public WindTurbineRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (WindTurbineDescriptor) descriptor;
	}
	RcInterpolator powerFactorFilter = new RcInterpolator(2);
	WindTurbineDescriptor descriptor;
	float alpha = (float) (Math.random()*360);
	@Override
	public void draw() {
		powerFactorFilter.setTarget(powerFactor);
		powerFactorFilter.stepGraphic();
		float alphaN_1 = alpha;
		alpha += FrameTime.get() * descriptor.speed * Math.sqrt(powerFactorFilter.get());
		if(alpha > 360) alpha -= 360;
		front.glRotateXnRef();
		descriptor.draw(alpha);
		
		if (alpha % 120 > 45 && alphaN_1 % 120 <= 45 && soundPlaying == false) {
			Coordonate coord = coordonate();
			SoundClient.play(new SoundParam(descriptor.soundName, tileEntity)
					.setVolume(descriptor.nominalVolume * (0.007f + 1f * (float)powerFactorFilter.get() * (float)powerFactorFilter.get()), 
								1f + (float)Math.sqrt(powerFactorFilter.get()) / 1.3f));

			soundPlaying = true;
		} else {
			soundPlaying = false;
		}
			
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
