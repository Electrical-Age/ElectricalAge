package mods.eln.ghost;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

public class GhostManagerNbt extends WorldSavedData
{
	public GhostManagerNbt(String par1Str) {
		super(par1Str);
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		Eln.ghostManager.loadFromNBT(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		//Eln.ghostManager.saveToNbt(nbt, Integer.MIN_VALUE);
	}
}
