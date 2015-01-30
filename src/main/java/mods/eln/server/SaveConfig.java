package mods.eln.server;

import mods.eln.Eln;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class SaveConfig extends WorldSavedData {

	public static SaveConfig instance;
	
	public boolean heatFurnaceFuel = true;
	public boolean electricalLampAging = true;
	public boolean batteryAging = true;
	public boolean infinitPortableBattery = false;

	public boolean reGenOre = false;

	public SaveConfig(String par1Str) {
		super(par1Str);
		instance = this;	
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		heatFurnaceFuel = nbt.getBoolean("heatFurnaceFuel");
		electricalLampAging = nbt.getBoolean("electricalLampAging");
		batteryAging = nbt.getBoolean("batteryAging");
		infinitPortableBattery = nbt.getBoolean("infinitPortableBattery");
		reGenOre = nbt.getBoolean("reGenOre");
		
		Eln.wind.readFromNBT(nbt, "wind");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("heatFurnaceFuel", heatFurnaceFuel);
		nbt.setBoolean("electricalLampAging", electricalLampAging);
		nbt.setBoolean("batteryAging", batteryAging);
		nbt.setBoolean("infinitPortableBattery", infinitPortableBattery);
		nbt.setBoolean("reGenOre", reGenOre);
		
		Eln.wind.writeToNBT(nbt, "wind");
	}

	@Override
	public boolean isDirty() {
		return true;
	}
}
