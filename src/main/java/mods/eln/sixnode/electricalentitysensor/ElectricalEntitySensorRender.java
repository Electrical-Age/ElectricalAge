package mods.eln.sixnode.electricalentitysensor;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ElectricalEntitySensorRender extends SixNodeElementRender {

	ElectricalEntitySensorDescriptor descriptor;
	public ElectricalEntitySensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalEntitySensorDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(),descriptor.pinDistance);

		descriptor.draw(state,filter);
	}

	boolean state = false;
	EntitySensorFilterDescriptor filter = null;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
			state = stream.readBoolean();
			ItemStack filterStack = Utils.unserialiseItemStack(stream);
			filter = (EntitySensorFilterDescriptor) EntitySensorFilterDescriptor.getDescriptor(filterStack);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalEntitySensorGui(player, inventory, this);
	}
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	
}
