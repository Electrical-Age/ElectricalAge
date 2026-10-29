package mods.eln.sixnode.resistor;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ResistorRender extends SixNodeElementRender {

    public ResistorDescriptor descriptor;
    private CableRenderType renderPreProcess;

    private float wiperPos = 0;
    /** The glow's temperature (C, on the resistor's warning scale), from the server. */
    private float glowCelsius;

    public ResistorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ResistorDescriptor) descriptor;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            if (descriptor.isRheostat) wiperPos = stream.readFloat();
            glowCelsius = stream.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw() {
        GL11.glRotatef(90, 1, 0, 0);
        front.glRotateOnX();
        descriptor.draw(wiperPos);
        // near its 130 C limit it glows dull red to orange, a warning (Incandescence.warningCelsius)
        mods.eln.misc.Incandescence.drawGlow(glowCelsius, descriptor::drawCore);
    }

}
