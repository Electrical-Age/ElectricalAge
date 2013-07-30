package mods.eln.transformer;

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


public class TransformerRender extends TransparentNodeElementRender{

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);
	TransformerDescriptor descriptor;
	public TransformerRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (TransformerDescriptor) descriptor;
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		
		GL11.glPushMatrix();
		front.glRotateXnRef();
		descriptor.draw(feroPart, primaryStackSize, secondaryStackSize);
		GL11.glPopMatrix();
		cableRenderType = drawCable(front.down(),priRender, priConn, cableRenderType);
		cableRenderType = drawCable(front.down(),secRender, secConn, cableRenderType);
		

		
	}

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new TransformerGuiDraw(player, inventory, this);
	}
	
	byte primaryStackSize,secondaryStackSize;
	CableRenderDescriptor priRender,secRender;
	
	Obj3DPart feroPart;
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			primaryStackSize = stream.readByte();
			secondaryStackSize = stream.readByte();
			ItemStack feroStack = Utils.unserialiseItemStack(stream);
			FerromagneticCoreDescriptor feroDesc = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(feroStack,FerromagneticCoreDescriptor.class);
			if(feroDesc == null)
				feroPart = null;
			else
				feroPart = feroDesc.feroPart;
			
			ItemStack priStack = Utils.unserialiseItemStack(stream);
			ElectricalCableDescriptor priDesc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(priStack,ElectricalCableDescriptor.class);
			if(priDesc == null)
				priRender = null;
			else
				priRender = priDesc.render;
			
			ItemStack secStack = Utils.unserialiseItemStack(stream);
			ElectricalCableDescriptor secDesc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(secStack,ElectricalCableDescriptor.class);
			if(secDesc == null)
				secRender = null;
			else
				secRender = secDesc.render;
					
			eConn.deserialize(stream);
			
			priConn.mask = 0;
			secConn.mask = 0;
			for(LRDU lrdu : LRDU.values()){
				if(eConn.get(lrdu) == false || front.down().applyLRDU(lrdu) == front.left() || front.down().applyLRDU(lrdu) == front.right()) continue;
				CableRenderDescriptor render = getCableRender(front.down().applyLRDU(lrdu), LRDU.Down);
				
				if(render == priRender) priConn.set(lrdu,true);
				if(render == secRender) secConn.set(lrdu,true);
			
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	LRDUMask priConn = new LRDUMask(),secConn = new LRDUMask(),eConn = new LRDUMask();
	CableRenderType cableRenderType;
	
	
	@Override
	public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
		if(lrdu == lrdu.Down)
		{
			if(side == front.left() ) return priRender;	
			if(side == front.right()) return secRender;	
			if(side == front&& ! grounded) return priRender;
			if(side == front.back() && ! grounded) return secRender;
		}
		return null;
	}
	@Override
	public void notifyNeighborSpawn() {
		// TODO Auto-generated method stub
		super.notifyNeighborSpawn();
		cableRenderType = null;
	}
}
