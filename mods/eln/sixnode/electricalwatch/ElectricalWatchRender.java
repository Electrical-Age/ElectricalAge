package mods.eln.sixnode.electricalwatch;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalWatchRender extends SixNodeElementRender {

	ElectricalWatchDescriptor descriptor;
	public ElectricalWatchRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalWatchDescriptor) descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		long time;
		if(upToDate)
			time = tileEntity.getWorldObj().getWorldTime();
		else
			time = oldDate;
		
		
		descriptor.draw(time/12000f + 0.5f,(time%1000)/1000f);
	}

	boolean upToDate = false;
	long oldDate = 1379;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		
		super.publishUnserialize(stream);
		try {
			upToDate = stream.readBoolean();
			oldDate = stream.readLong();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalWatchGui(player, inventory, this);
	}
	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	
}
