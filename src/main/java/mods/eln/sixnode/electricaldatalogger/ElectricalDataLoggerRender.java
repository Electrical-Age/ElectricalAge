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
        // the graph as a texture, painted when a point comes (LogTexture)
        net.minecraft.resources.Identifier graph = null;
        if (descriptor.mx > 0 && descriptor.my > 0) {
            if (texture == null && !noTexture) noTexture = (texture = LogTexture.of(descriptor.mx / descriptor.my)) == null;
            if (texture != null) graph = texture.update(log, this::ink);
        }
        descriptor.draw(log, side, front, pinEdge(), this.tileEntity.getBlockPos().getX(), this.tileEntity.getBlockPos().getZ(), graph);
    }
	/*
    @Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return descriptor.cableRender;
	}
	*/

    /** Within this many blocks the graph can be read: the server sends it only to clients this near or with the screen open. */
    public static final double WATCH_RANGE = 32;
    public final LogClient logClient = new LogClient(this, -1);
    LogTexture texture;
    boolean noTexture;
    /** The inputs' edge: quarter turns from the back (the tilted terminal: shift-C). */
    int pinTurn;

    LRDU pinEdge() {
        LRDU l = front.inverse();
        for (int i = 0; i < pinTurn; i++) l = l.getNextClockwise();
        return l;
    }

    /** Channel c's ink on the in-world graph: the logger's colour, or A red, B blue, C green. */
    int ink(int c) {
        float[] k = log.channelCount() > 1 ? DataLogs.INK[c % DataLogs.INK.length] : new float[]{descriptor.cr, descriptor.cg, descriptor.cb};
        return 0xFF000000 | Math.round(k[0] * 255) << 16 | Math.round(k[1] * 255) << 8 | Math.round(k[2] * 255);
    }

    @Override
    public void destructor() {
        super.destructor();
        if (texture != null) texture.close();
        texture = null;
    }

    /** Keeps the graph coming (the screen calls it while open; refresh while the logger is near). */
    public void watch() {
        logClient.watch();
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        if (mods.eln.misc.UtilsClient.distanceFromClientPlayer(tileEntity) < WATCH_RANGE) watch();
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            pause = stream.readBoolean();
            log = Logger.readPublish(stream, log);
            pinTurn = stream.readByte();
            if (log.channelCount() > 1)
                for (int c = 0; c < log.channelCount(); c++) log.channels[c].name = String.valueOf((char) ('A' + c));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) throws IOException {
        log = Logger.receive(stream, log);
        waitFistSync = false;
    }

}
