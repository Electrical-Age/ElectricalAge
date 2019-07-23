package mods.eln.sixnode.electricaltimeout;

import mods.eln.i18n.I18N;
import mods.eln.init.Config;
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
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalTimeoutElement extends SixNodeElement {

    public ElectricalTimeoutDescriptor descriptor;

    public NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate");
    public NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);

    public ElectricalTimeoutProcess slowProcess = new ElectricalTimeoutProcess(this);

    double timeOutCounter = 0, timeOutValue = 2;

    public static final byte resetId = 1;
    public static final byte setTimeOutValueId = 2;
    public static final byte setId = 3;

    public ElectricalTimeoutElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(inputGate);
        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        thermalProcessList.add(slowProcess);

        this.descriptor = (ElectricalTimeoutDescriptor) descriptor;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        timeOutValue = nbt.getFloat("timeOutValue");
        timeOutCounter = nbt.getFloat("timeOutCounter");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setFloat("timeOutValue", (float) timeOutValue);
        nbt.setFloat("timeOutCounter", (float) timeOutCounter);
        return nbt;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return inputGate;
        if (front.inverse() == lrdu) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return NodeBase.MASK_ELECTRICAL_INPUT_GATE;
        if (front.inverse() == lrdu) return NodeBase.MASK_ELECTRICAL_OUTPUT_GATE;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return inputGate.plot("Input:") + outputGate.plot("Output:");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Input"), inputGate.stateHigh() ? I18N.tr("ON") : I18N.tr("OFF"));
        info.put(I18N.tr("Output"), timeOutCounter > 0 ? I18N.tr("ON") : I18N.tr("OFF"));
        if (Config.INSTANCE.getWailaEasyMode()) {
            info.put(I18N.tr("Remaining"), Utils.plotValue(timeOutCounter, "s"));
        }
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
            stream.writeFloat((float) timeOutValue);
            stream.writeFloat((float) timeOutCounter);
            stream.writeBoolean(slowProcess.inputState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
    }

    void set() {
        timeOutCounter = timeOutValue;
        needPublish();
    }

    void reset() {
        timeOutCounter = 0.0;
        needPublish();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case resetId:
                    reset();
                    break;
                case setId:
                    set();
                    break;
                case setTimeOutValueId:
                    timeOutValue = stream.readFloat();
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }
}
