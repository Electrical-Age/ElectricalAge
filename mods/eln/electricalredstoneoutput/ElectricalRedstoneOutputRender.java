
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

		descriptor.draw(redOutput);
	}

	int redOutput;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			redOutput = stream.readByte();
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
