package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalRedstoneOutputElement extends SixNodeElement {

    public NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate");
    public ElectricalRedstoneOutputSlowProcess slowProcess = new ElectricalRedstoneOutputSlowProcess(this);

    int redstoneValue = 0;

    public ElectricalRedstoneOutputElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(inputGate);
        slowProcessList.add(slowProcess);
    }

    @Override
    public int isProvidingWeakPower() {
        return redstoneValue;
    }

    public boolean refreshRedstone() {
        int newValue = (int) (inputGate.getU() * 15.0 / Eln.SVU + 0.5);
        if (newValue != redstoneValue) {
            redstoneValue = newValue;
            notifyNeighbor();
            needPublish();
            return true;
        }
        return false;
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        redstoneValue = nbt.getInteger("redstoneValue");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setInteger("redstoneValue", redstoneValue);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu.left()) return inputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu.left()) return NodeBase.MASK_ELECTRICAL_INPUT_GATE;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", inputGate.getU()) + Utils.plotAmpere("I:", inputGate.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Redstone value"), Utils.plotValue(redstoneValue));
        info.put(I18N.tr("Input voltage"), Utils.plotVolt("", inputGate.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(redstoneValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
    }
}
