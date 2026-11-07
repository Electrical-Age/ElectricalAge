package mods.eln.transparentnode.wiredrawing;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** A wire-drawing machine (WireDrawingDescriptor): the model, and what the element publishes for its screen. */
public class WireDrawingRender extends TransparentNodeElementRender {
    final WireDrawingDescriptor desc;
    public int mode, mm2, tier, colour, amount;
    public boolean running;
    public byte status;
    public float progress, copper, rubber, watts;
    /** Set by the screen; true until the element's next publish has the settings it sent. */
    boolean published;

    public WireDrawingRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (WireDrawingDescriptor) d;
    }

    /** The spool's and capstan's turn (degrees), and how fast they go now (eased, so they spin up and run down). */
    float spoolAngle, capstanAngle, speed;

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        float target = status == WireDrawingElement.RUNNING ? 1f : 0f;
        speed += (target - speed) * Math.min(1f, deltaT * 3f);
        // the rod pays off the big spool slowly; the small capstan pulls the wire faster (the same wire speed)
        spoolAngle = (spoolAngle + speed * deltaT * 200f) % 360f;   // (Baughn: 60 and 150 degrees a second read as barely moving)
        capstanAngle = (capstanAngle - speed * deltaT * 500f) % 360f;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(spoolAngle, capstanAngle);
    }

    /** Sends what to make (the element clamps it to its limits). */
    public void clientSet(int mode, int mm2, int tier, int colour, int amount) {
        this.mode = mode;
        this.mm2 = mm2;
        this.tier = tier;
        this.colour = colour;
        this.amount = amount;
        clientSendString(WireDrawingElement.SET, mode + "," + mm2 + "," + tier + "," + colour + "," + amount);
    }

    public void clientRun(boolean run) {
        running = run;
        clientSendBoolean(WireDrawingElement.RUN, run);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            mode = stream.readByte();
            mm2 = stream.readByte();
            tier = stream.readByte();
            colour = stream.readByte();
            amount = stream.readShort();
            running = stream.readBoolean();
            status = stream.readByte();
            progress = stream.readFloat();
            copper = stream.readFloat();
            rubber = stream.readFloat();
            watts = stream.readFloat();
            published = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
