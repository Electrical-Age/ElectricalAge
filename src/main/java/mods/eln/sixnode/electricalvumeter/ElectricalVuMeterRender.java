package mods.eln.sixnode.electricalvumeter;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalVuMeterRender extends SixNodeElementRender {

    ElectricalVuMeterDescriptor descriptor;

    PhysicalInterpolator interpolator;
    float factor;
    LRDU front;

    boolean boot = true;

    public ElectricalVuMeterRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalVuMeterDescriptor) descriptor;
        interpolator = new PhysicalInterpolator(0.4f, 2.0f, 1.5f, 0.2f);
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front, descriptor.pinDistance);

        descriptor.draw(descriptor.onOffOnly ? interpolator.getTarget() : interpolator.get(), UtilsClient.distanceFromClientPlayer(tileEntity), tileEntity);
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.step(deltaT);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            front = LRDU.fromInt((b >> 4) & 3);
            if (boot) {
                interpolator.setPos(stream.readFloat());
            } else {
                interpolator.setTarget(stream.readFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (boot) {
            boot = false;
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Eln.instance.signalCableDescriptor.render;
    }
}
