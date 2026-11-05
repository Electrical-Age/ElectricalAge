package mods.eln.robotics.computer;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** The computer (ComputerDescriptor): its cabinet and run lamp, and this client's copy of its screen (Scene). */
public class ComputerRender extends TransparentNodeElementRender {
    final ComputerDescriptor desc;
    public byte status;
    public int address;
    public boolean onBus;
    /** The screen as the server sent it; the screen class draws it and keeps what is only this player's (edits). */
    public final Scene scene = new Scene();
    /** Bumped when the scene changes, so the screen can see it. */
    public int sceneVersion;

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

    /** Send a screen protocol event (one message) to the computer. */
    public void screenEvent(Object... parts) {
        clientSendString(ComputerElement.SCREEN_EVENT_ID, Form.print(parts));
    }

    /** Protocol lines from the server, applied to this copy of the scene. */
    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            byte kind = stream.readByte();
            if (kind != ComputerElement.SCREEN_ID) return;
            int n = stream.readInt();
            if (n < 0 || n > 1 << 20) return;
            byte[] b = new byte[n];
            stream.readFully(b);
            for (String line : new String(b, StandardCharsets.UTF_8).split("\n")) {
                var m = Form.parse(line);
                if (m != null) scene.apply(m);
            }
            sceneVersion++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
