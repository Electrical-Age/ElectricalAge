package mods.eln.transparentnode.steamcracker;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Steam Cracker: its model, status LEDs and the glow in its peepholes. */
public class SteamCrackerRender extends TransparentNodeElementRender {
    final SteamCrackerDescriptor desc;
    boolean powered, running;

    public SteamCrackerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SteamCrackerDescriptor) d;
    }

    /** The on/off lever's swing (mods.eln.anim). */
    final mods.eln.anim.Travel lever = mods.eln.anim.Travel.smooth(0.08).set(1);

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        lever.step(deltaT);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(powered, running, lever.get());
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
