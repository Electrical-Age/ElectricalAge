package mods.eln.transparentnode.carbonblack;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The Carbon Black Burner: its model, the fire in its sight port while it burns, and thick black smoke up its stack. */
public class CarbonBlackBurnerRender extends TransparentNodeElementRender {
    final CarbonBlackBurnerDescriptor desc;
    boolean burning;
    float smokeDue;

    public CarbonBlackBurnerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (CarbonBlackBurnerDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(burning);
    }

    @Override
    public void refresh(float deltaT) {
        var level = tileEntity.getLevel();
        if (!burning || level == null) return;
        smokeDue += deltaT * 6;
        var p = tileEntity.getBlockPos();
        // (the stack's top, in the block's frame: the model's x runs from the front to the back; its z is on the centre line)
        var back = front.back().toEnumFacing();
        double off = (CarbonBlackBurnerDescriptor.STACK_X - 8) / 16.0;
        double[] xz = {0.5 + back.getStepX() * off, 0.5 + back.getStepZ() * off};
        while (smokeDue >= 1) {
            smokeDue -= 1;
            double jx = (Math.random() - 0.5) * 0.1, jz = (Math.random() - 0.5) * 0.1;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, p.getX() + xz[0] + jx, p.getY() + 1.02, p.getZ() + xz[1] + jz, 0, 0.07, 0);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            burning = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
