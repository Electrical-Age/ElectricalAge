package mods.eln.sixnode.electricalwatch;


import mods.eln.misc.Utils;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class ElectricalWatchSlowProcess implements IProcess, INBTTReady {

    ElectricalWatchElement element;

    boolean upToDate = false;
    long oldDate = 1379;

    public ElectricalWatchSlowProcess(ElectricalWatchElement element) {
        this.element = element;
    }

    double getBatteryLevel() {
        ItemStack batteryStack = element.getInventory().getItem(ElectricalWatchContainer.batteryId);
        BatteryItem battery = (BatteryItem) BatteryItem.getDescriptor(batteryStack, BatteryItem.class);
        if (battery != null) {
            return battery.getEnergy(batteryStack) / battery.getEnergyMax(batteryStack);
        } else {
            return 0;
        }
    }

    @Override
    public void process(double time) {
        ItemStack batteryStack = element.getInventory().getItem(ElectricalWatchContainer.batteryId);
        BatteryItem battery = (BatteryItem) BatteryItem.getDescriptor(batteryStack, BatteryItem.class);
        double energy;
        if (battery == null || (energy = battery.getEnergy(batteryStack)) < element.descriptor.powerConsumtion * time * 4) {
            if (upToDate) {
                upToDate = false;
                oldDate = element.sixNode.coordonate.world().getOverworldClockTime();
                if (!Utils.isEmpty(batteryStack)) battery.setEnergy(batteryStack, 0);
                element.needPublish();
            }
        } else {
            if (!upToDate) {
                upToDate = true;
                element.needPublish();
            }
            battery.setEnergy(batteryStack, energy - element.descriptor.powerConsumtion * time);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        upToDate = nbt.getBooleanOr(str + "upToDate", false);
        oldDate = nbt.getLongOr(str + "oldDate", 0L);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putBoolean(str + "upToDate", upToDate);
        nbt.putLong(str + "oldDate", oldDate);
    }
}
