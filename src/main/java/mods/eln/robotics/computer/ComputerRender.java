package mods.eln.robotics.computer;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** The computer (ComputerDescriptor): its cabinet and run lamp, and its terminal's text for the screen. */
public class ComputerRender extends TransparentNodeElementRender {
    final ComputerDescriptor desc;
    public byte status;
    public int address;
    public boolean onBus;
    public final TerminalText terminal = new TerminalText();

    public ComputerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (ComputerDescriptor) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(status);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            status = stream.readByte();
            address = stream.readByte() & 0xFF;
            onBus = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Its state in words (the screen). */
    public String stateTextClient() {
        return switch (status) {
            case ComputerElement.RUNNING -> mods.eln.i18n.I18N.tr("Running");
            case ComputerElement.SLEEPING -> mods.eln.i18n.I18N.tr("Waiting");
            case ComputerElement.HALTED -> mods.eln.i18n.I18N.tr("Halted");
            case ComputerElement.NO_CHIPS -> mods.eln.i18n.I18N.tr("No chips");
            default -> mods.eln.i18n.I18N.tr("Off");
        };
    }

    /** Terminal output from the server, or the whole terminal again (the screen just opened, or a reset). */
    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            byte kind = stream.readByte();
            int n = stream.readInt();
            if (n < 0 || n > 1 << 16) return;
            byte[] b = new byte[n];
            stream.readFully(b);
            if (kind == ComputerElement.TERMINAL_ALL_ID) terminal.clear();
            terminal.append(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
