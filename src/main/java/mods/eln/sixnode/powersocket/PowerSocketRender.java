package mods.eln.sixnode.powersocket;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;

public class PowerSocketRender extends SixNodeElementRender {

	PowerSocketDescriptor descriptor;

    Coordonate coord;
    String channel;

    CableRenderDescriptor cableRender;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public PowerSocketRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (PowerSocketDescriptor) descriptor;
		coord = new Coordonate(tileEntity);
	}

	@Override
	public void draw() {
		super.draw();
		descriptor.draw();
	}

	@Override
	public void refresh(float deltaT) {

	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return cableRender;
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new PowerSocketGui(this, player, inventory);
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			channel = stream.readUTF();

			ItemStack cableStack = Utils.unserialiseItemStack(stream);
			if (cableStack != null) {
				ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
				cableRender = desc.render;
			} else {
				cableRender = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
