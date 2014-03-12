
package mods.eln.electricalwindsensor;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
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



	@Override
	public void draw() {
		super.draw();
		descriptor.draw();
	}


	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
}
