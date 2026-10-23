package mods.eln.sixnode.groundcable;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;

public class GroundCableRender extends SixNodeElementRender {

    GroundCableDescriptor descriptor;

    //double voltage = 0,current = 0;
    int color = 0;

    CableRenderDescriptor cableRender;


    public GroundCableRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (GroundCableDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();

        if (side.isY()) {
            front.glRotateOnX();
        }

        descriptor.draw();
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();

            color = (b >> 4) & 0xF;
            cableRender = ElnDeviceRegistry.lowVoltageCableDescriptor.render; // no cable slot (Baughn 2026-10-17)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CableRenderDescriptor getCableRender(mods.eln.misc.LRDU lrdu) {
        return cableRender;
    }

}
