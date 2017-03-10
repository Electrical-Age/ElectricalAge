package mods.eln.sixnode.electricalwatch;

import mods.eln.i18n.I18N;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.IPublishable;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalWatchElement extends SixNodeElement {

    ElectricalWatchDescriptor descriptor;

    public ElectricalWatchSlowProcess slowProcess = new ElectricalWatchSlowProcess(this);

    private AutoAcceptInventoryProxy inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
        .acceptIfEmpty(0, BatteryItem.class);

    public ElectricalWatchElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        slowProcessList.add(slowProcess);
        this.descriptor = (ElectricalWatchDescriptor) descriptor;
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
        return 0;
    }

    @Override
    public String multiMeterString() {
        return "";
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Battery level"), Utils.plotPercent("", slowProcess.getBatteryLevel()));
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
        return inventory.take(entityPlayer.getCurrentEquippedItem(), (IPublishable) this);
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public IInventory getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalWatchContainer(player, inventory.getInventory());
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        needPublish();
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(slowProcess.upToDate);
            stream.writeLong(slowProcess.oldDate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
