package mods.eln.batterycharger;

import ibxm.Channel;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.lampsupply.LampSupplyDescriptor;
import mods.eln.lampsupply.LampSupplyGui;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class BatteryChargerRender extends SixNodeElementRender{


	
	BatteryChargerDescriptor descriptor;
	public BatteryChargerRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (BatteryChargerDescriptor) descriptor;

		coord = new Coordonate(tileEntity);
	}
	Coordonate coord;
	PhysicalInterpolator interpolator;
	@Override
	public void draw() {	
		super.draw();
		
		if(Utils.isPlayerAround(tileEntity.worldObj,coord.getAxisAlignedBB(1)) == false)
			interpolator.setTarget(0f);
		else
			interpolator.setTarget(1f);
		
		
		interpolator.stepGraphic();
		
		
		LRDU.Down.glRotateOnX();
		descriptor.draw();
	}
	
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return cableRender;
	}
	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new BatteryChargerGui(this,player,inventory);
	}
	
	String channel;
	
	CableRenderDescriptor cableRender;
	EntityItem[] b = new EntityItem[4];
	private boolean powerOn;
	private float voltage;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			powerOn = stream.readBoolean();
			voltage = stream.readFloat();
			
			for(int idx = 0;idx < 4;idx++){
				b[idx] = Utils.unserializeItemStackToEntityItem(stream, b[idx], tileEntity);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}





	SixNodeElementInventory inventory = new SixNodeElementInventory(4, 64, this);
}
