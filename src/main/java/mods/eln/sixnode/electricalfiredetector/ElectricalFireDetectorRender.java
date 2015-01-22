package mods.eln.sixnode.electricalfiredetector;

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

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalFireDetectorRender extends SixNodeElementRender {

	ElectricalFireDetectorDescriptor descriptor;
	public ElectricalFireDetectorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalFireDetectorDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(),descriptor.pinDistance);

		descriptor.draw(firePresent);
	}

	boolean firePresent = false;

	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
            firePresent = stream.readBoolean();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
}
