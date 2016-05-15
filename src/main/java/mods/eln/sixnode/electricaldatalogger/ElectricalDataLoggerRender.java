package mods.eln.sixnode.electricaldatalogger;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalDataLoggerRender extends SixNodeElementRender {

	SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
	ElectricalDataLoggerDescriptor descriptor;
	long time;

    public boolean pause;

    DataLogs log = new DataLogs(ElectricalDataLoggerElement.logsSizeMax);
    boolean waitFistSync = true;

	public ElectricalDataLoggerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
		time = System.currentTimeMillis();
		clientSend(ElectricalDataLoggerElement.newClientId);
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}

	@Override
	public void draw() {
		super.draw();
        if (!descriptor.onFloor) {
			if (side.isY()) {
				GL11.glPushMatrix();
				front.right().glRotateOnX();
				drawSignalPin(LRDU.Right, new float[]{0, 5.67f, 0, 0});
				GL11.glPopMatrix();
			} else {
				drawSignalPin(front.inverse(), new float[]{6.37f, 6.37f, 5.67f, 6.12f});
			}
        }
        descriptor.draw(log, side, front, this.tileEntity.xCoord, this.tileEntity.zCoord);
	}

	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return descriptor.cableRender;
	}
	*/
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			log.unitType = stream.readByte();
			pause = stream.readBoolean();
			log.samplingPeriod = stream.readFloat();
			log.maxValue = stream.readFloat();
			log.minValue = stream.readFloat();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	@Override
	public void serverPacketUnserialize(DataInputStream stream) throws IOException {
		byte header = stream.readByte();
		
		switch(header) {
            case ElectricalDataLoggerElement.toClientLogsAdd:
            case ElectricalDataLoggerElement.toClientLogsClear:
                if (header == ElectricalDataLoggerElement.toClientLogsClear) {
                    log.reset();
                    waitFistSync = false;
                }
                int size = stream.available();
                while (size != 0) {
                    size--;
                    log.write(stream.readByte());
                }
            //	Utils.println(log);
                break;
		}
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalDataLoggerGui(player, inventory, this);
	}
}
