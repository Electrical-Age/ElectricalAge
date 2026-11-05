package mods.eln.sixnode.busdevice;

import mods.eln.bus.BusPort;
import mods.eln.bus.Packet;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small device on the RS485 bus with its own address (Baughn 2026-11-05: addresses are set in the device's screen,
 * 17..255): a bus pin on its front edge in a voxel block (none in a plain block), packets for it taken once a tick
 * (handle), its address saved as "addr".
 */
public abstract class BusDeviceElement extends SixNodeElement {
    /** Its bus pin, the same for the element's life (VoxelNode links it to the bus cable it touches). */
    public final BusPort port = new BusPort();

    /** Screen packet: the address (an int; anything but 17..255: none). */
    public static final byte setAddressId = 1;

    protected BusDeviceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        port.listen(true);
        slowProcessList.add(dt -> {
            for (Packet p; (p = port.poll()) != null; ) handle(p);
            tick();
        });
    }

    /** A packet for it (its address's, or a broadcast). */
    protected abstract void handle(Packet p);

    /** Once a tick, after its packets. */
    protected void tick() {
    }

    public int address() {
        return port.address();
    }

    public void setAddress(int a) {
        int was = port.address();
        port.setAddress(a);
        if (port.address() != was) needPublish();
    }

    /** Send a packet from its address; false: on no bus. */
    protected boolean send(Packet p) {
        return port.send(p);
    }

    boolean inVoxelBlock() {
        return sixNode instanceof mods.eln.node.voxel.VoxelNode;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return lrdu == front && inVoxelBlock() ? NodeBase.maskRs485 : 0;
    }

    @Override
    public BusPort busPort(LRDU lrdu) {
        return lrdu == front ? port : null;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        port.setAddress(nbt.getIntOr("addr", 0));
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (port.address() != 0) nbt.putInt("addr", port.address());
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(port.address());
            stream.writeBoolean(port.bus() != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            if (stream.readByte() == setAddressId) setAddress(stream.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put(I18N.tr("Address"), port.address() == 0 ? I18N.tr("none (set it in its screen)") : String.valueOf(port.address()));
        info.put(I18N.tr("Bus"), port.bus() == null ? I18N.tr("no bus") : I18N.tr("on a bus"));
        return info;
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Override
    public String thermoMeterString() {
        return "";
    }
}
