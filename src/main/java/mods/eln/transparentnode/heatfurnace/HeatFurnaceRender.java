package mods.eln.transparentnode.heatfurnace;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;

public class HeatFurnaceRender extends TransparentNodeElementRender {

	double temperature;
	float gainSyncValue = -1234, temperatureTargetSyncValue = -1234;
	boolean gainSyncNew = false, temperatureTargetSyncNew = false;
	short power;

	public boolean controleExternal, takeFuel;

	HeatFurnaceDescriptor descriptor;

	public HeatFurnaceRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (HeatFurnaceDescriptor) descriptor;
		interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
		coord = new Coordonate(tileEntity);
	}

	Coordonate coord;
	PhysicalInterpolator interpolator;

	@Override
	public void draw() {

		front.glRotateXnRef();
		descriptor.draw(interpolator.get());

		if (entityItemIn != null)
			drawEntityItem(entityItemIn, -0.1, -0.30, 0, counter, 0.8f);

	}

	@Override
	public void refresh(float deltaT) {

		if (Utils.isPlayerAround(tileEntity.getWorldObj(), coord.getAxisAlignedBB(1)) == false)
			interpolator.setTarget(0f);
		else
			interpolator.setTarget(1f);
		interpolator.step(deltaT);

		counter += deltaT * 60;
		if (counter >= 360f)
			counter -= 360;

	}

	float counter = 0;

	TransparentNodeElementInventory inventory = new HeatFurnaceInventory(4, 64, this);

	boolean boot = true;

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		
		return new HeatFurnaceGuiDraw(player, inventory, this);
	}

	@Override
	public void networkUnserialize(DataInputStream stream) {
		
		super.networkUnserialize(stream);
		try {
			controleExternal = stream.readBoolean();
			takeFuel = stream.readBoolean();

			temperature = stream.readShort() / NodeBase.networkSerializeTFactor;
			float readF;
			readF = stream.readFloat();
			if (gainSyncValue != readF || controleExternal)
			{
				gainSyncValue = readF;
				gainSyncNew = true;
			}
			readF = stream.readFloat();
			if (temperatureTargetSyncValue != readF || controleExternal)
			{
				temperatureTargetSyncValue = readF;
				temperatureTargetSyncNew = true;
			}

			power = stream.readShort();

			entityItemIn = unserializeItemStackToEntityItem(stream, entityItemIn);

			if (boot)
			{
				coord.move(front);
				//coord.move(front);
				boot = false;
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public void clientToogleControl()
	{
		clientSendId(HeatFurnaceElement.unserializeToogleControlExternalId);
	}

	public void clientToogleTakeFuel()
	{
		clientSendId(HeatFurnaceElement.unserializeToogleTakeFuelId);
	}

	EntityItem entityItemIn;

	public void clientSetGain(float value)
	{
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(bos);

			preparePacketForServer(stream);

			stream.writeByte(HeatFurnaceElement.unserializeGain);
			stream.writeFloat(value);

			sendPacketToServer(bos);
		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}

	public void clientSetTemperatureTarget(float value)
	{
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(bos);

			preparePacketForServer(stream);

			stream.writeByte(HeatFurnaceElement.unserializeTemperatureTarget);
			stream.writeFloat(value);

			sendPacketToServer(bos);
		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}

	@Override
	public boolean cameraDrawOptimisation() {
		
		return false;
	}
}
