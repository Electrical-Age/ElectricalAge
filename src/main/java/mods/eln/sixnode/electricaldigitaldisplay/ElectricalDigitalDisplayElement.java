package mods.eln.sixnode.electricaldigitaldisplay;

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

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalDigitalDisplayElement extends SixNodeElement {
    ElectricalDigitalDisplayDescriptor descriptor;

    public ElectricalDigitalDisplayProcess process = new ElectricalDigitalDisplayProcess(this);

    public NbtElectricalGateInput input, strobeIn;
    public float current = 0.0f;
    public float last = 0.0f;
    public float min = 0.0f;
    public float max = 1000.0f;
    public float strobe = 0.0f;
    public float strobeLast = 0.0f;

    public ElectricalDigitalDisplayElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        input = new NbtElectricalGateInput("input");
        strobeIn = new NbtElectricalGateInput("strobe");
        electricalLoadList.add(input);
        electricalLoadList.add(strobeIn);
        slowProcessList.add(process);
        this.descriptor = (ElectricalDigitalDisplayDescriptor) descriptor;
    }

    @Override
    public void initialize() {}

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if(lrdu == front) return strobeIn;
        if(lrdu == front.inverse()) return input;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if(lrdu == front.inverse() || lrdu == front) return NodeBase.maskElectricalInputGate;
        return 0;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        Utils.println("EDDE.nU");
        try {
            switch(stream.readByte()) {
                case ElectricalDigitalDisplayDescriptor.netSetRange:
                    min = stream.readFloat();
                    max = stream.readFloat();
                    Utils.println(String.format("EDDE.nu: nSR %f - %f", min, max));
                    needPublish();
                    break;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(current);
            stream.writeFloat(min);
            stream.writeFloat(max);
            stream.writeBoolean(strobe >= 0.5);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setFloat("current", current);
        nbt.setFloat("min", min);
        nbt.setFloat("max", max);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        current = nbt.getFloat("current");
        min = nbt.getFloat("min");
        max = nbt.getFloat("max");
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        HashMap<String, String> info = new HashMap<>();
        info.put("Input: ", Utils.plotVolt(input.getU()));
        info.put("Min: ", String.format("%.2f", min));
        info.put("Max: ", String.format("%.2f", max));
        return info;
    }
}
