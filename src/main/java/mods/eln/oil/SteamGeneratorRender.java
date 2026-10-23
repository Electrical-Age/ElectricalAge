package mods.eln.oil;

import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class SteamGeneratorRender extends TransparentNodeElementRender {
    /** The rotor's turn rate at nominal speed (degrees per second): slow enough to follow, a hint rather than 3000 rpm
     *  (Baughn: subtle). */
    static final float DEG_PER_S = 360;
    final SteamGeneratorDescriptor desc;
    final RcInterpolator speed = new RcInterpolator(1.5f);
    float angle = (float) (Math.random() * 360);

    public SteamGeneratorRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SteamGeneratorDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(angle);
    }

    @Override
    public void refresh(float deltaT) {
        speed.step(deltaT);
        angle = (angle + deltaT * DEG_PER_S * speed.get()) % 360;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            speed.setTarget(stream.readFloat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
