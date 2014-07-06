package mods.eln.sixnode.electricalbreaker;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalBreakerRender extends SixNodeElementRender {

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	ElectricalBreakerDescriptor descriptor;
	long time;
	public ElectricalBreakerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalBreakerDescriptor) descriptor;
		time = System.currentTimeMillis();
		interpol = new RcInterpolator(this.descriptor.speed);
	}
	RcInterpolator interpol;
	
	@Override
	public void draw() {
		super.draw();
		
		front.glRotateOnX();	
		descriptor.draw(interpol.get(), UtilsClient.distanceFromClientPlayer(tileEntity));			
	}
	
	@Override
	public void refresh(float deltaT) {
		interpol.setTarget(switchState ? 1f :0f);	
		interpol.step(deltaT);
		
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return cableRender;
	}

	float uMin, uMax;

	boolean boot = true;
	float switchAlpha = 0;
	public boolean switchState;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		Utils.println("Front : " + front);
		try {
			switchState = stream.readBoolean();
			uMax = stream.readFloat();
			uMin = stream.readFloat();
			
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream), ElectricalCableDescriptor.class);
			
			if(desc == null)
				cableRender = null;
			else
				cableRender = desc.render;
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		if(boot) {
			interpol.setValue(switchState ? 1f : 0f);
		}
		boot = false;
	}
	
	CableRenderDescriptor cableRender;
	public void clientSetVoltageMin(float value) {
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.setVoltageMinId);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}
	
	public void clientSetVoltageMax(float value) {
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.setVoltageMaxId);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}
	
	public void clientToogleSwitch() {
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalBreakerElement.toogleSwitchId);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			e.printStackTrace();
		}        
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalBreakerGui(player, inventory, this);
	}
}
