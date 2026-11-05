package mods.eln.sixnode.busdevice;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Addressable Button on the client: the button moves in and out, the lamp shows what it was sent. */
public class AddressableButtonRender extends BusDeviceRender {
    final AddressableButtonDescriptor descriptor;
    final RcInterpolator interpolator = new RcInterpolator(0.05f);
    boolean pushed, lit;

    public AddressableButtonRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (AddressableButtonDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawPin(3 / 16f);
        if (front == null) return;
        if (side.isY()) front.glRotateOnX();
        else LRDU.Down.glRotateOnX();
        descriptor.draw(interpolator.get(), lit, tileEntity);
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.setTarget(pushed ? 1 : 0);
        interpolator.step(deltaT);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    protected void unserializeMore(DataInputStream stream) throws IOException {
        pushed = stream.readBoolean();
        lit = stream.readBoolean();
    }
}
