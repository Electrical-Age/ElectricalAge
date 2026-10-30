package mods.eln.sixnode.electricaldatalogger;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalDataLoggerRender extends SixNodeElementRender {

    ElectricalDataLoggerDescriptor descriptor;
    long time;

    public boolean pause;

    DataLogs log = new DataLogs(1);
    boolean waitFistSync = true;

    public ElectricalDataLoggerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
        time = System.currentTimeMillis();
        clientSend(ElectricalDataLoggerElement.newClientId);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.signalCableDescriptor.render;
    }

    @Override
    public void draw() {
        super.draw();
        if (!descriptor.onFloor) {
            if (side.isY()) {
                GL11.glPushMatrix();
                front.glRotateOnX();
                drawSignalPin(LRDU.Right, new float[]{0, 5.67f, 0, 0});
                GL11.glPopMatrix();
            } else {
                drawSignalPin(front.inverse(), new float[]{6.37f, 6.37f, 5.67f, 6.12f});
            }
        }
        descriptor.draw(log, side, front, this.tileEntity.getBlockPos().getX(), this.tileEntity.getBlockPos().getZ());
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
            pause = stream.readBoolean();
            log.samplingPeriod = stream.readFloat();
            int n = stream.readByte();
            if (n != log.channelCount()) log = new DataLogs(n);
            for (LogChannel c : log.channels) c.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) throws IOException {
        byte header = stream.readByte();
        if (header != ElectricalDataLoggerElement.toClientLogsAdd && header != ElectricalDataLoggerElement.toClientLogsClear) return;
        int n = stream.readByte();
        if (n != log.channelCount()) {
            LogChannel[] keep = log.channels;
            log = new DataLogs(n);
            for (int c = 0; c < Math.min(n, keep.length); c++) log.channels[c] = keep[c];
        }
        if (header == ElectricalDataLoggerElement.toClientLogsClear) {
            log.readAll(stream);
            waitFistSync = false;
        } else log.add(DataLogs.readPoint(stream, n));
    }

    /** A channel's scale or unit to the server: header, channel, value. */
    public void sendChannel(byte id, int channel, float value) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream o = new java.io.DataOutputStream(bos);
            preparePacketForServer(o);
            o.writeByte(id);
            o.writeByte(channel);
            if (id == ElectricalDataLoggerElement.setUnitId) o.writeByte((int) value);
            else o.writeFloat(value);
            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
