
package mods.eln.electricalredstoneinput;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
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



	@Override
	public void draw() {
		super.draw();
		if(redLevelTimeout == 0){
			redLevelTimeout = 4 + (int)(Math.random()*2);
			redLevel = Utils.getRedstoneLevelAround(new Coordonate(this.tileEntity));
		}
		redLevelTimeout--;
		LRDU.Down.glRotateOnX();
		descriptor.draw(redLevel);
	}

	int redLevel = 0;
	int redLevelTimeout = 0;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);

	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return Eln.instance.signalCableDescriptor.render;
	}
}
