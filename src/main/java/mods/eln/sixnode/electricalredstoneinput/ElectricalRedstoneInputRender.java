package mods.eln.sixnode.electricalredstoneinput;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalRedstoneInputRender extends SixNodeElementRender {

    ElectricalRedstoneInputDescriptor descriptor;

    byte redLevel;

    public ElectricalRedstoneInputRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalRedstoneInputDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front.right(), descriptor.pinDistance);

        LRDU.Down.glRotateOnX();
        descriptor.draw(redLevel);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            redLevel = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Eln.instance.signalCableDescriptor.render;
    }
}
