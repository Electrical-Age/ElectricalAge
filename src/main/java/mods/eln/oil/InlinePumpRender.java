package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import mods.eln.sound.MachineSounds;

import java.io.DataInputStream;
import java.io.IOException;

/** The inline pump: its model with the coupling turning with its speed, a motor's hum. */
public class InlinePumpRender extends TransparentNodeElementRender {
    final InlinePumpDescriptor desc;
    /** The coupling: two turns a second at full speed, easing over half a second. */
    final mods.eln.anim.Spin coupling = new mods.eln.anim.Spin(720, 0.5);
    boolean cavitating;

    public InlinePumpRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (InlinePumpDescriptor) d;
        for (LoopedSound l : MachineSounds.electric(coordonate(), coupling::speedNow, coupling::speedNow, 1.8f, 0, 0.5f)) addLoopedSound(l);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(coupling.deg());
    }

    @Override
    public void refresh(float deltaT) {
        coupling.step(deltaT);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            coupling.speed(stream.readFloat());
            cavitating = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
