package mods.eln.sixnode.electricalfiredetector;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.electricalitem.BatteryItem;
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
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mods.eln.i18n.I18N.tr;

public class ElectricalFireDetectorElement extends SixNodeElement {

    ElectricalFireDetectorDescriptor descriptor;

    public NbtElectricalGateOutput outputGate;
    public NbtElectricalGateOutputProcess outputGateProcess;
    public ElectricalFireDetectorSlowProcess slowProcess;

    public boolean powered;
    public boolean firePresent = false;

    private AutoAcceptInventoryProxy inventory;

    public ElectricalFireDetectorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (ElectricalFireDetectorDescriptor) descriptor;

        slowProcess = new ElectricalFireDetectorSlowProcess(this);

        if (!this.descriptor.batteryPowered) {
            powered = true;
            outputGate = new NbtElectricalGateOutput("outputGate");
            outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
            electricalLoadList.add(outputGate);
            electricalComponentList.add(outputGateProcess);
        } else {
            powered = false;
            inventory = new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this))
                .acceptIfEmpty(0, BatteryItem.class);
        }

        slowProcessList.add(slowProcess);
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (!descriptor.batteryPowered && front == lrdu.left()) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (!descriptor.batteryPowered && front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        if (descriptor.batteryPowered) {
            return tr("Fire detected: ") + firePresent;
        } else {
            return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
        }
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Fire present"), firePresent ? I18N.tr("Yes") : I18N.tr("No"));
        if (Eln.wailaEasyMode && !descriptor.batteryPowered) {
            info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        }
        if (descriptor.batteryPowered) {
            info.put(I18N.tr("Battery level"), Utils.plotPercent("", slowProcess.getBatteryLevel()));
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        return inventory.take(entityPlayer.getCurrentEquippedItem(), this, false, true);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(powered);
            stream.writeBoolean(firePresent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return descriptor.batteryPowered;
    }

    @Override
    public IInventory getInventory() {
        return inventory.getInventory();
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        needPublish();
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalFireDetectorContainer(player, inventory.getInventory());
    }
}
