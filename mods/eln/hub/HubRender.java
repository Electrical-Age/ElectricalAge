package mods.eln.hub;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class HubRender extends SixNodeElementRender{

	HubDescriptor descriptor;

	public HubRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (HubDescriptor) descriptor;
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
			for(int idx = 0;idx < 4;idx++){
				ItemStack cableStack = Utils.unserialiseItemStack(stream);
				ElectricalCableDescriptor desc = (ElectricalCableDescriptor)ElectricalCableDescriptor.getDescriptor(cableStack, ElectricalCableDescriptor.class);
				if(desc == null)
					cableRender[idx] = null;
				else
					cableRender[idx] = desc.render;
			}
			for(int idx = 0;idx < 6;idx++){
				connectionGrid[idx] = stream.readBoolean();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public CableRenderDescriptor getCableRender(mods.eln.misc.LRDU lrdu)
	{
		return cableRender[lrdu.toInt()];
	}
	
	CableRenderDescriptor cableRender[] = new CableRenderDescriptor[4];
	boolean connectionGrid[] = new boolean[6];
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new HubGui(player, inventory, this);
	}

	SixNodeElementInventory inventory = new SixNodeElementInventory(4, 64, this);
}
