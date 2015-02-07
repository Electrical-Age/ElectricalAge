package mods.eln.sixnode.electricalgatesource;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalGateSourceRender extends SixNodeElementRender {

	ElectricalGateSourceDescriptor descriptor;

    LRDU front;

    RcInterpolator interpolator;

    float voltageSyncValue = 0;
    boolean voltageSyncNew = false;
    boolean boot = true;

	public ElectricalGateSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalGateSourceDescriptor) descriptor;
		interpolator = new RcInterpolator(this.descriptor.render.speed);
	}

	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front, new float[] { 3, 3, 3, 3 });

		LRDU.Down.glRotateOnX();
		descriptor.draw(interpolator.get(), UtilsClient.distanceFromClientPlayer(this.tileEntity), tileEntity);
	}

	@Override
	public void refresh(float deltaT) {
		interpolator.setTarget((float) (voltageSyncValue / Eln.SVU));
		interpolator.step(deltaT);
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b >> 4) & 3);
			float readF;
			readF = stream.readFloat();
			if (voltageSyncValue != readF) {
				voltageSyncValue = readF;
				voltageSyncNew = true;
			}

			if (boot) {
				boot = false;
				interpolator.setValue((float) (voltageSyncValue / Eln.SVU));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalGateSourceGui(player, this);
	}
}
