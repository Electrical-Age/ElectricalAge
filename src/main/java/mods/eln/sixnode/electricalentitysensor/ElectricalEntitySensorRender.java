package mods.eln.sixnode.electricalentitysensor;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalEntitySensorRender extends SixNodeElementRender {

    ElectricalEntitySensorDescriptor descriptor;

    boolean state = false;
    EntitySensorFilterDescriptor filter = null;


    public ElectricalEntitySensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalEntitySensorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front.right(), descriptor.pinDistance);

        descriptor.draw(state, filter);
    }
    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            state = stream.readBoolean();
            ItemStack filterStack = Utils.unserialiseItemStack(stream);
            filter = (EntitySensorFilterDescriptor) EntitySensorFilterDescriptor.getDescriptor(filterStack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.signalCableDescriptor.render;
    }

}
