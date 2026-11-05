package mods.eln.sixnode.busdevice;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/** A bus device on the client: its violet bus pin (in a voxel block), and the address and bus its screen shows. */
public abstract class BusDeviceRender extends SixNodeElementRender {
    int address;
    boolean onBus;

    protected BusDeviceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
    }

    boolean inVoxelBlock() {
        return tileEntity instanceof mods.eln.node.voxel.VoxelNodeEntity;
    }

    /** The bus pin, from the body (from, off the centre) to the block's edge. */
    protected void drawPin(float from) {
        if (front != null && inVoxelBlock()) drawBusPin(front, 0, from);
    }

    @Override
    protected boolean busPin(LRDU l) {
        return l == front;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return null;
    }

    @Override
    public boolean drawCableAuto() {
        return !inVoxelBlock();
    }

    /** Client: ask the server to take this address. */
    void sendAddress(int a) {
        clientSetInt(BusDeviceElement.setAddressId, a);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            address = stream.readByte() & 0xFF;
            onBus = stream.readBoolean();
            unserializeMore(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** The device's own published fields, after the address and bus. */
    protected abstract void unserializeMore(DataInputStream stream) throws IOException;
}
