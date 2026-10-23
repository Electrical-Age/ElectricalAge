package mods.eln.sixnode.thermalsensor;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThermalSensorElement extends SixNodeElement {

    public ThermalSensorDescriptor descriptor;
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    public NbtElectricalLoad outputGate = new NbtElectricalLoad("outputGate");

    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    public ThermalSensorProcess slowProcess = new ThermalSensorProcess(this);

    /**
     * Thermal probe: 1.12's cable slot, removed (Baughn 2026-10-17: built-in Copper Thermal Cable); kept, hidden, so a
     * cable left in it by an older world stays in the saved data and drops when the probe is broken.
     * Temperature probe: an optional mode slot. Empty or an electrical cable: the back joins electrical cables (an LV
     * cable, or the one in the slot); a thermal cable: the back joins thermal cables. One probe can't do both: the
     * simulator only joins slow (electrical cable) thermal loads to slow ones and fast (thermal cable) to fast.
     */
    AutoAcceptInventoryProxy inventory;

    static final byte powerType = 0, temperatureType = 1;
    int typeOfSensor = temperatureType;
    float lowValue = 0, highValue = 50;

    public static final byte setTypeOfSensorId = 1;
    public static final byte setValueId = 2;

    public ThermalSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        thermalLoadList.add(thermalLoad);
        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        slowProcessList.add(slowProcess);

        this.descriptor = (ThermalSensorDescriptor) descriptor;

        if (this.descriptor.temperatureOnly) {
            inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
                .acceptIfEmpty(0, ThermalCableDescriptor.class, ElectricalCableDescriptor.class);
        } else {
            inventory = new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this));
        }
    }

    public Container getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByteOr("front", (byte) 0);
        front = LRDU.fromInt((value >> 0) & 0x3);
        typeOfSensor = nbt.getByteOr("typeOfSensor", (byte) 0);
        lowValue = nbt.getFloatOr("lowValue", 0f);
        highValue = nbt.getFloatOr("highValue", 0f);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("front", (byte) ((front.toInt() << 0)));
        nbt.putByte("typeOfSensor", (byte) typeOfSensor);
        nbt.putFloat("lowValue", lowValue);
        nbt.putFloat("highValue", highValue);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return outputGate;

        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            if (front.left() == lrdu) return thermalLoad;
            if (front.right() == lrdu) return thermalLoad;
        } else {
            if (front.inverse() == lrdu) return thermalLoad;
        }
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            if (front.left() == lrdu) return NodeBase.maskThermal;
            if (front.right() == lrdu) return NodeBase.maskThermal;
            if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        } else {
            if (isItemThermalCable()) {
                if (front.inverse() == lrdu) return NodeBase.maskThermal;
            } else {
                if (front.inverse() == lrdu) return NodeBase.maskElectricalAll;
            }
            if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        }
        return 0;
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        if (Eln.wailaEasyMode) {
            switch (typeOfSensor) {
                case temperatureType:
                    info.put(I18N.tr("Measured temperature"), Utils.plotCelsius("", thermalLoad.getT()));
                    break;

                case powerType:
                    info.put(I18N.tr("Measured thermal power"), Utils.plotPower("", thermalLoad.getPower()));
                    break;
            }
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T :", thermalLoad.Tc);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte((front.toInt() << 4) + typeOfSensor);
            stream.writeFloat(lowValue);
            stream.writeFloat(highValue);
            Utils.serialiseItemStack(stream, getInventory().getItem(ThermalSensorContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        ElnDeviceRegistry.signalCableDescriptor.applyTo(outputGate);
        computeElectricalLoad();
    }

    @Override
    protected void inventoryChanged() {
        sixNode.disconnect();
        computeElectricalLoad();
        sixNode.connect();
    }

    public void computeElectricalLoad() {
        if (!this.descriptor.temperatureOnly) {
            // built-in Copper Thermal Cable (1.12: a thermal cable in the slot)
            ElnDeviceRegistry.copperThermalCableDescriptor.setThermalLoad(thermalLoad);
            thermalLoad.setAsFast();
            return;
        }
        ItemStack cable = getInventory().getItem(ThermalSensorContainer.cableSlotId);
        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(cable);
        if (descriptor != null && descriptor.getClass() == ThermalCableDescriptor.class) {
            ((ThermalCableDescriptor) descriptor).setThermalLoad(thermalLoad);
            thermalLoad.setAsFast();
        } else {
            ElectricalCableDescriptor cableDescriptor = descriptor instanceof ElectricalCableDescriptor e ? e
                : ElnDeviceRegistry.lowVoltageCableDescriptor; // empty slot: as an LV cable (Baughn 2026-10-17)
            cableDescriptor.applyTo(thermalLoad);
            thermalLoad.Rp = 1000000000.0;
            thermalLoad.setAsSlow();
        }
    }

    boolean isItemThermalCable() {
        if (!descriptor.temperatureOnly) return false; // the thermal probe's slot is gone
        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(getInventory().getItem(ThermalSensorContainer.cableSlotId));
        return descriptor != null && descriptor.getClass() == ThermalCableDescriptor.class;
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        ItemStack currentItemStack = entityPlayer.getMainHandItem();

        if (mods.eln.node.MeterItems.isMultiMeter(currentItemStack)) {
            return false;
        }
        if (mods.eln.node.MeterItems.isThermometer(currentItemStack)) {
            return false;
        }
        if (mods.eln.node.MeterItems.isAllMeter(currentItemStack)) {
            return false;
        }
        return inventory.take(currentItemStack, this, false, true);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setTypeOfSensorId:
                    typeOfSensor = stream.readByte();
                    needPublish();
                    break;
                case setValueId:
                    lowValue = stream.readFloat();
                    highValue = stream.readFloat();
                    if (lowValue == highValue) highValue += 0.0001;
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
