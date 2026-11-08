package mods.eln.sixnode.electricalswitch;

import mods.eln.misc.UtilsClient;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalSwitchRender extends SixNodeElementRender {

    ElectricalSwitchDescriptor descriptor;

    double voltageAnode = 0, voltageCatode = 0, current = 0, temperature = 0;

    /** The lever between its poses, easing at the descriptor's speed. */
    mods.eln.anim.Travel interpol;

    boolean boot = true;
    float switchAlpha = 0;
    boolean switchState;

    public ElectricalSwitchRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalSwitchDescriptor) descriptor;
        interpol = mods.eln.anim.Travel.smooth(this.descriptor.speed);
    }

    @Override
    public void draw() {
        super.draw();

        front.glRotateOnX();
        if (descriptor.signalSwitch) {
            drawSignalPin(LRDU.Left, descriptor.pinDistance);
            drawSignalPin(LRDU.Right, descriptor.pinDistance);
        } else {
            drawPowerPin(LRDU.Left, descriptor.pinDistance);
            drawPowerPin(LRDU.Right, descriptor.pinDistance);
        }
        descriptor.draw(interpol.get(), UtilsClient.distanceFromClientPlayer(tileEntity), tileEntity);
    }

    @Override
    public void refresh(float deltaT) {
        interpol.to(switchState);
        interpol.step(deltaT);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.cableRender;
    }

    @Override
    public void glListDraw() {
    }

    @Override
    public boolean glListEnable() {
        return false;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            switchState = stream.readBoolean();
            voltageAnode = stream.readShort() / NodeBase.networkSerializeUFactor;
            voltageCatode = stream.readShort() / NodeBase.networkSerializeUFactor;
            current = stream.readShort() / NodeBase.networkSerializeIFactor;
            temperature = stream.readShort() / NodeBase.networkSerializeTFactor;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot) {
            interpol.to(switchState).snap();
        }
        boot = false;
    }
}
