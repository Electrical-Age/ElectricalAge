package mods.eln.node.transparent;

import mods.eln.anim.Travel;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The render for a machine with a static model, power and run LEDs and an on/off lever (the Desulfuriser, the Steam
 * Cracker, the Industrial Resin Collector; the Polymeriser adds its stirrer). The element sends powered, running and
 * the lever's state; the descriptor draws in front-relative coordinates. Three identical render classes were folded
 * into this one (consolidation 2026-11-28).
 */
public class LeverMachineRender extends TransparentNodeElementRender {
    /** A descriptor that draws its model, LEDs and lever in front-relative coordinates. */
    public interface Drawn {
        void draw(boolean powered, boolean running, float lever);
    }

    protected boolean powered, running;
    /** The on/off lever's swing (mods.eln.anim). */
    protected final Travel lever = Travel.smooth(0.08).set(1);

    public LeverMachineRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        lever.step(deltaT);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        ((Drawn) transparentNodedescriptor).draw(powered, running, lever.get());
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            powered = stream.readBoolean();
            running = stream.readBoolean();
            lever.to(stream.readBoolean());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
