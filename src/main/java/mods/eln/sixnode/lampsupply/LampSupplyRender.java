package mods.eln.sixnode.lampsupply;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class LampSupplyRender extends SixNodeElementRender {

    LampSupplyDescriptor descriptor;

    Coordonate coord;
    PhysicalInterpolator interpolator;

    public ArrayList<LampSupplyElement.Entry> entries = new ArrayList<LampSupplyElement.Entry>();


    CableRenderDescriptor cableRender;


    public LampSupplyRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (LampSupplyDescriptor) descriptor;
        interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
        coord = new Coordonate(tileEntity);
        for (int i = 0; i < ((LampSupplyDescriptor) descriptor).channelCount; i++) {
            entries.add(new LampSupplyElement.Entry("", "", 2));
        }
    }

    @Override
    public void draw() {
        super.draw();

        float[] pinDistances = new float[]{4.98f, 4.98f, 5.98f, 5.98f};

        if (side.isY()) {
            drawPowerPin(front.rotate4PinDistances(pinDistances));
            front.glRotateOnX();
        } else {
            drawPowerPin(pinDistances);
            LRDU.Down.glRotateOnX();
        }
        descriptor.draw(interpolator.get());
    }
    @Override
    public void refresh(float deltaT) {
        if (!Utils.isPlayerAround(tileEntity.getLevel(), coord.getAxisAlignedBB(0)))
            interpolator.setTarget(0f);
        else
            interpolator.setTarget(1f);

        interpolator.step(deltaT);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return cableRender;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            for (LampSupplyElement.Entry e : entries) {
                e.powerChannel = stream.readUTF();
                e.wirelessChannel = stream.readUTF();
                e.aggregator = stream.readChar();
            }

            ItemStack cableStack = Utils.unserialiseItemStack(stream);
            ElectricalCableDescriptor desc = Utils.isEmpty(cableStack) ? null
                : (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack, ElectricalCableDescriptor.class);
            cableRender = desc != null ? desc.render : mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor.render;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void newConnectionType(CableRenderType connectionType) {
        for (int idx = 0; idx < 4; idx++) {
            connectionType.startAt[idx] = 5 / 16f;
        }
    }
}
