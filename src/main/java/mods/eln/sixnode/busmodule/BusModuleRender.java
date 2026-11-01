package mods.eln.sixnode.busmodule;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.gl.GL11;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.registry.ElnDeviceRegistry;

import java.io.DataInputStream;
import java.io.IOException;

/** The Bus I/O Module on the client: its model, a violet bus pin and its nine signal pins, and what its screen shows. */
public class BusModuleRender extends SixNodeElementRender {
    final BusModuleDescriptor descriptor;
    final String[] names = new String[BusModuleElement.TERMINALS];
    final boolean[] out = new boolean[BusModuleElement.TERMINALS];
    /** BusModuleElement.busState: 0 no bus, 1 on a bus, 2 on a bus past its 64 channels. */
    int busState;
    boolean activity;
    String message = "";

    public BusModuleRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (BusModuleDescriptor) descriptor;
        java.util.Arrays.fill(names, "");
    }

    boolean inVoxelBlock() {
        return tileEntity instanceof mods.eln.node.voxel.VoxelNodeEntity;
    }

    /** As BusModuleElement.edgeOf. */
    LRDU edgeOf(int i) {
        return switch (i / 3) {
            case 0 -> front.left();
            case 1 -> front.inverse();
            default -> front.right();
        };
    }

    @Override
    public void draw() {
        super.draw();
        if (front == null) return;
        // the pins, from the box's side (5 px out) to the block's edge, where the cables meet them
        float from = 5 / 16f, to = 8 / 16f;
        pinColour(front);
        drawPinBox(front, from, to, 2.8f / 16, 1 / 16f);
        boolean voxel = inVoxelBlock();
        for (int i = 0; i < BusModuleElement.TERMINALS; i++) {
            int o = BusModuleElement.PIN_OFFSETS[i % 3];
            if (!voxel && o != 0) continue;   // (a plain block's edge has one connection: the middle terminal's)
            LRDU l = edgeOf(i);
            // along the edge, toward l.left(), as VoxelNode places edge pins (element frame: Left/Right z, Down/Up y)
            LRDU a = l.left();
            float s = (a == LRDU.Left || a == LRDU.Down ? -1 : 1) * o / 16f;
            GL11.glPushMatrix();
            if (a == LRDU.Left || a == LRDU.Right) GL11.glTranslatef(0, 0, s);
            else GL11.glTranslatef(0, s, 0);
            pinColour(l);
            drawPinBox(l, from, to, 1.8f / 16, 1 / 16f);
            GL11.glPopMatrix();
        }
        GL11.glColor3f(1f, 1f, 1f);
        front.glRotateOnX();
        descriptor.draw(busState != 0, activity, busState == 2 || !message.isEmpty());
    }

    @Override
    protected boolean busPin(LRDU l) {
        return l == front;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return lrdu == front ? null : ElnDeviceRegistry.signalCableDescriptor.render;
    }

    /** No stubs in a voxel block: its pins reach the block's edge and meet the cables there. */
    @Override
    public boolean drawCableAuto() {
        return !inVoxelBlock();
    }

    /** Client: ask the server to set terminal i's channel name. */
    void sendName(int i, String name) {
        try {
            var bos = new java.io.ByteArrayOutputStream();
            var stream = new java.io.DataOutputStream(bos);
            preparePacketForServer(stream);
            stream.writeByte(BusModuleElement.setNameId);
            stream.writeByte(i);
            stream.writeUTF(name);
            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Client: ask the server to set terminal i's direction. */
    void sendOut(int i, boolean o) {
        try {
            var bos = new java.io.ByteArrayOutputStream();
            var stream = new java.io.DataOutputStream(bos);
            preparePacketForServer(stream);
            stream.writeByte(BusModuleElement.setOutId);
            stream.writeByte(i);
            stream.writeBoolean(o);
            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            for (int i = 0; i < BusModuleElement.TERMINALS; i++) {
                names[i] = stream.readUTF();
                out[i] = stream.readBoolean();
            }
            busState = stream.readByte();
            activity = stream.readBoolean();
            message = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
