package mods.eln.intelligenttransformer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.client.ClientProxy;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;


public class IntelligentTransformerRender extends TransparentNodeElementRender{

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 1, this);
	
	public IntelligentTransformerRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		GL11.glPointSize(20);
		/*
		double[] vector = new double[3];
		vector[0] = 0;
		vector[1] = 0;		
		vector[2] = 0;		
		front.applyTo(vector, 0.4);
		
		GL11.glBegin(GL11.GL_POINTS);	
			GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
			GL11.glVertex3d(0,0,0);
			GL11.glVertex3d(vector[0],vector[1],vector[2]);
		GL11.glEnd();	*/
		GL11.glEnable(GL11.GL_LIGHTING);
	//	Minecraft.getMinecraft().mcProfiler.startSection("monkey");
	//	ClientProxy.obj.draw("MASTERCUBE");	
	//	ClientProxy.masterCubeObj.draw();
	//	Minecraft.getMinecraft().mcProfiler.endSection();
	}

	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new IntelligentTransformerGuiDraw(player, inventory, this);
	}

	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
	}
	
	

}
