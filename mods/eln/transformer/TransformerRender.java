package mods.eln.transformer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.client.ClientProxy;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
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
		
		front.glRotateXnRef();
		descriptor.draw(feroPart, primaryStackSize, secondaryStackSize);
	}

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new TransformerGuiDraw(player, inventory, this);
	}
	
	byte primaryStackSize,secondaryStackSize;
	
	
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	


	
}
