package mods.eln.sixnode.TreeResinCollector;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class TreeResinCollectorRender extends SixNodeElementRender {

    TreeResinCollectorDescriptor descriptor;

    float stock;

    public TreeResinCollectorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (TreeResinCollectorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();

        LRDU.Down.glRotateOnX();
        descriptor.draw(stock);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            stock = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
