package mods.eln.sixnode.electricalwindsensor;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalWindSensorRender extends SixNodeElementRender {

    ElectricalWindSensorDescriptor descriptor;

    float wind = 0;

    /** The cups: 20 degrees a second per unit of wind, the wind eased over five seconds. */
    final mods.eln.anim.Spin cups = new mods.eln.anim.Spin(20, 5);

    public ElectricalWindSensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalWindSensorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front.right(), new float[]{2, 2, 2, 2});

        descriptor.draw(cups.deg());
    }

    @Override
    public void refresh(float deltaT) {
        cups.step(deltaT);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.signalCableDescriptor.render;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            wind = stream.readFloat();
            cups.speed(wind);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
