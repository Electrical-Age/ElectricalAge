package mods.eln.sixnode.electricalbreaker;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.debug.DebugType;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalBreakerRender extends SixNodeElementRender {

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
    ElectricalBreakerDescriptor descriptor;
    long time;

    RcInterpolator interpol;

    float uMin, uMax;

    boolean boot = true;
    float switchAlpha = 0;
    public boolean switchState;

    CableRenderDescriptor cableRender;

    public ElectricalBreakerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalBreakerDescriptor) descriptor;
        time = System.currentTimeMillis();
        interpol = new RcInterpolator(this.descriptor.speed);
    }

    @Override
    public void draw() {
        super.draw();

        front.glRotateOnX();
        descriptor.draw(interpol.get(), UtilsClient.distanceFromClientPlayer(tileEntity));
    }

    @Override
    public void refresh(float deltaT) {
        interpol.setTarget(switchState ? 1f : 0f);
        interpol.step(deltaT);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return cableRender;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        Eln.dp.println(DebugType.SIX_NODE, "Front : " + front);
        try {
            switchState = stream.readBoolean();
            uMax = stream.readFloat();
            uMin = stream.readFloat();

            ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream), ElectricalCableDescriptor.class);

            if (desc == null)
                cableRender = null;
            else
                cableRender = desc.render;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot) {
            interpol.setValue(switchState ? 1f : 0f);
        }
        boot = false;
    }

    public void clientSetVoltageMin(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.setVoltageMinId);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientSetVoltageMax(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.setVoltageMaxId);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientToogleSwitch() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(ElectricalBreakerElement.toogleSwitchId);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ElectricalBreakerGui(player, inventory, this);
    }
}
