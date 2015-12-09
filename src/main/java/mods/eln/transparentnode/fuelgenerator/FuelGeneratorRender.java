package mods.eln.transparentnode.fuelgenerator;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;

public class FuelGeneratorRender extends TransparentNodeElementRender {

    FuelGeneratorDescriptor descriptor;
    private CableRenderType renderPreProcess;
    private LRDUMask eConn = new LRDUMask();

    public FuelGeneratorRender(TransparentNodeEntity tileEntity,
                               TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (FuelGeneratorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.getCableRenderDescriptor(), eConn, renderPreProcess);
        front.glRotateZnRef();
        descriptor.draw();
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        eConn.deserialize(stream);
        renderPreProcess = null;
    }
}
