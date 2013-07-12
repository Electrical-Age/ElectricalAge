
package mods.eln.electricalredstoneoutput;

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


public class ElectricalRedstoneOutputRender extends SixNodeElementRender{

	ElectricalRedstoneOutputDescriptor descriptor;
	public ElectricalRedstoneOutputRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalRedstoneOutputDescriptor) descriptor;
	}

	float factor;
	LRDU front;

	float factorFiltred = 0;
	@Override
	public void draw() {
		super.draw();
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];
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
		//front.glRotateOnX();
		float ff = 0.05f;
		factorFiltred = factor * ff + factorFiltred * (1f-ff);
		descriptor.draw(factorFiltred);
		GL11.glPopMatrix();
	}

	boolean boot = true;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			factor = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		if(boot)
		{
			factorFiltred = factor;
			boot = false;
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
}
