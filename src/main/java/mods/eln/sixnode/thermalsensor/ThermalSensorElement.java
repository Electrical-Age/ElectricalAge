package mods.eln.sixnode.thermalsensor;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.init.Cable;
import mods.eln.init.Config;
import mods.eln.init.Items;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
            inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
                .acceptIfEmpty(0, ThermalCableDescriptor.class);
        }
    }

    public IInventory getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        typeOfSensor = nbt.getByte("typeOfSensor");
        lowValue = nbt.getFloat("lowValue");
        highValue = nbt.getFloat("highValue");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) ((front.toInt() << 0)));
        nbt.setByte("typeOfSensor", (byte) typeOfSensor);
        nbt.setFloat("lowValue", lowValue);
        nbt.setFloat("highValue", highValue);
        return nbt;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return outputGate;

        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            if (getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId) != null) {
                if (front.left() == lrdu) return thermalLoad;
                if (front.right() == lrdu) return thermalLoad;
            }
        } else {
            if (front.inverse() == lrdu) return thermalLoad;
        }
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (!descriptor.temperatureOnly) {
            if (getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId) != null) {
                if (front.left() == lrdu) return NodeBase.MASK_THERMAL;
                if (front.right() == lrdu) return NodeBase.MASK_THERMAL;
            }
            if (front == lrdu) return NodeBase.MASK_ELECTRICAL_OUTPUT_GATE;
        } else {
            if (isItemThermalCable()) {
                if (front.inverse() == lrdu) return NodeBase.MASK_THERMAL;
            } else if (isItemElectricalCable()) {
                if (front.inverse() == lrdu) return NodeBase.MASK_ELECTRICAL_ALL;
            }
            if (front == lrdu) return NodeBase.MASK_ELECTRICAL_OUTPUT_GATE;
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
        if (Config.INSTANCE.getWailaEasyMode()) {
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
            Utils.serialiseItemStack(stream, getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Cable.Companion.getSignal().descriptor.applyTo(outputGate);
        computeElectricalLoad();
    }

    @Override
    protected void inventoryChanged() {
        sixNode.disconnect();
        computeElectricalLoad();
        sixNode.connect();
    }

    public void computeElectricalLoad() {
        ItemStack cable = getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId);

        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(cable);
        if (descriptor == null) return;
        if (descriptor.getClass() == ThermalCableDescriptor.class) {
            ThermalCableDescriptor cableDescriptor = (ThermalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
            cableDescriptor.setThermalLoad(thermalLoad);
            thermalLoad.setAsFast();
        } else if (descriptor.getClass() == ElectricalCableDescriptor.class) {
            ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);
            cableDescriptor.applyTo(thermalLoad);
            thermalLoad.Rp = 1000000000.0;
            thermalLoad.setAsSlow();
        } else {
            thermalLoad.setHighImpedance();
        }
    }

    boolean isItemThermalCable() {
        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId));
        return descriptor != null && descriptor.getClass() == ThermalCableDescriptor.class;
    }

    boolean isItemElectricalCable() {
        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(getInventory().getStackInSlot(ThermalSensorContainer.cableSlotId));
        return descriptor != null && descriptor.getClass() == ElectricalCableDescriptor.class;
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        ItemStack currentItemStack = entityPlayer.getHeldItemMainhand();

        if (Items.multiMeterElement.checkSameItemStack(currentItemStack)) {
            return false;
        }
        if (Items.thermometerElement.checkSameItemStack(currentItemStack)) {
            return false;
        }
        if (Items.allMeterElement.checkSameItemStack(currentItemStack)) {
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

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ThermalSensorContainer(player, inventory.getInventory(), descriptor.temperatureOnly);
    }
}
