package mods.eln.robotics.servo;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Linear Servo Drive (LinearServoDriveDescriptor): the motor, its drum, and the status lamp. */
public class LinearServoDriveRender extends TransparentNodeElementRender {
    final LinearServoDriveDescriptor desc;
    public byte status;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
