package mods.eln.diode;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class DiodeRender extends SixNodeElementRender{


	public DiodeRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		// TODO Auto-generated constructor stub
	}

	double voltageAnode = 0,voltageCatode = 0,current = 0,temperature = 0;
	LRDU front;

	@Override
	public void draw() {
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];

		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glLineWidth(10);
		GL11.glBegin(GL11.GL_LINES);
		
		GL11.glTexCoord2f(0.0f,0.0f);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		
		if(i != null && i.getItem()  == Eln.voltMeterHelmet)
		{		
			double factor = voltageCatode*MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
		}
		else if(i != null && i.getItem()  == Eln.currentMeterHelmet)
		{		
			double factor = current*MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
		}
		else if(i != null && i.getItem()  == Eln.thermoMeterHelmet)
		{		
			double factor = temperature*MeterItemArmor.getBlockRenderColorFactor(i);
			GL11.glColor4d(factor, 1.0-factor,0.0, 1.0);
		}
		else
		{
			GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
		}
		
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
		
		GL11.glPointSize(20);
		
		double[] vector = new double[2];
		vector[0] = 0;
		vector[1] = 0;		
		front.inverse().applyTo(vector, 0.4);
		
		GL11.glBegin(GL11.GL_POINTS);	
			GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
			GL11.glVertex3d(0.07,vector[1],vector[0]);
		GL11.glEnd();
		

		GL11.glEnable(GL11.GL_TEXTURE_2D);					
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			voltageAnode = stream.readShort() /NodeBase.networkSerializeUFactor;
			voltageCatode = stream.readShort() /NodeBase.networkSerializeUFactor;
			current = stream.readShort() /NodeBase.networkSerializeIFactor;
			temperature = stream.readShort() /NodeBase.networkSerializeTFactor;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}