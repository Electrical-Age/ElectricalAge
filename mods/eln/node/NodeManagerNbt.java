package mods.eln.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class NodeManagerNbt extends WorldSavedData{
	public NodeManagerNbt(String par1Str) {	
		super(par1Str);		
	}

	@Override
    public boolean isDirty() {
        return true;
    }
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		NodeManager.instance.loadFromNbt(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		//NodeManager.instance.saveToNbt(nbt, Integer.MIN_VALUE);
	}
}
