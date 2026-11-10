package mods.eln.sixnode.electricalbreaker;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.network.ClientValues;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalBreakerElement extends SixNodeElement {

    public ElectricalBreakerDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public Resistor switchResistor = new Resistor(aLoad, bLoad);
    public ElectricalBreakerCutProcess cutProcess = new ElectricalBreakerCutProcess(this);

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public float voltageMax = (float) Eln.LVU, voltageMin = 0;   // (1.12: SVU, which was also its 50 V tier; the 48 V tier now)

    boolean switchState = false;
    double currantMax = 0;
    /**
     * The current it trips at, A (Baughn 2026-11-06: a UI, for cables of any cross-section); 0: its cable's, 1.05 x
     * that cable's nominal current, as before.
     */
    public float tripAmps = 0;
    boolean nbtBoot = false;

    public ElectricalCableDescriptor cableDescriptor = null;

    public static final byte setVoltageMaxId = 1;
    public static final byte setVoltageMinId = 2;
    public static final byte toogleSwitchId = 3;
    public static final byte setTripAmpsId = 4;

    /** Its cable's own trip current, A (1.05 x nominal), or 0 without a cable. */
    public double cableTripAmps() {
        return cableDescriptor == null ? 0 : 1.05 * cableDescriptor.electricalNominalPower / cableDescriptor.electricalNominalVoltage;
    }

    /** The current it trips at now, A. */
    public double tripAmpsNow() {
        return tripAmps > 0 ? tripAmps : cableTripAmps();
    }

    public ElectricalBreakerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        electricalComponentList.add(switchResistor);
        electricalComponentList.add(new Resistor(bLoad, null).pullDown());
        electricalComponentList.add(new Resistor(aLoad, null).pullDown());

        electricalProcessList.add(cutProcess);

        this.descriptor = (ElectricalBreakerDescriptor) descriptor;
    }

    public SixNodeElementInventory getInventory() {
        return inventory;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByteOr("front", (byte) 0);
        front = LRDU.fromInt((value >> 0) & 0x3);
        switchState = nbt.getBooleanOr("switchState", false);
        voltageMax = ClientValues.finite(nbt.getFloatOr("voltageMax", 0f), (float) Eln.LVU);
        voltageMin = ClientValues.finite(nbt.getFloatOr("voltageMin", 0f), 0f);
        tripAmps = nbt.getFloatOr("tripAmps", 0f);
        nbtBoot = true;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("front", (byte) (front.toInt() << 0));
        nbt.putBoolean("switchState", switchState);
        nbt.putFloat("voltageMax", voltageMax);
        nbt.putFloat("voltageMin", voltageMin);
        nbt.putFloat("tripAmps", tripAmps);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return aLoad;
        if (front.inverse() == lrdu) return bLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (Utils.isEmpty(inventory.getItem(ElectricalBreakerContainer.cableSlotId))) return 0;
        if (front == lrdu) return NodeBase.maskElectricalAll;
        if (front.inverse() == lrdu) return NodeBase.maskElectricalAll;

        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Ua:", aLoad.getU()) + Utils.plotVolt("Ub:", bLoad.getU()) + Utils.plotAmpere("I:", aLoad.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Contact"), switchState ? I18N.tr("Closed") : I18N.tr("Open"));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", aLoad.getCurrent()));
        info.put(I18N.tr("Trips at"), Utils.plotAmpere("", tripAmpsNow()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Voltages"), Utils.plotVolt("", aLoad.getU()) + Utils.plotVolt(" ", bLoad.getU()));
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
            stream.writeBoolean(switchState);
            stream.writeFloat(voltageMax);
            stream.writeFloat(voltageMin);
            stream.writeFloat((float) tripAmpsNow());

            Utils.serialiseItemStack(stream, inventory.getItem(ElectricalBreakerContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSwitchState(boolean state) {
        if (state == switchState) return;
        play(new SoundCommand("random.click").mulVolume(0.3F, 0.6f).smallRange());
        switchState = state;
        refreshSwitchResistor();
        needPublish();
    }

    public void refreshSwitchResistor() {
        ItemStack cable = inventory.getItem(ElectricalBreakerContainer.cableSlotId);
        ElectricalCableDescriptor cableDescriptor = mods.eln.node.voxel.CableItems.descriptor(cable);
        if (cableDescriptor == null || !switchState) {
            switchResistor.ultraImpedance();
        } else {
            cableDescriptor.applyTo(switchResistor);
        }
    }

    public boolean getSwitchState() {
        return switchState;
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
        setSwitchState(switchState);
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
        reconnect();
    }

    public void computeElectricalLoad() {
        ItemStack cable = inventory.getItem(ElectricalBreakerContainer.cableSlotId);

        if (!nbtBoot) setSwitchState(false);
        nbtBoot = false;

        cableDescriptor = mods.eln.node.voxel.CableItems.descriptor(cable);
        if (cableDescriptor == null) {
            aLoad.highImpedance();
            bLoad.highImpedance();
        } else {
            cableDescriptor.applyTo(aLoad);
            cableDescriptor.applyTo(bLoad);
            currantMax = cableDescriptor.electricalMaximalCurrent;
        }
        refreshSwitchResistor();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setVoltageMaxId:
                    voltageMax = ClientValues.finite(stream.readFloat(), voltageMax);   // (typed: any finite volts)
                    needPublish();
                    break;
                case setVoltageMinId:
                    voltageMin = ClientValues.finite(stream.readFloat(), voltageMin);
                    needPublish();
                    break;
                case toogleSwitchId:
                    setSwitchState(!getSwitchState());
                    needPublish();
                    break;
                case setTripAmpsId:
                    float a = stream.readFloat();
                    tripAmps = Float.isFinite(a) && a > 0 ? Math.min(a, 100000f) : 0;   // (0 or less: its cable's)
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
