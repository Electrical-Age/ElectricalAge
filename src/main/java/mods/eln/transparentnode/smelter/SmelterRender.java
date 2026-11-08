package mods.eln.transparentnode.smelter;

import mods.eln.gl.GL11;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The smelter (SmelterDescriptor): the model, its firebox window glowing with the temperature (unlit), and the GUI's values. */
public class SmelterRender extends TransparentNodeElementRender {
    final SmelterDescriptor desc;
    public float temperature, progress, perSecond;
    public boolean powered;
    ItemEntity entityItemIn;

    public SmelterRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (SmelterDescriptor) d;
    }

    static final mods.eln.anim.Glow.Ramp WINDOW = k -> new float[]{0.25f + 0.75f * Math.min(1, k * 2), 0.12f + 0.75f * k, 0.08f + 0.6f * k * k};

    @Override
    public void draw() {
        front.glRotateXnRef();
        if (desc.obj == null) return;
        var m = desc.obj.getPart("main");
        if (m != null) m.draw();
        // the window: dull red from ~400 °C to yellow-white at ~1000 °C (Tc is above the room's temperature)
        mods.eln.anim.Glow.draw(desc.obj.getPart("glow"), Math.max(0, Math.min(1, (temperature - 300) / 700f)), WINDOW);
    }

    @Override
    public void refresh(float deltaT) {
        progress = Math.min(1, progress + perSecond * deltaT);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            temperature = stream.readFloat();
            progress = stream.readFloat();
            perSecond = stream.readFloat();
            powered = stream.readBoolean();
            entityItemIn = Utils.unserializeItemStackToEntityItem(stream, entityItemIn, tileEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
