package mods.eln.sixnode.thermalsensor;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.init.Cable;
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
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;

public class ThermalSensorRender extends SixNodeElementRender {

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
    ThermalSensorDescriptor descriptor;
    long time;

    LRDU front;

    int typeOfSensor = 0;
    float lowValue = 0, highValue = 50;

    ThermalCableDescriptor cable;
    ElectricalCableDescriptor ecable;

    public ThermalSensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ThermalSensorDescriptor) descriptor;
        time = System.currentTimeMillis();
    }

    @Override
    public void draw() {
        super.draw();
        front.glRotateOnX();
        descriptor.draw(ecable != null);
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
            GenericItemBlockUsingDamageDescriptor desc = ThermalCableDescriptor.getDescriptor(stack);
            if (desc instanceof ThermalCableDescriptor) cable = (ThermalCableDescriptor) desc;
            else cable = null;
            if (desc instanceof ElectricalCableDescriptor) ecable = (ElectricalCableDescriptor) desc;
            else ecable = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ThermalSensorGui(player, inventory, this);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            if (front.left() == lrdu && cable != null) return cable.render;
            if (front.right() == lrdu && cable != null) return cable.render;
            if (front == lrdu) return Cable.Companion.getSignal().descriptor.render;
        } else {
            if (front.inverse() == lrdu && cable != null) return cable.render;
            if (front.inverse() == lrdu && ecable != null) return ecable.render;
            if (front == lrdu) return Cable.Companion.getSignal().descriptor.render;
        }
        return null;
    }
}
