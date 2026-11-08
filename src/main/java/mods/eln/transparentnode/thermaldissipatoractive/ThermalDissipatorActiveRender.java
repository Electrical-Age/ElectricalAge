package mods.eln.transparentnode.thermaldissipatoractive;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ThermalDissipatorActiveRender extends TransparentNodeElementRender {
    ThermalDissipatorActiveDescriptor descriptor;

    public ThermalDissipatorActiveRender(TransparentNodeEntity tileEntity,
                                         TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (ThermalDissipatorActiveDescriptor) descriptor;
    }

    /** The fan: a turn a second at full power, spinning up and down over two seconds. */
    final mods.eln.anim.Spin fan = new mods.eln.anim.Spin(360, 2);

    @Override
    public void draw() {
        front.glRotateXnRef();


        descriptor.draw(fan.deg());
    }

    @Override
    public void refresh(float deltaT) {
        fan.speed(powerFactor).step(deltaT);
    }

    float powerFactor;

    @Override
    public void networkUnserialize(DataInputStream stream) {

        super.networkUnserialize(stream);
        try {
            powerFactor = stream.readFloat();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
