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
    /** The scan settings, what a scan costs at them (J) and what the batteries can give (W). */
    int range = 1, detail = 1;
    float energy, rate;
    /** The last picture, and the game time it was taken. */
    GroundScan picture;
    long pictureAt;
    /** The screen's depth and cut, kept while the block is in view (reopening it comes back to them). */
    int viewLayer, viewRow = -1;

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
            range = stream.readByte();
            detail = stream.readByte();
            energy = stream.readFloat();
            rate = stream.readFloat();
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

    void setRange(int r) {
        range = r;
        clientSendFloat(GroundRadarElement.RANGE_ID, r);
    }

    void setDetail(int d) {
        detail = d;
        clientSendFloat(GroundRadarElement.DETAIL_ID, d);
    }
}
