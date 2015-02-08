package mods.eln.sixnode.hub;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class HubRender extends SixNodeElementRender {

	HubDescriptor descriptor;

    //double voltage = 0, current = 0;
    int color = 0;

    CableRenderDescriptor cableRender[] = new CableRenderDescriptor[4];
    boolean connectionGrid[] = new boolean[6];

    SixNodeElementInventory inventory = new SixNodeElementInventory(4, 64, this);

    public HubRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (HubDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		descriptor.draw(connectionGrid);			
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			for (int idx = 0; idx < 4; idx++) {
				ItemStack cableStack = Utils.unserialiseItemStack(stream);
				ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack, ElectricalCableDescriptor.class);
				if (desc == null)
					cableRender[idx] = null;
				else
					cableRender[idx] = desc.render;
			}
			for (int idx = 0; idx < 6; idx++) {
				connectionGrid[idx] = stream.readBoolean();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CableRenderDescriptor getCableRender(mods.eln.misc.LRDU lrdu) {
		return cableRender[lrdu.toInt()];
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new HubGui(player, inventory, this);
	}
}
