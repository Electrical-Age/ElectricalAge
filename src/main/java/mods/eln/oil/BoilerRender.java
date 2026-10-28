package mods.eln.oil;

import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;

import java.io.DataInputStream;
import java.io.IOException;

public class BoilerRender extends TransparentNodeElementRender {
    final BoilerDescriptor desc;
    /** The gauges (Baughn: the dials should work): the gauge glass's fill (0..1) and the water temperature (C). */
    float fill, celsius;
    /** How hard it boils (BoilerElement.boilW), smoothed: the bubbling grows with the heat going in, to full at
     *  FULL_BOIL_W. */
    final RcInterpolator boil = new RcInterpolator(1.5f);
    static final float FULL_BOIL_W = 2000, VOLUME = 0.2f;

    public BoilerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (BoilerDescriptor) d;
        addLoopedSound(new LoopedSound("eln:boiler_boil", coordonate()) {
            @Override
            public float getVolume() {
                return VOLUME * (float) Math.sqrt(Math.max(0, Math.min(1, boil.get())));
            }

            @Override
            public float getPitch() {
                return 0.68f + 0.12f * Math.max(0, Math.min(1, boil.get()));
            }
        });
    }

    @Override
    public void refresh(float deltaT) {
        boil.step(deltaT);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(fill, celsius);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            fill = stream.readFloat();
            celsius = stream.readFloat();
            boil.setTarget(stream.readFloat() / FULL_BOIL_W);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
