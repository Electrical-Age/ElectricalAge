
package mods.eln.electricalwindsensor;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ElectricalWindSensorRender extends SixNodeElementRender{

	ElectricalWindSensorDescriptor descriptor;
	public ElectricalWindSensorRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalWindSensorDescriptor) descriptor;
	}


	float alpha = 0;
	float wind = 0;
	
	RcInterpolator windFilter = new RcInterpolator(5);

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front.right(),descriptor.pinDistance);

		windFilter.stepGraphic();
		alpha += windFilter.get()*FrameTime.get()*20;
		if(alpha>360) alpha-=360;
		
		descriptor.draw(alpha);
	}


	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
	
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			wind = stream.readFloat();
			windFilter.setTarget(wind);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
