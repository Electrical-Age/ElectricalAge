package mods.eln.sixnode.wirelesssignal.repeater;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Map;

public class WirelessSignalRepeaterElement extends SixNodeElement {

    WirelessSignalRepeaterProcess slowProcess = new WirelessSignalRepeaterProcess(this);

    WirelessSignalRepeaterDescriptor descriptor;

    boolean fromNbt = false;

    public WirelessSignalRepeaterElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (WirelessSignalRepeaterDescriptor) descriptor;

        slowProcessList.add(slowProcess);

        IWirelessSignalSpot.spots.add(slowProcess);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return NodeBase.MASK_ELECTRICAL_OUTPUT_GATE;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        return null;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void globalBoot() {
        slowProcess.process(0.05);
    }

    @Override
    public void destroy(EntityPlayerMP entityPlayer) {
        unregister();
        super.destroy(entityPlayer);
    }

    @Override
    public void unload() {
        super.unload();
        unregister();
    }

    void unregister() {
        IWirelessSignalSpot.spots.remove(slowProcess);
    }

    @Override
    public void initialize() {
        if (!fromNbt) {
            slowProcess.process(0.05);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        fromNbt = true;
    }
}
