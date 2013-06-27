
package mods.eln.electricalvumeter;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ElectricalVuMeterRender extends SixNodeElementRender{

	ElectricalVuMeterDescriptor descriptor;
	public ElectricalVuMeterRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalVuMeterDescriptor) descriptor;
		interpolator = new PhysicalInterpolator(1.0f,0.01f,1.0f,0.2f);
	}

	PhysicalInterpolator interpolator;
	float factor;	LRDU front;

	@Override
	public void draw() {
		super.draw();
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];
		
		GL11.glPushMatrix();
		interpolator.stepGraphic();
		descriptor.draw(interpolator.get());
		GL11.glPopMatrix();
	}

	
	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
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
			if(boot)
			{
				interpolator.setPos(stream.readFloat());
			}
			else
			{
				interpolator.setTarget(stream.readFloat());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		if(boot)
		{
			
			boot = false;
		}
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
	

	
}
