package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricHeaterRender extends TransparentNodeElementRender {
    final ElectricHeaterDescriptor desc;
    /** From the server: the regulator's set point and the temperature (relative to ambient, like Tc), the power. */
    public float target, temperature, power;
    public boolean targetNew;

    public ElectricHeaterRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (ElectricHeaterDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
        // the coils glow through the grille when hot (Baughn 2026-10-28: what heats up on the outside glows)
        double c = mods.eln.misc.Incandescence.celsius(temperature);
        mods.eln.misc.Incandescence.drawGlow(c, desc::drawElement);
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glTranslatef(-0.36f, -0.1f, 0f);
        mods.eln.misc.Incandescence.drawHalo(c, 0.4f, tileEntity);
        mods.eln.gl.GL11.glPopMatrix();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            float t = stream.readFloat();
            if (t != target) targetNew = true;
            target = t;
            temperature = stream.readFloat();
            power = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
