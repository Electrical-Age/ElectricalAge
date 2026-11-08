package mods.eln.sixnode.electricalgatesource;

import mods.eln.misc.UtilsClient;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalGateSourceRender extends SixNodeElementRender {

    ElectricalGateSourceDescriptor descriptor;

    LRDU front;

    /** The lever or knob following the output (0..1 of the signal range), easing at the render's speed. */
    mods.eln.anim.Travel interpolator;

    float voltageSyncValue = 0;
    boolean voltageSyncNew = false;
    boolean boot = true;

    public ElectricalGateSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalGateSourceDescriptor) descriptor;
        interpolator = mods.eln.anim.Travel.smooth(this.descriptor.render.speed);
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front, new float[]{3, 3, 3, 3});

        if (side.isY()) {
            front.glRotateOnX();
        } else {
            LRDU.Down.glRotateOnX();
        }
        descriptor.draw(interpolator.get(), UtilsClient.distanceFromClientPlayer(this.tileEntity), tileEntity);
    }

    @Override
    public void refresh(float deltaT) {
        interpolator.target(voltageSyncValue / Eln.SVU);
        interpolator.step(deltaT);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            front = LRDU.fromInt((b >> 4) & 3);
            float readF;
            readF = stream.readFloat();
            if (voltageSyncValue != readF) {
                voltageSyncValue = readF;
                voltageSyncNew = true;
            }

            if (boot) {
                boot = false;
                interpolator.set(voltageSyncValue / Eln.SVU);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.signalCableDescriptor.render;
    }

}
