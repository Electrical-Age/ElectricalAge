package mods.eln.sixnode.mppt;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.registry.ElnDeviceRegistry;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The MPPT charge controller's model, its display lit while it converts, and its terminals capped by polarity: + red
 * on the front and back edges, - blue on the sides (which side is which: MpptElement.getElectricalLoad).
 */
public class MpptRender extends SixNodeElementRender {
    final MpptDescriptor descriptor;
    float chargeVolts, chargeAmps, uIn, iIn, uOut, iOut;
    int state;
    boolean flowing;
    boolean chargeVoltsNew;

    static final float[] PLUS = {0.85f, 0.12f, 0.1f}, MINUS = {0.12f, 0.25f, 0.85f};

    public MpptRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (MpptDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        if (front != null) {
            // the model's brass terminals: 1.3 voxels in from the edge, 3 wide, 1.6 high (mods.eln.cad.models.MpptController)
            float from = 6.7f / 16, to = 8f / 16, w = 3f / 16, top = 1.6f / 16;
            drawPinMark(front, from, to, w, top, PLUS);
            drawPinMark(front.right(), from, to, w, top, MINUS);
            drawPinMark(front.inverse(), from, to, w, top, PLUS);
            drawPinMark(front.left(), from, to, w, top, MINUS);
            if (side.isY()) front.left().glRotateOnX();
        }
        descriptor.draw(flowing);   // (lit while converting)
    }

    /** No cable stubs across the block: the terminals reach its edge and meet the cables there. */
    @Override
    public boolean drawCableAuto() {
        return false;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.meduimVoltageCableDescriptor.render;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            float cv = stream.readFloat();
            if (cv != chargeVolts) {
                chargeVolts = cv;
                chargeVoltsNew = true;
            }
            chargeAmps = stream.readFloat();
            uIn = stream.readFloat();
            iIn = stream.readFloat();
            uOut = stream.readFloat();
            iOut = stream.readFloat();
            state = stream.readByte();
            flowing = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
