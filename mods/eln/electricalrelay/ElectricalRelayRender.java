package mods.eln.electricalrelay;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalRelayRender extends SixNodeElementRender{

	SixNodeElementInventory inventory = new SixNodeElementInventory(0,64,this);
	ElectricalRelayDescriptor descriptor;
	long time;
	public ElectricalRelayRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalRelayDescriptor) descriptor;
		time = System.currentTimeMillis();
		interpolator = new RcInterpolator(this.descriptor.speed);
	}


	RcInterpolator interpolator;
	
	@Override
	public void draw() {
		super.draw();
		
		front.glRotateOnX();
		
		interpolator.stepGraphic();
		descriptor.draw(interpolator.get());
		
	}
	

	


	boolean boot = true;
	float switchAlpha = 0;
	public boolean switchState,defaultOutput;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {

			switchState = stream.readBoolean();
			defaultOutput = stream.readBoolean();

			interpolator.setTarget(switchState ? 1f : 0f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		if(boot)
		{
			interpolator.setValueFromTarget();
		}
		boot = false;
	}
	
	

	public void clientToogleDefaultOutput()
	{
        clientSend(ElectricalRelayElement.toogleOutputDefaultId);
	}
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalRelayGui(player,this);
	}
	
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if(lrdu == front) return Eln.instance.signalCableDescriptor.render;
		if(lrdu == front.left() || lrdu == front.right()) return descriptor.cable.render;
		return null;
	}
	
	
}
