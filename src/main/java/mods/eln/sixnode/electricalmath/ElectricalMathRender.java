package mods.eln.sixnode.electricalmath;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalMathRender extends SixNodeElementRender {

    ElectricalMathDescriptor descriptor;
    Coordonate coord;


    String expression;

    public int chipsRequired;
    /** Modules on the rails (the chips inserted, in cheap chips, up to 18). */
    int modules;
    public boolean equationIsValid;
    /** Why it doesn't compile, or "". */
    public String error = "";

    /** The logger's graph (A, B, C, out), kept while watched: the screen is open or the processor within WATCH_RANGE. */
    public mods.eln.sixnode.electricaldatalogger.DataLogs log = named(new mods.eln.sixnode.electricaldatalogger.DataLogs(4));
    public final mods.eln.sixnode.electricaldatalogger.LogClient logClient = new mods.eln.sixnode.electricaldatalogger.LogClient(this,
        ElectricalMathElement.LOG_HEADER);
    /** The tiny screen on top can be read from this near (blocks); further, nothing of the logger is sent. */
    public static final double WATCH_RANGE = 6;

    static mods.eln.sixnode.electricaldatalogger.DataLogs named(mods.eln.sixnode.electricaldatalogger.DataLogs d) {
        String[] names = {"A", "B", "C", "out"};
        for (int c = 0; c < Math.min(4, d.channelCount()); c++) d.channels[c].name = names[c];
        return d;
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) throws IOException {
        if (stream.readByte() == ElectricalMathElement.LOG_HEADER) log = named(mods.eln.sixnode.electricaldatalogger.Logger.receive(stream, log));
    }

    float ledTime = 0f;
    boolean[] ledOn = new boolean[8];

    public ElectricalMathRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalMathDescriptor) descriptor;
        coord = new Coordonate(tileEntity);
        ledOn[0] = true;
        ledOn[4] = true;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            expression = stream.readUTF();
            chipsRequired = stream.readInt();
            equationIsValid = stream.readBoolean();
            error = stream.readUTF();
            modules = stream.readByte();
            log = named(mods.eln.sixnode.electricaldatalogger.Logger.readPublish(stream, log));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw() {
        super.draw();

        float[] pinDistances = null;
        if (side.isY()) {
            pinDistances = front.rotate4PinDistances(descriptor.pinDistance);
        } else {
            pinDistances = descriptor.pinDistance;
        }

        if (UtilsClient.distanceFromClientPlayer(tileEntity) < 15) {
            GL11.glColor3f(0, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front, pinDistances, 1.8f, 1.35f);
            // A red (right), B blue (back), C green (left), as in the editor (Baughn 2026-10-30)
            GL11.glColor3f(1, 0, 0);
            UtilsClient.drawConnectionPinSixNode(front.right(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(0, 0.3f, 1);
            UtilsClient.drawConnectionPinSixNode(front.inverse(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(0, 1, 0);
            UtilsClient.drawConnectionPinSixNode(front.left(), pinDistances, 1.8f, 1.35f);
            GL11.glColor3f(1, 1, 1);
        }

        if (side.isY()) {
            front.left().glRotateOnX();
        }

        // the tiny logger screen only near enough to read (its data comes only then too)
        descriptor.draw(ledOn, UtilsClient.distanceFromClientPlayer(tileEntity) < WATCH_RANGE ? log : null, modules);
    }

    @Override
    public void refresh(float deltaT) {
        if (UtilsClient.distanceFromClientPlayer(tileEntity) < WATCH_RANGE) logClient.watch();
        ledTime += deltaT;

        if (ledTime > 0.4) {
            for (int idx = 1; idx <= 3; idx++) {
                ledOn[idx] = Math.random() < 0.3;
            }
            for (int idx = 5; idx <= 7; idx++) {
                ledOn[idx] = Math.random() < 0.3;
            }
            ledTime = 0;
        }

    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return ElnDeviceRegistry.signalCableDescriptor.render;
    }
}
