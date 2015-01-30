package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.node.NodeManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class ElnWorldStorage extends WorldSavedData {

	private int dim;

    final static String key = "eln.worldStorage";

	public ElnWorldStorage(String str) {
		super(str);
	}

	public static ElnWorldStorage forWorld(World world) {
		// Retrieves the MyWorldData instance for the given world, creating it if necessary
		MapStorage storage = world.perWorldStorage;
		int dim = world.provider.dimensionId;
		ElnWorldStorage result = (ElnWorldStorage) storage.loadData(ElnWorldStorage.class, key + dim);

		if (result == null) {
			result = new ElnWorldStorage(key + dim);
			result.dim = dim;
			storage.setData(key + dim, result);
		}
		return result;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		dim = nbt.getInteger("dim");
		try {
			NodeManager.instance.loadFromNbt(nbt.getCompoundTag("nodes"));
		} catch (Exception e) {
		}
		try {
			Eln.ghostManager.loadFromNBT(nbt.getCompoundTag("ghost"));
		} catch (Exception e) {
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("dim", dim);
		try {
			NodeManager.instance.saveToNbt(Utils.newNbtTagCompund(nbt, "nodes"), dim);
		} catch (Exception e) {
		}
		try {
			Eln.ghostManager.saveToNBT(Utils.newNbtTagCompund(nbt, "ghost"), dim);
		} catch (Exception e) {
		}

	}

	@Override
	public boolean isDirty() {
		return true;
	}
}
