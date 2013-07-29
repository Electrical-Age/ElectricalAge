package mods.eln.electricalbreaker;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalBreakerRender extends SixNodeElementRender{

	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);
	ElectricalBreakerDescriptor descriptor;
	long time;
	public ElectricalBreakerRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalBreakerDescriptor) descriptor;
		time = System.currentTimeMillis();
		interpol = new RcInterpolator(this.descriptor.speed);
	}
	RcInterpolator interpol;
	
	@Override
	public void draw() {
		super.draw();
		interpol.setTarget(switchState ? 1f :0f);	
		interpol.stepGraphic();
		
		front.glRotateOnX();	
		descriptor.draw(interpol.get(),Utils.distanceFromClientPlayer(tileEntity));			

	}
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return cableRender;
	}

	
	float uMin,uMax;

	boolean boot = true;
	float switchAlpha = 0;
	public boolean switchState;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		System.out.println("Front : " + front);
		try {
			switchState = stream.readBoolean();
			uMax = stream.readFloat();
			uMin = stream.readFloat();
			ItemStack stack = Utils.unserialiseItemStack(stream);
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(stack, ElectricalCableDescriptor.class);
			if(desc == null)
				cableRender = null;
			else
				cableRender = desc.render;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		if(boot)
		{
			interpol.setValue(switchState ? 1f : 0f);
		}
		boot = false;
	}
	
	CableRenderDescriptor cableRender;
	public void clientSetVoltageMin(float value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.setVoltageMinId);
			stream.writeFloat(value);
			

			
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	
	public void clientSetVoltageMax(float value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.setVoltageMaxId);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	public void clientToogleSwitch()
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.toogleSwitchId);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalBreakerGui(player,inventory,this);
	}
	
	
	
}
