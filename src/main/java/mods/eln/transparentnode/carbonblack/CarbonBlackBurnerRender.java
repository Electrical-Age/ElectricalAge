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
    final mods.eln.anim.Emitter smoke = new mods.eln.anim.Emitter();

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
        int n = smoke.step(deltaT, burning ? 6 : 0);
        if (level == null || n == 0) return;
        var p = tileEntity.getBlockPos();
        // (the stack's top, in the block's frame: the model's x runs from the front to the back; its z is on the centre line)
        var back = front.back().toEnumFacing();
        double off = (CarbonBlackBurnerDescriptor.STACK_X - 8) / 16.0;
        double[] xz = {0.5 + back.getStepX() * off, 0.5 + back.getStepZ() * off};
        for (int i = 0; i < n; i++) mods.eln.anim.Plume.SOOT.puff(level, p.getX() + xz[0], p.getY() + 1.02, p.getZ() + xz[1], 0.05);
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
