package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The flare: a plume over the stack by what goes up it (anim.Plume: flames by the fuel, steam, a flue's smoke). */
public class FlareStackRender extends TransparentNodeElementRender {
    final FlareStackDescriptor desc;
    String burning = "";
    final mods.eln.anim.Emitter puffs = new mods.eln.anim.Emitter();

    public FlareStackRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (FlareStackDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            burning = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        var plume = mods.eln.anim.Plume.burnt(burning);
        var level = tileEntity.getLevel();
        int n = puffs.step(deltaT, plume == mods.eln.anim.Plume.NONE ? 0 : 20);   // ~20 puffs a second
        if (level == null || n == 0) return;
        var p = tileEntity.getBlockPos();
        for (int i = 0; i < n; i++) plume.puff(level, p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5, 0.09);   // inside the tip (r 1.75 px)
    }
}
