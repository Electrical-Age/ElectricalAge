package mods.eln.sixnode.thermalsensor;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;

public class ThermalSensorRender extends SixNodeElementRender {

    ThermalSensorDescriptor descriptor;

    LRDU front;

    int typeOfSensor = 0;
    float lowValue = 0, highValue = 50;

    ThermalCableDescriptor cable;
    ElectricalCableDescriptor ecable;

    public ThermalSensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ThermalSensorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        front.glRotateOnX();
        descriptor.draw(descriptor.temperatureOnly && cable == null); // the adapter: on electrical cables
    }

	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return descriptor.cableRender;
	}
	*/

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            front = LRDU.fromInt((b >> 4) & 3);
            typeOfSensor = b & 0x3;
            lowValue = (float) (stream.readFloat() + PhysicalConstant.Tamb);
            highValue = (float) (stream.readFloat() + PhysicalConstant.Tamb);
            ItemStack stack = Utils.unserialiseItemStack(stream);
            cable = mods.eln.node.voxel.CableItems.thermal(stack);   // (1.12 items or kits)
            ecable = cable == null ? mods.eln.node.voxel.CableItems.descriptor(stack) : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            // built-in Copper Thermal Cable (Baughn 2026-10-17)
            if (front.left() == lrdu) return ElnDeviceRegistry.copperThermalCableDescriptor.render;
            if (front.right() == lrdu) return ElnDeviceRegistry.copperThermalCableDescriptor.render;
            if (front == lrdu) return ElnDeviceRegistry.signalCableDescriptor.render;
        } else {
            if (front.inverse() == lrdu && cable != null) return cable.render;
            if (front.inverse() == lrdu && ecable != null) return ecable.render;
            if (front.inverse() == lrdu) return ElnDeviceRegistry.lowVoltageCableDescriptor.render; // empty slot
            if (front == lrdu) return ElnDeviceRegistry.signalCableDescriptor.render;
        }
        return null;
    }
}
