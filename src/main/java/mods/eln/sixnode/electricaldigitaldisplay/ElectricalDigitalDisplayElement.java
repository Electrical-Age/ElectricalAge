package mods.eln.sixnode.electricaldigitaldisplay;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.IConfigurable;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalDigitalDisplayElement extends SixNodeElement implements IConfigurable {
    ElectricalDigitalDisplayDescriptor descriptor;

    public ElectricalDigitalDisplayProcess process = new ElectricalDigitalDisplayProcess(this);

    public NbtElectricalGateInput input, strobeIn, dotsIn;
    public float current = 0.0f;
    public float last = 0.0f;
    public float min = 0.0f;
    public float max = 1000.0f;
    public float strobe = 0.0f;
    public float strobeLast = 0.0f;
    public float dots = 0.0f;
    public float dotsLast = 0.0f;
    public int dye = 5;

    public ElectricalDigitalDisplayElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        input = new NbtElectricalGateInput("input");
        strobeIn = new NbtElectricalGateInput("strobe");
        dotsIn = new NbtElectricalGateInput("dots");
        electricalLoadList.add(input);
        electricalLoadList.add(strobeIn);
        electricalLoadList.add(dotsIn);
        slowProcessList.add(process);
        this.descriptor = (ElectricalDigitalDisplayDescriptor) descriptor;
    }

    @Override
    public void initialize() {}

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if(lrdu == front) return dotsIn;
        if(lrdu == front.inverse()) return input;
        return strobeIn;
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
        if(lrdu == front.inverse()) return NodeBase.MASK_ELECTRICAL_INPUT_GATE;
        if(lrdu == front) return NodeBase.MASK_ELECTRICAL_INPUT_GATE | (11 << NodeBase.MASK_COLOR_SHIFT);
        return NodeBase.MASK_ELECTRICAL_INPUT_GATE | (1 << NodeBase.MASK_COLOR_SHIFT);
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        Eln.dp.println(DebugType.SIX_NODE, "EDDE.nU");
        try {
            switch(stream.readByte()) {
                case ElectricalDigitalDisplayDescriptor.netSetRange:
                    min = stream.readFloat();
                    max = stream.readFloat();
                    Eln.dp.println(DebugType.SIX_NODE, String.format("EDDE.nu: nSR %f - %f", min, max));
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
            stream.writeFloat(dots);
            stream.writeBoolean(strobe >= 0.5);
            stream.writeByte(dye);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack stack = entityPlayer.getCurrentEquippedItem();
        if(stack != null) {
            GenericItemUsingDamageDescriptor desc = BrushDescriptor.getDescriptor(stack);
            if(desc != null && desc instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) desc;
                int color = brush.getColor(stack);
                if(color != dye && brush.use(stack, entityPlayer)) {
                    dye = color;
                    needPublish();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setFloat("current", current);
        nbt.setFloat("min", min);
        nbt.setFloat("max", max);
        nbt.setByte("dye", (byte) dye);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        current = nbt.getFloat("current");
        min = nbt.getFloat("min");
        max = nbt.getFloat("max");
        dye = nbt.getByte("dye");
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

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("min"))
            min = compound.getFloat("min");
        if(compound.hasKey("max"))
            max = compound.getFloat("max");
        needPublish();
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setFloat("min", min);
        compound.setFloat("max", max);
    }
}
