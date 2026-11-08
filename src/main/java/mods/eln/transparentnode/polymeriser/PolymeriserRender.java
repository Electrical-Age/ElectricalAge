package mods.eln.transparentnode.polymeriser;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Polymeriser: its model, status LEDs and the agitator drive turning while it runs. */
public class PolymeriserRender extends TransparentNodeElementRender {
    final PolymeriserDescriptor desc;
    boolean powered, running;
    float stir;
    long last = System.nanoTime();

    public PolymeriserRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (PolymeriserDescriptor) d;
    }

    @Override
    public void draw() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        if (running) stir = (stir + 180 * dt) % 360;   // (half a turn a second)
        front.glRotateXnRef();
        desc.draw(powered, running, stir);
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
