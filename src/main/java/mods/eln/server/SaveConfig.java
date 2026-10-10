package mods.eln.server;

import net.minecraft.nbt.CompoundTag;

/**
 * Per-world EA settings (1.12: WorldSavedData "SaveConfig", toggled with /eln commands). Saved with the overworld's EA
 * file (ElnWorldSaves, key "config"). The wind state that 1.12 also kept here moves with the wind process.
 */
public class SaveConfig {

    public static SaveConfig instance = new SaveConfig();

    public boolean heatFurnaceFuel = true;
    public boolean electricalLampAging = true;
    public boolean batteryAging = true;
    public boolean infinitePortableBattery = false;
    public boolean reGenOre = false;
    public double cableRsFactor_lastUsed = 1.0;

    public void readFromNBT(CompoundTag nbt) {
        heatFurnaceFuel = nbt.getBooleanOr("heatFurnaceFuel", true);
        electricalLampAging = nbt.getBooleanOr("electricalLampAging", true);
        batteryAging = nbt.getBooleanOr("batteryAging", true);
        infinitePortableBattery = nbt.getBooleanOr("infinitPortableBattery", false);
        reGenOre = nbt.getBooleanOr("reGenOre", false);
        cableRsFactor_lastUsed = nbt.getDoubleOr("cableRsFactor_lastUsed", 1.0);
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("heatFurnaceFuel", heatFurnaceFuel);
        nbt.putBoolean("electricalLampAging", electricalLampAging);
        nbt.putBoolean("batteryAging", batteryAging);
        nbt.putBoolean("infinitPortableBattery", infinitePortableBattery);
        nbt.putBoolean("reGenOre", reGenOre);
        nbt.putDouble("cableRsFactor_lastUsed", cableRsFactor_lastUsed);
        return nbt;
    }
}
