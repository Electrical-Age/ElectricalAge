package mods.eln.sixnode.electricalrelay;

import mods.eln.misc.UtilsClient;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalRelayRender extends SixNodeElementRender {

    ElectricalRelayDescriptor descriptor;
    /** The contacts between their poses, easing at the descriptor's speed. */
    mods.eln.anim.Travel interpolator;

    boolean boot = true;
    float switchAlpha = 0;
    public boolean switchState, defaultOutput;

    public ElectricalRelayRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalRelayDescriptor) descriptor;
        interpolator = mods.eln.anim.Travel.smooth(this.descriptor.speed);
    }

    @Override
    public void draw() {
        super.draw();
        //UtilsClient.enableDepthTest();
        drawSignalPin(front, new float[]{2.5f, 2.5f, 2.5f, 2.5f});
        front.glRotateOnX();

        descriptor.draw(interpolator.get());
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.step(deltaT);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            switchState = stream.readBoolean();
            defaultOutput = stream.readBoolean();

            interpolator.to(switchState);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot) {
            interpolator.snap();
        }
        boot = false;
    }

    public void clientToogleDefaultOutput() {
        clientSend(ElectricalRelayElement.toogleOutputDefaultId);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if (lrdu == front) return ElnDeviceRegistry.signalCableDescriptor.render;
        if (lrdu == front.left() || lrdu == front.right()) return descriptor.cable.render;
        return null;
    }
}
