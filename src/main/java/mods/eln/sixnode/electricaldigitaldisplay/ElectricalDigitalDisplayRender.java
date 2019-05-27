package mods.eln.sixnode.electricaldigitaldisplay;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

import static mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayDescriptor.Style.LED;

public class ElectricalDigitalDisplayRender extends SixNodeElementRender {
    ElectricalDigitalDisplayDescriptor descriptor;

    public float current = 0.0f;
    public float max = 1000.0f;
    public float min = 0.0f;
    public float dots = 0.0f;
    public boolean strobe = false;
    public ElectricalDigitalDisplayDescriptor.Style style = LED;
    public int dye;

    public ElectricalDigitalDisplayRender(SixNodeEntity entity, Direction side, SixNodeDescriptor descriptor) {
        super(entity, side, descriptor);
        this.descriptor = (ElectricalDigitalDisplayDescriptor) descriptor;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Eln.instance.stdCableRenderSignal;
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front.inverse(), descriptor.pinDistance);
        descriptor.draw((int) (min + current * (max - min)), strobe, style, dye, (int) (dots * descriptor.DOT_STATES));
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            current = stream.readFloat();
            min = stream.readFloat();
            max = stream.readFloat();
            dots = stream.readFloat();
            strobe = stream.readBoolean();
            dye = stream.readByte();
            //Utils.println(String.format("EDDR values %f (%f - %f)", current, min, max));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ElectricalDigitalDisplayGui(this);
    }
}
