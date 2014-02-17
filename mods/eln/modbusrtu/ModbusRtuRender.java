package mods.eln.modbusrtu;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
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
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;


public class ModbusRtuRender extends TransparentNodeElementRender{

	ModbusRtuDescriptor descriptor;
	public ModbusRtuRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (ModbusRtuDescriptor) descriptor;
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub


	}

	int station = -1;
	String name;
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
		try {
			station = stream.readInt();
			name = stream.readUTF();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ModbusRtuGui(player, this);
	}



}
