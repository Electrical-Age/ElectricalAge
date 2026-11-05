package mods.eln.sixnode.busdevice;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Floor Sensor on the client: its halo lights while a car is level with its floor. */
public class FloorSensorRender extends BusDeviceRender {
    final FloorSensorDescriptor descriptor;
    boolean level;

    public FloorSensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (FloorSensorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawPin(3 / 16f);
        descriptor.draw(level);
    }

    @Override
    protected void unserializeMore(DataInputStream stream) throws IOException {
        level = stream.readBoolean();
    }
}
