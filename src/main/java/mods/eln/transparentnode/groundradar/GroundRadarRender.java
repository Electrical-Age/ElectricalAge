package mods.eln.transparentnode.groundradar;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Ground Radar: its model and LEDs, the published state, and the last picture the server sent (for the screen). */
public class GroundRadarRender extends TransparentNodeElementRender {
    final GroundRadarDescriptor desc;
    /** The batteries' charge (0..1; -1: none), the scan's progress (-1: not scanning), why the last scan stopped. */
    float battery = -1, progress = -1;
    String fault = "";
    /** The last picture, and the game time it was taken. */
    GroundScan picture;
    long pictureAt;
    /** The screen's depth and cut, kept while the block is in view (reopening it comes back to them). */
    int viewLayer, viewRow = GroundScan.N / 2;

    public GroundRadarRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (GroundRadarDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(battery > 0, progress >= 0);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            battery = stream.readFloat();
            progress = stream.readFloat();
            fault = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            if (stream.readByte() != GroundRadarElement.PICTURE_ID) return;
            long at = stream.readLong();
            int n = stream.readInt();
            if (n < 0 || n > 1 << 22) return;
            byte[] b = new byte[n];
            stream.readFully(b);
            GroundScan s = GroundScan.decode(b);
            if (s == null) return;
            picture = s;
            pictureAt = at;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void scan() {
        clientSendId(GroundRadarElement.SCAN_ID);
    }
}
