package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalSourceRender extends SixNodeElementRender {

	ElectricalSourceDescriptor descriptor;

    double voltage = 0, current = 0;
    int color = 0;

	public ElectricalSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalSourceDescriptor)descriptor;
	}

	@Override
	public void draw() {
		super.draw();

		descriptor.draw();
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			
			color = (b >> 4) & 0xF;
			voltage = stream.readFloat();

			needRedrawCable();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalSourceGui(this);
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if (voltage < Eln.instance.lowVoltageCableDescriptor.electricalMaximalVoltage) return Eln.instance.lowVoltageCableDescriptor.render;
		if (voltage < Eln.instance.meduimVoltageCableDescriptor.electricalMaximalVoltage) return Eln.instance.meduimVoltageCableDescriptor.render;
		return Eln.instance.highVoltageCableDescriptor.render;
	}
}
