package mods.eln.electricalsource;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalSourceRender extends SixNodeElementRender{



	public ElectricalSourceRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		// TODO Auto-generated constructor stub
	}




	double voltage = 0,current = 0;
	int color = 0;
	
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		if(i != null && i.getItem()  == Eln.voltMeterHelmet)
		{		
			double factor = voltage * MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
			GL11.glDisable(GL11.GL_LIGHTING);
		}
		else if(i != null && i.getItem()  == Eln.currentMeterHelmet)
		{		
			double factor = current * MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
			GL11.glDisable(GL11.GL_LIGHTING);
		}
		else
		{
			GL11.glColor4d(1.0, 1.0, color/16.0, 1.0);
		}
		
		
		GL11.glPointSize(10);
		GL11.glLineWidth(10);
		GL11.glBegin(GL11.GL_LINES);
		//GL11.glBegin(GL11.GL_POINTS);
		
		GL11.glTexCoord2f(0.0f,0.0f);
		GL11.glNormal3f(1f, 0.0f, 0.0f);
			
		/*
		GL11.glVertex3f(0.05f,0.0f,0f);
		if(connectedSide.down()) 		GL11.glVertex3f(0.05f,-0.4f,0f);
		if(connectedSide.up()) 			GL11.glVertex3f(0.05f,0.4f,0f);
		if(connectedSide.left()) 		GL11.glVertex3f(0.05f,0f,-0.4f);
		if(connectedSide.right()) 		GL11.glVertex3f(0.05f,0f,0.4f);*/
		if(connectedSide.down())
		{
			GL11.glVertex3f(0.05f,0f,0f);
			GL11.glVertex3f(0.05f,-0.55f,0f);
		}
		if(connectedSide.up())
		{
			GL11.glVertex3f(0.05f,0f,0f);
			GL11.glVertex3f(0.05f,0.55f,0f);
		}
		if(connectedSide.left())
		{
			GL11.glVertex3f(0.05f,0f,0f);
			GL11.glVertex3f(0.05f,0f,-0.55f);
		}
		if(connectedSide.right())
		{
			GL11.glVertex3f(0.05f,0f,0f);
			GL11.glVertex3f(0.05f,0f,0.55f);
		}
		


		GL11.glEnd();
		
		GL11.glPointSize(25);
		GL11.glColor3f(1.0f, 1.0f, 1.0f);
		GL11.glBegin(GL11.GL_POINTS);
			GL11.glVertex3f(0.08f,0f,0f);
		GL11.glEnd();
		
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);				
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			
			color = (b>>4) & 0xF;
			voltage = stream.readFloat();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalSourceGui(this);
	}
	
	
	
	
}
