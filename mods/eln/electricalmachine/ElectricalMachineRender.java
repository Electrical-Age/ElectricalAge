	package mods.eln.electricalmachine;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalMachineRender extends TransparentNodeElementRender{
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);

	

	ElectricalMachineDescriptor descriptor;
	public ElectricalMachineRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (ElectricalMachineDescriptor) descriptor;
		drawHandle = this.descriptor.newDrawHandle();
	}

	Object drawHandle;



	private CableRenderType connectionType;
	LRDUMask eConn = new LRDUMask(),maskTemp = new LRDUMask();
	@Override
	public void draw() {	
		
		processState += processStatePerSecond * FrameTime.getNotCaped();
		if(processState > 1f) processState = 1f;
		
		GL11.glPushMatrix();
		front.glRotateXnRef();
		descriptor.draw(this,drawHandle, inEntity, outEntity, powerFactor,processState);
		GL11.glPopMatrix();
		/*
		if(connectionType == null)
		{
			cableRefresh = false;
			connectionType = CableRender.connectionType(tileEntity,eConn, front.getInverse());
		}
				
		glCableTransforme(front.down());
		descriptor.getPowerCableRender().bindCableTexture();
		
		for(LRDU lrdu : LRDU.values())
		{
			Utils.setGlColorFromDye(connectionType.otherdry[lrdu.toInt()]);
			if(eConn.get(lrdu) == false) continue;
			if(lrdu != front.down().getLRDUGoingTo(front) && lrdu.inverse() != front.down().getLRDUGoingTo(front)) continue;
			maskTemp.set(1<<lrdu.toInt());
			CableRender.drawCable(descriptor.getPowerCableRender(), maskTemp,connectionType);
		}	*/
		
		if(descriptor.drawCable()) connectionType = drawCable(front.down(), descriptor.getPowerCableRender(), eConn, connectionType);
		
	}
	
	float counter = 0;


	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
	}
		
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalMachineGuiDraw(player, inventory, this);
	}

	
	EntityItem inEntity,outEntity;
	float powerFactor,processState,processStatePerSecond;
	
	float UFactor;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
		try {
			powerFactor = stream.readByte() / 64f;
			inEntity = unserializeItemStackToEntityItem(stream, inEntity);
			outEntity = unserializeItemStackToEntityItem(stream, outEntity);
			processState = stream.readFloat();
			processStatePerSecond = stream.readFloat();
			eConn.deserialize(stream);
			UFactor = stream.readFloat();
			connectionType = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	
	}
	

	
	@Override
	public void notifyNeighborSpawn() {
		// TODO Auto-generated method stub
		super.notifyNeighborSpawn();
		connectionType = null;
	}

	
	


}