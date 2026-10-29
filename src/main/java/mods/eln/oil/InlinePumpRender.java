package mods.eln.oil;

import mods.eln.misc.RcInterpolator;
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
    final RcInterpolator speed = new RcInterpolator(0.5f);
    boolean cavitating;
    float angle;

    public InlinePumpRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (InlinePumpDescriptor) d;
        for (LoopedSound l : MachineSounds.electric(coordonate(), speed::get, speed::get, 1.8f, 0, 0.5f)) addLoopedSound(l);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(angle);
    }

    @Override
    public void refresh(float deltaT) {
        speed.step(deltaT);
        angle = (angle + deltaT * 720 * speed.get()) % 360;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            speed.setTarget(stream.readFloat());
            cavitating = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
