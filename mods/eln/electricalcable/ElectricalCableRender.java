package mods.eln.electricalcable;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ElectricalCableRender extends SixNodeElementRender{

	ElectricalCableDescriptor descriptor;
	
	public ElectricalCableRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalCableDescriptor) descriptor;
		// TODO Auto-generated constructor stub
	}


	
	double voltage = 0,current = 0,temperature = 0;
	int color = 0;
	
	public boolean drawCableAuto() {
		
		return false;
	}
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		Minecraft.getMinecraft().mcProfiler.startSection("ECable");
		
		//ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];
		
	//	GL11.glDisable(GL11.GL_TEXTURE_2D);

		/*if(i != null && i.getItem()  == Eln.voltMeterHelmet)
		{		
			double factor = voltage * MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_TEXTURE_2D);	
		}
		else if(i != null && i.getItem()  == Eln.currentMeterHelmet)
		{		
			double factor = current * MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_TEXTURE_2D);	
		}
		else if(i != null && i.getItem()  == Eln.thermoMeterHelmet)
		{		
			double factor = temperature  *MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_TEXTURE_2D);	
		}
		else*/
		{
			Utils.setGlColorFromDye(color);
		}
		

		
		//GL11.glDisable(GL11.GL_LIGHTING);
		Utils.bindTexture(descriptor.render.cableTexture);
		glListCall();
		//GL11.glEnable(GL11.GL_LIGHTING);
		
		//GL11.glEnable(GL11.GL_TEXTURE_2D);		
		
		
		
		GL11.glColor3f(1f,1f,1f);
		Minecraft.getMinecraft().mcProfiler.endSection();
	}

	@Override
	public void glListDraw() {
		CableRender.drawCable(descriptor.render, connectedSide,CableRender.connectionType(this, side));
		CableRender.drawNode(descriptor.render, connectedSide,CableRender.connectionType(this, side));
	}
	@Override
	public boolean glListEnable() {
		return true;	
	}
	

	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			color = (b>>4) & 0xF;
			voltage = stream.readShort() /Node.networkSerializeUFactor;
			current = stream.readShort() /Node.networkSerializeIFactor;
			temperature = stream.readShort() /Node.networkSerializeTFactor;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return descriptor.render;
	}

	@Override
	public int getCableDry(LRDU lrdu) {
		// TODO Auto-generated method stub
		return color;
	}
}
