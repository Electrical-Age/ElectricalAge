package mods.eln.transparentnode.fuelgenerator;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class FuelGeneratorRender extends TransparentNodeElementRender {

    RcInterpolator powerFactorFilter = new RcInterpolator(1);
    RcInterpolator dirFilter = new RcInterpolator(0.5f);
    FuelGeneratorDescriptor descriptor;
    private CableRenderType renderPreProcess;
    private float powerFactor;

    public FuelGeneratorRender(TransparentNodeEntity tileEntity,
                               TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (FuelGeneratorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        front.glRotateZnRef();
        renderPreProcess = drawCable(Direction.YN, descriptor.getCableRenderDescriptor(), new LRDUMask(), renderPreProcess);
        descriptor.draw();
    }

    public void refresh(float deltaT) {
        powerFactorFilter.setTarget((float) (dirFilter.get() * Math.sqrt(powerFactor)));
        powerFactorFilter.step(deltaT);
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

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
