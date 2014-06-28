package mods.eln.lampsupply;

import ibxm.Channel;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class LampSupplyRender extends SixNodeElementRender {

	LampSupplyDescriptor descriptor;

	public LampSupplyRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (LampSupplyDescriptor) descriptor;
		interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
		coord = new Coordonate(tileEntity);
	}

	Coordonate coord;
	PhysicalInterpolator interpolator;

	@Override
	public void draw() {
		super.draw();

		drawPowerPin(new float[] { 4, 4, 5, 5 });

		LRDU.Down.glRotateOnX();
		descriptor.draw(interpolator.get());
	}

	@Override
	public void refresh(float deltaT) {

		if (Utils.isPlayerAround(tileEntity.getWorldObj(), coord.getAxisAlignedBB(1)) == false)
			interpolator.setTarget(0f);
		else
			interpolator.setTarget(1f);

		interpolator.step(deltaT);

	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return cableRender;
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new LampSupplyGui(this, player, inventory);
	}

	String channel;

	CableRenderDescriptor cableRender;

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			channel = stream.readUTF();

			ItemStack cableStack = Utils.unserialiseItemStack(stream);
			if (cableStack != null) {
				ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
				cableRender = desc.render;
			}
			else {
				cableRender = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
}
