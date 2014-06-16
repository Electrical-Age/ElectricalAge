package mods.eln.solarpannel;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.electricalfurnace.ElectricalFurnaceElement;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class SolarPannelRender extends TransparentNodeElementRender {

	public SolarPannelDescriptor descriptor;
	private CableRenderType renderPreProcess;

	public SolarPannelRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (SolarPannelDescriptor) descriptor;
		// TODO Auto-generated constructor stub
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
			alpha = (float) descriptor.alphaTrunk(SolarPannelSlowProcess.getSolarAlpha(tileEntity.getWorldObj()));
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
		// TODO Auto-generated method stub
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void clientSetPannelAlpha(float value)
	{
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(bos);

			preparePacketForServer(stream);

			stream.writeByte(SolarPannelElement.unserializePannelAlpha);
			stream.writeFloat(value);

			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new SolarPannelGuiDraw(player, inventory, this);
	}

	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
	}

}
