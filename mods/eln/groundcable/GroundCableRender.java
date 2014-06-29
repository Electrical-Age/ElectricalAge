package mods.eln.groundcable;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class GroundCableRender extends SixNodeElementRender{

	GroundCableDescriptor descriptor;

	public GroundCableRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (GroundCableDescriptor) descriptor;
		// TODO Auto-generated constructor stub
	}

	//double voltage = 0,current = 0;
	int color = 0;
	
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		super.draw();
		
		descriptor.draw();			
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			
			color = (b>>4) & 0xF;
			
			ItemStack cableStack = Utils.unserialiseItemStack(stream);
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor)ElectricalCableDescriptor.getDescriptor(cableStack, ElectricalCableDescriptor.class);
			if(desc == null)
				cableRender = Eln.instance.lowVoltageCableDescriptor.render;
			else
				cableRender = desc.render;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public CableRenderDescriptor getCableRender(mods.eln.misc.LRDU lrdu)
	{
		return cableRender;
	}
	
	CableRenderDescriptor cableRender;
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new GroundCableGui(player, inventory, this);
	}

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
}
