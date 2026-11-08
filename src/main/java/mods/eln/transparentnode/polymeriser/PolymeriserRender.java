package mods.eln.transparentnode.polymeriser;

import mods.eln.anim.Spin;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Polymeriser: its model, status LEDs and the agitator drive turning while it runs. */
public class PolymeriserRender extends TransparentNodeElementRender {
    final PolymeriserDescriptor desc;
    boolean powered, running;
    /** The agitator: three quarters of a turn a second, easing up and down over a fraction of a second. */
    final Spin stir = new Spin(270, 0.3);

    public PolymeriserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (PolymeriserDescriptor) d;
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        stir.speed(running ? 1 : 0).step(deltaT);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(powered, running, stir.deg());
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            powered = stream.readBoolean();
            running = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
