package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.init.Cable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalRedstoneOutputRender extends SixNodeElementRender {

    ElectricalRedstoneOutputDescriptor descriptor;

    float factor;
    float factorFiltered = 0;

    int redOutput;

    public ElectricalRedstoneOutputRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalRedstoneOutputDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();

        drawSignalPin(front.right(), descriptor.pinDistance);

        descriptor.draw(redOutput);
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        return redOutput;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            redOutput = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Cable.Companion.getSignal().descriptor.render;
    }
}
