package mods.eln.transparentnode.windturbine;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

import mods.eln.client.FrameTime;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.SoundClient;
import mods.eln.sound.SoundCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class WindTurbineRender extends TransparentNodeElementRender {

	private float haloBlink_OnTime = 0.5f;
	private float haloBlink_OffTime = 2.5f;

	private boolean soundPlaying = false;
	private float haloBlinkCounter = 0.f;
	private boolean haloState = false;

	private Random rand = new Random();

	public WindTurbineRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (WindTurbineDescriptor) descriptor;
		this.haloBlinkCounter = rand.nextFloat()*this.haloBlink_OffTime;
		this.haloBlink_OffTime += rand.nextFloat()*this.haloBlink_OffTime/15.f;
		this.haloBlink_OnTime += rand.nextFloat()*this.haloBlink_OnTime/15.f;
	}

	RcInterpolator powerFactorFilter = new RcInterpolator(2);
	WindTurbineDescriptor descriptor;
	float alpha = (float) (Math.random() * 360);

	@Override
	public void draw() {
		front.glRotateXnRef();
		descriptor.draw(alpha,haloState);
	}

	public void refresh(float deltaT) {
		powerFactorFilter.setTarget(powerFactor);
		powerFactorFilter.step(deltaT);
		float alphaN_1 = alpha;
		alpha += deltaT * descriptor.speed * Math.sqrt(powerFactorFilter.get());
		if (alpha > 360) alpha -= 360;
		if (alpha % 120 > 45 && alphaN_1 % 120 <= 45 && soundPlaying == false) {
			Coordonate coord = coordonate();
			this.play(new SoundCommand(descriptor.soundName)
				.mediumRange()
				.mulBlockAttenuation(2)
				.applyNominalVolume(descriptor.nominalVolume)
				.mulVolume((0.007f + 1f * (float)Math.sqrt( powerFactorFilter.get())),
							1f + (float) Math.sqrt(powerFactorFilter.get()) / 1.3f));

			soundPlaying = true;
		} else {
			soundPlaying = false;
		}
		haloBlinkCounter += deltaT;
		if(haloState == false) {
			if (haloBlinkCounter > haloBlink_OffTime) {
				haloBlinkCounter -= haloBlink_OffTime;
				haloState = !haloState;
			}
		}
		else{
			if (haloBlinkCounter > haloBlink_OnTime) {
				haloBlinkCounter -= haloBlink_OnTime;
				haloState = !haloState;
			}
		}

	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0, 64, this);
	private float wind;
	private float powerFactor;

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {

		return new WindTurbineGuiDraw(player, inventory, this);
	}

	@Override
	public boolean cameraDrawOptimisation() {

		return false;
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {

		super.networkUnserialize(stream);
		try {
			wind = stream.readFloat();
			powerFactor = stream.readFloat();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}
