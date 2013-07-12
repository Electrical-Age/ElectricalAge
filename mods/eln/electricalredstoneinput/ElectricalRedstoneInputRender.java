
package mods.eln.electricalredstoneinput;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ElectricalRedstoneInputRender extends SixNodeElementRender{

	ElectricalRedstoneInputDescriptor descriptor;
	public ElectricalRedstoneInputRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalRedstoneInputDescriptor) descriptor;
	}


	LRDU front;


	@Override
	public void draw() {
		super.draw();
		
/*
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);


		GL11.glPointSize(20);
		
	
		
		double[] vector = new double[2];
		vector[0] = 0;
		vector[1] = 0;		
		front.inverse().applyTo(vector, 0.4);
		
		GL11.glBegin(GL11.GL_POINTS);	
		GL11.glColor3f(1f-factor, factor, 0);
			GL11.glVertex3d(0.07,vector[1],vector[0]);
		GL11.glEnd();
		

		
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);		
		
		*/
		GL11.glPushMatrix();
		front.glRotateOnX();

		
		//descriptor.draw(factorFiltred);
		GL11.glPopMatrix();
	}

	boolean warm = false;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			warm = (b & 1) != 0 ? true : false;
			System.out.println("WARM : " + warm);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
}
