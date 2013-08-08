package mods.eln.eggincubator;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;


public class EggIncubatorRender extends TransparentNodeElementRender{

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);
	EggIncubatorDescriptor descriptor;
	public EggIncubatorRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (EggIncubatorDescriptor) descriptor;
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		
		GL11.glPushMatrix();
		front.glRotateXnRef();
		descriptor.draw(eggStackSize);
		GL11.glPopMatrix();
		cableRenderType = drawCable(front.down(),descriptor.cable.render, eConn, cableRenderType);
	
	}

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new EggIncubatorGuiDraw(player, inventory, this);
	}
	
	byte eggStackSize;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			eggStackSize = stream.readByte();
			
					
			eConn.deserialize(stream);
			

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	LRDUMask priConn = new LRDUMask(),secConn = new LRDUMask(),eConn = new LRDUMask();
	CableRenderType cableRenderType;
	
	
	@Override
	public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
		return descriptor.cable.render;
	}
	@Override
	public void notifyNeighborSpawn() {
		// TODO Auto-generated method stub
		super.notifyNeighborSpawn();
		cableRenderType = null;
	}
}
