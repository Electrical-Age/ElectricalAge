package mods.eln.transparentnode.solarpannel;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class SolarPannelRender extends TransparentNodeElementRender {

	public SolarPannelDescriptor descriptor;
	private CableRenderType renderPreProcess;

	public SolarPannelRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (SolarPannelDescriptor) descriptor;
		
	}

	RcInterpolator interpol = new RcInterpolator(1f);
	boolean boot = true;

	@Override
	public void draw() {

		renderPreProcess = drawCable(Direction.YN, descriptor.cableRender, eConn, renderPreProcess);

		descriptor.draw((float) (interpol.get() * 180 / Math.PI - 90), front);

	}

	@Override
	public void refresh(float deltaT) {
		float alpha;
		if (hasTracker == false)
		{
			alpha = (float) descriptor.alphaTrunk(pannelAlphaSyncValue);
		}
		else
		{
			alpha = (float) descriptor.alphaTrunk(SolarPanelSlowProcess.getSolarAlpha(tileEntity.getWorldObj()));
		}
		interpol.setTarget(alpha);
		if (boot) {
			boot = false;
			interpol.setValueFromTarget();
		}

		interpol.step(deltaT);
	}

	@Override
	public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
		return descriptor.cableRender;
	}

	public boolean pannelAlphaSyncNew = false;
	public float pannelAlphaSyncValue = -1234;

	public boolean hasTracker;

	LRDUMask eConn = new LRDUMask();

	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);

		short read;

		try {

			Byte b;

			hasTracker = stream.readBoolean();

			float pannelAlphaIncoming = stream.readFloat();

			if (pannelAlphaIncoming != pannelAlphaSyncValue)
			{
				pannelAlphaSyncValue = pannelAlphaIncoming;
				pannelAlphaSyncNew = true;
			}

			eConn.deserialize(stream);

			renderPreProcess = null;

		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}

	public void clientSetPannelAlpha(float value)
	{
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(bos);

			preparePacketForServer(stream);

			stream.writeByte(SolarPanelElement.unserializePannelAlpha);
			stream.writeFloat(value);

			sendPacketToServer(bos);
		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		
		return new SolarPannelGuiDraw(player, inventory, this);
	}

	@Override
	public boolean cameraDrawOptimisation() {
		
		return false;
	}

}
