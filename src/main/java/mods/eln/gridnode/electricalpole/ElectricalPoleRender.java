package mods.eln.gridnode.electricalpole;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.gridnode.GridRender;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;

/**
 * Created by svein on 07/08/15.
 */
public class ElectricalPoleRender extends GridRender {

    CableRenderType cableRenderType;
    LRDUMask eConn = new LRDUMask();

    public ElectricalPoleRender(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
    }

    @Override
    public void draw() {
        super.draw();
        cableRenderType = drawCable(front.down(), Eln.instance.stdCableRender3200V, eConn, cableRenderType);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        eConn.deserialize(stream);
        cableRenderType = null;
    }

    //    @Override
//    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
//        return Eln.instance.stdCableRender3200V;
//    }

}
