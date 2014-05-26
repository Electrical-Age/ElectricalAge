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
import mods.eln.sound.SoundClient;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class WaterTurbineRender extends TransparentNodeElementRender {

	public WaterTurbineRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (WaterTurbineDescriptor) descriptor;

	}

	Coordonate waterCoord, waterCoordRight;
	RcInterpolator powerFactorFilter = new RcInterpolator(1);
	RcInterpolator dirFilter = new RcInterpolator(0.5f);
	WaterTurbineDescriptor descriptor;
	float alpha = (float) (Math.random() * 360);
	boolean soundPlaying = false;

	@Override
	public void draw() {
		// front.glRotateXnRef();
		float flowDir = waterCoord.getMeta() > waterCoordRight.getMeta() ? 1 : -1;
		if (Utils.isWater(waterCoord) == false)
			flowDir = 0;

		dirFilter.setTarget(flowDir);
		dirFilter.stepGraphic();
		powerFactorFilter.setTarget(dirFilter.get() * powerFactor);
		powerFactorFilter.stepGraphic();

		// Utils.println(powerFactorFilter.get());
		float alphaN_1 = alpha;
		alpha += FrameTime.get() * descriptor.speed * (powerFactorFilter.get());
		if (alpha > 360)
			alpha -= 360;
		if (alpha < 0)
			alpha += 360;
		front.glRotateXnRef();
		descriptor.draw(alpha);

	//	if (Math.signum((alpha % 45) - 40f) != Math.signum((alphaN_1 % 45) - 40f) && soundPlaying == false) {
		if ((int)(alpha/45) != (int)(alphaN_1/45) && soundPlaying == false) {
			Coordonate coord = coordonate();
			tileEntity.worldObj.playSound(coord.x, coord.y, coord.z, descriptor.soundName,
					descriptor.nominalVolume * (0.007f + 0.2f * (float) powerFactorFilter.get() * (float) powerFactorFilter.get()),
					1.1f, false);
			//SoundClient.playFromBlock(tileEntity.worldObj,coord.x, coord.y, coord.z, descriptor.soundName,1,1,5,15);
			soundPlaying = true;
		} else
			soundPlaying = false;
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0, 64, this);
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
