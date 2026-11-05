package mods.eln.sixnode.busdevice;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Addressable Light on the client. */
public class AddressableLightRender extends BusDeviceRender {
    final AddressableLightDescriptor descriptor;
    float level;

    public AddressableLightRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (AddressableLightDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawPin(2 / 16f);
        if (front == null) return;
        if (side.isY()) front.right().glRotateOnX();
        descriptor.draw(level, tileEntity);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    protected void unserializeMore(DataInputStream stream) throws IOException {
        level = stream.readFloat();
    }
}
