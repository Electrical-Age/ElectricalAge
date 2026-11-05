package mods.eln.robotics.servo;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Linear Servo Drive (LinearServoDriveDescriptor): the motor, its drum, and the status lamp. */
public class LinearServoDriveRender extends TransparentNodeElementRender implements mods.eln.introspect.ClientDescribable {
    final LinearServoDriveDescriptor desc;
    public byte status;
    /** Its bus address (0: none), and whether its bus pin is on a bus (its screen). */
    public int address;
    public boolean onBus;
    /** The car panel's first address (0: none). */
    public int panelBase;

    public LinearServoDriveRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (LinearServoDriveDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(status);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            status = stream.readByte();
            address = stream.readByte() & 0xFF;
            onBus = stream.readBoolean();
            panelBase = stream.readByte() & 0xFF;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void describeClient(mods.eln.introspect.Report r) {
        var names = LinearServoDriveElement.STATUS_NAMES;
        r.sync().choice(mods.eln.introspect.Keys.STATUS, status >= 0 && status < names.size() ? names.get(status) : names.get(0), names);
    }
}
