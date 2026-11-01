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
    /** Each terminal's channel, or BusChannel.UNUSED. */
    final int[] channels = new int[BusModuleElement.TERMINALS];
    final boolean[] out = new boolean[BusModuleElement.TERMINALS];
    boolean onBus;
    boolean activity;

    public BusModuleRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (BusModuleDescriptor) descriptor;
        java.util.Arrays.fill(channels, mods.eln.bus.BusChannel.UNUSED);
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
        boolean voxel = inVoxelBlock();
        if (voxel) drawBusPin(front, 0, from);   // (a plain block has no bus cable to meet)
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
        descriptor.draw(onBus, activity, false);
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

    /** Client: ask the server to tune terminal i to channel c (BusChannel.UNUSED: unused). */
    void sendChannel(int i, int c) {
        try {
            var bos = new java.io.ByteArrayOutputStream();
            var stream = new java.io.DataOutputStream(bos);
            preparePacketForServer(stream);
            stream.writeByte(BusModuleElement.setChannelId);
            stream.writeByte(i);
            stream.writeByte(c);
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
                channels[i] = mods.eln.bus.BusChannel.orUnused(stream.readByte());
                out[i] = stream.readBoolean();
            }
            onBus = stream.readBoolean();
            activity = stream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
