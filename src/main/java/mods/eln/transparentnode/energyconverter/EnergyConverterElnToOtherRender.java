package mods.eln.transparentnode.energyconverter;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** Client side of the exporter: the GUI's values. The block is drawn by its baked model (FRONT state), not here. */
public class EnergyConverterElnToOtherRender extends TransparentNodeElementRender {
    public float inPowerFactor;
    public float inPowerMax;
    /** A publish arrived since the screen last read inPowerFactor. */
    public boolean hasChanges;

    public EnergyConverterElnToOtherRender(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
    }

    @Override
    public void draw() {
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            inPowerFactor = stream.readFloat();
            inPowerMax = stream.readFloat();
            hasChanges = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
