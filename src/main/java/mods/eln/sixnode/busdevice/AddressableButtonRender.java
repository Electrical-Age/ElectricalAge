package mods.eln.sixnode.busdevice;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Addressable Button on the client: the button moves in and out, the lamp shows what it was sent. */
public class AddressableButtonRender extends BusDeviceRender {
    final AddressableButtonDescriptor descriptor;
    /** The button's travel: quick (0.05 s; an Euler-stepped RcInterpolator rang at that below 20 fps). */
    final mods.eln.anim.Travel interpolator = mods.eln.anim.Travel.smooth(0.05);
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
        interpolator.to(pushed);
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
